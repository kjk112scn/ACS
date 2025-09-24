package com.gtlsystems.acs_api.service.udp

import com.gtlsystems.acs_api.config.ThreadManager
import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.model.SystemInfo
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.icd.ICDService
import com.gtlsystems.acs_api.util.JKUtil
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.Duration
import java.util.BitSet
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class UdpFwICDService(
    private val dataStoreService: DataStoreService,
    private val environment: Environment,
    private val eventBus: ACSEventBus,
    private val threadManager: ThreadManager // ✅ 통합 쓰레드 관리자 주입
) {

    private val logger = LoggerFactory.getLogger(UdpFwICDService::class.java)
    private val icdService = ICDService.Classify(dataStoreService, eventBus)
    private val trackingStatus = PushData.TRACKING_STATUS

    // UDP 채널 및 버퍼
    private lateinit var channel: DatagramChannel
    private val receiveBuffer = ByteBuffer.allocate(512)
    private var readData: PushData.ReadData = PushData.ReadData()

    // Stow Command 실행 중인지 추적하기 위한 변수
    private var stowCommandDisposable: Disposable? = null

    // 프로퍼티 값들
    @Value("\${firmware.udp.ip:127.0.0.1}")
    private lateinit var firmwareIp: String

    @Value("\${firmware.udp.port:8080}")
    private var firmwarePort: Int = 0

    @Value("\${server.udp.ip:127.0.0.1}")
    private lateinit var serverIp: String

    @Value("\${server.udp.port:8081}")
    private var serverPort: Int = 0

    var firmwareAddress = InetSocketAddress("127.0.0.1", 8080)

    // Kotlin 방식 (동일한 효과)
    /** 설정 변경 로그 메시지 실제 메시지: "설정이 변경되었습니다: {0} ({1} → {2})" */
    private var realtimeExecutor: ScheduledExecutorService? = null

    // 통신 상태 관리
    private val isUdpRunning = AtomicBoolean(false)
    private val sendCount = AtomicLong(0)
    private val receiveCount = AtomicLong(0)

    @PostConstruct
    fun init() {
        logger.info("UDP 통신 서비스 초기화 시작")
        initializeUdpChannel()
    }

    // BitSet helper for axis selection (0: Azimuth, 1: Elevation, 2: train)
    private fun bitsetOf(index: Int): BitSet {
        val bs = BitSet(3)
        bs.set(index)
        return bs
    }

    /** UDP 채널 초기화 및 실시간 통신 시작 */
    private fun initializeUdpChannel() {
        try {
            // 프로퍼티 값 설정
            firmwareIp = environment.getProperty("firmware.udp.ip") ?: "127.0.0.1"
            firmwarePort = environment.getProperty("firmware.udp.port")?.toInt() ?: 8080
            serverIp = environment.getProperty("server.udp.ip") ?: "127.0.0.1"
            serverPort = environment.getProperty("server.udp.port")?.toInt() ?: 8081
            firmwareAddress = InetSocketAddress(firmwareIp, firmwarePort)

            // UDP 채널 설정
            channel = DatagramChannel.open()
            val serverAddress = InetSocketAddress(serverIp, serverPort)
            channel.bind(serverAddress)
            channel.configureBlocking(false)

            logger.info("UDP 채널 초기화 완료: {}:{}", serverIp, serverPort)
            logger.info("펌웨어 주소: {}:{}", firmwareIp, firmwarePort)

            // 실시간 통신 시작
            startRealtimeCommunication()
        } catch (e: Exception) {
            logger.error("UDP 초기화 실패: {}", e.message, e)
            scheduleReconnection()
        }
    }

    /** ✅ 실시간 UDP 통신 시작 (통합 쓰레드 관리자 사용) */
    private fun startRealtimeCommunication() {
        if (isUdpRunning.compareAndSet(false, true)) {
            logger.info("실시간 UDP 통신 시작")
            logger.debug("Send 간격: 30ms, Receive 간격: 10ms")

            // ✅ 통합 실시간 실행기 사용
            realtimeExecutor = threadManager.getRealtimeExecutor()

            // ✅ ThreadManager가 null인 경우 대체 타이머 생성
            if (realtimeExecutor == null) {
                logger.warn("⚠️ ThreadManager의 realtimeExecutor가 null입니다. 대체 타이머를 생성합니다.")
                realtimeExecutor =
                    Executors.newScheduledThreadPool(2) { r ->
                        Thread(r, "udp-fallback").apply {
                            priority = Thread.MAX_PRIORITY
                            isDaemon = true
                        }
                    }
            }

            // ✅ UDP Receive (안정성 보장, 10ms 간격)
            realtimeExecutor?.scheduleAtFixedRate(
                {
                    try {
                        val startTime = System.nanoTime()
                        receiveUdpData()
                        receiveCount.incrementAndGet()

                        // ✅ 안정성 우선 모니터링
                        val processingTime = (System.nanoTime() - startTime) / 1_000_000
                        if (processingTime > 15) { // 15ms 임계값으로 안정성 보장
                            logger.warn(
                                "⚠️ UDP Receive 지연 감지: {}ms (임계값: 15ms)",
                                processingTime
                            )
                        }
                    } catch (e: Exception) {
                        logger.debug("UDP Receive 오류: {}", e.message)
                    }
                },
                0,
                10,
                TimeUnit.MILLISECONDS
            ) // 10ms로 안정성 보장

            // ✅ UDP Send (안정성 보장, 30ms 간격) - 디버깅 로그 추가
            realtimeExecutor?.scheduleAtFixedRate(
                {
                    try {
                        val startTime = System.nanoTime()
                        logger.debug("🔄 UDP Send 명령 실행 중... (카운트: {})", sendCount.get())
                        sendReadStatusCommand()
                        sendCount.incrementAndGet()

                        // ✅ 안정성 우선 모니터링
                        val processingTime = (System.nanoTime() - startTime) / 1_000_000
                        if (processingTime > 25) { // 10ms 임계값으로 안정성 보장
                            logger.warn("⚠️ UDP Send 지연 감지: {}ms (임계값: 25ms)", processingTime)
                        }
                    } catch (e: Exception) {
                        logger.error("❌ UDP Send 오류: {}", e.message, e)
                    }
                },
                0,
                30,
                TimeUnit.MILLISECONDS
            ) // 30ms로 안정성 보장

            logger.info(
                "✅ 실시간 UDP 통신 시작 완료 (Send 카운트: {}, Receive 카운트: {})",
                sendCount.get(),
                receiveCount.get()
            )
        }
    }

    /** 연결 재시도 스케줄링 */
    private fun scheduleReconnection() {
        Mono.delay(Duration.ofSeconds(5)).subscribeOn(Schedulers.boundedElastic()).subscribe {
            logger.info("UDP 연결 재시도 중...")
            initializeUdpChannel()
        }
    }

    /** 실시간 UDP 데이터 수신 (논블로킹) */
    private fun receiveUdpData() {
        try {
            receiveBuffer.clear()
            val address = channel.receive(receiveBuffer)
            if (address != null) {
                receiveBuffer.flip()
                val receivedData = ByteArray(receiveBuffer.remaining())
                receiveBuffer.get(receivedData)
                processICDData(receivedData)
            }
        } catch (e: Exception) {
            // 논블로킹이므로 데이터가 없을 때는 정상
        }
    }

    // === 단순한 Mono 비동기 방식 명령 메서드들 ===
    /** 2.1 Default Info 기본 정보 명령 - Mono 비동기 처리 */
    fun defaultInfoCommand() {
        Mono.fromCallable {
            val utcTime = GlobalData.Time.utcNow
            val setDataFrameInstance =
                ICDService.DefaultInfo.SetDataFrame(
                    cmd = 'W',
                    year = utcTime.year.toUShort(),
                    month = utcTime.month.value.toByte(),
                    day = utcTime.dayOfMonth.toByte(),
                    hour = utcTime.hour.toByte(),
                    minute = utcTime.minute.toByte(),
                    second = utcTime.second.toByte(),
                    ms = (utcTime.nano / 1000000).toUShort(),
                    timeOffset = GlobalData.Offset.TimeOffset,
                    azimuthOffset = GlobalData.Offset.azimuthPositionOffset,
                    elevationOffset = GlobalData.Offset.elevationPositionOffset,
                    trainOffset = GlobalData.Offset.trainPositionOffset,
                    crc16 = 0u
                )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("DefaultInfo 명령 전송 완료")
            logger.debug(
                "DefaultInfo 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("기본 정보 명령 처리 오류: {}", error.message, error) }
            )
    }

    /** 2.2 Read Status 주기적 상태 요청 전송 - 디버깅 로그 추가 */
    private fun sendReadStatusCommand() {
        try {
            logger.debug("📤 Read Status 명령 전송 시작...")
            val setDataFrameInstance = ICDService.ReadStatus.SetDataFrame()
            val dataToSend = setDataFrameInstance.setDataFrame()

            logger.debug(
                "📤 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
            logger.debug("📤 펌웨어 주소: {}", firmwareAddress)

            val bytesSent = channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
            logger.debug("📤 전송 완료: {} bytes", bytesSent)
        } catch (e: Exception) {
            logger.error("❌ Read Status 명령 전송 실패: {}", e.message, e)
        }
    }

    /** 수신 데이터 처리 */
    private fun processICDData(receivedData: ByteArray) {
        try {
            icdService.receivedCmd(receivedData)
        } catch (e: Exception) {
            logger.error("ICD 데이터 처리 오류: {}", e.message, e)
        }
    }

    /** 2.5 Write NTP Info */
    fun writeNTPCommand() {
        Mono.fromCallable {
            val Time = GlobalData.Time.utcNow
            val setDataFrameInstance =
                ICDService.WriteNTP.SetDataFrame(
                    cmd = 'I',
                    year = Time.year.toUShort(),
                    month = Time.month.value.toByte(),
                    day = Time.dayOfMonth.toByte(),
                    hour = Time.hour.toByte(),
                    minute = Time.minute.toByte(),
                    second = Time.second.toByte(),
                    ms = (Time.nano / 1000000).toUShort(),
                    timeOffset = GlobalData.Offset.TimeOffset,
                    crc16 = 0u
                )
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("writeNTPCommand 명령 전송 완료")
            logger.debug(
                "writeNTPCommand 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("기본 정보 명령 처리 오류: {}", error.message, error) }
            )
    }

    /** 2.6 ACU S/W Emergency Command 비상 명령 - Mono 비동기 처리 */
    fun onEmergencyCommand(commandChar: Char) {
        Mono.fromCallable {
            val cmdOnOffValue =
                when (commandChar) {
                    'E' -> {
                        logger.info("비상 모드 활성화 요청")
                        true
                    }

                    'S' -> {
                        logger.info("비상 모드 비활성화 요청")
                        false
                    }

                    else -> {
                        logger.error("유효하지 않은 비상 명령 문자: {}", commandChar)
                        throw IllegalArgumentException("유효하지 않은 명령 문자: $commandChar")
                    }
                }

            val setDataFrameInstance =
                ICDService.Emergency.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'E',
                    cmdOnOff = cmdOnOffValue,
                    crc16 = 0u,
                    etx = 0x03
                )
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("Emergency 명령 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug(
                "Emergency 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("비상 명령 처리 오류: {}", error.message, error) }
            )
    }

    /** 2.7 Manual Controls Command(1-Axis) 단일 축 수동 제어 명령 - Mono 비동기 처리 */
    fun singleManualCommand(singleAxis: BitSet, angle: Float, speed: Float) {
        Mono.fromCallable {
            val setDataFrameInstance =
                ICDService.SingleManualControl.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'M',
                    axis = singleAxis,
                    axisAngle = angle,
                    axisSpeed = speed,
                    crc16 = 0u,
                    etx = 0x03
                )

            val dataToSend = setDataFrameInstance.setDataFrame()

            PushData.CMD.apply {
                when {
                    singleAxis.get(0) -> { // Azimuth (0x01)
                        // cmdAzimuthAngle = angle
                    }

                    singleAxis.get(1) -> { // Elevation (0x02)
                        // cmdElevationAngle = angle
                    }

                    singleAxis.get(2) -> { // Train (0x04)
                        // cmdTrainAngle = angle
                    }
                }
            }

            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            val axisStr =
                when {
                    singleAxis.get(0) -> "Azimuth"
                    singleAxis.get(1) -> "Elevation"
                    singleAxis.get(2) -> "Train"
                    else -> "Unknown"
                }

            logger.info("단일 축 수동 제어 명령 전송 완료: {} - 각도: {}°, 속도: {}", axisStr, angle, speed)
            logger.debug(
                "단일 축 제어 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("단일 축 수동 제어 명령 처리 오류: {}", error.message, error) }
            )
    }

    /** 2.8 Manual Control(Multi-Axis) 수동 제어 명령 - Mono 비동기 처리 */
    fun multiManualCommand(
        multiAxis: BitSet,
        azAngle: Float,
        azSpeed: Float,
        elAngle: Float,
        elSpeed: Float,
        trainAngle: Float,
        trainSpeed: Float
    ) {
        Mono.fromCallable {
            val setDataFrameInstance =
                ICDService.MultiManualControl.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'A',
                    axis = multiAxis,
                    azimuthAngle = azAngle,
                    azimuthSpeed = azSpeed,
                    elevationAngle = elAngle,
                    elevationSpeed = elSpeed,
                    trainAngle = trainAngle,
                    trainSpeed = trainSpeed,
                    crc16 = 0u,
                    etx = 0x03
                )

            val dataToSend = setDataFrameInstance.setDataFrame()
            PushData.CMD.apply {
                cmdAzimuthAngle = azAngle + GlobalData.Offset.azimuthPositionOffset
                cmdElevationAngle = elAngle + GlobalData.Offset.elevationPositionOffset
                cmdTrainAngle =
                    trainAngle +
                            GlobalData.Offset.trainPositionOffset +
                            GlobalData.Offset.trueNorthOffset
            }
            /*
            if(PushData.TRACKING_STATUS.sunTrackTrackingState == "TRACKING") {
                PushData.CMD.apply {
                    cmdAzimuthAngle = azAngle + GlobalData.Offset.azimuthPositionOffset
                    cmdElevationAngle = elAngle + GlobalData.Offset.elevationPositionOffset
                    cmdTrainAngle = trainAngle + GlobalData.Offset.tiltPositionOffset + GlobalData.Offset.trueNorthOffset
                }
            }
            else{
                PushData.CMD.apply {
                    cmdAzimuthAngle = azAngle + GlobalData.Offset.azimuthPositionOffset
                    cmdElevationAngle = elAngle + GlobalData.Offset.elevationPositionOffset
                    cmdTrainAngle = trainAngle + GlobalData.Offset.tiltPositionOffset + GlobalData.Offset.trueNorthOffset
                }
            }
            */
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info(
                "Manual 제어 명령 전송 완료: Az={}°, El={}°, Train={}°",
                azAngle,
                elAngle,
                trainAngle
            )
            logger.debug(
                "Manual 제어 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("수동 제어 명령 처리 오류: {}", error.message, error) }
            )
    }

    /** 2.9 Stop Command 정지 명령 - Mono 비동기 처리 */
    fun stopCommand(bitStop: BitSet) {
        Mono.fromCallable {
            // 진행 중인 Stow Command 중단
            if (stowCommandDisposable != null && !stowCommandDisposable!!.isDisposed) {
                stowCommandDisposable!!.dispose()
                stowCommandDisposable = null
                logger.info("StowCommand 중단됨: stop 명령 실행")
            }

            val setDataFrameInstance =
                ICDService.Stop.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'S',
                    axis = bitStop,
                    crc16 = 0u,
                    etx = 0x03
                )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            // 모든 추적 중지
            // stopAllCommand()

            logger.info("Stop 명령 전송 완료")
            logger.debug(
                "Stop 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("정지 명령 처리 오류: {}", error.message, error) }
            )
    }

    /**
     * 2.10 Standby
     * 대기 명령 - Mono 비동기 처리
     */
    fun standbyCommand(bitStandby: BitSet) {
        Mono.fromCallable {
            val setDataFrameInstance = ICDService.Standby.SetDataFrame(
                stx = 0x02,
                cmdOne = 'B',
                axis = bitStandby,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("Standby 명령 전송 완료")
            logger.debug("Standby 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("대기 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 2.11 Feed On/Off
     * 피드 On/Off 명령 - Mono 비동기 처리
     **/
    fun feedOnOffCommand(bitFeedOnOff: BitSet) {
        Mono.fromCallable {
            val setDataFrameInstance =
                ICDService.FeedOnOff.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'F',
                    feedOnOff = bitFeedOnOff,
                    crc16 = 0u,
                    etx = 0x03
                )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("FeedOnOff 명령 전송 완료")
            logger.debug(
                "FeedOnOff 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("피드 On/Off 명령 처리 오류: {}", error.message, error) }
            )
    }
    // === 위성 추적 관련 메서드들 ===
    /** 12.1 위성 추적 헤더 정보 전송 */
    fun sendSatelliteTrackHeader(headerFrame: ICDService.SatelliteTrackOne.SetDataFrame) {
        try {
            val dataToSend = headerFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("위성 추적 헤더 정보 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug(
                "위성 추적 헤더 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        } catch (e: Exception) {
            logger.error("위성 추적 헤더 정보 전송 오류: {}", e.message, e)
            throw e
        }
    }

    /** 12.2 위성 추적 초기 제어 명령 전송 */
    fun sendSatelliteTrackInitialControl(controlFrame: ICDService.SatelliteTrackTwo.SetDataFrame) {
        try {
            val dataToSend = controlFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("위성 추적 초기 제어 명령 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug(
                "위성 추적 초기 제어 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        } catch (e: Exception) {
            logger.error("위성 추적 초기 제어 명령 전송 오류: {}", e.message, e)
            throw e
        }
    }

    /** 12.3 위성 추적 추가 데이터 전송 */
    fun sendSatelliteTrackAdditionalData(dataFrame: ICDService.SatelliteTrackThree.SetDataFrame) {
        try {
            val dataToSend = dataFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("위성 추적 추가 데이터 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug(
                "위성 추적 추가 데이터 전송: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        } catch (e: Exception) {
            logger.error("위성 추적 추가 데이터 전송 오류: {}", e.message, e)
            throw e
        }
    }

    /** 2.13 Time Offset 시간 오프셋 명령 - Mono 비동기 처리 */
    fun timeOffsetCommand(inputTimeOffset: Float) {
        Mono.fromCallable {
            val localTime = GlobalData.Time.utcNow
            val setDataFrameInstance =
                ICDService.TimeOffset.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'O',
                    cmdTwo = 'T',
                    year = localTime.year.toUShort(),
                    month = localTime.month.value.toByte(),
                    day = localTime.dayOfMonth.toByte(),
                    hour = localTime.hour.toByte(),
                    minute = localTime.minute.toByte(),
                    second = localTime.second.toByte(),
                    ms = (localTime.nano / 1000000).toUShort(),
                    timeOffset = inputTimeOffset,
                    crc16 = 0u,
                    etx = 0x03
                )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            // 글로벌 데이터 업데이트
            GlobalData.Offset.TimeOffset = inputTimeOffset

            logger.info("TimeOffset 명령 전송 완료: {}s", inputTimeOffset)
            logger.debug(
                "TimeOffset 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("시간 오프셋 명령 처리 오류: {}", error.message, error) }
            )
    }

    /**
     * 2.14 Position Offset
     * 위치 오프셋 명령 - Mono 비동기 처리 
     **/
    fun positionOffsetCommand(azOffset: Float, elOffset: Float, trainOffset: Float) {
        Mono.fromCallable {
            val setDataFrameInstance =
                ICDService.PositionOffset.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'O',
                    cmdTwo = 'P',
                    azimuthOffset = azOffset,
                    elevationOffset = elOffset,
                    trainOffset = trainOffset,
                    crc16 = 0u,
                    etx = 0x03
                )

            // OP 프레임 전송 및 GlobalData 갱신
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            // 변경 여부 판단 (현재 GlobalData 값과 비교)
            val azChanged = azOffset != GlobalData.Offset.azimuthPositionOffset
            val elChanged = elOffset != GlobalData.Offset.elevationPositionOffset
            val trainChanged = trainOffset != GlobalData.Offset.trainPositionOffset

            // 오프셋 갱신
            GlobalData.Offset.azimuthPositionOffset = azOffset
            GlobalData.Offset.elevationPositionOffset = elOffset
            GlobalData.Offset.trainPositionOffset = trainOffset
            // 의미 있는 조건 변수로 분리 추적 여부 확인.
            val isAnyModeOn =
                trackingStatus.ephemerisStatus == true ||
                        trackingStatus.passScheduleStatus == true ||
                        trackingStatus.geostationaryStatus == true ||
                        trackingStatus.sunTrackStatus == true

            val isTracking =
                trackingStatus.ephemerisTrackingState == "TRACKING" ||
                        trackingStatus.sunTrackTrackingState == "TRACKING"

            // TRACKING 상태라면 OFFSET 수행 시 수동 제어 실시
            // 추적 중이라면 펌웨어에서 OFFSET만 적용되도록 수정 수정제어안함.
            val isNotTracking =
                trackingStatus.ephemerisTrackingState != "TRACKING" &&
                        trackingStatus.sunTrackTrackingState != "TRACKING"

            if (isAnyModeOn && isTracking) {
                var angle = 0f
                if (trackingStatus.ephemerisStatus == true && trainChanged) {
                    angle = GlobalData.EphemerisTrakingAngle.trainAngle
                    singleManualCommand(bitsetOf(2), angle, 5f)
                    PushData.CMD.cmdTrainAngle =
                        angle +
                                GlobalData.Offset.trainPositionOffset +
                                GlobalData.Offset.trueNorthOffset
                } else if (trackingStatus.sunTrackStatus == true && trainChanged) {
                    angle = GlobalData.SunTrackingData.trainAngle
                    singleManualCommand(bitsetOf(2), angle, 5f)
                    PushData.CMD.cmdTrainAngle =
                        angle +
                                GlobalData.Offset.trainPositionOffset +
                                GlobalData.Offset.trueNorthOffset
                }
            }

            // 조건 충족 시 변경된 축만 이동 + 표시값 Offset 반영
            if (isAnyModeOn && isNotTracking) {
                if (azChanged) {
                    val angle = GlobalData.EphemerisTrakingAngle.azimuthAngle
                    singleManualCommand(bitsetOf(0), angle, 5f)
                    PushData.CMD.cmdAzimuthAngle =
                        angle + GlobalData.Offset.azimuthPositionOffset
                }
                if (elChanged) {
                    val angle = GlobalData.EphemerisTrakingAngle.elevationAngle
                    singleManualCommand(bitsetOf(1), angle, 5f)
                    PushData.CMD.cmdElevationAngle =
                        angle + GlobalData.Offset.elevationPositionOffset
                }
                if (trainChanged) {
                    val angle = GlobalData.EphemerisTrakingAngle.trainAngle
                    singleManualCommand(bitsetOf(2), angle, 5f)
                    PushData.CMD.cmdTrainAngle =
                        angle +
                                GlobalData.Offset.trainPositionOffset +
                                GlobalData.Offset.trueNorthOffset
                }
            }

            logger.info(
                "PositionOffset 명령 전송 완료: Az={}°, El={}°, Ti={}°",
                azOffset,
                elOffset,
                trainOffset
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("위치 오프셋 명령 처리 오류: {}", error.message, error) }
            )
    }


    fun servoPresetCommand(bitStop: BitSet) {
        Mono.fromCallable {
            // 진행 중인 Stow Command 중단
            if (stowCommandDisposable != null && !stowCommandDisposable!!.isDisposed) {
                stowCommandDisposable!!.dispose()
                stowCommandDisposable = null
                logger.info("StowCommand 중단됨: servoPreset 명령 실행")
            }

            val setDataFrameInstance =
                ICDService.ServoEncoderPreset.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'P',
                    cmdTwo = 'P',
                    axis = bitStop,
                    crc16 = 0u,
                    etx = 0x03
                )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("ServoPreset 명령 전송 완료")
            logger.debug(
                "ServoPreset 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            { /* 성공 */ },
            { error -> logger.error("서보 프리셋 명령 처리 오류: {}", error.message, error) }
        )
    }

    /** 모든 추적 중지 (이벤트 발행) */
    fun stopAllCommand() {
        try {
            eventBus.publish(ACSEvent.TrackingEvent.StopAllTracking)
            logger.info("모든 추적 중지 이벤트 발행됨")
        } catch (e: Exception) {
            logger.error("stopAllCommand 오류: {}", e.message, e)
        }
    }

    // === Stow Command 구현 (단순한 Mono 방식) ===

    /**
     * Stow 명령 - 단순한 Mono 비동기 처리
     */
    fun StowCommand() {
        // 기존 Stow Command 중단
        stowCommandDisposable?.dispose()
        stopAllCommand()

        val stowTrainAngle = 0.0f
        val stowTrainSpeed = 5.0f
        val stowAzimuthAngle = 0.0f
        val stowAzimuthSpeed = 5.0f
        val stowElevationAngle = 90.0f
        val stowElevationSpeed = 5.0f

        logger.info("Stow 명령 시작")

        // 1단계: 틸트 축 제어
        val trainAxis = BitSet().apply {
            set(2) // 틸트 축
            set(7) // STOW 비트
        }

        Mono.fromCallable {
            PushData.CMD.cmdTrainAngle = stowTrainAngle
            stowTrainCommand(trainAxis, stowTrainAngle, stowTrainSpeed)
            logger.info("Stow 1단계: 틸트 축 제어 명령 전송 완료")
        }
            .subscribeOn(Schedulers.boundedElastic())
            .delayElement(Duration.ofMillis(100)) // 명령 전송 후 잠시 대기
            .flatMap {
                // 2단계: 틸트 안정화 대기
                logger.info("Stow 2단계: 틸트 안정화 대기 시작 (목표: {}°)", stowTrainAngle)
                waitForTrainStabilization(stowTrainAngle)
            }
            .flatMap {
                // 3단계: 방위각/고도각 제어
                logger.info("Stow 3단계: 방위각/고도각 제어 시작")
                val azElAxis = BitSet().apply {
                    set(0) // 방위각 축
                    set(1) // 고도각 축
                    set(7) // STOW 비트
                }

                Mono.fromCallable {
                    PushData.CMD.cmdAzimuthAngle = stowAzimuthAngle
                    PushData.CMD.cmdElevationAngle = stowElevationAngle
                    stowAzElCommand(
                        azElAxis,
                        stowAzimuthAngle, stowAzimuthSpeed,
                        stowElevationAngle, stowElevationSpeed
                    )
                    logger.info("Stow 방위각/고도각 제어 명령 전송 완료")
                }
                    .subscribeOn(Schedulers.boundedElastic())
            }
            .subscribe(
                {
                    logger.info("Stow 명령 완료")
                },
                { error ->
                    logger.error("Stow 명령 실패: {}", error.message, error)
                }
            )
    }

    /** 틸트 안정화 대기 - 단순한 Mono 방식 */
    private fun waitForTrainStabilization(targetAngle: Float): Mono<String> {
        return Mono.create { sink ->
            val startTime = System.currentTimeMillis()
            val maxWaitTime = 30000L // 30초 최대 대기

            val checkStabilization =
                object : Runnable {
                    override fun run() {
                        try {
                            val currentTime = System.currentTimeMillis()
                            val elapsedTime = currentTime - startTime

                            if (elapsedTime > maxWaitTime) {
                                logger.warn("틸트 안정화 타임아웃 (30초)")
                                sink.error(RuntimeException("틸트 안정화 타임아웃"))
                                return
                            }

                            // ✅ 변경: readData 대신 getCurrentReadData() 사용
                            val currentAngle = getCurrentReadData().trainAngle ?: 0.0f
                            val isStable = Math.abs(currentAngle - targetAngle) <= 0.1f

                            if (isStable) {
                                logger.info(
                                    "틸트 안정화 완료: 현재={}°, 목표={}°",
                                    currentAngle,
                                    targetAngle
                                )
                                sink.success("stabilized")
                            } else {
                                logger.debug(
                                    "틸트 안정화 중: 현재={}°, 목표={}°, 차이={}°",
                                    currentAngle,
                                    targetAngle,
                                    Math.abs(currentAngle - targetAngle)
                                )

                                // 100ms 후 다시 체크
                                Mono.delay(Duration.ofMillis(100))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe { this.run() }
                            }
                        } catch (e: Exception) {
                            logger.error("틸트 안정화 체크 중 오류: {}", e.message, e)
                            sink.error(e)
                        }
                    }
                }

            // 첫 번째 체크 시작
            checkStabilization.run()
        }
    }

    /** 틸트 축 제어 명령 (Stow용) */
    private fun stowTrainCommand(multiAxis: BitSet, trainAngle: Float, trainSpeed: Float) {
        try {
            // ✅ 변경: readData 대신 getCurrentReadData() 사용
            val currentData = getCurrentReadData()

            val setDataFrameInstance =
                ICDService.MultiManualControl.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'A',
                    axis = multiAxis,
                    azimuthAngle = currentData.azimuthAngle ?: 0.0f,
                    azimuthSpeed = 0.0f, // 틸트만 제어
                    elevationAngle = currentData.elevationAngle ?: 0.0f,
                    elevationSpeed = 0.0f, // 틸트만 제어
                    trainAngle = trainAngle,
                    trainSpeed = trainSpeed,
                    crc16 = 0u,
                    etx = 0x03
                )
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("Stow 틸트 제어: 각도={}°, 속도={}°/s", trainAngle, trainSpeed)
            logger.debug(
                "Stow 틸트 제어 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        } catch (e: Exception) {
            logger.error("틸트 축 제어 명령 오류: {}", e.message, e)
            throw e
        }
    }

    /** 방위각/고도각 제어 명령 (Stow용) */
    private fun stowAzElCommand(
        multiAxis: BitSet,
        azAngle: Float,
        azSpeed: Float,
        elAngle: Float,
        elSpeed: Float
    ) {
        try {
            // ✅ 변경: readData 대신 getCurrentReadData() 사용
            val currentData = getCurrentReadData()

            val setDataFrameInstance =
                ICDService.MultiManualControl.SetDataFrame(
                    stx = 0x02,
                    cmdOne = 'A',
                    axis = multiAxis,
                    azimuthAngle = azAngle,
                    azimuthSpeed = azSpeed,
                    elevationAngle = elAngle,
                    elevationSpeed = elSpeed,
                    trainAngle = currentData.trainAngle ?: 0.0f,
                    trainSpeed = 0.0f, // 방위각/고도각만 제어
                    crc16 = 0u,
                    etx = 0x03
                )
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info(
                "Stow 방위각/고도각 제어: Az={}°({}°/s), El={}°({}°/s)",
                azAngle,
                azSpeed,
                elAngle,
                elSpeed
            )
            logger.debug(
                "Stow 방위각/고도각 제어 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        } catch (e: Exception) {
            logger.error("방위각/고도각 제어 명령 오류: {}", e.message, e)
            throw e
        }
    }

    // ✅ 추가: DataStoreService에서 현재 데이터 가져오는 헬퍼 메서드
    private fun getCurrentReadData(): PushData.ReadData {
        return dataStoreService.getLatestData()
    }

    // === 성능 모니터링 및 상태 확인 메서드들 ===
    /** UDP 통신 성능 통계 */
    fun getUdpPerformanceStats(): Map<String, Any> {
        return mapOf(
            "isRunning" to isUdpRunning.get(),
            "sendCount" to sendCount.get(),
            "receiveCount" to receiveCount.get(),
            "architecture" to "Simple Mono Async",
            "sendInterval" to "10ms",
            "receiveInterval" to "20ms",
            "commandProcessing" to "Mono Async",
            "firmwareAddress" to "$firmwareIp:$firmwarePort",
            "serverAddress" to "$serverIp:$serverPort"
        )
    }

    /** 통신 상태 확인 */
    fun isCommunicationHealthy(): Boolean {
        return isUdpRunning.get() && ::channel.isInitialized && channel.isOpen
    }

    /** 통신 상태 리포트 */
    fun getCommunicationStatusReport(): String {
        val stats = getUdpPerformanceStats()
        return buildString {
            appendLine("=== UDP 단순 Mono 통신 상태 ===")
            appendLine("실행 상태: ${if (isUdpRunning.get()) "실행 중" else "중지됨"}")
            appendLine("송신 횟수: ${stats["sendCount"]}")
            appendLine("수신 횟수: ${stats["receiveCount"]}")
            appendLine("펌웨어 주소: ${stats["firmwareAddress"]}")
            appendLine("서버 주소: ${stats["serverAddress"]}")
            appendLine("아키텍처: ${stats["architecture"]}")
            appendLine("건강 상태: ${if (isCommunicationHealthy()) "양호" else "문제 있음"}")
        }
    }

    /** 실시간 통신 상태 체크 */
    fun checkRealtimeCommunication(): Map<String, Any> {
        val currentSendCount = sendCount.get()
        val currentReceiveCount = receiveCount.get()

        // 1초 후 다시 체크하여 증가율 확인
        Thread.sleep(1000)

        val newSendCount = sendCount.get()
        val newReceiveCount = receiveCount.get()

        val sendRate = newSendCount - currentSendCount
        val receiveRate = newReceiveCount - currentReceiveCount

        return mapOf(
            "sendRate" to "${sendRate}/sec (예상: ~100/sec)",
            "receiveRate" to "${receiveRate}/sec (예상: ~50/sec)",
            "sendHealth" to (sendRate > 50), // 50% 이상이면 건강
            "receiveHealth" to (receiveRate > 25), // 50% 이상이면 건강
            "channelOpen" to (::channel.isInitialized && channel.isOpen),
            "executorsRunning" to (realtimeExecutor != null)
        )
    }

    // === 리소스 정리 및 종료 처리 ===

    @PreDestroy
    fun stop() {
        logger.info("UDP 통신 서비스 종료 시작...")

        // 1. 통신 상태 플래그 변경
        isUdpRunning.set(false)

        // 2. Stow Command 중단
        try {
            stowCommandDisposable?.dispose()
            stowCommandDisposable = null
            logger.info("Stow Command 중단 완료")
        } catch (e: Exception) {
            logger.warn("Stow Command 중단 중 오류: {}", e.message, e)
        }

        // 3. 실시간 Thread 통신 중단
        logger.info("실시간 Thread 통신 중단 중...")

        // ✅ 통합 쓰레드 관리자 사용 (개별 종료 불필요)
        logger.info("✅ 통합 쓰레드 관리자 사용으로 개별 종료 불필요")

        // 4. UDP 채널 닫기
        try {
            if (::channel.isInitialized && channel.isOpen) {
                channel.close()
                logger.info("UDP 채널 닫기 완료")
            }
        } catch (e: Exception) {
            logger.warn("UDP 채널 닫기 중 오류: {}", e.message, e)
        }

        // 5. 최종 통계 출력
        val finalStats = getUdpPerformanceStats()
        logger.info("최종 통계:")
        logger.info("  총 송신 횟수: {}", finalStats["sendCount"])
        logger.info("  총 수신 횟수: {}", finalStats["receiveCount"])

        logger.info("UDP 통신 서비스 종료 완료")
    }

    /** 강제 재연결 (비상용) */
    fun forceReconnect() {
        logger.warn("강제 재연결 시도...")

        Mono.fromCallable {
            // 기존 연결 정리
            isUdpRunning.set(false)

            if (::channel.isInitialized && channel.isOpen) {
                channel.close()
            }

            // 잠시 대기
            Thread.sleep(1000)

            // 재연결 시도
            initializeUdpChannel()

            logger.info("강제 재연결 완료")
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("강제 재연결 실패: {}", error.message, error) }
            )
    }

    /** 아키텍처 정보 */
    fun getArchitectureInfo(): String {
        return """
        UDP 단순 Mono 비동기 통신 아키텍처
        
        실시간 통신 (Thread 기반):
        ├── UDP Receive: 20ms 간격, MAX_PRIORITY (우선순위 10)
        ├── UDP Send: 10ms 간격, MAX_PRIORITY-1 (우선순위 9)
        └── 목적: 펌웨어와의 실시간 상태 송수신
        
        제어 명령 (Mono 기반):
        ├── 비동기 처리: Mono.fromCallable()
        ├── 스케줄러: Schedulers.boundedElastic()
        ├── 오류 처리: subscribe() 에러 핸들링
        └── 목적: 사용자 명령의 단순한 비동기 처리
        
        처리 흐름:
        1. 사용자 API 호출 → Mono로 비동기 처리 (즉시 반환)
        2. 각 명령은 독립적으로 비동기 실행
        3. 실시간 Thread는 지속적으로 상태 송수신
        4. WebSocket은 별도 스레드에서 프론트엔드에 스트리밍
        
        장점:
        - 단순성: 복잡한 큐나 스트림 없이 직접적인 비동기 처리
        - 실시간성: Thread 기반 고정 주기 통신
        - 안정성: 각 명령의 독립적 처리로 상호 영향 최소화
        - 가독성: 명확하고 이해하기 쉬운 코드 구조
        """.trimIndent()
    }

    /**
     * 2.16 Servo Alarm Reset
     * 서보 알람 리셋 명령 - Mono 비동기 처리
     */
    fun servoAlarmResetCommand(bitAxis: BitSet) {
        Mono.fromCallable {
            val setDataFrameInstance = ICDService.ServoAlarmReset.SetDataFrame(
                stx = 0x02,
                cmdOne = 'P',
                cmdTwo = 'A',
                axis = bitAxis,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            val axesStr = listOfNotNull(
                if (bitAxis.get(0)) "AZIMUTH" else null,
                if (bitAxis.get(1)) "ELEVATION" else null,
                if (bitAxis.get(2)) "TRAIN" else null
            ).joinToString(",")

            logger.info("Servo Alarm Reset 명령 전송 완료: {}", axesStr)
            logger.debug(
                "Servo Alarm Reset 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("Servo Alarm Reset 명령 처리 오류: {}", error.message, error) }
            )
    }

    /**
     * 2.17 M/C On/Off
     * 서보 모터 전원 제어 명령 - Mono 비동기 처리
     */
    fun mcOnOffCommand(cmdOnOff: Boolean) {
        Mono.fromCallable {
            val setDataFrameInstance = ICDService.MCOnOff.SetDataFrame(
                stx = 0x02,
                cmdOne = 'C',
                cmdOnOff = cmdOnOff,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            val status = if (cmdOnOff) "ON" else "OFF"
            logger.info("M/C On/Off 명령 전송 완료: {}", status)
            logger.debug(
                "M/C On/Off 전송 데이터: {}",
                JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error -> logger.error("M/C On/Off 명령 처리 오류: {}", error.message, error) }
            )
    }

    /**
     * 2.4 Read Firmware Version/Serial Number Info
     * 각 축의 Board F/W Version, Serial Number 정보를 수신 받기 위한 프로토콜이다.
     * 주요 정보: Board F/W Version, Serial Number
     * 주요 사용처: 설정모드
     */
    fun readFwVerSerialNoStatusCommand(): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                logger.info("Read Firmware Version/Serial Number Info 명령 시작")

                val setDataFrame = ICDService.ReadFwVerSerialNoStatus.SetDataFrame(
                    cmdOne = 'R',
                    cmdTwo = 'F'
                )

                val dataToSend = setDataFrame.setDataFrame()
                channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

                logger.info("Read Firmware Version/Serial Number Info 명령 전송 완료")
                logger.debug(
                    "Read Firmware Version/Serial Number Info 전송 데이터: {}",
                    JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend)
                )

                mapOf(
                    "status" to "success",
                    "message" to "Read Firmware Version/Serial Number Info 명령 전송 완료",
                    "timestamp" to System.currentTimeMillis()
                ) as Map<String, Any>
            } catch (e: Exception) {
                logger.error("Read Firmware Version/Serial Number Info 명령 오류: {}", e.message, e)
                throw e
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
