package com.gtlsystems.acs_api.service.mode

import com.gtlsystems.acs_api.algorithm.axislimitangle.LimitAngleCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.algorithm.satellitetracker.processor.SatelliteTrackingProcessor
import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.event.subscribeToType
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.icd.ICDService
import com.gtlsystems.acs_api.service.udp.UdpFwICDService
import com.gtlsystems.acs_api.service.system.settings.SettingsService
import com.gtlsystems.acs_api.config.ThreadManager
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.core.Disposable
import java.io.IOException
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import io.netty.handler.timeout.TimeoutException
import java.util.BitSet
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import com.gtlsystems.acs_api.service.mode.passSchedule.PassScheduleTLECache
import com.gtlsystems.acs_api.service.mode.passSchedule.PassScheduleDataRepository

/**
 * TLE 데이터를 캐시로 관리하고 위성 패스 스케줄링을 담당하는 서비스
 * 
 * 주요 기능:
 * - 100ms 정밀 타이머로 추적 스케줄 모니터링
 * - 상태 머신 패턴으로 추적 상태 관리
 * - 2분 기준 Stow/시작 위치 자동 이동
 * - 성능 최적화된 캐시 시스템
 */
@Service
class PassScheduleService(
    private val orekitCalculator: OrekitCalculator,
    private val satelliteTrackingProcessor: SatelliteTrackingProcessor, // ✅ 추가
    private val acsEventBus: ACSEventBus,
    private val udpFwICDService: UdpFwICDService,
    private val dataStoreService: DataStoreService,
    private val settingsService: SettingsService,
    private val threadManager: ThreadManager,
    private val passScheduleTLECache: PassScheduleTLECache, // ✅ Phase 5: TLE 캐시 분리
    private val passScheduleDataRepository: PassScheduleDataRepository // ✅ Phase 5: 데이터 저장소 분리
) {
    private val logger = LoggerFactory.getLogger(PassScheduleService::class.java)

    // ===== 추적 상태 관리 (개선된 방식) =====
    
    /**
     * 추적 상태를 정의하는 열거형 (사용자 요구사항에 맞게 단순화)
     * 
     * 핵심: 스케줄 판단만 하고, 실제 추적은 사용자가 제어
     */
    enum class TrackingState {
        /** 초기 대기 상태 - 추적 모니터링 시작 전 */
        IDLE,
        
        /** 2분 이상 남음 - Stow 위치에서 대기 */
        WAITING,
        
        /** 2분 이내 - 시작 위치로 이동 */
        PREPARING,
        
        /** 실제 추적 중 - 사용자가 시작한 후 */
        TRACKING,
        
        /** 개별 스케줄 완료 - Stow 위치로 이동 */
        COMPLETED
    }

    /**
     * PREPARING 상태 내에서의 진행 단계를 정의하는 열거형
     * 
     * PREPARING 상태 내에서 Train 회전 → 안정화 대기 → Az/El 이동을 순차적으로 처리하기 위한 내부 플래그
     */
    private enum class PreparingStep {
        /** 초기화 */
        INIT,

        /** Train 회전 중 */
        MOVING_TRAIN,

        /** Train 안정화 대기 */
        WAITING_TRAIN,

        /** Az/El 이동 중 */
        MOVING_AZ_EL
    }

    // ===== 신규 상태 머신 (v2.0 - 통합 상태) =====

    /**
     * PassSchedule 통합 상태 머신 (v2.0)
     *
     * 모든 상태를 단일 열거형으로 관리하여 이중 상태 문제 해결.
     * 시간 기반 상태 결정 (calTime 우선)으로 Time Offset 지원.
     *
     * @see DESIGN.md PassSchedule 상태 머신 재설계 상세 설계서
     */
    enum class PassScheduleState {
        // ===== 초기 상태 =====
        /** 시작 전 대기 상태 */
        IDLE,

        // ===== 대기 상태 (2분 이상 남음) =====
        /** Stow 위치로 이동 중 */
        STOWING,
        /** Stow 위치 도달, 대기 중 */
        STOWED,

        // ===== 준비 상태 (2분 이내) =====
        /** Train 각도 이동 중 (키홀 대응) */
        MOVING_TRAIN,
        /** Train 안정화 대기 중 (3초) */
        TRAIN_STABILIZING,
        /** 시작 위치(Az/El)로 이동 중 */
        MOVING_TO_START,
        /** 시작 위치 도달, 시작 시간 대기 */
        READY,

        // ===== 추적 상태 =====
        /** 실시간 위성 추적 중 */
        TRACKING,

        // ===== 종료 상태 =====
        /** 추적 종료, 다음 스케줄 평가 중 */
        POST_TRACKING,
        /** 모든 스케줄 완료 */
        COMPLETED,

        // ===== 오류 상태 =====
        /** 오류 발생 */
        ERROR
    }

    /**
     * 개별 스케줄 추적 컨텍스트 (v2.0)
     *
     * 각 스케줄에 대한 일회성 플래그와 상태 정보를 관리.
     * 스케줄 전환 시 resetFlags()로 플래그 초기화 필요.
     *
     * ⚠️ 시간 타입: ZonedDateTime (GlobalData.Time.calUtcTimeOffsetTime과 동일)
     */
    data class ScheduleTrackingContext(
        // ===== 스케줄 식별 =====
        val mstId: Long,
        val detailId: Int,
        val satelliteName: String,

        // ===== 시간 정보 (ZonedDateTime - 스케줄 고정값) =====
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,

        // ===== 시작 위치 정보 =====
        val startAzimuth: Float,    // 시작 방위각 (radians)
        val startElevation: Float,  // 시작 고도각 (radians)
        val trainAngle: Float,      // Train 각도 (radians)

        // ===== 일회성 명령 플래그 (한 번만 전송 보장) =====
        var stowCommandSent: Boolean = false,
        var trainMoveCommandSent: Boolean = false,
        var azElMoveCommandSent: Boolean = false,
        var headerSent: Boolean = false,
        var initialTrackingDataSent: Boolean = false,

        // ===== 진행 완료 플래그 (상태 결정에 사용) =====
        var trainMoveCompleted: Boolean = false,
        var trainStabilizationCompleted: Boolean = false,
        var azElMoveCompleted: Boolean = false,

        // ===== 타이밍 정보 (ZonedDateTime - 진행 중 기록) =====
        var trainStabilizationStartTime: ZonedDateTime? = null,
        var stateEntryTime: ZonedDateTime? = null
    ) {
        /**
         * 플래그 리셋 함수
         *
         * 스케줄 전환 시 모든 일회성/진행 플래그를 초기화
         */
        fun resetFlags(): ScheduleTrackingContext {
            return this.copy(
                stowCommandSent = false,
                trainMoveCommandSent = false,
                azElMoveCommandSent = false,
                headerSent = false,
                initialTrackingDataSent = false,
                trainMoveCompleted = false,
                trainStabilizationCompleted = false,
                azElMoveCompleted = false,
                trainStabilizationStartTime = null,
                stateEntryTime = null
            )
        }
    }

    // ===== 신규 상태 머신 변수 (v2.0) =====

    /** 현재 상태 (v2.0) */
    private var currentPassScheduleState: PassScheduleState = PassScheduleState.IDLE

    /** 이전 상태 (v2.0) */
    private var previousPassScheduleState: PassScheduleState = PassScheduleState.IDLE

    /** 현재 스케줄 컨텍스트 (v2.0) */
    private var currentScheduleContext: ScheduleTrackingContext? = null

    /** 다음 스케줄 컨텍스트 (v2.0) */
    private var nextScheduleContext: ScheduleTrackingContext? = null

    /** 스케줄 큐 (v2.0) */
    private val scheduleContextQueue = mutableListOf<ScheduleTrackingContext>()

    /** 타이머 카운트 (v2.0) */
    private var v2CheckCount: Long = 0L

    /** 종료 중 플래그 (v2.0) */
    private var isV2ShuttingDown: Boolean = false

    /** v2.0 상태 머신 활성화 플래그 (기본 활성화) */
    private var useV2StateMachine: Boolean = true

    // ===== 신규 상수 (v2.0) =====
    companion object {
        /** 준비 시간 (2분) */
        const val V2_PREPARATION_TIME_MS = 2 * 60 * 1000L
        /** Train 안정화 시간 (3초) */
        const val V2_TRAIN_STABILIZATION_MS = 3000L
        /** 위치 허용 오차 (~0.057도) */
        const val V2_POSITION_TOLERANCE_RAD = 0.001f
        /** 타이머 주기 (100ms) */
        const val V2_CHECK_INTERVAL_MS = 100L
    }

    // ===== 기존 상태 관리 (v1.0 - 호환성 유지) =====

    /**
     * 현재 추적 상태
     * 상태 변경 시에만 실제 액션을 실행하여 중복 실행을 방지합니다.
     */
    private var currentTrackingState = TrackingState.IDLE

    /**
     * PREPARING 상태 내에서의 진행 단계
     */
    private var currentPreparingStep = PreparingStep.INIT

    /**
     * PREPARING 상태에서 처리 중인 전역 고유 패스 ID (Long 타입)
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 UInt → Long으로 변경.
     * 전역 고유 MstId를 저장하기 위해 Long 타입 사용.
     */
    private var preparingPassId: Long? = null

    /**
     * 목표 Azimuth 각도
     */
    private var targetAzimuth: Float = 0f

    /**
     * 목표 Elevation 각도
     */
    private var targetElevation: Float = 0f

    /**
     * Train 안정화 대기 시작 시간
     */
    private var trainStabilizationStartTime: Long = 0

    /**
     * Train 안정화 대기 시간 (밀리초)
     */
    private val TRAIN_STABILIZATION_TIMEOUT = 3000L // 3초
    
    /**
     * 마지막 상태 변경 시간 (밀리초)
     * 상태 변경 간격을 제어하여 과도한 상태 전환을 방지합니다.
     */
    private var lastStateChangeTime = 0L

    /**
     * 상태 변경 최소 간격 (밀리초)
     * 너무 빈번한 상태 변경을 방지하기 위한 설정
     */
    private val MIN_STATE_CHANGE_INTERVAL = 500 // 0.5초

    // ===== 기존 저장소들 =====
    // ✅ Phase 5: TLE 캐시는 passScheduleTLECache로 분리됨
    // ✅ Phase 5: 데이터 저장소는 passScheduleDataRepository로 분리됨
    // 내부 저장소 접근자 (기존 코드 호환성 유지, 읽기 전용)
    private val passScheduleTrackMstStorage: ConcurrentHashMap<String, List<Map<String, Any?>>>
        get() = ConcurrentHashMap(passScheduleDataRepository.getAllMst())
    private val passScheduleTrackDtlStorage: ConcurrentHashMap<String, List<Map<String, Any?>>>
        get() = ConcurrentHashMap(passScheduleDataRepository.getAllDtl())
    private val trackingTargetList = mutableListOf<TrackingTarget>()
    private val selectedTrackMstStorage = ConcurrentHashMap<String, List<Map<String, Any?>>>()

    // ===== 기존 상태 관리 변수들 (Boolean 제거) =====
    private var lastPreparedSchedule: Map<String, Any?>? = null
    private val PREPARATION_TIME_MINUTES = 2L
    private val subscriptions: MutableList<Disposable> = mutableListOf()

    // ✅ 새로 추가: 성능 최적화용 캐시 및 스레드 (기존 동작에 영향 없음)
    /**
     * 추적 데이터 캐시 (Long 타입으로 변경)
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 UInt → Long으로 변경.
     * 전역 고유 MstId를 키로 사용.
     */
    private val trackingDataCache = ConcurrentHashMap<Long, TrackingDataCache>()
    // ✅ ThreadManager 통합 사용 (LOW 우선순위)
    private val batchExecutor = threadManager.getBatchExecutor()

    // ✅ 새로 추가: 성능 최적화용 데이터 클래스
    /**
     * 추적 데이터 캐시 데이터 클래스
     * 
     * @param passId 전역 고유 MstId (Long 타입)
     * @param trackingPoints 추적 포인트 배열
     * @param totalSize 전체 크기
     * @param createdAt 생성 시간 (밀리초)
     */
    data class TrackingDataCache(
        val passId: Long,  // ✅ UInt → Long 변경
        val trackingPoints: Array<TrackingPoint>,
        val totalSize: Int,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        data class TrackingPoint(
            val timeMs: Int,
            val elevation: Float,
            val azimuth: Float
        )

        fun isExpired(maxAgeMs: Long = 3600000L): Boolean = // 1시간 후 만료 (기본값)
            System.currentTimeMillis() - createdAt > maxAgeMs
    }

    // ✅ 기존 데이터 클래스들 (타입 변경)
    /**
     * 추적 대상 데이터 클래스
     * 
     * @param mstId 전역 고유 MstId (Long 타입으로 변경)
     * @param satelliteId 위성 카탈로그 번호
     * @param satelliteName 위성 이름
     * @param startTime 추적 시작 시간
     * @param endTime 추적 종료 시간
     * @param maxElevation 최대 고도
     * @param createdAt 생성 시간
     */
    data class TrackingTarget(
        val mstId: Long,  // ✅ UInt → Long 변경
        val satelliteId: String,
        val satelliteName: String? = null,
        val startTime: ZonedDateTime,
        val endTime: ZonedDateTime,
        val maxElevation: Double,
        val createdAt: ZonedDateTime = ZonedDateTime.now()
    )

    // ✅ 기존 설정들 (변경 없음)
    private val locationData = settingsService.locationData
    private val limitAngleCalculator = LimitAngleCalculator()
    
    /**
     * ✅ 전역 고유 MstId 카운터 (AtomicLong)
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 추가됨.
     * 모든 위성의 MST에 대해 전역적으로 고유한 ID를 생성하기 위해 사용.
     * 위성별 인덱스가 아닌 전역 넘버링을 보장.
     * 
     * 초기값: 0 (첫 번째 MST는 1부터 시작)
     * 
     * @see generateAllPassScheduleTrackingDataAsync 전체 위성 스케줄 생성 시 초기화
     * @see generatePassScheduleTrackingDataAsync 개별 위성 스케줄 생성 시 사용
     */
    private val mstIdCounter = AtomicLong(0)

    @PostConstruct
    fun init() {
        logger.info("PassScheduleService 초기화 완료 (상태 머신 패턴 적용)")
        setupEventSubscriptions()
    }

    /**
     * 이벤트 구독 설정
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 "No" 필드 → "MstId" 필드로 변경.
     * 타입 캐스팅도 UInt → Long으로 변경.
     */
    private fun setupEventSubscriptions() {
        // ✅ 기존 이벤트 구독 로직 유지 (필드명 및 타입 변경)
        val headerSubscription =
            acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackHeaderReceived>().subscribe(
                { event ->
                    val currentSchedule = getCurrentSelectedTrackingPassWithTime(GlobalData.Time.calUtcTimeOffsetTime)
                    currentSchedule?.let { schedule ->
                        // ✅ "No" → "MstId" 변경, UInt → Long 변경
                        val passId = (schedule["MstId"] as? Number)?.toLong()
                        if (passId != null) {
                            sendInitialTrackingData(passId)
                        }
                    }
                },
                { error -> logger.error("위성 추적 헤더 이벤트 처리 중 오류: {}", error.message, error) }
            )

        val dataRequestSubscription =
            acsEventBus.subscribeToType<ACSEvent.ICDEvent.SatelliteTrackDataRequested>().subscribe(
                { event ->
                    val currentSchedule = getCurrentSelectedTrackingPassWithTime(GlobalData.Time.calUtcTimeOffsetTime)
                    currentSchedule?.let { schedule ->
                        // ✅ "No" → "MstId" 변경, UInt → Long 변경
                        val passId = (schedule["MstId"] as? Number)?.toLong()
                        if (passId != null) {
                            val requestData = event.requestData as ICDService.SatelliteTrackThree.GetDataFrame
                            // ✅ 최적화된 메서드 호출 (기존 인터페이스 유지)
                            handleTrackingDataRequest(passId, requestData.timeAcc, requestData.requestDataLength)
                        }
                    }
                },
                { error -> logger.error("위성 추적 데이터 요청 이벤트 처리 중 오류: {}", error.message, error) }
            )

        subscriptions.add(headerSubscription)
        subscriptions.add(dataRequestSubscription)
    }

    // ✅ 기존 추적 모니터링 필드들 (변경 없음)
    private var trackingExecutor: ScheduledExecutorService? = null
    private var trackingMonitorTask: ScheduledFuture<*>? = null
    private var isTrackingMonitorRunning = AtomicBoolean(false)
    private var lastDisplayedSchedule: Map<String, Any?>? = null
    private var trackingCheckCount = 0L  // Long 타입으로 변경

    private val trackingMonitorThreadFactory = ThreadFactory { runnable ->
        Thread(runnable, "tracking-monitor").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY + 1
        }
    }

    /**
     * 추적 모니터링을 시작하는 함수
     * 
     * 100ms 정밀 타이머로 스케줄을 모니터링하고 상태 머신을 통해
     * 적절한 추적 상태로 전환합니다.
     */
    fun startScheduleTracking() {
        logger.info("═══════════════════════════════════════════════════════════════")
        logger.info("🚀 [STEP-1] startScheduleTracking() 호출됨")

        if (isTrackingMonitorRunning.get()) {
            logger.warn("[TRACKING] 추적 모니터링이 이미 실행 중입니다.")
            return
        }

        // 🔧 DEBUG: 데이터 상태 확인
        val targetCount = synchronized(trackingTargetList) { trackingTargetList.size }
        val selectedStorageSize = selectedTrackMstStorage.size
        val selectedTotalPasses = selectedTrackMstStorage.values.sumOf { it.size }
        logger.info("📊 [STEP-1] 데이터 상태 확인:")
        logger.info("   - trackingTargetList: ${targetCount}개")
        logger.info("   - selectedTrackMstStorage: ${selectedStorageSize}개 위성, ${selectedTotalPasses}개 패스")

        if (targetCount == 0) {
            logger.error("❌ [STEP-1] trackingTargetList가 비어있음! 스케줄 선택이 필요합니다.")
        }
        if (selectedTotalPasses == 0) {
            logger.error("❌ [STEP-1] selectedTrackMstStorage가 비어있음! generateSelectedTrackingData() 호출 필요.")
        }

        // 기존 추적 중지 및 상태 초기화
        dataStoreService.stopAllTracking()
        resetTrackingState()

        // ✅ 통합 추적 실행기 사용 (NORMAL 우선순위)
        trackingExecutor = threadManager.getTrackingExecutor()
        trackingMonitorTask = trackingExecutor?.scheduleAtFixedRate(
            { checkTrackingScheduleWithStateMachine() }, 0, 100, TimeUnit.MILLISECONDS
        )

        isTrackingMonitorRunning.set(true)
        logger.info("✅ [STEP-1] 추적 모니터링 시작 완료 (100ms 주기)")
        logger.info("═══════════════════════════════════════════════════════════════")
    }
    /**
     * 추적 모니터링을 중지하는 함수
     * 
     * 모든 리소스를 정리하고 안전하게 종료합니다.
     */
    fun stopScheduleTracking() {
        if (!isTrackingMonitorRunning.get()) {
            return
        }

        isTrackingMonitorRunning.set(false)
        trackingMonitorTask?.cancel(false)
        
        // ✅ ThreadManager의 trackingExecutor는 ThreadManager에서 관리되므로
        // 여기서는 태스크만 취소하고 상태만 정리

        // 현재 상태에 따라 적절한 종료 액션 수행
        handleShutdownAction()

        // 리소스 정리
        trackingDataCache.clear()
        trackingExecutor = null
        trackingMonitorTask = null
        lastDisplayedSchedule = null
        lastPreparedSchedule = null

        logger.info("[TRACKING] 추적 모니터링 중지 완료 (상태 머신 정리됨)")
        dataStoreService.clearTrackingMstIds()
    }

    /**
     * 상태 머신 기반 추적 스케줄 체크 함수 (수정됨)
     *
     * V2 상태 머신이 활성화된 경우 V2 로직을 실행합니다.
     */
    private fun checkTrackingScheduleWithStateMachine() {
        // ═══ V2.0 상태 머신 분기 ═══
        if (useV2StateMachine) {
            checkV2StateMachine()
            return
        }

        // ═══ 기존 V1.0 로직 ═══
        try {
            val calTime = GlobalData.Time.calUtcTimeOffsetTime
            val currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)
            val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

            // 디버깅 로그 (처음 20회만 상세 출력)
            if (trackingCheckCount < 20) {
                logCurrentStatus(calTime, currentSchedule, nextSchedule)
            }

            // 🔧 DEBUG: 10초마다 상태 요약 출력 (100회 = 10초)
            if (trackingCheckCount % 100L == 0L) {
                val nextMstId = (nextSchedule?.get("MstId") as? Number)?.toLong()
                val nextStartTime = nextSchedule?.get("StartTime")
                val isWithin2Min = if (nextSchedule != null) isWithinPreparationTime(nextSchedule, calTime) else false
                logger.info("🔄 [DEBUG] 상태요약: state=$currentTrackingState, nextMstId=$nextMstId, nextStart=$nextStartTime, within2min=$isWithin2Min, calTime=$calTime")
            }

            trackingCheckCount++

            // ✅ 상태 머신을 통한 상태 결정 및 전환
            val newState = determineTrackingState(currentSchedule, nextSchedule, calTime)
            transitionToState(newState, currentSchedule, nextSchedule, calTime)
            
            // ✅ 별도로 추적 상태 변경 처리 (기존 로직과 분리)
            handleTrackingStateChangeSeparately(currentSchedule, calTime)

        } catch (e: Exception) {
            logger.error("추적 체크 중 오류: ${e.message}", e)
        }
    }

    /**
     * 현재 상황을 분석하여 적절한 추적 상태를 결정하는 함수 (사용자 요구사항에 맞게 개선)
     * 
     * @param currentSchedule 현재 추적 중인 스케줄
     * @param nextSchedule 다음 추적 예정 스케줄
     * @param calTime 현재 계산된 시간
     * @return 결정된 추적 상태
     */
    private fun determineTrackingState(
        currentSchedule: Map<String, Any?>?,
        nextSchedule: Map<String, Any?>?,
        calTime: ZonedDateTime
    ): TrackingState {
        
        return when {
            // 현재 추적 중인 경우
            currentSchedule != null -> {
                logger.debug("[STATE] 현재 추적 중 - TRACKING 상태")
                TrackingState.TRACKING
            }
            
            // 다음 스케줄이 2분 이내인 경우 (추적 준비)
            nextSchedule != null && isWithinPreparationTime(nextSchedule, calTime) -> {
                logger.debug("[STATE] 2분 이내 - PREPARING 상태")
                TrackingState.PREPARING
            }
            
            // 다음 스케줄이 2분 이상 남은 경우 (대기)
            nextSchedule != null -> {
                logger.debug("[STATE] 2분 이상 남음 - WAITING 상태 (Stow 위치)")
                TrackingState.WAITING
            }
            
            // 개별 스케줄 완료
            else -> {
                logger.debug("[STATE] 개별 스케줄 완료 - COMPLETED 상태")
                TrackingState.COMPLETED
            }
        }
    }

    /**
     * 결정된 상태로 전환하는 함수 (수정됨)
     */
    private fun transitionToState(
        newState: TrackingState,
        currentSchedule: Map<String, Any?>?,
        nextSchedule: Map<String, Any?>?,
        calTime: ZonedDateTime
    ) {
        // ✅ PREPARING 상태는 내부 단계 처리를 위해 항상 액션 실행
        if (newState == TrackingState.PREPARING && currentTrackingState == TrackingState.PREPARING) {
            // PREPARING 상태 유지 중 - 내부 PreparingStep 단계 처리 계속
            executeStateAction(newState, currentSchedule, nextSchedule, calTime)
            return
        }

        // 상태가 변경되지 않았거나 최소 간격이 지나지 않은 경우 액션 실행하지 않음
        if (currentTrackingState == newState) {
            // 🔧 DEBUG: 상태 동일로 스킵 (10초마다 출력)
            if (trackingCheckCount % 100L == 0L) {
                logger.info("🔄 [DEBUG] 상태 동일로 스킵: current=$currentTrackingState, new=$newState")
            }
            return
        }
        if (!canChangeState()) {
            logger.info("⏳ [DEBUG] 상태 변경 간격 미달로 스킵: current=$currentTrackingState, new=$newState")
            return
        }

        val oldState = currentTrackingState
        currentTrackingState = newState
        lastStateChangeTime = System.currentTimeMillis()

        // ✅ PushData에 passScheduleTrackingState 업데이트 (프론트엔드로 상태 전송)
        PushData.TRACKING_STATUS.passScheduleTrackingState = newState.name
        logger.info("[STATE] 상태 전환: $oldState -> $newState (passScheduleTrackingState=${newState.name})")

        // ✅ 상태별 액션만 실행 (중복 제거)
        executeStateAction(newState, currentSchedule, nextSchedule, calTime)

        // ✅ handleTrackingStateChange는 별도로 호출하지 않음
        // (상태 머신이 모든 액션을 관리하므로)
    }

    /**
     * 상태 변경 가능 여부를 확인하는 함수
     * 
     * @return true: 상태 변경 가능, false: 아직 최소 간격이 지나지 않음
     */
    private fun canChangeState(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastChange = currentTime - lastStateChangeTime
        return timeSinceLastChange >= MIN_STATE_CHANGE_INTERVAL
    }

    /**
     * 상태별 액션을 실행하는 함수 (사용자 요구사항에 맞게 단순화)
     * 
     * 핵심: 스케줄 판단만 하고, 실제 추적 시작/종료는 사용자가 제어
     */
    private fun executeStateAction(
        state: TrackingState,
        currentSchedule: Map<String, Any?>?,
        nextSchedule: Map<String, Any?>?,
        calTime: ZonedDateTime
    ) {
        when (state) {
            TrackingState.TRACKING -> {
                // ✅ 현재 스케줄 추적 중 - 추적 시작 처리
                if (currentSchedule != null) {
                    val satelliteName = currentSchedule["SatelliteName"] as? String ?: "Unknown"
                    // ✅ "No" → "MstId" 변경, UInt → Long 변경
                    val mstId = (currentSchedule["MstId"] as? Number)?.toLong()
                    logger.info("[ACTION] TRACKING 상태 - 추적 중: $satelliteName (ID: $mstId)")

                    // ✅ 이전 추적의 tracking 각도 값 초기화 (TRACKING 전환 시 이전 값으로 점프 방지)
                    dataStoreService.clearTrackingAngles()

                    // ✅ 추적 시작 처리 (캐시 로딩 + 헤더 전송)
                    prepareTrackingStart(mstId)
                }
            }
            
            TrackingState.WAITING -> {
                // ✅ 대기 상태 - Stow 위치로 이동 (2분 이상 남음)
                logger.info("[ACTION] WAITING 상태 - Stow 위치로 이동 (2분 이상 남음)")
                moveToStowPosition(calTime)
            }
            
            TrackingState.PREPARING -> {
                // ✅ PREPARING 상태 내에서 단계별 처리
                // ✅ "No" → "MstId" 변경, UInt → Long 변경
                val nextMstId = (nextSchedule?.get("MstId") as? Number)?.toLong()

                // 🔧 DEBUG: PREPARING 상태 진입 로그
                logger.info("🔧 [STEP-4] executeStateAction(PREPARING) - currentPreparingStep=$currentPreparingStep, nextMstId=$nextMstId")

                when (currentPreparingStep) {
                    PreparingStep.INIT -> {
                        // 초기화: moveToStartPosition() 호출
                        if (nextMstId != null) {
                            logger.info("✅ [STEP-4] PREPARING/INIT - 시작 위치로 이동 명령 (nextMstId=$nextMstId)")
                            moveToStartPosition(nextMstId)
                        } else {
                            logger.error("❌ [STEP-4] PREPARING/INIT - nextMstId가 null! nextSchedule=$nextSchedule")
                        }
                    }
                    
                    PreparingStep.MOVING_TRAIN -> {
                        // Train 회전 중
                        preparingPassId?.let { passId ->
                            val selectedPass = getTrackingPassMst(passId)
                            val isKeyhole = selectedPass?.get("IsKeyhole") as? Boolean ?: false
                            val recommendedTrainAngle = selectedPass?.get("RecommendedTrainAngle") as? Double ?: 0.0
                            
                            val trainAngle = if (isKeyhole) {
                                recommendedTrainAngle.toFloat()
                            } else {
                                0f
                            }
                            
                            // Train 각도 이동 명령 전송 (한 번만)
                            moveTrainToZero(trainAngle)
                            
                            // Train 각도 도달 확인
                            if (isTrainAtZero()) {
                                currentPreparingStep = PreparingStep.WAITING_TRAIN
                                trainStabilizationStartTime = System.currentTimeMillis()
                                logger.info("✅ Train가 ${trainAngle}도에 도달, 안정화 대기 시작")
                            }
                        }
                    }
                    
                    PreparingStep.WAITING_TRAIN -> {
                        // Train 안정화 대기
                        if (System.currentTimeMillis() - trainStabilizationStartTime >= TRAIN_STABILIZATION_TIMEOUT && isTrainStabilized()) {
                            moveToTargetAzEl()
                            currentPreparingStep = PreparingStep.MOVING_AZ_EL
                            logger.info("✅ Train 안정화 완료, 목표 Az/El로 이동 시작")
                        }
                    }
                    
                    PreparingStep.MOVING_AZ_EL -> {
                        // Az/El 이동 완료 (목표 위치 도달 체크는 생략, 즉시 완료)
                        currentPreparingStep = PreparingStep.INIT
                        preparingPassId = null
                        logger.info("✅ 목표 위치 이동 완료")
                    }
                }
            }
            
            TrackingState.COMPLETED -> {
                // ✅ 완료 상태 - Stow 위치로 이동 (추적 완료)
                logger.info("[ACTION] COMPLETED 상태 - 개별 스케줄 완료, Stow 위치로 이동")
                
                // ✅ 이전 추적 종료 처리
                lastDisplayedSchedule?.let { completedSchedule ->
                    // ✅ "No" → "MstId" 변경, UInt → Long 변경
                    val completedMstId = (completedSchedule["MstId"] as? Number)?.toLong()
                    if (completedMstId != null) {
                        cleanupTrackingEnd(completedMstId, completedSchedule)
                    }
                }
                
                // Stow 위치로 이동
                moveToStowPosition(calTime)
            }
            
            TrackingState.IDLE -> {
                // ✅ 대기 상태 - 특별한 액션 없음
                logger.debug("[ACTION] IDLE 상태 - 액션 없음")
            }
        }
    }

    /**
     * 다음 스케줄이 2분 이내인지 확인하는 함수
     * 
     * @param nextSchedule 다음 스케줄
     * @param calTime 현재 시간
     * @return true: 2분 이내, false: 2분 이상
     */
    private fun isWithinPreparationTime(nextSchedule: Map<String, Any?>?, calTime: ZonedDateTime): Boolean {
        val nextStartTime = nextSchedule?.get("StartTime") as? ZonedDateTime ?: return false
        val timeUntilNext = Duration.between(calTime, nextStartTime)

        // ✅ 전체 초 단위로 계산 (초 단위 버림 방지)
        val totalSecondsUntilNext = timeUntilNext.seconds
        val minutesUntilNext = totalSecondsUntilNext / 60
        val secondsUntilNext = totalSecondsUntilNext % 60

        // ✅ 2분 = 120초로 정확히 계산
        val preparationTimeSeconds = PREPARATION_TIME_MINUTES * 60 // 2분 = 120초

        val result = totalSecondsUntilNext <= preparationTimeSeconds && totalSecondsUntilNext >= 0

        // 🔧 DEBUG: 5초마다 또는 상태 변경 시점에 로그 출력
        val shouldLog = trackingCheckCount % 50L == 0L
        if (shouldLog) {
            logger.info("⏱️ [STEP-3] isWithinPreparationTime 체크:")
            logger.info("   - 다음 스케줄까지: ${minutesUntilNext}분 ${secondsUntilNext}초 (총 ${totalSecondsUntilNext}초)")
            logger.info("   - 임계값: ${preparationTimeSeconds}초 (2분)")
            logger.info("   - 2분 이내 여부: $result")
        }

        // 🔧 DEBUG: 상태 변경 시점 (WAITING → PREPARING)에는 항상 로그
        if (result && currentTrackingState == TrackingState.WAITING) {
            logger.info("🎉 [STEP-3] 2분 이내 진입! WAITING → PREPARING 전환 예정")
            logger.info("   - 남은 시간: ${minutesUntilNext}분 ${secondsUntilNext}초")
        }

        return result
    }

    /**
     * 종료 시 적절한 액션을 수행하는 함수
     * 
     * 사용자 요구사항: 사용자가 직접 Stow 버튼을 누르므로 자동 Stow 명령 제거
     */
    private fun handleShutdownAction() {
        logger.info("[SHUTDOWN] 추적 모니터링 종료 - 사용자가 직접 Stow 버튼을 눌러주세요.")
        // ✅ 사용자가 직접 제어하므로 자동 Stow 명령 제거
    }

    /**
     * 추적 상태를 초기화하는 함수
     * 
     * 새로운 추적 모니터링 시작 시 호출됩니다.
     */
    private fun resetTrackingState() {
        currentTrackingState = TrackingState.IDLE
        lastStateChangeTime = 0L
        trackingCheckCount = 0
        // ✅ PushData에 passScheduleTrackingState 초기화
        PushData.TRACKING_STATUS.passScheduleTrackingState = TrackingState.IDLE.name
        logger.debug("[STATE] 추적 상태 초기화 완료 (passScheduleTrackingState=IDLE)")
    }

    /**
     * 현재 상태를 로깅하는 함수 (최적화됨)
     * 
     * @param calTime 현재 시간
     * @param currentSchedule 현재 스케줄
     * @param nextSchedule 다음 스케줄
     */
    private fun logCurrentStatus(
        calTime: ZonedDateTime,
        currentSchedule: Map<String, Any?>?,
        nextSchedule: Map<String, Any?>?
    ) {
        // ✅ 최적화: 처음 1회만 상세 로그, 이후는 20초마다만 로그 (100ms * 200 = 20초)
        val shouldLogDetailed = trackingCheckCount < 1L || trackingCheckCount % 200L == 0L
        
        if (shouldLogDetailed) {
            logger.info("[STATUS] 추적 체크 #${trackingCheckCount}")
            logger.info("  현재시간: $calTime")
            logger.info("  현재상태: $currentTrackingState")
            logger.info("  현재 스케줄: ${if (currentSchedule != null) "있음" else "없음"}")

            if (nextSchedule != null) {
                val nextMstId = nextSchedule["No"] as? UInt
                val nextSatName = nextSchedule["SatelliteName"] as? String
                val nextStartTime = nextSchedule["StartTime"] as? ZonedDateTime

                logger.info("  다음 스케줄: MST=$nextMstId, Name=$nextSatName")
                logger.info("  시작시간: $nextStartTime")

                if (nextStartTime != null) {
                    val timeUntilNext = Duration.between(calTime, nextStartTime)
                    val minutesUntilNext = timeUntilNext.toMinutes()
                    val secondsUntilNext = (timeUntilNext.seconds % 60).toInt()
                    val hoursUntilNext = timeUntilNext.toHours()

                    logger.info("  남은시간: ${hoursUntilNext}시간 ${minutesUntilNext % 60}분 ${secondsUntilNext}초")
                }
            } else {
                logger.info("  다음 스케줄: 없음")
            }
        } else {
            // ✅ 간소화된 로그: 상태 변경이나 중요한 정보만
            if (nextSchedule != null) {
                val nextStartTime = nextSchedule["StartTime"] as? ZonedDateTime
                if (nextStartTime != null) {
                    val timeUntilNext = Duration.between(calTime, nextStartTime)
                    val minutesUntilNext = timeUntilNext.toMinutes()
                    val secondsUntilNext: Long = timeUntilNext.seconds % 60
                    
                    // ✅ 1분 단위로만 로그 출력 (중복 방지)
                    if (secondsUntilNext == 0L) {
                        logger.debug("[STATUS] 다음 스케줄까지: ${minutesUntilNext}분 남음")
                    }
                }
            }
        }
    }

    /**
     * 추적 상태 변경을 별도로 처리하는 함수 (새로 추가)
     * 
     * 상태 머신의 액션과 분리하여 기존 로직을 유지합니다.
     */
    private fun handleTrackingStateChangeSeparately(currentSchedule: Map<String, Any?>?, calTime: ZonedDateTime) {
        // ✅ mstId 업데이트만 수행
        updateTrackingMstIds(currentSchedule, calTime)

        // ✅ 로깅만 수행 (액션은 상태 머신이 담당)
        when {
            // 새로운 추적 시작 (로깅만)
            lastDisplayedSchedule == null && currentSchedule != null -> {
                outputCurrentScheduleInfo(currentSchedule, calTime)
                outputNextScheduleInfo(calTime)
            }

            // 추적 종료 (로깅만)
            lastDisplayedSchedule != null && currentSchedule == null -> {
                lastDisplayedSchedule?.let { lastSchedule ->
                    outputTrackingEnd(lastSchedule, calTime)

                    val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)
                    if (nextSchedule != null) {
                        outputUpcomingScheduleInfo(nextSchedule, calTime)
                    } else {
                        outputScheduleFixed(lastSchedule, calTime)
                    }
                }
            }

            // 추적 변경 (로깅만)
            lastDisplayedSchedule != null && currentSchedule != null &&
                    // ✅ "No" → "MstId" 변경
                    lastDisplayedSchedule?.get("MstId") != currentSchedule["MstId"] -> {
                lastDisplayedSchedule?.let { lastSchedule ->
                    outputScheduleChange(lastSchedule, currentSchedule, calTime)
                    outputNextScheduleInfo(calTime)
                }
            }
        }
        lastDisplayedSchedule = currentSchedule
    }

    /**
     * 추적 중인 mstId 업데이트 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 "No" 필드 → "MstId" 필드로 변경.
     * 타입도 UInt → Long으로 변경.
     * 
     * @param currentSchedule 현재 추적 중인 스케줄
     * @param calTime 현재 시간
     */
    private fun updateTrackingMstIds(currentSchedule: Map<String, Any?>?, calTime: ZonedDateTime) {
        // 현재 추적 중인 mstId와 detailId 업데이트
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val currentMstId = (currentSchedule?.get("MstId") as? Number)?.toLong()
        val currentDetailId = (currentSchedule?.get("DetailId") as? Number)?.toInt()
        
        // 다음 추적 예정 mstId와 detailId 업데이트
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val nextMstId = (nextSchedule?.get("MstId") as? Number)?.toLong()
        val nextDetailId = (nextSchedule?.get("DetailId") as? Number)?.toInt()
        
        dataStoreService.setCurrentTrackingMstId(currentMstId, currentDetailId)
        dataStoreService.setNextTrackingMstId(nextMstId, nextDetailId)

        // 로그 출력
        logger.debug("🔄 mstId/detailId 업데이트: 현재={}/{}, 다음={}/{}", currentMstId, currentDetailId, nextMstId, nextDetailId)
    }

    /**
     * 추적 시작을 준비하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param mstId 추적할 위성의 전역 고유 MST ID (Long 타입)
     */
    private fun prepareTrackingStart(mstId: Long?) {
        if (mstId == null) return
        
        try {
            logger.info("🚀 추적 시작 준비: MST ID = $mstId")
            
            // ✅ 병렬로 캐시 로딩 시작
            preloadTrackingDataCache(mstId)
            
            // ✅ 추적 상태 설정
            dataStoreService.setPassScheduleTracking(true)
            
            // ✅ 헤더 데이터 전송
            sendHeaderTrackingData(mstId)
            
            logger.info("✅ 추적 시작 준비 완료: MST ID = $mstId")
            
        } catch (e: Exception) {
            logger.error("❌ 추적 시작 준비 실패: MST ID = $mstId, ${e.message}", e)
        }
    }

    /**
     * 추적 종료 시 정리 작업을 수행하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param mstId 종료된 추적의 전역 고유 MST ID (Long 타입)
     * @param completedSchedule 완료된 스케줄 정보
     */
    private fun cleanupTrackingEnd(mstId: Long, completedSchedule: Map<String, Any?>) {
        try {
            val satelliteName = completedSchedule["SatelliteName"] as? String ?: "Unknown"
            logger.info("🛑 추적 종료 정리: $satelliteName (ID: $mstId)")
            
            // ✅ 캐시 정리 (메모리 절약)
            trackingDataCache.remove(mstId)
            logger.info("✅ 캐시 정리 완료: MST ID = $mstId")
            
            // ✅ 추적 상태 해제
            dataStoreService.setPassScheduleTracking(false)
            logger.info("✅ 추적 상태 해제 완료: MST ID = $mstId")
            
            logger.info("✅ 추적 종료 정리 완료: $satelliteName (ID: $mstId)")
            
        } catch (e: Exception) {
            logger.error("❌ 추적 종료 정리 실패: MST ID = $mstId, ${e.message}", e)
        }
    }

    /**
     * Train 축만 활성화하여 목표 각도로 회전합니다.
     *
     * 이 함수는 PREPARING 상태에서 Train을 먼저 회전하기 위해 사용됩니다.
     * Train 축만 활성화하여 다른 축(Az, El)에는 영향을 주지 않습니다.
     *
     * @param trainAngle 목표 Train 각도 (도 단위, Float)
     *
     * @see moveToTargetAzEl Train 회전 후 Az/El 이동
     * @see isTrainAtZero Train 각도 도달 확인
     */
    private fun moveTrainToZero(trainAngle: Float) {
        val multiAxis = BitSet()
        multiAxis.set(2)  // Train 축만 활성화
        udpFwICDService.singleManualCommand(
            multiAxis, trainAngle, 5f
        )
        logger.info("🔄 Train 각도 이동 시작: ${trainAngle}°")
    }

    /**
     * Azimuth와 Elevation 축만 활성화하여 목표 위치로 이동합니다.
     *
     * 이 함수는 Train 회전 및 안정화 완료 후 Az/El을 이동하기 위해 사용됩니다.
     * Az와 El 축만 활성화하여 Train 축에는 영향을 주지 않습니다.
     *
     * @see moveTrainToZero Train 회전 먼저 수행
     * @see isTrainStabilized Train 안정화 확인
     */
    private fun moveToTargetAzEl() {
        val multiAxis = BitSet()
        multiAxis.set(0)  // Azimuth
        multiAxis.set(1)  // Elevation
        udpFwICDService.multiManualCommand(
            multiAxis, targetAzimuth, 5f, targetElevation, 5f, 0f, 0f
        )
        logger.info("🔄 목표 Az/El로 이동: Az=${targetAzimuth}°, El=${targetElevation}°")
    }

    /**
     * Train 각도가 목표 각도에 도달했는지 확인합니다.
     *
     * @return Train 각도가 목표 각도에 도달했으면 true, 아니면 false
     *
     * @see moveTrainToZero Train 회전 명령 후 확인
     */
    private fun isTrainAtZero(): Boolean {
        val cmdTrain = PushData.CMD.cmdTrainAngle ?: 0f
        val currentTrain = dataStoreService.getLatestData().trainAngle ?: 0.0
        return kotlin.math.abs(cmdTrain - currentTrain.toFloat()) <= 0.1f
    }

    /**
     * Train 각도가 안정화되었는지 확인합니다.
     *
     * @return Train 각도가 안정화되었으면 true, 아니면 false
     *
     * @see isTrainAtZero Train 각도 도달 확인 후 안정화 확인
     */
    private fun isTrainStabilized(): Boolean {
        val cmdTrain = PushData.CMD.cmdTrainAngle ?: 0f
        val currentTrain = dataStoreService.getLatestData().trainAngle ?: 0.0
        return kotlin.math.abs(cmdTrain - currentTrain.toFloat()) <= 0.1f
    }

    /**
     * 시작 위치로 이동하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     */
    private fun moveToStartPosition(passId: Long) {  // ✅ UInt → Long 변경
        logger.info("═══════════════════════════════════════════════════════════════")
        logger.info("🎯 [STEP-5] moveToStartPosition() 호출 - passId=$passId")

        // ✅ Keyhole 여부에 따라 적절한 MST 선택
        val selectedPass = getTrackingPassMst(passId)

        if (selectedPass == null) {
            logger.error("❌ [STEP-5] 패스 ID ${passId}에 해당하는 MST 데이터를 찾을 수 없습니다!")
            logger.error("   - passScheduleTrackMstStorage 크기: ${passScheduleTrackMstStorage.size}")
            logger.error("   - selectedTrackMstStorage 크기: ${selectedTrackMstStorage.size}")
            return
        }

        logger.info("✅ [STEP-5] MST 데이터 조회 성공")
        logger.info("   - SatelliteName: ${selectedPass["SatelliteName"]}")
        logger.info("   - StartTime: ${selectedPass["StartTime"]}")
        logger.info("   - IsKeyhole: ${selectedPass["IsKeyhole"]}")

        // DTL 데이터 조회 (Keyhole 여부에 따라 적절한 DataType)
        val passDetails = getSelectedTrackDtlByMstId(passId)
        logger.info("📊 [STEP-5] DTL 데이터 조회: ${passDetails.size}개 포인트")

        if (passDetails.isNotEmpty()) {
            val startPoint = passDetails.first()
            targetAzimuth = (startPoint["Azimuth"] as Double).toFloat()
            targetElevation = (startPoint["Elevation"] as Double).toFloat()

            logger.info("✅ [STEP-5] 시작 위치 설정:")
            logger.info("   - targetAzimuth: ${targetAzimuth}°")
            logger.info("   - targetElevation: ${targetElevation}°")

            // ✅ PREPARING 상태 내에서 Train 회전 시작
            preparingPassId = passId
            currentPreparingStep = PreparingStep.MOVING_TRAIN
            logger.info("🔄 [STEP-5] PreparingStep → MOVING_TRAIN")
            logger.info("═══════════════════════════════════════════════════════════════")
        } else {
            logger.error("❌ [STEP-5] DTL 데이터가 비어있음! passId=$passId")
            logger.error("═══════════════════════════════════════════════════════════════")
        }
    }

    fun moveStartAnglePosition(
        cmdAzimuthAngle: Float,
        cmdAzimuthSpeed: Float,
        cmdElevationAngle: Float,
        cmdElevationSpeed: Float,
        cmdTrainAngle: Float,
        cmdTrainSpeed: Float
    ) {
        val multiAxis = BitSet()
        multiAxis.set(0)
        multiAxis.set(1)
        udpFwICDService.multiManualCommand(
            multiAxis, cmdAzimuthAngle,
            cmdAzimuthSpeed, cmdElevationAngle, cmdElevationSpeed, cmdTrainAngle ?: 0.0f, cmdTrainSpeed ?: 0.0f
        )
    }

    private fun moveToStowPosition(calTime: ZonedDateTime) {
        logger.info("[ACTION] Stow 위치로 이동 시작 (${calTime})")

        try {
            udpFwICDService.StowCommand()
            logger.info("[ACTION] Stow 명령 전송 완료")

            lastPreparedSchedule = null

            logger.info("[ACTION] Stow 위치로 이동 완료")
        } catch (e: Exception) {
            logger.error("[ERROR] Stow 위치 이동 실패: ${e.message}", e)
        }
    }
/**
     * 시간 오프셋 명령 - Mono 비동기 처리
     * 위성 초기 정보전달인 2.12.2 진행 후 Time Offset 값 전달
     */
    fun passScheduleTimeOffsetCommand(inputTimeOffset: Float) {
        Mono.fromCallable {
            GlobalData.Offset.TimeOffset = inputTimeOffset
            udpFwICDService.writeNTPCommand()
            // 현재 추적 중인 패스가 있을 때만 초기 데이터 전송
            dataStoreService.getCurrentTrackingMstId()?.let { mstId ->
                logger.info("추적 중인 패스 발견, 초기 데이터 전송 시작: mstId={}", mstId)
                sendInitialTrackingData(mstId)
                logger.info("초기 추적 데이터 전송 완료: mstId={}", mstId)
            } ?: run {
                logger.warn("현재 추적 중인 패스가 없어서 초기 데이터를 전송하지 않습니다")
            }
            //Time Offset 전달
            udpFwICDService.timeOffsetCommand(inputTimeOffset)
            // 글로벌 데이터 업데이트


            logger.info("TimeOffset 명령 전송 완료: {}s", inputTimeOffset)
        }.subscribeOn(Schedulers.boundedElastic()).subscribe({ /* 성공 */ }, { error ->
                logger.error("시간 오프셋 명령 처리 오류: {}", error.message, error)
            })
    }
    // 12.1
    // ✅ 기존 메서드 시그니처 유지하면서 내부 최적화

    /**
     * 헤더 추적 데이터를 전송하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     */
    fun sendHeaderTrackingData(passId: Long) {  // ✅ UInt → Long 변경
        try {
            udpFwICDService.writeNTPCommand()
            
            // ✅ Keyhole 여부에 따라 적절한 MST 선택
            val selectedPass = getTrackingPassMst(passId)
            
            if (selectedPass == null) {
                logger.error("선택된 패스 ID($passId)에 해당하는 데이터를 찾을 수 없습니다.")
                return
            }

            // Keyhole 정보 로깅
            val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
            val recommendedTrainAngle = selectedPass["RecommendedTrainAngle"] as? Double ?: 0.0
            logger.info("📊 헤더 전송 패스 정보: Keyhole=${if (isKeyhole) "YES" else "NO"}, RecommendedTrainAngle=${recommendedTrainAngle}°")

            val startTime = (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
            val endTime = (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)

            logger.info("위성 추적 시작: ${selectedPass["SatelliteName"]} (패스 ID: $passId)")
            logger.info("시작 시간: $startTime, 종료 시간: $endTime")

            val startTimeMs = (startTime.nano / 1_000_000).toUShort()
            val endTimeMs = (endTime.nano / 1_000_000).toUShort()

            val headerFrame = ICDService.SatelliteTrackOne.SetDataFrame(
                cmdOne = 'T',
                cmdTwo = 'T',
                dataLen = calculateDataLength(passId).toUShort(),
                aosYear = startTime.year.toUShort(),
                aosMonth = startTime.monthValue.toByte(),
                aosDay = startTime.dayOfMonth.toByte(),
                aosHour = startTime.hour.toByte(),
                aosMinute = startTime.minute.toByte(),
                aosSecond = startTime.second.toByte(),
                aosMs = startTimeMs,
                losYear = endTime.year.toUShort(),
                losMonth = endTime.monthValue.toByte(),
                losDay = endTime.dayOfMonth.toByte(),
                losHour = endTime.hour.toByte(),
                losMinute = endTime.minute.toByte(),
                losSecond = endTime.second.toByte(),
                losMs = endTimeMs,
            )

            udpFwICDService.sendSatelliteTrackHeader(headerFrame)
            logger.info("위성 추적 헤더 정보 전송 완료")

        } catch (e: Exception) {
            logger.error("위성 추적 시작 중 오류 발생: ${e.message}", e)
        }
    }
    /**
     * 초기 추적 데이터를 전송하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     */
    fun sendInitialTrackingData(passId: Long) {  // ✅ UInt → Long 변경
        try {
            // ✅ Keyhole 여부에 따라 적절한 MST 선택
            val selectedPass = getTrackingPassMst(passId)
            
            if (selectedPass == null) {
                logger.error("선택된 패스 ID($passId)에 해당하는 데이터를 찾을 수 없습니다.")
                return
            }

            // Keyhole 정보 확인
            val isKeyhole = selectedPass["IsKeyhole"] as? Boolean ?: false
            logger.info("📊 초기 추적 데이터 전송 패스 정보: Keyhole=${if (isKeyhole) "YES" else "NO"}")

            val startTime = (selectedPass["StartTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
            val endTime = (selectedPass["EndTime"] as ZonedDateTime).withZoneSameInstant(ZoneOffset.UTC)
            val calTime = GlobalData.Time.calUtcTimeOffsetTime

            logger.info("위성 추적 시작: ${selectedPass["SatelliteName"]} (패스 ID: $passId)")
            logger.info("시작 시간: $startTime, 종료 시간: $endTime, 현재 시간: $calTime")

            // ✅ Keyhole 여부에 따라 적절한 DataType의 DTL 조회
            val passDetails = getSelectedTrackDtlByMstId(passId) // 내부에서 Keyhole 여부에 따라 적절한 DataType 반환
            var initialTrackingData: List<Triple<UInt, Float, Float>> = emptyList()

            when {
                calTime.isBefore(startTime) -> {
                    // ✅ 추적 시작 전 - 초기 데이터 준비
                    logger.info("🕐 추적 시작 전 - 초기 데이터 준비")
                    initialTrackingData = passDetails.take(50).mapIndexed { index, point ->
                        Triple(
                            (index * 100).toUInt(),
                            (point["Elevation"] as Double).toFloat(),
                            (point["Azimuth"] as Double).toFloat()
                        )
                    }
                }

                calTime.isAfter(endTime) -> {
                    // ✅ 추적 종료 후
                    logger.warn("⚠️ 추적 종료 후 - 추적 중지")
                    dataStoreService.setPassScheduleTracking(false)
                    return
                }

                else -> {
                    // ✅ 핵심: 현재 시간 기준 실시간 추적 위치 계산
                    logger.info("🎯 현재 시간 기준 실시간 추적 시작")

                    val timeDifferenceMs = Duration.between(startTime, calTime).toMillis()
                    val calculatedIndex = (timeDifferenceMs / 100).toInt()
                    val totalSize = passDetails.size

                    val safeStartIndex = when {
                        calculatedIndex < 0 -> 0
                        calculatedIndex >= totalSize -> maxOf(0, totalSize - 50)
                        else -> calculatedIndex
                    }

                    val actualCount = minOf(50, totalSize - safeStartIndex)
                    val progressPercentage = if (totalSize > 0) {
                        (safeStartIndex.toDouble() / totalSize.toDouble()) * 100.0
                    } else 0.0

                    logger.info("📊 실시간 추적: 진행률=${String.format("%.1f", progressPercentage)}%, 인덱스=${safeStartIndex}/${totalSize}")

                    initialTrackingData = passDetails.drop(safeStartIndex).take(actualCount).mapIndexed { index, point ->
                        Triple(
                            ((safeStartIndex + index) * 100).toUInt(),  // ✅ 실제 시간 인덱스
                            (point["Elevation"] as Double).toFloat(),
                            (point["Azimuth"] as Double).toFloat()
                        )
                    }

                    // 현재 위치 로깅
                    val currentPoint = initialTrackingData.firstOrNull()
                    if (currentPoint != null) {
                        logger.info("📍 현재 추적 위치: 시간=${currentPoint.first}ms, 고도=${currentPoint.second}°, 방위=${currentPoint.third}°")
                    }
                }
            }

            if (initialTrackingData.isEmpty()) {
                logger.error("전송할 초기 추적 데이터가 없습니다.")
                return
            }

            // ✅ 기존 전송 로직 유지
            val currentTime = GlobalData.Time.utcNow
            val initialControlFrame = ICDService.SatelliteTrackTwo.SetDataFrame(
                cmdOne = 'T',
                cmdTwo = 'M',
                dataLen = initialTrackingData.size.toUShort(),
                ntpYear = currentTime.year.toUShort(),
                ntpMonth = currentTime.monthValue.toByte(),
                ntpDay = currentTime.dayOfMonth.toByte(),
                ntpHour = currentTime.hour.toByte(),
                ntpMinute = currentTime.minute.toByte(),
                ntpSecond = currentTime.second.toByte(),
                ntpMs = (currentTime.nano / 1_000_000).toUShort(),
                timeOffset = GlobalData.Offset.TimeOffset.toInt(),
                satelliteTrackData = initialTrackingData
            )

            udpFwICDService.sendSatelliteTrackInitialControl(initialControlFrame)
            logger.info("위성 추적 초기 제어 명령 전송 완료 (${initialTrackingData.size}개 데이터 포인트)")

        } catch (e: Exception) {
            logger.error("위성 추적 초기 제어 명령 전송 중 오류 발생: ${e.message}", e)
        }
    }


    /**
     * 추적 데이터 요청을 처리하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @param timeAcc 시간 누적값
     * @param requestDataLength 요청 데이터 길이
     */
    fun handleTrackingDataRequest(passId: Long, timeAcc: UInt, requestDataLength: UShort) {  // ✅ UInt → Long 변경
        val startIndex = timeAcc.toInt()
        sendAdditionalTrackingData(passId, startIndex, requestDataLength.toInt())
    }

    /**
     * 추가 추적 데이터를 전송하는 함수
     *
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     *
     * 이 함수는 캐시 여부에 따라 동기/비동기 처리를 선택합니다:
     * - 캐시 있으면: 동기 처리 (빠름, 즉시 전송)
     * - 캐시 없으면: 비동기 처리 (메모리 저장소 조회는 느릴 수 있으므로 블로킹 방지)
     *
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @param startIndex 시작 인덱스
     * @param requestDataLength 요청 데이터 길이
     */
    private fun sendAdditionalTrackingData(passId: Long, startIndex: Int, requestDataLength: Int = 25) {  // ✅ UInt → Long 변경
        val cache = trackingDataCache[passId]
        
        if (cache != null && !cache.isExpired()) {
            // ✅ 캐시 있으면 동기 처리 (빠름, 즉시 전송)
            val processingStart = System.nanoTime()
            try {
                sendAdditionalTrackingDataFromCache(cache, startIndex, requestDataLength, processingStart)
            } catch (e: Exception) {
                logger.error("캐시에서 추적 데이터 전송 실패: passId=$passId, ${e.message}", e)
                // 폴백: 메모리 저장소에서 동기 처리로 재시도
                try {
                    sendAdditionalTrackingDataFromDatabase(passId, startIndex, requestDataLength, processingStart)
                } catch (fallbackError: Exception) {
                    logger.error("폴백 전송도 실패: passId=$passId, ${fallbackError.message}", fallbackError)
                }
            }
        } else {
            // ✅ 캐시 없으면 비동기 처리 (메모리 저장소 조회는 느릴 수 있으므로 블로킹 방지)
            CompletableFuture.runAsync({
                try {
                    val processingStart = System.nanoTime()
                    sendAdditionalTrackingDataFromDatabase(passId, startIndex, requestDataLength, processingStart)
                } catch (e: Exception) {
                    logger.error("추적 데이터 전송 실패: passId=$passId, ${e.message}", e)
                    // 폴백: 동기 처리로 재시도
                    try {
                        val processingStart = System.nanoTime()
                        sendAdditionalTrackingDataFromDatabase(passId, startIndex, requestDataLength, processingStart)
                    } catch (fallbackError: Exception) {
                        logger.error("폴백 전송도 실패: passId=$passId, ${fallbackError.message}", fallbackError)
                    }
                }
            }, batchExecutor)
        }
    }

    /**
     * 캐시에서 추가 추적 데이터를 전송합니다.
     *
     * @param cache TrackingDataCache 객체
     * @param startIndex 시작 인덱스
     * @param requestDataLength 요청 데이터 길이
     * @param processingStart 처리 시작 시간 (성능 측정용)
     */
    private fun sendAdditionalTrackingDataFromCache(
        cache: TrackingDataCache,
        startIndex: Int,
        requestDataLength: Int,
        processingStart: Long
    ) {
        val indexMs = startIndex / 100
        val totalIndexes = cache.totalSize
        val remainingIndexes = maxOf(0, totalIndexes - indexMs)

        if (indexMs >= totalIndexes) {
            logger.info("📋 추적 완료: passId=${cache.passId}, 인덱스=$indexMs/$totalIndexes")
            return
        }

        // ✅ 고속 Array 접근
        val endIndex = minOf(indexMs + requestDataLength, totalIndexes)
        val additionalTrackingData = (indexMs until endIndex).map { index ->
            val point = cache.trackingPoints[index]
            Triple(startIndex + (index - indexMs) * 100, point.elevation, point.azimuth)
        }

        if (additionalTrackingData.isEmpty()) {
            logger.warn("📋 전송할 데이터가 없습니다: passId=${cache.passId}, startIndex=$startIndex")
            return
        }

        val additionalDataFrame = ICDService.SatelliteTrackThree.SetDataFrame(
            cmdOne = 'T', cmdTwo = 'R',
            dataLength = additionalTrackingData.size.toUShort(),
            satelliteTrackData = additionalTrackingData
        )

        udpFwICDService.sendSatelliteTrackAdditionalData(additionalDataFrame)

        val processingTime = (System.nanoTime() - processingStart) / 1_000_000
        val progressPercentage = (indexMs.toDouble() / totalIndexes.toDouble() * 100.0)

        logger.info(
            "🚀 [캐시] 고속 추적 데이터 전송 완료 (${additionalTrackingData.size}개 / 인덱스: $indexMs / 남은: $remainingIndexes / 총: $totalIndexes) [진행률: ${
                String.format(
                    "%.1f",
                    progressPercentage
                )
            }%] 처리시간: ${processingTime}ms"
        )

        // ✅ 성능 경고
        if (processingTime > 10) {
            logger.warn("⚠️ 캐시 처리 지연: ${processingTime}ms")
        }
    }

    /**
     * 메모리 저장소에서 추가 추적 데이터를 전송합니다.
     *
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     *
     * 현재는 메모리 저장소(passScheduleTrackDtlStorage)에서 조회하지만,
     * 추후 getSelectedTrackDtlByMstId() 내부를 DB 조회로 변경하면 자동으로 DB 연계됩니다.
     *
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @param startIndex 시작 인덱스
     * @param requestDataLength 요청 데이터 길이
     * @param processingStart 처리 시작 시간 (성능 측정용)
     */
    private fun sendAdditionalTrackingDataFromDatabase(
        passId: Long,  // ✅ UInt → Long 변경
        startIndex: Int,
        requestDataLength: Int,
        processingStart: Long
    ) {
        // ✅ Keyhole-aware 데이터 사용
        // 현재: getSelectedTrackDtlByMstId()는 메모리 저장소(passScheduleTrackDtlStorage)에서 조회
        // 추후: getSelectedTrackDtlByMstId() 내부를 DB 조회로 변경하면 자동으로 DB 연계됨
        val passDetails = getSelectedTrackDtlByMstId(passId)
        if (passDetails.isEmpty()) {
            logger.error("선택된 패스 ID($passId)에 해당하는 세부 데이터를 찾을 수 없습니다.")
            return
        }

        val indexMs = startIndex / 100
        val totalSize = passDetails.size
        val remainingIndexes = maxOf(0, totalSize - indexMs)

        if (indexMs >= totalSize) {
            logger.info("📋 추적 완료: passId=$passId, 인덱스=$indexMs/$totalSize")
            return
        }

        val additionalTrackingData = passDetails.drop(indexMs).take(requestDataLength).mapIndexed { index, point ->
            Triple(
                startIndex + index * 100,
                (point["Elevation"] as Double).toFloat(),
                (point["Azimuth"] as Double).toFloat()
            )
        }

        if (additionalTrackingData.isEmpty()) {
            logger.warn("📋 전송할 데이터가 없습니다: passId=$passId, startIndex=$startIndex")
            return
        }

        val additionalDataFrame = ICDService.SatelliteTrackThree.SetDataFrame(
            cmdOne = 'T', cmdTwo = 'R',
            dataLength = additionalTrackingData.size.toUShort(),
            satelliteTrackData = additionalTrackingData
        )

        udpFwICDService.sendSatelliteTrackAdditionalData(additionalDataFrame)

        val processingTime = (System.nanoTime() - processingStart) / 1_000_000
        val progressPercentage = (indexMs.toDouble() / totalSize.toDouble() * 100.0)

        logger.info(
            "📋 [DB] 추적 데이터 전송 완료 (${additionalTrackingData.size}개 / 인덱스: $indexMs / 남은: $remainingIndexes / 총: $totalSize) [진행률: ${
                String.format(
                    "%.1f",
                    progressPercentage
                )
            }%] 처리시간: ${processingTime}ms"
        )

        // ✅ 성능 경고
        if (processingTime > 20) {
            logger.warn("⚠️ 메모리 저장소 처리 지연: ${processingTime}ms")
        }
    }

   

    // ✅ 서비스 종료 시 정리
    @PreDestroy
    fun cleanup() {
        // 기존 구독 해제
        subscriptions.forEach { it.dispose() }
        subscriptions.clear()

        // 추적 모니터링 중지
        stopScheduleTracking()

        // ✅ ThreadManager의 batchExecutor는 ThreadManager에서 관리되므로
        // 여기서는 캐시만 정리

        // ✅ 캐시 정리
        trackingDataCache.clear()

        logger.info("[CLEANUP] PassScheduleService 정리 완료 (상태 머신 패턴 적용)")
    }

    // ✅ 성능 통계 조회
    fun getTrackingPerformanceStats(): Map<String, Any> {
        val cacheStats = trackingDataCache.mapValues { (passId, cache) ->
            mapOf(
                "totalPoints" to cache.totalSize,
                "createdAt" to cache.createdAt,
                "isExpired" to cache.isExpired(),
                "memorySizeKB" to (cache.totalSize * 12 / 1024) // 대략적인 메모리 사용량
            )
        }

        return mapOf(
            "cacheSize" to trackingDataCache.size,
            "cachedPassIds" to trackingDataCache.keys.toList(),
            "totalCachedPoints" to trackingDataCache.values.sumOf { it.totalSize },
            "totalMemoryKB" to trackingDataCache.values.sumOf { it.totalSize * 12 / 1024 },
            "executorActive" to (batchExecutor != null),
            "executorTerminated" to false, // ThreadManager에서 관리
            "cacheDetails" to cacheStats,
            "optimizationEnabled" to true
        )
    }

    /**
     * 캐시 상태를 확인하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @return 캐시 상태 정보
     */
    fun getCacheStatus(passId: Long): Map<String, Any> {  // ✅ UInt → Long 변경
        // ✅ Long 타입 명시
        val cache: TrackingDataCache? = trackingDataCache[passId]
        return if (cache != null) {
            mapOf(
                "cached" to true,
                "passId" to passId,
                "totalPoints" to cache.totalSize,
                "createdAt" to cache.createdAt,
                "isExpired" to cache.isExpired(),
                "memorySizeKB" to (cache.totalSize * 12 / 1024)
            )
        } else {
            mapOf(
                "cached" to false,
                "passId" to passId,
                "message" to "캐시되지 않음 - DB에서 조회됨"
            )
        }
    }

    /**
     * 만료된 캐시를 수동으로 정리하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 Long 타입 명시.
     */
    fun clearExpiredCache() {
        // ✅ Long 타입 명시 (타입 추론 실패 방지)
        val expiredKeys: Set<Long> = trackingDataCache.filter { (_, cache) -> cache.isExpired() }.keys.toSet()
        expiredKeys.forEach { key: Long -> trackingDataCache.remove(key) }

        if (expiredKeys.isNotEmpty()) {
            logger.info("만료된 캐시 정리 완료: ${expiredKeys.size}개 항목")
        }
    }

    /**
     * 캐시를 강제로 새로고침하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     */
    fun refreshCache(passId: Long) {  // ✅ UInt → Long 변경
        trackingDataCache.remove(passId)  // ✅ Long 타입으로 제거
        preloadTrackingDataCache(passId)  // ✅ Long 타입으로 전달
        logger.info("캐시 강제 새로고침: passId=$passId")
    }

    // ✅ 기존 메서드들 - 변경 없음 (호환성 보장)
    private fun getCurrentSelectedTrackingPassWithTime(targetTime: ZonedDateTime): Map<String, Any?>? {
        // ✅ 모든 스케줄을 수집한 후 DataType별로 필터링하여 중복 제거
        val allSchedules = mutableListOf<Map<String, Any?>>()
        selectedTrackMstStorage.values.forEach { mstDataList ->
            allSchedules.addAll(mstDataList)
        }
        
        // ✅ final_transformed 또는 keyhole_final_transformed만 사용하여 중복 제거
        val uniqueSchedules = allSchedules
            .filter { schedule ->
                val dataType = schedule["DataType"] as? String
                dataType == "final_transformed" || dataType == "keyhole_final_transformed"
            }
            .distinctBy { schedule ->
                // MstId와 DetailId 조합으로 고유성 보장
                val mstId = (schedule["MstId"] as? Number)?.toLong()
                val detailId = (schedule["DetailId"] as? Number)?.toInt()
                Pair(mstId, detailId)
            }
        
        // ✅ 현재 시간이 시작 시간과 종료 시간 사이에 있는 스케줄 찾기
        val currentPass = uniqueSchedules.find { mstRecord ->
                val startTime = mstRecord["StartTime"] as? ZonedDateTime
                val endTime = mstRecord["EndTime"] as? ZonedDateTime

            if (startTime != null && endTime != null) {
                val isAfterStart = !targetTime.isBefore(startTime)  // targetTime >= startTime
                val isBeforeEnd = targetTime.isBefore(endTime)     // targetTime < endTime
                isAfterStart && isBeforeEnd
            } else {
                false
            }
        }
        
        return currentPass
    }

    private fun getNextSelectedTrackingPassWithTime(targetTime: ZonedDateTime): Map<String, Any?>? {
        // ✅ 현재 스케줄을 먼저 확인하여 제외
        val currentSchedule = getCurrentSelectedTrackingPassWithTime(targetTime)
        val currentMstId = (currentSchedule?.get("MstId") as? Number)?.toLong()
        val currentDetailId = (currentSchedule?.get("DetailId") as? Number)?.toInt()

        val allSchedules = getSelectedTrackingSchedule()

        // 🔧 DEBUG: 5초마다 상세 로그 (50회 = 5초)
        val shouldLog = trackingCheckCount % 50L == 0L
        if (shouldLog) {
            logger.info("🔍 [STEP-2] getNextSelectedTrackingPassWithTime 호출")
            logger.info("   - targetTime: $targetTime")
            logger.info("   - allSchedules (selectedTrackMstStorage): ${allSchedules.size}개")
        }

        // ✅ DataType별로 중복 제거: final_transformed 또는 keyhole_final_transformed만 사용
        // 같은 MstId와 DetailId 조합에 대해 하나만 선택
        val uniqueSchedules = allSchedules
            .filter { schedule ->
                val dataType = schedule["DataType"] as? String
                dataType == "final_transformed" || dataType == "keyhole_final_transformed"
            }
            .distinctBy { schedule ->
                // MstId와 DetailId 조합으로 고유성 보장
                val mstId = (schedule["MstId"] as? Number)?.toLong()
                val detailId = (schedule["DetailId"] as? Number)?.toInt()
                Pair(mstId, detailId)
            }

        if (shouldLog) {
            logger.info("   - uniqueSchedules (final_transformed 필터): ${uniqueSchedules.size}개")
            uniqueSchedules.forEach { sch ->
                val mstId = (sch["MstId"] as? Number)?.toLong()
                val startTime = sch["StartTime"]
                val dataType = sch["DataType"]
                logger.info("     - mstId=$mstId, startTime=$startTime, dataType=$dataType")
            }
        }

        // ✅ 다음 스케줄 필터링: 시작 시간이 현재 시간보다 나중이고, 현재 스케줄이 아닌 것만
        val filteredSchedules = uniqueSchedules.filter { mstRecord ->
            val startTime = mstRecord["StartTime"] as? ZonedDateTime
            val mstId = (mstRecord["MstId"] as? Number)?.toLong()
            val detailId = (mstRecord["DetailId"] as? Number)?.toInt()

            // ✅ 시작 시간이 현재 시간보다 나중이고, 현재 스케줄이 아닌 것만
            val isAfterCurrentTime = startTime != null && startTime.isAfter(targetTime)
            val isNotCurrentSchedule = !(mstId == currentMstId && detailId == currentDetailId)

            isAfterCurrentTime && isNotCurrentSchedule
        }

        val nextSchedule = filteredSchedules.minByOrNull { mstRecord ->
            mstRecord["StartTime"] as ZonedDateTime
        }

        if (shouldLog) {
            logger.info("   - filteredSchedules (시간 필터): ${filteredSchedules.size}개")
            val nextMstId = (nextSchedule?.get("MstId") as? Number)?.toLong()
            val nextStartTime = nextSchedule?.get("StartTime")
            logger.info("   - 선택된 nextSchedule: mstId=$nextMstId, startTime=$nextStartTime")
        }

        return nextSchedule
    }

    fun getCurrentSelectedTrackingPass(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getCurrentSelectedTrackingPassWithTime(calTime)
    }

    fun getNextSelectedTrackingPass(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getNextSelectedTrackingPassWithTime(calTime)
    }

    fun getCurrentDisplayedSchedule(): Map<String, Any?>? {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        return getCurrentSelectedTrackingPassWithTime(calTime) ?: lastDisplayedSchedule
    }

    fun getTrackingMonitorStatus(): Map<String, Any> {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        val currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

        return mapOf(
            "isRunning" to isTrackingMonitorRunning.get(),
            "hasCurrentSchedule" to (currentSchedule != null),
            "hasNextSchedule" to (nextSchedule != null),
            "hasLastDisplayedSchedule" to (lastDisplayedSchedule != null),
            "currentSchedule" to (currentSchedule ?: "없음"),
            "nextSchedule" to (nextSchedule ?: "없음"),
            "lastDisplayedSchedule" to (lastDisplayedSchedule ?: "없음"),
            "totalSelectedSchedules" to getSelectedTrackingSchedule().size,
            "calTime" to calTime.toString(),
            "cacheSize" to trackingDataCache.size,  // ✅ 캐시 정보 추가
            "optimizationActive" to (batchExecutor != null)  // ✅ 최적화 상태 추가
        )
    }

    /**
     * 현재 스케줄 정보를 출력하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 "No" 필드 → "MstId" 필드로 변경.
     * 타입도 UInt → Long으로 변경.
     */
    private fun outputCurrentScheduleInfo(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val passId = (schedule["MstId"] as? Number)?.toLong()
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val startTime = schedule["StartTime"] as? ZonedDateTime
        val endTime = schedule["EndTime"] as? ZonedDateTime
        val maxElevation = schedule["MaxElevation"] as? Double
        val duration = schedule["Duration"] as? String

        logger.info("🎯 [현재 추적] $satelliteName (ID: $passId)")
        logger.info("   ⏰ 시간: $startTime ~ $endTime")
        logger.info("   📏 지속: $duration")
        logger.info("   📐 최대고도: ${maxElevation}°")
        logger.info("   🕐 계산시간: $calTime")

        if (passId != null) {
            // ✅ null 체크 후 Long 타입으로 사용
            val detailData = getSelectedTrackDtlByMstId(passId)
            // ✅ Long 타입으로 캐시 확인 (null 체크 후)
            val cacheStatus = if (trackingDataCache.containsKey(passId)) "캐시됨" else "DB조회"
            logger.info("   📊 추적포인트: ${detailData.size}개 ($cacheStatus)")  // ✅ 캐시 상태 표시
        }
    }

    private fun outputNextScheduleInfo(calTime: ZonedDateTime) {
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)

        if (nextSchedule != null) {
            val nextName = nextSchedule["SatelliteName"] as? String ?: "Unknown"
            val nextStart = nextSchedule["StartTime"] as? ZonedDateTime
            val nextId = nextSchedule["No"] as? UInt
            val nextMaxElevation = nextSchedule["MaxElevation"] as? Double

            if (nextStart != null) {
                val waitTime = Duration.between(calTime, nextStart)
                val minutes = waitTime.toMinutes()
                val seconds = waitTime.seconds % 60  // ✅ 초 단위 추가

                logger.info("   📅 다음예정: $nextName (ID: $nextId)")
                logger.info("   ⏰ 시작: $nextStart")
                logger.info("   📐 최대고도: ${nextMaxElevation}°")
                logger.info("   ⏳ 대기: ${minutes}분 ${seconds}초")  // ✅ 개선
            }
        } else {
            logger.info("   📭 다음예정: 없음")
        }
    }

    private fun outputUpcomingScheduleInfo(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val startTime = schedule["StartTime"] as? ZonedDateTime
        val passId = schedule["No"] as? UInt

        if (startTime != null) {
            val waitTime = Duration.between(calTime, startTime)
            val minutes = waitTime.toMinutes()
            val seconds = waitTime.seconds % 60  // ✅ 초 단위 추가

            logger.info("📅 [다음 예정] $satelliteName (ID: $passId)")
            logger.info("   ⏰ 시작예정: $startTime")
            logger.info("   ⏳ 대기시간: ${minutes}분 ${seconds}초")  // ✅ 개선
        }
    }

    private fun outputTrackingEnd(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val passId = schedule["No"] as? UInt

        logger.info("🏁 [추적 종료] $satelliteName (ID: $passId)")
        logger.info("   🕐 종료시간: $calTime")
    }

    private fun outputScheduleChange(prev: Map<String, Any?>, new: Map<String, Any?>, calTime: ZonedDateTime) {
        val prevName = prev["SatelliteName"] as? String ?: "Unknown"
        val newName = new["SatelliteName"] as? String ?: "Unknown"
        val prevId = prev["No"] as? UInt
        val newId = new["No"] as? UInt

        logger.info("🔄 [추적 변경] $prevName(ID:$prevId) → $newName(ID:$newId)")
        logger.info("   🕐 변경시간: $calTime")

        outputCurrentScheduleInfo(new, calTime)
    }

    private fun outputScheduleFixed(schedule: Map<String, Any?>, calTime: ZonedDateTime) {
        val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
        val passId = schedule["No"] as? UInt

        logger.info("📌 [스케줄 고정] $satelliteName (ID: $passId)")
        logger.info("   🕐 고정시간: $calTime")
        logger.info("   📭 모든 스케줄 완료 - 마지막 스케줄로 고정")
    }

    // ✅ 기존 메서드들 - 변경 없음
    fun generateAllPassScheduleTrackingDataAsync(): Mono<Map<String, Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>>> {
        val allTleIds = getAllPassScheduleTleIds()

        if (allTleIds.isEmpty()) {
            logger.warn("캐시된 TLE 데이터가 없습니다.")
            return Mono.just(emptyMap())
        }

        logger.info("전체 위성 패스 스케줄 추적 데이터 생성 시작 (비동기 병렬 처리) - 총 ${allTleIds.size}개 위성")
        
        // ✅ 전역 MstId 카운터 초기화 (전체 생성 시작 시)
        mstIdCounter.set(0)
        logger.info("🔄 전역 MstId 카운터 초기화 완료 (시작값: 0)")

        return Flux.fromIterable(allTleIds).flatMap { satelliteId ->
            val tleData = passScheduleTLECache.getWithName(satelliteId)  // ✅ Phase 5: TLE 캐시 사용
            if (tleData != null) {
                val (tleLine1, tleLine2, satelliteName) = tleData

                // ✅ 동시성 문제 해결: 패스 개수만 먼저 계산 (빠른 계산)
                val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
                val sourceMinEl = settingsService.sourceMinElevationAngle.toFloat()
                val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
                    tleLine1 = tleLine1,
                    tleLine2 = tleLine2,
                    startDate = today.withZoneSameInstant(ZoneOffset.UTC),
                    durationDays = settingsService.durationDays.toInt(),  // 설정값 사용 (기본: 1일)
                    minElevation = sourceMinEl,
                    latitude = locationData.latitude,
                    longitude = locationData.longitude,
                    altitude = locationData.altitude,
                )
                val passCount = schedule.trackingPasses.size

                // ✅ 원자적으로 범위 할당 (동시성 문제 해결)
                val startMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1
                logger.debug("📊 위성 $satelliteId($satelliteName) 할당된 MstId 범위: $startMstId ~ ${startMstId + passCount - 1} (${passCount}개 패스)")
                
                // ✅ 계산된 schedule을 재사용하여 실제 데이터 생성 (중복 계산 없음)
                generatePassScheduleTrackingDataAsyncWithSchedule(
                    satelliteId, tleLine1, tleLine2, satelliteName, startMstId, schedule
                ).map { (mstData, dtlData) ->
                    logger.debug("📊 위성 $satelliteId($satelliteName) 생성 완료: ${passCount}개 패스, MstId 범위: $startMstId ~ ${startMstId + passCount - 1}")
                    satelliteId to (mstData to dtlData)
                }.doOnSuccess {
                    logger.info("위성 $satelliteId($satelliteName) 추적 데이터 생성 완료")
                }.onErrorResume { error ->
                    logger.error("위성 $satelliteId($satelliteName) 추적 데이터 생성 중 오류 발생: ${error.message}", error)
                    Mono.empty()
                }
            } else {
                logger.warn("위성 $satelliteId 의 TLE 데이터를 찾을 수 없습니다.")
                Mono.empty()
            }
        }.collectMap({ it.first }, { it.second }).doOnSuccess { results ->
            val finalCounter = mstIdCounter.get()
            logger.info("전체 위성 패스 스케줄 추적 데이터 생성 완료 (비동기) - ${results.size}개 위성 처리 완료, 최종 MstId 카운터: $finalCounter")
        }.doOnError { error ->
            logger.error("전체 위성 패스 스케줄 추적 데이터 생성 실패 (비동기): ${error.message}", error)
        }.timeout(Duration.ofMinutes(60)).onErrorMap { error ->
            when (error) {
                is IOException -> RuntimeException("네트워크 연결 오류: ${error.message}", error)
                is TimeoutException -> RuntimeException("계산 시간 초과", error)
                else -> RuntimeException("전체 위성 패스 스케줄 추적 데이터 생성 실패: ${error.message}", error)
            }
        }
    }

    /**
     * 개별 위성의 패스 스케줄 추적 데이터를 비동기로 생성하는 함수 (schedule 재사용 버전)
     * 
     * 동시성 문제 해결을 위해 미리 계산된 schedule을 재사용하여 중복 계산을 방지합니다.
     * 
     * @param satelliteId 위성 카탈로그 번호
     * @param tleLine1 TLE 라인 1
     * @param tleLine2 TLE 라인 2
     * @param satelliteName 위성 이름 (선택)
     * @param startMstId 전역 고유 MstId 시작값
     * @param schedule 미리 계산된 위성 추적 스케줄 (중복 계산 방지)
     * @return MST와 DTL 데이터 쌍
     */
    private fun generatePassScheduleTrackingDataAsyncWithSchedule(
        satelliteId: String, 
        tleLine1: String, 
        tleLine2: String, 
        satelliteName: String? = null,
        startMstId: Long,
        schedule: OrekitCalculator.SatelliteTrackingSchedule  // ✅ 미리 계산된 schedule
    ): Mono<Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>> {
        return Mono.fromCallable {
            val actualSatelliteName = satelliteName ?: satelliteId

            logger.info("$actualSatelliteName 위성의 패스 스케줄 추적 시작 (MstId: $startMstId ~ ${startMstId + schedule.trackingPasses.size - 1})")

            // ✅ generateSatelliteTrackingSchedule 호출 제거 (이미 전달받은 schedule 사용)

            // ✅ 2. SatelliteTrackingProcessor로 모든 변환 수행
            logger.info("🔄 SatelliteTrackingProcessor로 데이터 변환 시작... (시작 MstId: $startMstId)")
            val processedData = try {
                satelliteTrackingProcessor.processFullTransformation(
                    schedule,  // ✅ 전달받은 schedule 사용
                    actualSatelliteName,
                    startMstId  // ✅ 전역 시작 MstId 전달
                )
            } catch (e: Exception) {
                logger.error("❌ 위성 추적 데이터 처리 실패: ${e.message}", e)
                throw e
            }
            logger.info("✅ SatelliteTrackingProcessor 데이터 변환 완료")

            // ✅ 3. 5가지 DataType 모두 저장
            val allMstData = mutableListOf<Map<String, Any?>>()
            allMstData.addAll(processedData.originalMst)
            allMstData.addAll(processedData.axisTransformedMst)
            allMstData.addAll(processedData.finalTransformedMst)
            allMstData.addAll(processedData.keyholeAxisTransformedMst)
            allMstData.addAll(processedData.keyholeFinalTransformedMst)

            val allDtlData = mutableListOf<Map<String, Any?>>()
            allDtlData.addAll(processedData.originalDtl)
            allDtlData.addAll(processedData.axisTransformedDtl)
            allDtlData.addAll(processedData.finalTransformedDtl)
            allDtlData.addAll(processedData.keyholeAxisTransformedDtl)
            allDtlData.addAll(processedData.keyholeFinalTransformedDtl)

            // ✅ Phase 5: Repository에 데이터 저장
            passScheduleDataRepository.saveSatelliteData(satelliteId, allMstData, allDtlData)

            logger.info("✅ 위성 $satelliteId 추적 데이터 저장 완료: ${allMstData.size}개 MST 레코드 (5가지 DataType 포함), ${allDtlData.size}개 DTL 레코드")

            // 하위 호환성을 위해 final_transformed 데이터 반환
            Pair(processedData.finalTransformedMst, processedData.finalTransformedDtl)
        }.subscribeOn(Schedulers.boundedElastic()).doOnSubscribe {
            logger.info("위성 패스 스케줄 추적 데이터 생성 시작 (비동기): $satelliteId")
        }.doOnSuccess {
            logger.info("위성 패스 스케줄 추적 데이터 생성 완료 (비동기): $satelliteId")
        }.doOnError { error ->
            logger.error("위성 패스 스케줄 추적 데이터 생성 실패 (비동기): $satelliteId - ${error.message}", error)
        }.timeout(Duration.ofMinutes(30)).onErrorMap { error ->
            when (error) {
                is IOException -> RuntimeException("네트워크 연결 오류: ${error.message}", error)
                is TimeoutException -> RuntimeException("계산 시간 초과", error)
                else -> RuntimeException("위성 패스 스케줄 추적 데이터 생성 실패: $satelliteId - ${error.message}", error)
            }
        }
    }

    /**
     * 개별 위성의 패스 스케줄 추적 데이터를 비동기로 생성하는 함수 (하위 호환성 유지)
     * 
     * 내부적으로 schedule을 생성합니다. 동시성 문제 해결을 위해서는
     * `generatePassScheduleTrackingDataAsyncWithSchedule`를 사용하세요.
     * 
     * @param satelliteId 위성 카탈로그 번호
     * @param tleLine1 TLE 라인 1
     * @param tleLine2 TLE 라인 2
     * @param satelliteName 위성 이름 (선택)
     * @param startMstId 전역 고유 MstId 시작값 (null이면 자동 할당, 0이면 자동 할당)
     * @return MST와 DTL 데이터 쌍
     */
    fun generatePassScheduleTrackingDataAsync(
        satelliteId: String, 
        tleLine1: String, 
        tleLine2: String, 
        satelliteName: String? = null,
        startMstId: Long? = null  // ✅ null이면 자동 할당, 0이면 자동 할당
    ): Mono<Pair<List<Map<String, Any?>>, List<Map<String, Any?>>>> {
        return Mono.fromCallable {
            val actualSatelliteName = satelliteName ?: satelliteId

            logger.info("$actualSatelliteName 위성의 패스 스케줄 추적 시작")

            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)

            // ✅ 1. OrekitCalculator로 2축 데이터 생성 (유지)
            // ✅ EphemerisService와 동일한 설정 사용 (sourceMinElevationAngle)
            val sourceMinEl = settingsService.sourceMinElevationAngle.toFloat()
            val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
                tleLine1 = tleLine1,
                tleLine2 = tleLine2,
                startDate = today.withZoneSameInstant(ZoneOffset.UTC),
                durationDays = settingsService.durationDays.toInt(),  // 설정값 사용 (기본: 1일)
                minElevation = sourceMinEl,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude,
            )

            val passCount = schedule.trackingPasses.size
            logger.info("위성 $satelliteId 추적 스케줄 생성 완료: ${passCount}개 패스")

            // ✅ 2. startMstId가 null이거나 0이면 자동으로 전역 카운터에서 할당
            val actualStartMstId = if (startMstId == null || startMstId == 0L) {
                // ✅ 동시성 문제 해결: 원자적으로 범위 할당
                val allocatedStartMstId = mstIdCounter.getAndAdd(passCount.toLong()) + 1
                logger.debug("📊 위성 $satelliteId($actualSatelliteName) 자동 할당된 MstId 범위: $allocatedStartMstId ~ ${allocatedStartMstId + passCount - 1} (${passCount}개 패스)")
                allocatedStartMstId
            } else {
                logger.debug("📊 위성 $satelliteId($actualSatelliteName) 지정된 MstId 범위: $startMstId ~ ${startMstId + passCount - 1} (${passCount}개 패스)")
                startMstId
            }

            // ✅ schedule과 actualStartMstId를 Pair로 반환하여 flatMap에서 사용
            Pair(schedule, actualStartMstId)
        }.flatMap { (schedule, actualStartMstId) ->
            // ✅ 3. 계산된 schedule을 재사용하여 실제 데이터 생성 (중복 계산 없음)
            val actualSatelliteName = satelliteName ?: satelliteId
            generatePassScheduleTrackingDataAsyncWithSchedule(
                satelliteId, tleLine1, tleLine2, actualSatelliteName, actualStartMstId, schedule
            )
        }.subscribeOn(Schedulers.boundedElastic()).doOnSubscribe {
            logger.info("위성 패스 스케줄 추적 데이터 생성 시작 (비동기): $satelliteId")
        }.doOnSuccess {
            logger.info("위성 패스 스케줄 추적 데이터 생성 완료 (비동기): $satelliteId")
        }.doOnError { error ->
            logger.error("위성 패스 스케줄 추적 데이터 생성 실패 (비동기): $satelliteId - ${error.message}", error)
        }.timeout(Duration.ofMinutes(30)).onErrorMap { error ->
            when (error) {
                is IOException -> RuntimeException("네트워크 연결 오류: ${error.message}", error)
                is TimeoutException -> RuntimeException("계산 시간 초과", error)
                else -> RuntimeException("위성 패스 스케줄 추적 데이터 생성 실패: $satelliteId - ${error.message}", error)
            }
        }
    }

    // ✅ 기존 조회 메서드들 - 변경 없음
    fun getPassScheduleTrackMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return passScheduleTrackMstStorage[satelliteId]
    }

    fun getPassScheduleTrackDtlBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return passScheduleTrackDtlStorage[satelliteId]
    }

    /**
     * 특정 위성의 특정 MstId에 대한 DTL 데이터를 조회하는 함수 (간단 버전)
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * MstId 비교 시 Long 타입으로 변환.
     * 
     * @param satelliteId 위성 카탈로그 번호
     * @param mstId 전역 고유 MstId (Long 타입)
     * @return 필터링된 DTL 데이터 리스트
     */
    fun getPassScheduleTrackDtlByMstId(satelliteId: String, mstId: Long): List<Map<String, Any?>> {  // ✅ UInt → Long 변경
        val dtlData = passScheduleTrackDtlStorage[satelliteId] ?: return emptyList()
        // ✅ MstId 비교 시 Long 타입으로 변환
        return dtlData.filter { (it["MstId"] as? Number)?.toLong() == mstId }
    }

    fun getAllPassScheduleTrackMst(): Map<String, List<Map<String, Any?>>> {
        return passScheduleTrackMstStorage.toMap()
    }

    /**
     * 모든 PassSchedule MST 데이터를 병합하여 반환합니다.
     *
     * 이 함수는 5가지 DataType(original, axis_transformed, final_transformed,
     * keyhole_axis_transformed, keyhole_final_transformed)의 MST 데이터를 병합하여
     * Keyhole 정보를 포함한 단일 리스트로 반환합니다.
     *
     * 병합된 데이터에는 다음 정보가 포함됩니다:
     * - Original (2축) 메타데이터: OriginalMaxElevation, OriginalMaxAzRate, OriginalMaxElRate 등
     * - FinalTransformed (3축, Train=0, ±270°) 메타데이터: FinalTransformedMaxAzRate, FinalTransformedMaxElRate 등
     * - KeyholeAxisTransformed (3축, Train≠0) 메타데이터: KeyholeAxisTransformedMaxAzRate 등
     * - KeyholeFinalTransformed (3축, Train≠0, ±270°) 메타데이터: KeyholeFinalTransformedMaxAzRate 등
     * - Keyhole 정보: IsKeyhole, RecommendedTrainAngle
     * - 필터링된 MaxElevation: displayMinElevationAngle 기준으로 필터링된 데이터의 MaxElevation
     *
     * @return 병합된 MST 데이터 리스트 (Keyhole 정보 포함)
     *
     * @see getAllEphemerisTrackMstMerged EphemerisService의 동일한 로직 참고
     * @see getTrackingPassMst Keyhole 판단 기준과 일치
     */
    fun getAllPassScheduleTrackMstMerged(): List<Map<String, Any?>> {
        try {
            logger.info("📊 Original, FinalTransformed, KeyholeAxisTransformed, KeyholeFinalTransformed 데이터 병합 시작")
            
            // 6가지 DataType 모두 조회 (위성별 그룹화된 구조에서 flatten)
            val allMstData = passScheduleTrackMstStorage.values.flatten()
            val originalMst = allMstData.filter { it["DataType"] == "original" }
            val finalMst = allMstData.filter { it["DataType"] == "final_transformed" }
            val keyholeAxisMst = allMstData.filter { it["DataType"] == "keyhole_axis_transformed" }
            val keyholeMst = allMstData.filter { it["DataType"] == "keyhole_final_transformed" }
            val keyholeOptimizedMst = allMstData.filter { it["DataType"] == "keyhole_optimized_final_transformed" }
            
            if (finalMst.isEmpty()) {
                logger.warn("⚠️ FinalTransformed 데이터가 없습니다")
                return emptyList()
            }
            
            // final_transformed MST 기준으로 병합
            val mergedData = finalMst.map { final ->
                // ✅ "No" → "MstId" 변경, UInt → Long 변경
                val mstId = (final["MstId"] as? Number)?.toLong()
                    ?: throw IllegalStateException("MstId 필드가 없거나 유효하지 않습니다: $final")
                val original = originalMst.find { (it["MstId"] as? Number)?.toLong() == mstId }
                val keyholeAxis = keyholeAxisMst.find { (it["MstId"] as? Number)?.toLong() == mstId }
                val keyhole = keyholeMst.find { (it["MstId"] as? Number)?.toLong() == mstId }
                val keyholeOptimized = keyholeOptimizedMst.find { (it["MstId"] as? Number)?.toLong() == mstId }
                
                // Keyhole 판단: final_transformed (Train=0) 기준으로 판단
                val train0MaxAzRate = final["MaxAzRate"] as? Double ?: 0.0
                val threshold = settingsService.keyholeAzimuthVelocityThreshold
                val isKeyhole = train0MaxAzRate >= threshold
                
                // 병합된 데이터 생성 (EphemerisService와 동일한 구조)
                final.toMutableMap().apply {
                    // ✅ MstId와 DetailId 명시적으로 보존 (중요!)
                    put("MstId", mstId)  // ✅ 명시적으로 MstId 설정
                    put("DetailId", final["DetailId"] ?: 0)  // ✅ DetailId도 명시적으로 설정
                    
                    // Original (2축) 메타데이터 추가
                    put("OriginalMaxElevation", original?.get("MaxElevation"))
                    put("OriginalMaxAzRate", original?.get("MaxAzRate"))
                    put("OriginalMaxElRate", original?.get("MaxElRate"))
                    
                    // FinalTransformed 속도 (Train=0, ±270°)
                    put("FinalTransformedMaxAzRate", final["MaxAzRate"])
                    put("FinalTransformedMaxElRate", final["MaxElRate"])
                    
                    // Keyhole Axis Transformed 데이터 추가 (각도 제한 ❌, Train≠0)
                    if (keyholeAxis != null && isKeyhole) {
                        put("KeyholeAxisTransformedMaxAzRate", keyholeAxis["MaxAzRate"])
                        put("KeyholeAxisTransformedMaxElRate", keyholeAxis["MaxElRate"])
                    }
                    
                    // Keyhole Final Transformed 데이터 추가 (각도 제한 ✅, Train≠0)
                    if (keyhole != null && isKeyhole) {
                        put("KeyholeFinalTransformedMaxAzRate", keyhole["MaxAzRate"])
                        put("KeyholeFinalTransformedMaxElRate", keyhole["MaxElRate"])
                    }
                    
                    // FinalTransformed 시작/종료 각도 및 최대 고도 (Train=0, ±270°)
                    put("FinalTransformedStartAzimuth", final["StartAzimuth"])
                    put("FinalTransformedEndAzimuth", final["EndAzimuth"])
                    put("FinalTransformedStartElevation", final["StartElevation"])
                    put("FinalTransformedEndElevation", final["EndElevation"])
                    put("FinalTransformedMaxElevation", final["MaxElevation"])
                    
                    // KeyholeFinalTransformed 시작/종료 각도 및 최대 고도 (Train≠0, ±270°)
                    put("KeyholeFinalTransformedStartAzimuth", keyhole?.get("StartAzimuth"))
                    put("KeyholeFinalTransformedEndAzimuth", keyhole?.get("EndAzimuth"))
                    put("KeyholeFinalTransformedStartElevation", keyhole?.get("StartElevation"))
                    put("KeyholeFinalTransformedEndElevation", keyhole?.get("EndElevation"))
                    put("KeyholeFinalTransformedMaxElevation", keyhole?.get("MaxElevation"))

                    // ✅ KeyholeOptimizedFinalTransformed 시작/종료 각도 및 최대 고도 (최적화된 Train 각도, ±270°)
                    val optimizedStartAz = keyholeOptimized?.get("StartAzimuth") as? Double
                    val optimizedEndAz = keyholeOptimized?.get("EndAzimuth") as? Double
                    val optimizedStartEl = keyholeOptimized?.get("StartElevation") as? Double
                    val optimizedEndEl = keyholeOptimized?.get("EndElevation") as? Double
                    val optimizedMaxEl = keyholeOptimized?.get("MaxElevation") as? Double

                    put("KeyholeOptimizedFinalTransformedStartAzimuth", optimizedStartAz)
                    put("KeyholeOptimizedFinalTransformedEndAzimuth", optimizedEndAz)
                    put("KeyholeOptimizedFinalTransformedStartElevation", optimizedStartEl)
                    put("KeyholeOptimizedFinalTransformedEndElevation", optimizedEndEl)
                    put("KeyholeOptimizedFinalTransformedMaxElevation", optimizedMaxEl)
                    put("KeyholeOptimizedFinalTransformedMaxAzRate", keyholeOptimized?.get("MaxAzRate"))
                    put("KeyholeOptimizedFinalTransformedMaxElRate", keyholeOptimized?.get("MaxElRate"))

                    // ✅ 키홀일 때 기본 필드들도 keyhole_optimized_final_transformed 값으로 덮어쓰기
                    if (isKeyhole && keyholeOptimized != null) {
                        if (optimizedStartAz != null) put("StartAzimuth", optimizedStartAz)
                        if (optimizedEndAz != null) put("EndAzimuth", optimizedEndAz)
                        if (optimizedStartEl != null) put("StartElevation", optimizedStartEl)
                        if (optimizedEndEl != null) put("EndElevation", optimizedEndEl)
                        if (optimizedMaxEl != null) put("MaxElevation", optimizedMaxEl)
                    }

                    // ✅ 하드웨어 제한 각도 기준으로 필터링된 데이터의 MaxElevation 재계산
                    // 전체 저장소에서 해당 MST ID의 DTL 데이터 조회 (Keyhole-aware)
                    val satelliteId = final["SatelliteID"] as? String
                    val allDtlData = if (satelliteId != null) {
                        passScheduleTrackDtlStorage[satelliteId] ?: emptyList()
                    } else {
                        emptyList()
                    }
                    
                    // Keyhole 여부에 따라 적절한 DataType의 DTL 필터링
                    val dataType = determineKeyholeDataType(mstId, passScheduleTrackMstStorage)
                    val dtlByDataType = if (dataType != null) {
                        allDtlData.filter {
                            it["MstId"] == mstId && it["DataType"] == dataType
                        }
                    } else {
                        emptyList()
                    }
                    
                    // 하드웨어 제한 각도 기준으로 필터링
                    val elevationMin = settingsService.angleElevationMin
                    val filteredDtl = dtlByDataType.filter {
                        (it["Elevation"] as? Double ?: 0.0) >= elevationMin
                    }
                    
                    val filteredMaxElevation = if (filteredDtl.isNotEmpty()) {
                        filteredDtl.maxOfOrNull { (it["Elevation"] as? Double) ?: Double.NEGATIVE_INFINITY }
                    } else {
                        null
                    }
                    put("MaxElevation", filteredMaxElevation)
                    
                    // Keyhole 정보
                    put("IsKeyhole", isKeyhole)
                    // ✅ RecommendedTrainAngle은 keyhole_optimized_final_transformed에서 가져오기 (없으면 0.0)
                    put("RecommendedTrainAngle", keyholeOptimized?.get("RecommendedTrainAngle") as? Double ?: 0.0)
                }
            }
            
            // ✅ 필터링 (하드웨어 제한 각도 기준)
            val elevationMin = settingsService.angleElevationMin
            
            val filteredMergedData = mergedData.filter { item ->
                val maxElevation = item["MaxElevation"] as? Double
                maxElevation != null && maxElevation >= elevationMin
            }
            
            logger.info("✅ 병합 완료: ${mergedData.size}개 MST 레코드 (KeyholeAxis + KeyholeFinal 데이터 포함)")
            logger.info("✅ 필터링 완료: ${mergedData.size}개 → ${filteredMergedData.size}개 (elevationMin=${elevationMin}° 기준)")
            return filteredMergedData
            
        } catch (error: Exception) {
            logger.error("❌ 데이터 병합 실패: ${error.message}", error)
            return emptyList()
        }
    }

    fun getAllPassScheduleTrackDtl(): Map<String, List<Map<String, Any?>>> {
        return passScheduleTrackDtlStorage.toMap()
    }

    fun clearPassScheduleTrackingData(satelliteId: String) {
        // ✅ Phase 5: Repository 사용
        passScheduleDataRepository.removeSatelliteData(satelliteId)
        logger.info("위성 $satelliteId 의 패스 스케줄 추적 데이터가 삭제되었습니다.")
    }

    /**
     * 모든 패스 스케줄 추적 데이터를 삭제하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 globalMstId → mstIdCounter로 변경.
     */
    fun clearAllPassScheduleTrackingData() {
        val summary = passScheduleDataRepository.getStorageSummary()
        val mstSize = summary["totalPasses"] as? Int ?: 0
        val dtlSize = summary["totalTrackingPoints"] as? Int ?: 0
        // ✅ globalMstId → mstIdCounter로 변경
        mstIdCounter.set(0)
        // ✅ Phase 5: Repository 사용
        passScheduleDataRepository.clear()

        logger.info("모든 패스 스케줄 추적 데이터가 삭제되었습니다. (마스터: ${mstSize}개, 세부: ${dtlSize}개, MstId 카운터 초기화)")
    }    // ✅ 기존 추적 대상 관리 메서드들 - 변경 없음

    fun setTrackingTargetList(targets: List<TrackingTarget>) {
        synchronized(trackingTargetList) {
            trackingTargetList.clear()
            trackingTargetList.addAll(targets)
        }
        logger.info("위성 추적 스케줄 대상 목록이 설정되었습니다. 총 ${targets.size}개 대상")

        targets.forEach { target ->
            logger.info("추적 대상: ${target.satelliteName ?: target.satelliteId} (MST ID: ${target.mstId}, 최대 고도: ${target.maxElevation}°, 시작 시간: ${target.startTime}, 종료 시간: ${target.endTime})")
        }

        generateSelectedTrackingData()
        
        // ✅ WebSocket으로 클라이언트에 상태 전송을 위한 mstId 설정
        updateTrackingMstIdsAfterTargetSet()
    }
    
    /**
     * ✅ 추적 대상 설정 후 mstId 업데이트
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 "No" 필드 → "MstId" 필드로 변경.
     * 타입도 UInt → Long으로 변경.
     */
    private fun updateTrackingMstIdsAfterTargetSet() {
        val calTime = GlobalData.Time.calUtcTimeOffsetTime
        val currentSchedule = getCurrentSelectedTrackingPassWithTime(calTime)
        val nextSchedule = getNextSelectedTrackingPassWithTime(calTime)
        
        // 현재 추적 중인 mstId와 detailId 설정
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val currentMstId = (currentSchedule?.get("MstId") as? Number)?.toLong()
        val currentDetailId = (currentSchedule?.get("DetailId") as? Number)?.toInt()
        dataStoreService.setCurrentTrackingMstId(currentMstId, currentDetailId)
        
        // 다음 추적 예정 mstId와 detailId 설정
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val nextMstId = (nextSchedule?.get("MstId") as? Number)?.toLong()
        val nextDetailId = (nextSchedule?.get("DetailId") as? Number)?.toInt()
        
        dataStoreService.setNextTrackingMstId(nextMstId, nextDetailId)
        
        logger.info("🎯 추적 대상 설정 후 mstId/detailId 업데이트: 현재={}/{}, 다음={}/{}", currentMstId, currentDetailId, nextMstId, nextDetailId)
    }

    fun getTrackingTargetList(): List<TrackingTarget> {
        return synchronized(trackingTargetList) {
            trackingTargetList.toList()
        }
    }

    fun getTrackingTargetsBySatelliteId(satelliteId: String): List<TrackingTarget> {
        return synchronized(trackingTargetList) {
            trackingTargetList.filter { it.satelliteId == satelliteId }
        }
    }

    /**
     * MstId로 추적 대상을 조회하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param mstId 전역 고유 MstId (Long 타입)
     * @return 추적 대상, 없으면 null
     */
    fun getTrackingTargetByMstId(mstId: Long): TrackingTarget? {
        return synchronized(trackingTargetList) {
            trackingTargetList.find { it.mstId == mstId }
        }
    }

    fun clearTrackingTargetList() {
        val size = synchronized(trackingTargetList) {
            val currentSize = trackingTargetList.size
            trackingTargetList.clear()
            currentSize
        }

        clearSelectedTrackingData()
        logger.info("위성 추적 스케줄 대상 목록이 초기화되었습니다. ${size}개 항목 삭제")
    }

    /**
     * 선택된 추적 데이터를 생성합니다.
     *
     * 이 함수는 사용자가 선택한 패스만 필터링하여 selectedTrackMstStorage에 저장합니다.
     * trackingTargetList에 있는 MST ID만 필터링하며, 5가지 DataType 모두 처리합니다.
     *
     * @note 이 함수는 passScheduleTrackMstStorage에서 5가지 DataType 모두 필터링합니다.
     * @note selectedTrackMstStorage를 사용하는 모든 함수가 Keyhole 정보를 포함하도록 개선됩니다.
     */
    fun generateSelectedTrackingData() {
        logger.info("═══════════════════════════════════════════════════════════════")
        logger.info("📦 [STEP-0] generateSelectedTrackingData() 호출됨")

        synchronized(trackingTargetList) {
            if (trackingTargetList.isEmpty()) {
                logger.warn("❌ [STEP-0] trackingTargetList가 비어있습니다!")
                selectedTrackMstStorage.clear()
                return
            }

            // 🔧 DEBUG: 원본 데이터 상태 확인
            val sourceTotalPasses = passScheduleTrackMstStorage.values.sumOf { it.size }
            logger.info("📊 [STEP-0] 원본 데이터 상태:")
            logger.info("   - passScheduleTrackMstStorage: ${passScheduleTrackMstStorage.size}개 위성, ${sourceTotalPasses}개 패스")
            logger.info("   - trackingTargetList: ${trackingTargetList.size}개 대상")

            // 🔧 DEBUG: trackingTargetList 상세 출력
            trackingTargetList.forEachIndexed { idx, target ->
                logger.info("   - [${idx}] mstId=${target.mstId}, satelliteId=${target.satelliteId}, satelliteName=${target.satelliteName}")
            }

            if (sourceTotalPasses == 0) {
                logger.error("❌ [STEP-0] passScheduleTrackMstStorage가 비어있음! 패스 데이터 로드 필요.")
            }

            logger.info("선별된 추적 데이터 생성 시작: ${trackingTargetList.size}개 대상")

            selectedTrackMstStorage.clear()
            val targetMstIds = trackingTargetList.map { it.mstId }.toSet()
            logger.info("   - 대상 MstId 목록: $targetMstIds")

            // ✅ 5가지 DataType 모두 필터링
            val dataTypes = listOf(
                "original",
                "axis_transformed",
                "final_transformed",
                "keyhole_axis_transformed",
                "keyhole_final_transformed"
            )

            passScheduleTrackMstStorage.forEach { (satelliteId, allMstData) ->
                val selectedMstData = mutableListOf<Map<String, Any?>>()

                // 각 DataType별로 필터링
                dataTypes.forEach { dataType ->
                    val filteredByDataType = allMstData.filter { mstRecord ->
                        // ✅ "No" → "MstId" 변경, UInt → Long 변경
                        val mstId = (mstRecord["MstId"] as? Number)?.toLong()
                        val recordDataType = mstRecord["DataType"] as? String
                        mstId != null && targetMstIds.contains(mstId) && recordDataType == dataType
                    }
                    if (filteredByDataType.isNotEmpty()) {
                        logger.info("   - 위성 $satelliteId, DataType=$dataType: ${filteredByDataType.size}개 매칭")
                    }
                    selectedMstData.addAll(filteredByDataType)
                }

                if (selectedMstData.isNotEmpty()) {
                    selectedTrackMstStorage[satelliteId] = selectedMstData
                    logger.info("위성 $satelliteId 선별된 패스: ${selectedMstData.size}개 (5가지 DataType 포함)")
                }
            }

            val totalSelectedPasses = selectedTrackMstStorage.values.sumOf { it.size }
            logger.info("✅ [STEP-0] 선별 완료: ${selectedTrackMstStorage.size}개 위성, ${totalSelectedPasses}개 패스")
            logger.info("═══════════════════════════════════════════════════════════════")
        }
    }

    fun getSelectedTrackMstBySatelliteId(satelliteId: String): List<Map<String, Any?>>? {
        return selectedTrackMstStorage[satelliteId]
    }

    fun getAllSelectedTrackMst(): Map<String, List<Map<String, Any?>>> {
        return selectedTrackMstStorage.toMap()
    }

    /**
     * Keyhole 여부를 확인하고 적절한 DataType을 반환합니다.
     *
     * 이 함수는 final_transformed MST에서 IsKeyhole 정보를 확인하여,
     * Keyhole 발생 시 keyhole_final_transformed, 미발생 시 final_transformed를 반환합니다.
     *
     * @param passId 패스 ID (MST ID)
     * @param storage 조회할 저장소 (passScheduleTrackMstStorage 또는 selectedTrackMstStorage)
     * @return Keyhole 여부에 따라 선택된 DataType ("keyhole_final_transformed" 또는 "final_transformed"), 없으면 null
     *
     * @see getTrackingPassMst 이 함수에서 사용하여 MST 선택
     * @see getSelectedTrackDtlByMstId 이 함수에서 사용하여 DTL 선택
     *
     * @note final_transformed MST에 IsKeyhole 정보가 저장되어 있어야 함
     * @note keyhole_final_transformed 데이터가 없으면 final_transformed로 폴백
     */
    /**
     * Keyhole 여부를 확인하고 적절한 DataType을 반환하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * "No" 필드 → "MstId" 필드로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @param storage 조회할 저장소
     * @return Keyhole 여부에 따라 선택된 DataType, 없으면 null
     */
    private fun determineKeyholeDataType(
        passId: Long,  // ✅ UInt → Long 변경
        storage: Map<String, List<Map<String, Any?>>>
    ): String? {
        // final_transformed MST에서 IsKeyhole 확인
        val allMstData = storage.values.flatten()
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val finalMst = allMstData.find {
            (it["MstId"] as? Number)?.toLong() == passId && it["DataType"] == "final_transformed"
        } ?: return null
        
        val isKeyhole = finalMst["IsKeyhole"] as? Boolean ?: false
        
        return if (isKeyhole) {
            // Keyhole 발생 시 keyhole_final_transformed 데이터 존재 여부 확인
            val keyholeDataExists = allMstData.any {
                (it["MstId"] as? Number)?.toLong() == passId && it["DataType"] == "keyhole_final_transformed"
            }
            
            if (!keyholeDataExists) {
                logger.warn("⚠️ 패스 ID ${passId}: Keyhole로 판단되었으나 keyhole_final_transformed 데이터가 없습니다. final_transformed로 폴백합니다.")
                "final_transformed"  // 폴백
            } else {
                "keyhole_final_transformed"
            }
        } else {
            "final_transformed"
        }
    }

    /**
     * Keyhole 여부에 따라 적절한 MST(Master) 데이터를 반환합니다.
     *
     * 이 함수는 위성 추적 시작 시 currentTrackingPass를 설정하기 위해 사용됩니다.
     * passId로 조회하며, Keyhole 여부에 따라 DataType을 **동적으로 선택**합니다:
     * - Keyhole 발생: keyhole_final_transformed MST (Train≠0, ±270° 제한 적용)
     * - Keyhole 미발생: final_transformed MST (Train=0, ±270° 제한 적용)
     *
     * 선택된 MST에는 다음 정보가 포함됩니다:
     * - IsKeyhole: Keyhole 여부 (Boolean)
     * - RecommendedTrainAngle: 권장 Train 각도 (Double, Keyhole 발생 시만 0이 아님)
     * - StartTime, EndTime: 추적 시작/종료 시간
     * - 기타 추적 메타데이터
     *
     * @param passId 패스 ID (MST ID)
     * @return Keyhole 여부에 따라 선택된 MST 데이터, 없으면 null
     *
     * @see getSelectedTrackDtlByMstId 동일한 Keyhole 판단 로직 사용 (DTL 데이터 반환)
     * @see getAllPassScheduleTrackMstMerged Keyhole 판단 기준과 일치
     *
     * @note 이 함수는 passScheduleTrackMstStorage에서 직접 조회합니다.
     * @note selectedTrackMstStorage를 사용하는 함수들과 달리, 전체 저장소에서 조회합니다.
     * @note DataType은 정해져 있지 않고, Keyhole 여부에 따라 동적으로 선택됩니다.
     */
    /**
     * Keyhole 여부에 따라 적절한 MST 데이터를 반환하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * "No" 필드 → "MstId" 필드로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @return Keyhole 여부에 따라 선택된 MST 데이터, 없으면 null
     */
    private fun getTrackingPassMst(passId: Long): Map<String, Any?>? {  // ✅ UInt → Long 변경
        // determineKeyholeDataType()을 사용하여 적절한 DataType 결정
        val dataType = determineKeyholeDataType(passId, passScheduleTrackMstStorage) ?: return null
        
        // 선택된 DataType의 MST 반환
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val selectedMst = passScheduleTrackMstStorage.values.flatten().find {
            (it["MstId"] as? Number)?.toLong() == passId && it["DataType"] == dataType
        }
        
        if (selectedMst == null) {
            logger.error("❌ 패스 ID ${passId}: 선택된 DataType($dataType)의 MST를 찾을 수 없습니다.")
            return null
        }
        
        val isKeyhole = selectedMst["IsKeyhole"] as? Boolean ?: false
        logger.info("📊 패스 ID ${passId} MST 선택: Keyhole=${if (isKeyhole) "YES" else "NO"}, DataType=${dataType}")
        
        return selectedMst
    }

    /**
     * MstId로 선택된 MST 데이터를 조회하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * "No" 필드 → "MstId" 필드로 변경.
     * 
     * @param mstId 전역 고유 MstId (Long 타입)
     * @return 선택된 MST 데이터, 없으면 null
     */
    fun getSelectedTrackMstByMstId(mstId: Long): Map<String, Any?>? {  // ✅ UInt → Long 변경
        selectedTrackMstStorage.values.forEach { mstDataList ->
            // ✅ "No" → "MstId" 변경, UInt → Long 변경
            val found = mstDataList.find { (it["MstId"] as? Number)?.toLong() == mstId }
            if (found != null) return found
        }
        return null
    }

    /**
     * 선택된 패스의 DTL 데이터를 조회합니다.
     *
     * 이 함수는 Keyhole 여부에 따라 적절한 DataType의 DTL 데이터를 반환합니다.
     * selectedTrackMstStorage에서 MST를 조회한 후, Keyhole 여부를 확인하여 적절한 DataType의 DTL을 반환합니다.
     *
     * @param mstId MST ID (패스 ID)
     * @return Keyhole 여부에 따라 선택된 DataType의 DTL 데이터 리스트
     *
     * @see getTrackingPassMst 동일한 Keyhole 판단 로직 사용 (MST 데이터 반환)
     * @see getEphemerisTrackDtlByMstId EphemerisService의 동일한 로직 참고
     */
    /**
     * 선택된 패스의 DTL 데이터를 조회하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * "No" 필드 → "MstId" 필드로 변경.
     * 
     * @param mstId 전역 고유 MstId (Long 타입)
     * @return Keyhole 여부에 따라 선택된 DataType의 DTL 데이터 리스트
     */
    fun getSelectedTrackDtlByMstId(mstId: Long): List<Map<String, Any?>> {  // ✅ UInt → Long 변경
        // 1. selectedTrackMstStorage에서 MST 조회
        val selectedMst = getSelectedTrackMstByMstId(mstId) ?: return emptyList()
        val satelliteId = selectedMst["SatelliteID"] as? String ?: return emptyList()
        
        // 2. determineKeyholeDataType()을 사용하여 적절한 DataType 결정
        val dataType = determineKeyholeDataType(mstId, selectedTrackMstStorage) ?: return emptyList()
        
        // 3. 선택된 DataType의 DTL 데이터 조회
        val allDtlData = passScheduleTrackDtlStorage[satelliteId] ?: return emptyList()
        
        // ✅ MstId 비교 시 Long 타입으로 변환
        val filteredDtl = allDtlData.filter {
            (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == dataType
        }
        
        // ✅ "No" → "MstId" 변경, UInt → Long 변경
        val isKeyhole = selectedTrackMstStorage.values.flatten().find {
            (it["MstId"] as? Number)?.toLong() == mstId && it["DataType"] == "final_transformed"
        }?.get("IsKeyhole") as? Boolean ?: false
        
        logger.info("📊 MST ID ${mstId} DTL 조회: Keyhole=${if (isKeyhole) "YES" else "NO"}, DataType=${dataType}, ${filteredDtl.size}개 포인트")
        
        return filteredDtl
    }

    /**
     * ✅ API용: 특정 위성의 특정 패스에 대한 DTL 데이터 조회
     * 프론트엔드에서 DataType을 명시적으로 지정할 수 있음
     * 
     * @param satelliteId 위성 ID
     * @param passId 패스 ID (MST ID)
     * @param dataType DataType (optional) - null이면 기존 로직 사용 (하위 호환성)
     * @return 필터링된 DTL 데이터 리스트
     */
    /**
     * 특정 위성의 특정 패스에 대한 DTL 데이터를 조회하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * "No" 필드 → "MstId" 필드로 변경.
     * 
     * @param satelliteId 위성 카탈로그 번호
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @param dataType DataType (optional) - null이면 기존 로직 사용
     * @return 필터링된 DTL 데이터 리스트
     */
    /**
     * MstId와 DetailId로 DTL 데이터를 조회하는 함수 (satelliteId 불필요)
     * 
     * 모든 위성의 DTL 데이터를 순회하여 mstId와 detailId로 필터링합니다.
     * 
     * @param mstId 전역 고유 패스 ID (Long 타입)
     * @param detailId 패스 인덱스 (Int 타입)
     * @param dataType DataType (optional)
     * @return 세부 데이터 리스트
     */
    fun getPassScheduleTrackDtlByMstIdAndDetailId(
        mstId: Long,
        detailId: Int,
        dataType: String? = null
    ): List<Map<String, Any?>> {
        // ✅ 디버깅: 조회 파라미터 확인
        logger.info("🔍 DTL 조회 시작: mstId=$mstId, detailId=$detailId, dataType=$dataType")
        
        // ✅ 디버깅: 저장소 상태 확인
        val storageSize = passScheduleTrackDtlStorage.size
        val storageKeys = passScheduleTrackDtlStorage.keys.toList()
        logger.info("🔍 DTL 저장소 상태: 위성 개수=$storageSize, 위성 ID 목록=$storageKeys")
        
        // 1. ✅ 모든 위성의 DTL 데이터를 순회하여 mstId와 detailId로 필터링
        val allDtlData = passScheduleTrackDtlStorage.values.flatten()
        
        // ✅ 디버깅: 사용 가능한 MstId 목록 확인
        val uniqueMstIds = allDtlData.mapNotNull { (it["MstId"] as? Number)?.toLong() }.distinct().sorted()
        logger.info("🔍 사용 가능한 전체 MstId 목록: $uniqueMstIds (전체 DTL 데이터 개수: ${allDtlData.size})")
        
        // 2. DataType 결정
        val targetDataType = if (dataType != null) {
            // ✅ 프론트엔드에서 명시적으로 DataType을 전달한 경우
            dataType
        } else {
            // ✅ Keyhole 판단을 위해 MST 데이터에서 확인
            val allMstData = passScheduleTrackMstStorage.values.flatten()
            val mstData = allMstData.find { (it["MstId"] as? Number)?.toLong() == mstId }
            
            if (mstData != null) {
                // ✅ selectedTrackMstStorage에서 먼저 확인
            val selectedMst = selectedTrackMstStorage.values.flatten().find {
                    (it["MstId"] as? Number)?.toLong() == mstId
            }
            
            if (selectedMst != null) {
                    determineKeyholeDataType(mstId, selectedTrackMstStorage) ?: "final_transformed"
            } else {
                // selectedTrackMstStorage에 없으면 전체 저장소에서 확인
                    determineKeyholeDataType(mstId, passScheduleTrackMstStorage) ?: "final_transformed"
                }
            } else {
                "final_transformed" // 기본값
            }
        }
        
        // 3. MstId, DetailId, DataType으로 필터링
        // ✅ 디버깅: 필터링 전 DTL 데이터 샘플 확인
        if (allDtlData.isNotEmpty()) {
            val sampleDtl = allDtlData.first()
            logger.info("🔍 DTL 데이터 샘플 (첫 번째 항목): MstId=${sampleDtl["MstId"]}, DetailId=${sampleDtl["DetailId"]}, Index=${sampleDtl["Index"]}, DataType=${sampleDtl["DataType"]}, 모든 키=${sampleDtl.keys}")
        }
        
        // ✅ mstId에 해당하는 모든 데이터 샘플 확인 (처음 5개)
        val mstIdSamples = allDtlData.filter { (it["MstId"] as? Number)?.toLong() == mstId }.take(5)
        if (mstIdSamples.isNotEmpty()) {
            logger.info("🔍 mstId=${mstId}에 해당하는 DTL 데이터 샘플 (처음 5개):")
            mstIdSamples.forEachIndexed { index, dtl ->
                logger.info("   [$index] MstId=${dtl["MstId"]}, DetailId=${dtl["DetailId"]}, DataType=${dtl["DataType"]}")
            }
        }
        
        // ✅ mstId와 detailId에 해당하는 모든 DataType 확인
        val mstIdAndDetailIdSamples = allDtlData.filter { 
            (it["MstId"] as? Number)?.toLong() == mstId && 
            (it["DetailId"] as? Number)?.toInt() == detailId 
        }
        val dataTypesForMstIdAndDetailId = mstIdAndDetailIdSamples.mapNotNull { it["DataType"] as? String }.distinct().sorted()
        logger.info("🔍 mstId=${mstId}, detailId=${detailId}에 해당하는 모든 DataType: $dataTypesForMstIdAndDetailId")
        logger.info("🔍 mstId=${mstId}, detailId=${detailId}의 총 DTL 개수: ${mstIdAndDetailIdSamples.size}")
        
        // ✅ 필터링 전에 mstId, detailId, targetDataType에 해당하는 데이터 개수 확인
        val preFilteredCount = allDtlData.count {
            val dtlMstId = (it["MstId"] as? Number)?.toLong()
            val dtlDetailId = (it["DetailId"] as? Number)?.toInt()
            val dtlDataType = it["DataType"] as? String
            dtlMstId == mstId && dtlDetailId == detailId && dtlDataType == targetDataType
        }
        logger.info("🔍 필터링 전 예상 개수: mstId=$mstId, detailId=$detailId, DataType=$targetDataType → ${preFilteredCount}개")
        
        // ✅ 성능 최적화: indexOf 대신 forEachIndexed 사용
        val filteredDtl = mutableListOf<Map<String, Any?>>()
        var checkCount = 0
        val startTime = System.currentTimeMillis()
        
        allDtlData.forEachIndexed { index, it ->
            val dtlMstId = (it["MstId"] as? Number)?.toLong()
            val dtlDetailId = (it["DetailId"] as? Number)?.toInt()
            val dtlDataType = it["DataType"] as? String
            
            val matches = dtlMstId == mstId && 
                         dtlDetailId == detailId && 
                         dtlDataType == targetDataType
            
            // ✅ 디버깅: 필터링 과정 로그 (처음 3개만)
            if (index < 3) {
                logger.info("🔍 필터링 체크 [$index]: dtlMstId=$dtlMstId, mstId=$mstId, dtlDetailId=$dtlDetailId, detailId=$detailId, dtlDataType=$dtlDataType, targetDataType=$targetDataType, matches=$matches")
            }
            
            if (matches) {
                filteredDtl.add(it)
                checkCount++
                
                // ✅ 디버깅: 매칭된 데이터 샘플 (처음 3개만)
                if (filteredDtl.size <= 3) {
                    logger.info("✅ 매칭 발견 [${filteredDtl.size}]: MstId=$dtlMstId, DetailId=$dtlDetailId, DataType=$dtlDataType, Index=${it["Index"]}")
                }
            }
        }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime
        
        logger.info("📊 mstId=$mstId, detailId=$detailId DTL 조회: DataType=${targetDataType}, ${filteredDtl.size}개 포인트 (처리 시간: ${processingTime}ms, 전체 데이터: ${allDtlData.size}개)")
        
        // ✅ 필터링 결과 샘플 확인 (처음 3개)
        if (filteredDtl.isNotEmpty()) {
            logger.info("✅ 필터링 성공 - 결과 샘플 (처음 3개):")
            filteredDtl.take(3).forEachIndexed { index, dtl ->
                logger.info("   [$index] MstId=${dtl["MstId"]}, DetailId=${dtl["DetailId"]}, DataType=${dtl["DataType"]}, Index=${dtl["Index"]}")
            }
        }
        
        // ✅ 디버깅: 조회 결과가 없을 때 상세 정보 출력
        if (filteredDtl.isEmpty()) {
            // ✅ 해당 DataType의 모든 DTL 데이터의 MstId 확인
            val dtlByDataType = allDtlData.filter { it["DataType"] == targetDataType }
            val mstIdsInDtl = dtlByDataType.mapNotNull { (it["MstId"] as? Number)?.toLong() }.distinct().sorted()
            
            // ✅ 해당 mstId의 모든 detailId 확인
            val dtlByMstId = allDtlData.filter { (it["MstId"] as? Number)?.toLong() == mstId }
            val detailIdsForMstId = dtlByMstId.mapNotNull { (it["DetailId"] as? Number)?.toInt() }.distinct().sorted()
            
            // ✅ 해당 mstId와 dataType의 모든 detailId 확인
            val dtlByMstIdAndDataType = allDtlData.filter { 
                (it["MstId"] as? Number)?.toLong() == mstId && 
                it["DataType"] == targetDataType 
            }
            val detailIdsForMstIdAndDataType = dtlByMstIdAndDataType.mapNotNull { (it["DetailId"] as? Number)?.toInt() }.distinct().sorted()
            
            logger.warn("⚠️ mstId=$mstId, detailId=$detailId DTL 조회 실패:")
            logger.warn("   요청한 mstId=$mstId, detailId=$detailId, DataType=$targetDataType")
            logger.warn("   사용 가능한 전체 MstId=$uniqueMstIds")
            logger.warn("   해당 DataType의 DTL에 있는 MstId=$mstIdsInDtl")
            logger.warn("   해당 DataType의 DTL 총 개수=${dtlByDataType.size}")
            logger.warn("   mstId=${mstId}의 모든 DetailId=$detailIdsForMstId")
            logger.warn("   mstId=${mstId}, DataType=${targetDataType}의 DetailId=$detailIdsForMstIdAndDataType")
            logger.warn("   mstId=${mstId}, DataType=${targetDataType}의 DTL 총 개수=${dtlByMstIdAndDataType.size}")
        }
        
        return filteredDtl
    }

    fun getSelectedTrackingSchedule(): List<Map<String, Any?>> {
        val allSelectedPasses = mutableListOf<Map<String, Any?>>()

        selectedTrackMstStorage.values.forEach { mstDataList ->
            allSelectedPasses.addAll(mstDataList)
        }
        
        // ✅ 디버깅 로그 주석 처리 (100ms마다 호출되어 과도한 로그 발생)
        // logger.debug("🔍 [디버깅] getSelectedTrackingSchedule 전체 스케줄 수: ${allSelectedPasses.size}")
        // logger.debug("🔍 [디버깅] getSelectedTrackingSchedule 위성 수: ${selectedTrackMstStorage.size}")
        //
        // if (allSelectedPasses.isNotEmpty()) {
        //     allSelectedPasses.take(3).forEachIndexed { index, schedule ->
        //         val mstId = schedule["MstId"]
        //         val detailId = schedule["DetailId"]
        //         val startTime = schedule["StartTime"] as? ZonedDateTime
        //         val dataType = schedule["DataType"]
        //         logger.debug("🔍 [디버깅] getSelectedTrackingSchedule[$index]: MstId=$mstId, DetailId=$detailId, StartTime=$startTime, DataType=$dataType")
        //     }
        // }

        return allSelectedPasses.sortedBy { mstRecord ->
            mstRecord["StartTime"] as? ZonedDateTime
        }
    }

    fun clearSelectedTrackingData() {
        val size = selectedTrackMstStorage.values.sumOf { it.size }
        selectedTrackMstStorage.clear()
        logger.info("선별된 추적 데이터가 초기화되었습니다. ${size}개 패스 삭제")
    }

    // ✅ Phase 5: TLE 캐시 관리 메서드들 - PassScheduleTLECache 사용
    fun addPassScheduleTle(satelliteId: String, tleLine1: String, tleLine2: String, satelliteName: String? = null) {
        passScheduleTLECache.add(satelliteId, tleLine1, tleLine2, satelliteName)
    }

    fun getPassScheduleTle(satelliteId: String): Pair<String, String>? {
        return passScheduleTLECache.get(satelliteId)
    }

    fun getPassScheduleSatelliteName(satelliteId: String): String? {
        return passScheduleTLECache.getName(satelliteId)
    }

    fun getPassScheduleTleWithName(satelliteId: String): Triple<String, String, String>? {
        return passScheduleTLECache.getWithName(satelliteId)
    }

    fun removePassScheduleTle(satelliteId: String) {
        passScheduleTLECache.remove(satelliteId)
        // ✅ Phase 5: 관련 추적 데이터 저장소도 함께 정리
        passScheduleDataRepository.removeSatelliteData(satelliteId)
    }

    fun getAllPassScheduleTleIds(): List<String> {
        return passScheduleTLECache.getAllIds()
    }

    fun getCacheSize(): Int {
        return passScheduleTLECache.size()
    }

    fun clearCache() {
        val size = passScheduleTLECache.size()
        passScheduleTLECache.clear()
        // ✅ Phase 5: Repository 사용
        passScheduleDataRepository.clear()

        // ✅ 최적화 캐시도 함께 정리
        trackingDataCache.clear()

        logger.info("TLE 캐시 및 추적 데이터 전체 삭제 완료: ${size}개 항목 삭제 (최적화 캐시 포함)")
    }

    // ✅ 기존 통계 메서드 + 최적화 정보 추가
    fun getTrackingDataStatistics(): Map<String, Any> {
        val totalSatellites = passScheduleTrackMstStorage.size
        val totalPasses = passScheduleTrackMstStorage.values.sumOf { it.size }
        val totalTrackingPoints = passScheduleTrackDtlStorage.values.sumOf { it.size }

        val satelliteStats = passScheduleTrackMstStorage.map { (satelliteId, mstData) ->
            val dtlData = passScheduleTrackDtlStorage[satelliteId] ?: emptyList()
            satelliteId to mapOf(
                "passes" to mstData.size,
                "trackingPoints" to dtlData.size,
                "satelliteName" to (mstData.firstOrNull()?.get("SatelliteName") ?: "Unknown")
            )
        }.toMap()

        return mapOf(
            "totalSatellites" to totalSatellites,
            "totalPasses" to totalPasses,
            "totalTrackingPoints" to totalTrackingPoints,
            "averagePassesPerSatellite" to if (totalSatellites > 0) totalPasses.toDouble() / totalSatellites else 0.0,
            "averagePointsPerPass" to if (totalPasses > 0) totalTrackingPoints.toDouble() / totalPasses else 0.0,
            "satelliteDetails" to satelliteStats,
            // ✅ 최적화 정보 추가
            "optimizationStats" to mapOf(
                "cacheSize" to trackingDataCache.size,
                "cachedPassIds" to trackingDataCache.keys.toList(),
                "totalCachedPoints" to trackingDataCache.values.sumOf { it.totalSize },
                "cacheMemoryKB" to trackingDataCache.values.sumOf { it.totalSize * 12 / 1024 },
                "optimizationEnabled" to true
            )
        )
    }

    /**
     * 데이터 길이를 계산하는 헬퍼 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @return 데이터 길이
     */
    private fun calculateDataLength(passId: Long): Int {  // ✅ UInt → Long 변경
        val passDetails = getSelectedTrackDtlByMstId(passId)
        logger.debug("전체 데이터 길이 계산: 패스 ID = $passId, 사이즈: ${passDetails.size}")
        return passDetails.size
    }

    // ✅ 디버깅 및 모니터링 메서드 추가
    fun getOptimizationStatus(): Map<String, Any> {
        return mapOf(
            "version" to "1.0-optimized",
            "cacheEnabled" to true,
            "asyncProcessingEnabled" to true,
            "performanceMonitoring" to true,
            "cacheStats" to getTrackingPerformanceStats(),
            "executorStatus" to mapOf(
                "isShutdown" to false, // ThreadManager에서 관리
                "isTerminated" to false, // ThreadManager에서 관리
                "activeCount" to if (batchExecutor != null) 1 else 0
            ),
            "memoryOptimization" to mapOf(
                "expiredCacheCleanup" to true,
                "automaticCacheEviction" to true,
                "maxCacheAge" to "1 hour"
            )
        )
    }

    /**
     * 성능 테스트 메서드 (개발/테스트용)
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     * @param iterations 반복 횟수
     * @return 성능 테스트 결과
     */
    fun performanceTest(passId: Long, iterations: Int = 100): Map<String, Any> {  // ✅ UInt → Long 변경
        logger.info("성능 테스트 시작: passId=$passId, iterations=$iterations")

        val results = mutableMapOf<String, Any>()

        // 캐시 성능 테스트
        val cacheResults = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            val cache = trackingDataCache[passId]
            if (cache != null) {
                // 캐시에서 데이터 접근 시뮬레이션
                val point = cache.trackingPoints.getOrNull(0)
            }
            val elapsed = (System.nanoTime() - start) / 1000 // 마이크로초
            cacheResults.add(elapsed)
        }

        // DB 성능 테스트
        val dbResults = mutableListOf<Long>()
        repeat(iterations) {
            val start = System.nanoTime()
            val details = getSelectedTrackDtlByMstId(passId)
            val point = details.firstOrNull()
            val elapsed = (System.nanoTime() - start) / 1000 // 마이크로초
            dbResults.add(elapsed)
        }

        results["cachePerformance"] = mapOf(
            "averageTimeUs" to cacheResults.average(),
            "minTimeUs" to (cacheResults.minOrNull() ?: 0),
            "maxTimeUs" to (cacheResults.maxOrNull() ?: 0),
            "iterations" to iterations
        )

        results["dbPerformance"] = mapOf(
            "averageTimeUs" to dbResults.average(),
            "minTimeUs" to (dbResults.minOrNull() ?: 0),
            "maxTimeUs" to (dbResults.maxOrNull() ?: 0),
            "iterations" to iterations
        )

        val speedup = dbResults.average() / cacheResults.average()
        results["speedupRatio"] = speedup
        results["performanceGain"] = "${String.format("%.1f", speedup)}x faster"

        logger.info("성능 테스트 완료: 캐시가 ${String.format("%.1f", speedup)}배 빠름")

        return results
    }

    /**
     * 추적 데이터 캐시를 비동기로 미리 로딩하는 함수
     * 
     * PassSchedule 데이터 구조 리팩토링에 따라 파라미터 타입을 UInt → Long으로 변경.
     * 
     * @param passId 전역 고유 패스 ID (Long 타입)
     */
    private fun preloadTrackingDataCache(passId: Long) {  // ✅ UInt → Long 변경
        CompletableFuture.runAsync({
            try {
                val startTime = System.nanoTime()
                val passDetails = getSelectedTrackDtlByMstId(passId)

                if (passDetails.isNotEmpty()) {
                    val trackingPoints = Array(passDetails.size) { index ->
                        val point = passDetails[index]
                        TrackingDataCache.TrackingPoint(
                            timeMs = index * 100,
                            elevation = (point["Elevation"] as Double).toFloat(),
                            azimuth = (point["Azimuth"] as Double).toFloat()
                        )
                    }

                    val cache = TrackingDataCache(
                        passId = passId,
                        trackingPoints = trackingPoints,
                        totalSize = passDetails.size
                    )

                    trackingDataCache[passId] = cache

                    val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
                    logger.info("✅ 추적 데이터 캐시 완료: passId=$passId, ${cache.totalSize}개 포인트, ${elapsedMs}ms")
                }
            } catch (e: Exception) {
                logger.error("추적 데이터 캐싱 실패: passId=$passId, ${e.message}", e)
            }
        }, batchExecutor)
    }

    /**
     * 기존 추적 준비 로직 (상태 머신으로 대체되었지만 호환성을 위해 유지)
     *
     * @deprecated 상태 머신 패턴으로 대체되었습니다. executeStateAction()을 사용하세요.
     */
    @Deprecated("상태 머신 패턴으로 대체되었습니다. executeStateAction()을 사용하세요.")
    private fun handleTrackingPreparation(nextSchedule: Map<String, Any?>?, calTime: ZonedDateTime) {
        // 이 함수는 더 이상 사용되지 않습니다. 상태 머신이 모든 로직을 처리합니다.
        logger.debug("[DEPRECATED] handleTrackingPreparation 호출됨 - 상태 머신이 처리 중")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ===== V2.0 상태 머신 구현 (신규) =====
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * V2.0 상태 머신 메인 타이머 루프
     *
     * 100ms 주기로 실행되며, 시간 기반 상태 결정을 수행합니다.
     *
     * 순서:
     * 0. 종료 중 체크 (isV2ShuttingDown)
     * 1. 진행 상태 업데이트 (하드웨어 위치 확인)
     * 2. ERROR 상태 복구 시도
     * 3. 시간 기반 상태 결정 (calTime 우선!)
     * 4. 상태 전환 시 진입 액션 실행
     * 5. 주기적 작업 (추적 데이터 전송 등)
     */
    private fun checkV2StateMachine() {
        // 0️⃣ 종료 중이면 아무 작업도 하지 않음
        if (isV2ShuttingDown) {
            return
        }

        v2CheckCount++

        val calTime = GlobalData.Time.calUtcTimeOffsetTime

        // 10초마다 상태 로깅
        if (v2CheckCount % 100L == 0L) {
            logger.info("[V2-STATE] 현재: $currentPassScheduleState, 스케줄: ${currentScheduleContext?.satelliteName}, calTime: $calTime")
        }

        // 1️⃣ 진행 상태 업데이트 (Train/Az/El 위치 확인)
        updateV2ProgressFlags(calTime)

        // 2️⃣ ERROR 상태 복구 시도
        if (currentPassScheduleState == PassScheduleState.ERROR) {
            handleV2ErrorRecovery(calTime)
            return  // ERROR 복구 중에는 다른 처리 스킵
        }

        // 3️⃣ 시간 기반 상태 결정
        val nextState = evaluateV2Transition(calTime)

        // 4️⃣ 상태 전환
        if (nextState != null && nextState != currentPassScheduleState) {
            transitionToV2(nextState, calTime)
        }

        // 5️⃣ 상태별 주기적 작업 (추적 데이터 전송 등)
        executeV2PeriodicAction(calTime)
    }

    /**
     * V2.0 진행 상태 플래그 업데이트
     *
     * 매 100ms마다 하드웨어 위치를 확인하고 컨텍스트 플래그를 갱신합니다.
     */
    private fun updateV2ProgressFlags(calTime: ZonedDateTime) {
        val ctx = currentScheduleContext ?: return

        // Train 이동 완료 체크
        if (ctx.trainMoveCommandSent && !ctx.trainMoveCompleted) {
            if (isV2TrainAtTarget(ctx.trainAngle)) {
                ctx.trainMoveCompleted = true
                ctx.trainStabilizationStartTime = calTime  // 안정화 시작
                logger.info("[V2] ✅ Train 목표 도달, 안정화 시작")
            }
        }

        // Train 안정화 완료 체크 (3초 경과)
        if (ctx.trainMoveCompleted && !ctx.trainStabilizationCompleted) {
            val stabilizationStart = ctx.trainStabilizationStartTime
            if (stabilizationStart != null) {
                val elapsed = Duration.between(stabilizationStart, calTime)
                if (elapsed.toMillis() >= V2_TRAIN_STABILIZATION_MS) {
                    ctx.trainStabilizationCompleted = true
                    logger.info("[V2] ✅ Train 안정화 완료 (3초 경과)")
                }
            }
        }

        // Az/El 이동 완료 체크
        if (ctx.azElMoveCommandSent && !ctx.azElMoveCompleted) {
            if (isV2AzElAtTarget(ctx.startAzimuth, ctx.startElevation)) {
                ctx.azElMoveCompleted = true
                logger.info("[V2] ✅ Az/El 목표 도달")
            }
        }
    }

    /**
     * V2.0 시간 기반 상태 결정 (핵심!)
     *
     * 매 100ms마다 calTime 기준으로 상태를 결정합니다.
     * 현재 내부 상태와 무관하게 시간이 우선!
     *
     * EphemerisService 참조:
     * - calTime이 추적 범위 내면 즉시 TRACKING
     * - 준비 중이라도 시간 도달하면 상태 점프
     */
    private fun determineStateByTime(calTime: ZonedDateTime): PassScheduleState {
        val ctx = currentScheduleContext ?: return PassScheduleState.COMPLETED

        val startTime = ctx.startTime
        val endTime = ctx.endTime

        // 1️⃣ 최우선: 추적 시간 범위 체크 (EphemerisService와 동일)
        val isInTrackingTime = calTime.isAfter(startTime) && calTime.isBefore(endTime)
        if (isInTrackingTime) {
            logger.info("[V2] 🎯 calTime이 추적 범위 내 → 즉시 TRACKING")
            return PassScheduleState.TRACKING
        }

        // 2️⃣ 추적 종료 체크
        if (calTime.isAfter(endTime)) {
            logger.info("[V2] ⏹️ 추적 종료 시간 경과 → POST_TRACKING")
            return PassScheduleState.POST_TRACKING
        }

        // 3️⃣ 추적 시작 전: 남은 시간으로 상태 결정
        val timeToStart = Duration.between(calTime, startTime)
        val minutesToStart = timeToStart.toMinutes()

        return when {
            minutesToStart <= 2 -> {
                // 2분 이내: PREPARING (시작 위치로 이동)
                // 내부 진행 상태에 따라 세부 상태 결정
                determinePreparingSubState()
            }
            else -> {
                // 2분 이상: WAITING (Stow 대기)
                PassScheduleState.STOWED
            }
        }
    }

    /**
     * V2.0 PREPARING 내부 세부 상태 결정
     *
     * 2분 이내일 때 Train → Az/El 순서로 진행 상태 결정
     */
    private fun determinePreparingSubState(): PassScheduleState {
        val ctx = currentScheduleContext ?: return PassScheduleState.ERROR

        return when {
            // Train 이동 완료 + 안정화 완료 + Az/El 도달
            ctx.azElMoveCompleted && isV2AzElAtTarget(ctx.startAzimuth, ctx.startElevation) -> {
                PassScheduleState.READY
            }
            // Train 이동 완료 + 안정화 완료
            ctx.trainMoveCompleted && ctx.trainStabilizationCompleted -> {
                PassScheduleState.MOVING_TO_START
            }
            // Train 이동 완료 (안정화 대기)
            ctx.trainMoveCompleted -> {
                PassScheduleState.TRAIN_STABILIZING
            }
            // Train 이동 중 또는 시작 안함
            else -> {
                PassScheduleState.MOVING_TRAIN
            }
        }
    }

    /**
     * V2.0 메인 상태 평가 함수
     *
     * 시간 기반 상태 + 현재 상태를 비교하여 전환 결정
     */
    private fun evaluateV2Transition(calTime: ZonedDateTime): PassScheduleState? {
        // IDLE 상태는 START 버튼에 의해서만 변경
        if (currentPassScheduleState == PassScheduleState.IDLE) {
            return null
        }

        // 시간 기반으로 결정된 상태
        val timeBasedState = determineStateByTime(calTime)

        // 현재 상태와 다르면 전환
        return if (timeBasedState != currentPassScheduleState) {
            timeBasedState
        } else {
            null
        }
    }

    /**
     * V2.0 다음 스케줄 평가
     *
     * 추적 완료 후 다음 스케줄을 평가하고 상태를 결정합니다.
     */
    private fun evaluateV2NextSchedule(calTime: ZonedDateTime): PassScheduleState {
        // 다음 스케줄 가져오기 (아직 종료되지 않은 것)
        val nextSchedule = scheduleContextQueue
            .filter { it.endTime.isAfter(calTime) }
            .minByOrNull { it.startTime }

        if (nextSchedule == null) {
            logger.info("[V2-SCHEDULE] 다음 스케줄 없음 → COMPLETED")
            currentScheduleContext = null
            nextScheduleContext = null
            return PassScheduleState.COMPLETED
        }

        // ⚠️ 플래그 리셋하여 새 컨텍스트로 전환
        currentScheduleContext = nextSchedule.resetFlags()
        val nextIdx = scheduleContextQueue.indexOf(nextSchedule) + 1
        nextScheduleContext = if (nextIdx < scheduleContextQueue.size) scheduleContextQueue[nextIdx] else null

        val timeToStart = Duration.between(calTime, nextSchedule.startTime)

        return if (timeToStart.toMinutes() <= 2) {
            logger.info("[V2-SCHEDULE] 다음 스케줄 2분 이내 → MOVING_TRAIN")
            PassScheduleState.MOVING_TRAIN
        } else {
            logger.info("[V2-SCHEDULE] 다음 스케줄 2분 이상 → STOWING")
            PassScheduleState.STOWING
        }
    }

    /**
     * V2.0 상태 전환 실행
     *
     * @param newState 새 상태
     * @param calTime 현재 calTime (ZonedDateTime)
     */
    private fun transitionToV2(newState: PassScheduleState, calTime: ZonedDateTime) {
        val ctx = currentScheduleContext

        logger.info("═══════════════════════════════════════════════")
        logger.info("[V2-TRANSITION] $currentPassScheduleState → $newState")
        logger.info("  - 스케줄: ${ctx?.satelliteName} (mstId: ${ctx?.mstId})")
        logger.info("  - calTime: $calTime")
        logger.info("═══════════════════════════════════════════════")

        // 이전 상태 저장
        previousPassScheduleState = currentPassScheduleState
        currentPassScheduleState = newState

        // 진입 시간 기록 (calTime 기준)
        ctx?.stateEntryTime = calTime

        // 진입 액션 실행
        executeV2EnterAction(newState, ctx, calTime)

        // 프론트엔드 상태 전송
        sendV2StateToFrontend(newState, ctx)
    }

    /**
     * V2.0 상태 진입 시 1회 실행되는 액션
     */
    private fun executeV2EnterAction(
        state: PassScheduleState,
        ctx: ScheduleTrackingContext?,
        calTime: ZonedDateTime
    ) {
        when (state) {
            PassScheduleState.STOWING -> {
                if (ctx?.stowCommandSent != true) {
                    logger.info("[V2-ACTION] Stow 명령 전송")
                    udpFwICDService.StowCommand()
                    ctx?.stowCommandSent = true
                }
            }

            PassScheduleState.STOWED -> {
                logger.info("[V2-ACTION] Stow 위치 도달, 대기 시작")
            }

            PassScheduleState.MOVING_TRAIN -> {
                if (ctx != null && !ctx.trainMoveCommandSent) {
                    val trainDeg = Math.toDegrees(ctx.trainAngle.toDouble()).toFloat()
                    logger.info("[V2-ACTION] Train 이동 명령: ${trainDeg}°")
                    // Train만 이동 (기존 moveTrainToZero 로직 활용)
                    val axisBits = BitSet(8).apply { set(2) }  // Train 축만 활성화
                    udpFwICDService.singleManualCommand(axisBits, trainDeg, 5f)
                    ctx.trainMoveCommandSent = true
                }
            }

            PassScheduleState.TRAIN_STABILIZING -> {
                ctx?.trainStabilizationStartTime = calTime
                logger.info("[V2-ACTION] Train 안정화 시작 (3초 대기)")
            }

            PassScheduleState.MOVING_TO_START -> {
                if (ctx != null && !ctx.azElMoveCommandSent) {
                    val azDeg = Math.toDegrees(ctx.startAzimuth.toDouble()).toFloat()
                    val elDeg = Math.toDegrees(ctx.startElevation.toDouble()).toFloat()
                    logger.info("[V2-ACTION] Az/El 이동 명령: Az=$azDeg°, El=$elDeg°")
                    // Az/El만 이동 (기존 moveToTargetAzEl 로직 활용)
                    val axisBits = BitSet(8).apply { set(0); set(1) }  // Az, El 축만 활성화
                    udpFwICDService.multiManualCommand(axisBits, azDeg, 5f, elDeg, 5f, 0f, 0f)
                    ctx.azElMoveCommandSent = true
                }
            }

            PassScheduleState.READY -> {
                if (ctx != null && !ctx.headerSent) {
                    logger.info("[V2-ACTION] 헤더 전송 준비 완료")
                    sendHeaderTrackingData(ctx.mstId)
                    ctx.headerSent = true
                }
            }

            PassScheduleState.TRACKING -> {
                // ⚠️ 상태 점프 대응: calTime이 추적 범위로 점프한 경우
                if (ctx != null) {
                    if (!ctx.trainMoveCompleted) {
                        logger.warn("[V2-ACTION] ⚡ 상태 점프로 인해 Train 이동 강제 완료 처리")
                        ctx.trainMoveCompleted = true
                        ctx.trainStabilizationCompleted = true
                    }
                    if (!ctx.azElMoveCompleted) {
                        logger.warn("[V2-ACTION] ⚡ 상태 점프로 인해 Az/El 이동 강제 완료 처리")
                        ctx.azElMoveCompleted = true
                    }

                    if (!ctx.initialTrackingDataSent) {
                        logger.info("[V2-ACTION] 추적 시작 - 초기 데이터 전송")
                        sendInitialTrackingData(ctx.mstId)
                        ctx.initialTrackingDataSent = true
                    }
                }
            }

            PassScheduleState.POST_TRACKING -> {
                logger.info("[V2-ACTION] 추적 종료 - 다음 스케줄 평가")
            }

            PassScheduleState.COMPLETED -> {
                logger.info("[V2-ACTION] 모든 스케줄 완료 - Stow 이동")
                udpFwICDService.StowCommand()
            }

            PassScheduleState.ERROR -> {
                logger.error("[V2-ACTION] 오류 상태 진입 - 안전을 위해 Stow로 이동")
                udpFwICDService.StowCommand()
            }

            else -> {}
        }
    }

    /**
     * V2.0 매 100ms마다 실행되는 주기적 액션
     */
    private fun executeV2PeriodicAction(calTime: ZonedDateTime) {
        when (currentPassScheduleState) {
            PassScheduleState.TRACKING -> {
                val ctx = currentScheduleContext ?: return

                // 추적 데이터 전송 (기존 로직 활용)
                val calTimeEpoch = calTime.toInstant().toEpochMilli()
                // 추적 데이터는 기존 메서드를 통해 전송됨 (이벤트 기반)
            }

            PassScheduleState.POST_TRACKING -> {
                // POST_TRACKING 상태에서 다음 스케줄 평가
                val nextState = evaluateV2NextSchedule(calTime)
                if (nextState != currentPassScheduleState) {
                    transitionToV2(nextState, calTime)
                }
            }

            else -> {}
        }
    }

    /**
     * V2.0 프론트엔드로 상태 전송
     */
    private fun sendV2StateToFrontend(state: PassScheduleState, ctx: ScheduleTrackingContext?) {
        // PushData에 상태 동기화 (기존 TRACKING_STATUS 활용)
        PushData.TRACKING_STATUS.passScheduleTrackingState = state.name

        // DataStoreService를 통해 현재/다음 추적 정보 동기화
        if (ctx != null) {
            dataStoreService.setCurrentTrackingMstId(ctx.mstId)
        } else {
            dataStoreService.clearTrackingMstIds()
        }

        logger.debug("[V2-STATE] 프론트엔드 동기화: state=$state, mstId=${ctx?.mstId}")
    }

    /**
     * V2.0 Time Offset 변경 시 호출되는 핸들러
     *
     * Time Offset이 변경되면 스케줄 큐 재평가 및 상태 재결정
     */
    fun handleV2TimeOffsetChange() {
        if (currentPassScheduleState == PassScheduleState.IDLE) {
            return
        }

        val calTime = GlobalData.Time.calUtcTimeOffsetTime

        logger.info("═══════════════════════════════════════════════")
        logger.info("[V2-TIME_OFFSET] Time Offset 변경 감지!")
        logger.info("  - 새 calTime: $calTime")
        logger.info("  - 현재 상태: $currentPassScheduleState")
        logger.info("═══════════════════════════════════════════════")

        // 스케줄 큐 재평가
        reevaluateV2ScheduleQueue(calTime)

        // 현재 상태 재결정 (시간 기반)
        val newState = determineStateByTime(calTime)
        if (newState != currentPassScheduleState) {
            logger.info("[V2-TIME_OFFSET] 상태 전환: $currentPassScheduleState → $newState")
            transitionToV2(newState, calTime)
        }
    }

    /**
     * V2.0 스케줄 큐 재평가
     */
    private fun reevaluateV2ScheduleQueue(calTime: ZonedDateTime) {
        val activeSchedules = scheduleContextQueue.filter { it.endTime.isAfter(calTime) }

        if (activeSchedules.isEmpty() && scheduleContextQueue.isNotEmpty()) {
            logger.warn("[V2-TIME_OFFSET] 모든 스케줄이 과거로 이동함")
        }

        // 현재/다음 컨텍스트 재설정
        val currentSchedule = activeSchedules
            .filter { it.startTime.isBefore(calTime) || Duration.between(calTime, it.startTime).toMinutes() <= 2 }
            .minByOrNull { it.startTime }

        if (currentSchedule != null && currentSchedule.mstId != currentScheduleContext?.mstId) {
            logger.info("[V2-TIME_OFFSET] 현재 스케줄 변경: ${currentScheduleContext?.satelliteName} → ${currentSchedule.satelliteName}")
            currentScheduleContext = currentSchedule.resetFlags()
        }
    }

    /**
     * V2.0 ERROR 상태 복구 시도
     */
    private fun handleV2ErrorRecovery(calTime: ZonedDateTime) {
        if (currentPassScheduleState != PassScheduleState.ERROR) return

        val ctx = currentScheduleContext ?: return
        val errorEntryTime = ctx.stateEntryTime ?: return
        val elapsed = Duration.between(errorEntryTime, calTime)

        // 5초 후 자동 복구 시도
        if (elapsed.seconds >= 5) {
            logger.info("[V2-ERROR_RECOVERY] 자동 복구 시도 중...")

            // 통신 상태 확인 (간단한 체크 - 데이터가 있으면 연결된 것으로 간주)
            val latestData = dataStoreService.getLatestData()
            val isCommOk = latestData.azimuthAngle != null

            if (isCommOk) {
                val recoveryState = determineStateByTime(calTime)
                logger.info("[V2-ERROR_RECOVERY] 복구 성공, $recoveryState 상태로 전환")
                transitionToV2(recoveryState, calTime)
            } else if (elapsed.seconds >= 30) {
                logger.error("[V2-ERROR_RECOVERY] 30초 동안 복구 실패, IDLE로 전환")
                stopV2ScheduleTracking()
            }
        }
    }

    /**
     * V2.0 안전한 일괄 종료
     */
    private fun safeV2BatchShutdown() {
        logger.info("[V2-SHUTDOWN] 일괄 종료 시작")

        try {
            isV2ShuttingDown = true

            // Stow 명령 전송
            udpFwICDService.StowCommand()

            logger.info("[V2-SHUTDOWN] 일괄 종료 완료, Stow 이동 시작")
        } catch (e: Exception) {
            logger.error("[V2-SHUTDOWN] 일괄 종료 중 오류: ${e.message}", e)
            try {
                udpFwICDService.StowCommand()
            } catch (stowError: Exception) {
                logger.error("[V2-SHUTDOWN] Stow 명령 실패: ${stowError.message}", stowError)
            }
        } finally {
            isV2ShuttingDown = false
        }
    }

    // ===== V2.0 위치 판정 함수 =====

    /**
     * V2.0 Train 위치 확인
     *
     * 기존 isTrainAtZero/isTrainStabilized와 유사하게 dataStoreService 사용
     */
    private fun isV2TrainAtTarget(targetTrain: Float): Boolean {
        val currentTrain = dataStoreService.getLatestData().trainAngle?.toFloat() ?: return false
        val targetTrainDeg = Math.toDegrees(targetTrain.toDouble()).toFloat()
        return kotlin.math.abs(currentTrain - targetTrainDeg) <= 0.1f
    }

    /**
     * V2.0 Az/El 위치 확인
     *
     * dataStoreService를 통해 현재 위치 확인
     */
    private fun isV2AzElAtTarget(targetAz: Float, targetEl: Float): Boolean {
        val latestData = dataStoreService.getLatestData()
        val currentAz = latestData.azimuthAngle ?: return false
        val currentEl = latestData.elevationAngle ?: return false

        val targetAzDeg = Math.toDegrees(targetAz.toDouble()).toFloat()
        val targetElDeg = Math.toDegrees(targetEl.toDouble()).toFloat()

        val azDiff = kotlin.math.abs(currentAz - targetAzDeg)
        val elDiff = kotlin.math.abs(currentEl - targetElDeg)

        return azDiff <= 0.1f && elDiff <= 0.1f
    }

    // ===== V2.0 공개 API =====

    /**
     * V2.0 스케줄 추적 시작
     *
     * @return 시작 성공 여부
     */
    fun startV2ScheduleTracking(): Mono<Boolean> {
        return Mono.fromCallable {
            try {
                logger.info("════════════════════════════════════════")
                logger.info("[V2-START] 스케줄 추적 시작")
                logger.info("════════════════════════════════════════")

                val calTime = GlobalData.Time.calUtcTimeOffsetTime

                // 1. 스케줄 큐 생성
                scheduleContextQueue.clear()
                val allContexts = buildV2ScheduleQueue(calTime)
                scheduleContextQueue.addAll(allContexts)

                if (scheduleContextQueue.isEmpty()) {
                    logger.warn("[V2-START] 추적 가능한 스케줄 없음")
                    return@fromCallable false
                }

                logger.info("[V2-START] ${scheduleContextQueue.size}개 스케줄 로드됨")
                scheduleContextQueue.forEach { ctx ->
                    logger.info("  - ${ctx.satelliteName}: ${ctx.startTime} ~ ${ctx.endTime}")
                }

                // 2. 첫 스케줄 선택
                val firstSchedule = scheduleContextQueue.first()
                currentScheduleContext = firstSchedule
                nextScheduleContext = scheduleContextQueue.getOrNull(1)

                // 3. 초기 상태 결정
                val timeToStart = Duration.between(calTime, firstSchedule.startTime)
                val initialState = if (timeToStart.toMinutes() <= 2) {
                    PassScheduleState.MOVING_TRAIN
                } else {
                    PassScheduleState.STOWING
                }

                // 4. 상태 전환
                transitionToV2(initialState, calTime)

                // 5. v2 활성화
                useV2StateMachine = true

                true
            } catch (e: Exception) {
                logger.error("[V2-START] 시작 실패: ${e.message}", e)
                false
            }
        }
    }

    /**
     * V2.0 스케줄 추적 정지
     *
     * @return 정지 성공 여부
     */
    fun stopV2ScheduleTracking(): Mono<Boolean> {
        return Mono.fromCallable {
            try {
                logger.info("════════════════════════════════════════")
                logger.info("[V2-STOP] 스케줄 추적 정지")
                logger.info("════════════════════════════════════════")

                // 1. 안전한 일괄 종료
                safeV2BatchShutdown()

                // 2. 상태 초기화
                currentPassScheduleState = PassScheduleState.IDLE
                previousPassScheduleState = PassScheduleState.IDLE

                // 3. 컨텍스트 초기화
                currentScheduleContext = null
                nextScheduleContext = null
                scheduleContextQueue.clear()

                // 4. v2 비활성화
                useV2StateMachine = false

                // 5. 프론트엔드 알림
                sendV2StateToFrontend(PassScheduleState.IDLE, null)

                true
            } catch (e: Exception) {
                logger.error("[V2-STOP] 정지 실패: ${e.message}", e)
                false
            }
        }
    }

    /**
     * V2.0 스케줄 컨텍스트 큐 생성
     *
     * 선택된 스케줄 데이터를 ScheduleTrackingContext로 변환합니다.
     */
    private fun buildV2ScheduleQueue(calTime: ZonedDateTime): List<ScheduleTrackingContext> {
        // getAllSelectedTrackMst()는 Map<String, List<Map<String, Any?>>> 반환
        // 모든 위성의 스케줄을 평탄화하여 필터링
        val allSchedules = getAllSelectedTrackMst().values.flatten()

        val selectedSchedules = allSchedules
            .filter { schedule ->
                val endTime = schedule["EndTime"] as? ZonedDateTime
                endTime?.isAfter(calTime) == true
            }
            .sortedBy { it["StartTime"] as? ZonedDateTime }

        return selectedSchedules.mapNotNull { schedule ->
            try {
                val mstId = (schedule["MstId"] as? Number)?.toLong() ?: return@mapNotNull null
                val detailId = (schedule["DetailId"] as? Number)?.toInt() ?: 0
                val satelliteName = schedule["SatelliteName"] as? String ?: "Unknown"
                val startTime = schedule["StartTime"] as? ZonedDateTime ?: return@mapNotNull null
                val endTime = schedule["EndTime"] as? ZonedDateTime ?: return@mapNotNull null

                // 첫 번째 추적 포인트에서 시작 위치 정보 가져오기
                val trackingDetails = getSelectedTrackDtlByMstId(mstId)
                val firstPoint = trackingDetails.firstOrNull()

                val startAzimuth = (firstPoint?.get("Azimuth") as? Number)?.toFloat() ?: 0f
                val startElevation = (firstPoint?.get("Elevation") as? Number)?.toFloat() ?: 0f
                val trainAngle = (firstPoint?.get("TrainAngle") as? Number)?.toFloat() ?: 0f

                ScheduleTrackingContext(
                    mstId = mstId,
                    detailId = detailId,
                    satelliteName = satelliteName,
                    startTime = startTime,
                    endTime = endTime,
                    startAzimuth = startAzimuth,
                    startElevation = startElevation,
                    trainAngle = trainAngle
                )
            } catch (e: Exception) {
                logger.error("[V2] 스케줄 컨텍스트 생성 실패: ${e.message}")
                null
            }
        }
    }

    /**
     * V2.0 현재 상태 조회
     */
    fun getV2CurrentState(): PassScheduleState = currentPassScheduleState

    /**
     * V2.0 현재 컨텍스트 조회
     */
    fun getV2CurrentContext(): ScheduleTrackingContext? = currentScheduleContext
}