package com.gtlsystems.acs_api.service.udp

import com.gtlsystems.acs_api.event.ACSEvent
import com.gtlsystems.acs_api.event.ACSEventBus
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.icd.ICDService
import com.gtlsystems.acs_api.util.JKUtil
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.Duration
import java.util.BitSet
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Service
class UdpFwICDService(
    private val dataStoreService: DataStoreService,
    private val environment: Environment,
    private val eventBus: ACSEventBus
) {

    private val logger = LoggerFactory.getLogger(UdpFwICDService::class.java)
    private val icdService = ICDService.Classify(dataStoreService, eventBus)

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

    // 실시간 통신용 Thread 팩토리들
    private val udpReceiveThreadFactory = ThreadFactory { r ->
        Thread(r, "udp-receive-realtime").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY - 1
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { thread, ex ->
                logger.error("UDP Receive 스레드 오류", ex)
            }
        }
    }

    private val udpSendThreadFactory = ThreadFactory { r ->
        Thread(r, "udp-send-periodic").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY - 1
        }
    }

    // 실시간 통신용 Executor들
    private val udpReceiveExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(udpReceiveThreadFactory)
    private val udpSendExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(udpSendThreadFactory)

    // 통신 상태 관리
    private val isUdpRunning = AtomicBoolean(false)
    private val sendCount = AtomicLong(0)
    private val receiveCount = AtomicLong(0)

    @PostConstruct
    fun init() {
        logger.info("UDP 통신 서비스 초기화 시작")
        initializeUdpChannel()
    }

    /**
     * UDP 채널 초기화 및 실시간 통신 시작
     */
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

    /**
     * 실시간 UDP 통신 시작
     */
    private fun startRealtimeCommunication() {
        if (isUdpRunning.compareAndSet(false, true)) {
            logger.info("실시간 UDP 통신 시작")
            logger.debug("Send 간격: 10ms, Receive 간격: 20ms")

            // UDP Receive (최고 우선순위, 20ms 간격)
            udpReceiveExecutor.scheduleAtFixedRate({
                try {
                    val startTime = System.nanoTime()
                    receiveUdpData()
                    receiveCount.incrementAndGet()

                    // 성능 모니터링
                    val processingTime = (System.nanoTime() - startTime) / 1_000_000
                    if (processingTime > 15) {
                        logger.warn("UDP Receive 지연 감지: {}ms", processingTime)
                    }
                } catch (e: Exception) {
                    logger.debug("UDP Receive 오류: {}", e.message)
                }
            }, 0, 10, TimeUnit.MILLISECONDS)

            // UDP Send (높은 우선순위, 10ms 간격)
            udpSendExecutor.scheduleAtFixedRate({
                try {
                    sendReadStatusCommand()
                    sendCount.incrementAndGet()
                } catch (e: Exception) {
                    logger.debug("UDP Send 오류: {}", e.message)
                }
            }, 0, 30, TimeUnit.MILLISECONDS)

            logger.info("실시간 UDP 통신 시작 완료")
        }
    }

    /**
     * 연결 재시도 스케줄링
     */
    private fun scheduleReconnection() {
        Mono.delay(Duration.ofSeconds(5))
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe {
                logger.info("UDP 연결 재시도 중...")
                initializeUdpChannel()
            }
    }

    /**
     * 실시간 UDP 데이터 수신 (논블로킹)
     */
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

    /**
     * 주기적 상태 요청 전송
     */
    private fun sendReadStatusCommand() {
        try {
            val setDataFrameInstance = ICDService.ReadStatus.SetDataFrame()
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)
        } catch (e: Exception) {
            // 주기적 전송 실패는 로그 생략
        }
    }

    /**
     * 수신 데이터 처리
     */
    private fun processICDData(receivedData: ByteArray) {
        try {
            icdService.receivedCmd(receivedData)
        } catch (e: Exception) {
            logger.error("ICD 데이터 처리 오류: {}", e.message, e)
        }
    }

    // === 단순한 Mono 비동기 방식 명령 메서드들 ===
    /**
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
     * 비상 명령 - Mono 비동기 처리
     */
    fun onEmergencyCommand(commandChar: Char) {
        Mono.fromCallable {
            val cmdOnOffValue = when (commandChar) {
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

            val setDataFrameInstance = ICDService.Emergency.SetDataFrame(
                stx = 0x02,
                cmdOne = 'E',
                cmdOnOff = cmdOnOffValue,
                crc16 = 0u,
                etx = 0x03
            )
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("Emergency 명령 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug("Emergency 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("비상 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 시간 오프셋 명령 - Mono 비동기 처리
     */
    fun timeOffsetCommand(inputTimeOffset: Float) {
        Mono.fromCallable {
            val localTime = GlobalData.Time.utcNow
            val setDataFrameInstance = ICDService.TimeOffset.SetDataFrame(
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
            logger.debug("TimeOffset 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("시간 오프셋 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 수동 제어 명령 - Mono 비동기 처리
     */
    fun multiManualCommand(
        multiAxis: BitSet,
        azAngle: Float, azSpeed: Float,
        elAngle: Float, elSpeed: Float,
        tiAngle: Float, tiSpeed: Float
    ) {
        Mono.fromCallable {
            val setDataFrameInstance = ICDService.MultiManualControl.SetDataFrame(
                stx = 0x02,
                cmdOne = 'A',
                axis = multiAxis,
                azimuthAngle = azAngle,
                azimuthSpeed = azSpeed,
                elevationAngle = elAngle,
                elevationSpeed = elSpeed,
                tiltAngle = tiAngle,
                tiltSpeed = tiSpeed,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("Manual 제어 명령 전송 완료: Az={}°, El={}°, Ti={}°", azAngle, elAngle, tiAngle)
            logger.debug("Manual 제어 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("수동 제어 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 정지 명령 - Mono 비동기 처리
     */
    fun stopCommand(bitStop: BitSet) {
        Mono.fromCallable {
            // 진행 중인 Stow Command 중단
            if (stowCommandDisposable != null && !stowCommandDisposable!!.isDisposed) {
                stowCommandDisposable!!.dispose()
                stowCommandDisposable = null
                logger.info("StowCommand 중단됨: stop 명령 실행")
            }

            val setDataFrameInstance = ICDService.Stop.SetDataFrame(
                stx = 0x02,
                cmdOne = 'S',
                axis = bitStop,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            // 모든 추적 중지
            //stopAllCommand()

            logger.info("Stop 명령 전송 완료")
            logger.debug("Stop 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("정지 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 위치 오프셋 명령 - Mono 비동기 처리
     */
    fun positionOffsetCommand(azOffset: Float, elOffset: Float, tiOffset: Float) {
        Mono.fromCallable {
            val setDataFrameInstance = ICDService.PositionOffset.SetDataFrame(
                stx = 0x02,
                cmdOne = 'O',
                cmdTwo = 'P',
                azimuthOffset = azOffset,
                elevationOffset = elOffset,
                tiltOffset = tiOffset,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            // 글로벌 데이터 업데이트
            GlobalData.Offset.azimuthPositionOffset = azOffset
            GlobalData.Offset.elevationPositionOffset = elOffset
            GlobalData.Offset.tiltPositionOffset = tiOffset

            logger.info("PositionOffset 명령 전송 완료: Az={}°, El={}°, Ti={}°", azOffset, elOffset, tiOffset)
            logger.debug("PositionOffset 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("위치 오프셋 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 피드 On/Off 명령 - Mono 비동기 처리
     */
    fun feedOnOffCommand(bitFeedOnOff: BitSet) {
        Mono.fromCallable {
            val setDataFrameInstance = ICDService.FeedOnOff.SetDataFrame(
                stx = 0x02,
                cmdOne = 'F',
                feedOnOff = bitFeedOnOff,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("FeedOnOff 명령 전송 완료")
            logger.debug("FeedOnOff 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("피드 On/Off 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 서보 프리셋 명령 - Mono 비동기 처리
     */
    fun servoPresetCommand(bitStop: BitSet) {
        Mono.fromCallable {
            // 진행 중인 Stow Command 중단
            if (stowCommandDisposable != null && !stowCommandDisposable!!.isDisposed) {
                stowCommandDisposable!!.dispose()
                stowCommandDisposable = null
                logger.info("StowCommand 중단됨: servoPreset 명령 실행")
            }

            val setDataFrameInstance = ICDService.ServoEncoderPreset.SetDataFrame(
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
            logger.debug("ServoPreset 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("서보 프리셋 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    fun writeNTPCommand() {
        Mono.fromCallable {
            val Time = GlobalData.Time.utcNow
            val setDataFrameInstance = ICDService.WriteNTP.SetDataFrame(
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
            logger.debug("writeNTPCommand 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("기본 정보 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 기본 정보 명령 - Mono 비동기 처리
     */
    fun defaultInfoCommand() {
        Mono.fromCallable {
            val utcTime = GlobalData.Time.utcNow
            val setDataFrameInstance = ICDService.DefaultInfo.SetDataFrame(
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
                tiltOffset = GlobalData.Offset.tiltPositionOffset,
                crc16 = 0u
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("DefaultInfo 명령 전송 완료")
            logger.debug("DefaultInfo 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { /* 성공 */ },
                { error ->
                    logger.error("기본 정보 명령 처리 오류: {}", error.message, error)
                }
            )
    }

    /**
     * 모든 추적 중지 (이벤트 발행)
     */
    fun stopAllCommand() {
        try {
            eventBus.publish(ACSEvent.TrackingEvent.StopAllTracking)
            logger.info("모든 추적 중지 이벤트 발행됨")
        } catch (e: Exception) {
            logger.error("stopAllCommand 오류: {}", e.message, e)
        }
    }

    // === 위성 추적 관련 메서드들 ===

    /**
     * 12.1 위성 추적 헤더 정보 전송
     */
    fun sendSatelliteTrackHeader(headerFrame: ICDService.SatelliteTrackOne.SetDataFrame) {
        try {
            val dataToSend = headerFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("위성 추적 헤더 정보 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug("위성 추적 헤더 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))

        } catch (e: Exception) {
            logger.error("위성 추적 헤더 정보 전송 오류: {}", e.message, e)
            throw e
        }
    }

    /**
     * 12.2 위성 추적 초기 제어 명령 전송
     */
    fun sendSatelliteTrackInitialControl(controlFrame: ICDService.SatelliteTrackTwo.SetDataFrame) {
        try {
            val dataToSend = controlFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("위성 추적 초기 제어 명령 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug("위성 추적 초기 제어 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))

        } catch (e: Exception) {
            logger.error("위성 추적 초기 제어 명령 전송 오류: {}", e.message, e)
            throw e
        }
    }

    /**
     * 12.3 위성 추적 추가 데이터 전송
     */
    fun sendSatelliteTrackAdditionalData(dataFrame: ICDService.SatelliteTrackThree.SetDataFrame) {
        try {
            val dataToSend = dataFrame.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("위성 추적 추가 데이터 전송 완료: {}:{}", firmwareIp, firmwarePort)
            logger.debug("위성 추적 추가 데이터 전송: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))

        } catch (e: Exception) {
            logger.error("위성 추적 추가 데이터 전송 오류: {}", e.message, e)
            throw e
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

        val stowTiltAngle = 0.0f
        val stowTiltSpeed = 5.0f
        val stowAzimuthAngle = 0.0f
        val stowAzimuthSpeed = 5.0f
        val stowElevationAngle = 90.0f
        val stowElevationSpeed = 5.0f

        logger.info("Stow 명령 시작")

        // 1단계: 틸트 축 제어
        val tiltAxis = BitSet().apply {
            set(2) // 틸트 축
            set(7) // STOW 비트
        }

        Mono.fromCallable {
            stowTiltCommand(tiltAxis, stowTiltAngle, stowTiltSpeed)
            logger.info("Stow 1단계: 틸트 축 제어 명령 전송 완료")
        }
            .subscribeOn(Schedulers.boundedElastic())
            .delayElement(Duration.ofMillis(100)) // 명령 전송 후 잠시 대기
            .flatMap {
                // 2단계: 틸트 안정화 대기
                logger.info("Stow 2단계: 틸트 안정화 대기 시작 (목표: {}°)", stowTiltAngle)
                waitForTiltStabilization(stowTiltAngle)
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

    /**
     * 틸트 안정화 대기 - 단순한 Mono 방식
     */
    private fun waitForTiltStabilization(targetAngle: Float): Mono<String> {
        return Mono.create { sink ->
            val startTime = System.currentTimeMillis()
            val maxWaitTime = 30000L // 30초 최대 대기

            val checkStabilization = object : Runnable {
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
                        val currentAngle = getCurrentReadData().tiltAngle ?: 0.0f
                        val isStable = Math.abs(currentAngle - targetAngle) <= 0.1f

                        if (isStable) {
                            logger.info("틸트 안정화 완료: 현재={}°, 목표={}°", currentAngle, targetAngle)
                            sink.success("stabilized")
                        } else {
                            logger.debug(
                                "틸트 안정화 중: 현재={}°, 목표={}°, 차이={}°",
                                currentAngle, targetAngle, Math.abs(currentAngle - targetAngle)
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

    /**
     * 틸트 축 제어 명령 (Stow용)
     */
    private fun stowTiltCommand(
        multiAxis: BitSet,
        tiAngle: Float,
        tiSpeed: Float
    ) {
        try {
            // ✅ 변경: readData 대신 getCurrentReadData() 사용
            val currentData = getCurrentReadData()

            val setDataFrameInstance = ICDService.MultiManualControl.SetDataFrame(
                stx = 0x02,
                cmdOne = 'A',
                axis = multiAxis,
                azimuthAngle = currentData.azimuthAngle ?: 0.0f,
                azimuthSpeed = 0.0f,  // 틸트만 제어
                elevationAngle = currentData.elevationAngle ?: 0.0f,
                elevationSpeed = 0.0f,  // 틸트만 제어
                tiltAngle = tiAngle,
                tiltSpeed = tiSpeed,
                crc16 = 0u,
                etx = 0x03
            )
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("Stow 틸트 제어: 각도={}°, 속도={}°/s", tiAngle, tiSpeed)
            logger.debug("Stow 틸트 제어 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))

        } catch (e: Exception) {
            logger.error("틸트 축 제어 명령 오류: {}", e.message, e)
            throw e
        }
    }

    /**
     * 방위각/고도각 제어 명령 (Stow용)
     */
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

            val setDataFrameInstance = ICDService.MultiManualControl.SetDataFrame(
                stx = 0x02,
                cmdOne = 'A',
                axis = multiAxis,
                azimuthAngle = azAngle,
                azimuthSpeed = azSpeed,
                elevationAngle = elAngle,
                elevationSpeed = elSpeed,
                tiltAngle = currentData.tiltAngle ?: 0.0f,
                tiltSpeed = 0.0f,  // 방위각/고도각만 제어
                crc16 = 0u,
                etx = 0x03
            )
            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info(
                "Stow 방위각/고도각 제어: Az={}°({}°/s), El={}°({}°/s)",
                azAngle, azSpeed, elAngle, elSpeed
            )
            logger.debug("Stow 방위각/고도각 제어 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))

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

    /**
     * UDP 통신 성능 통계
     */
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

    /**
     * 통신 상태 확인
     */
    fun isCommunicationHealthy(): Boolean {
        return isUdpRunning.get() &&
                ::channel.isInitialized &&
                channel.isOpen
    }

    /**
     * 통신 상태 리포트
     */
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

    /**
     * 실시간 통신 상태 체크
     */
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
            "executorsRunning" to (!udpSendExecutor.isShutdown && !udpReceiveExecutor.isShutdown)
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

        // Receive Executor 종료
        udpReceiveExecutor.shutdown()
        try {
            if (!udpReceiveExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.warn("UDP Receive Executor 강제 종료")
                udpReceiveExecutor.shutdownNow()
                if (!udpReceiveExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    logger.error("UDP Receive Executor 종료 실패")
                }
            } else {
                logger.info("UDP Receive Executor 정상 종료")
            }
        } catch (e: InterruptedException) {
            logger.warn("UDP Receive Executor 종료 중 인터럽트")
            udpReceiveExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        // Send Executor 종료
        udpSendExecutor.shutdown()
        try {
            if (!udpSendExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.warn("UDP Send Executor 강제 종료")
                udpSendExecutor.shutdownNow()
                if (!udpSendExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    logger.error("UDP Send Executor 종료 실패")
                }
            } else {
                logger.info("UDP Send Executor 정상 종료")
            }
        } catch (e: InterruptedException) {
            logger.warn("UDP Send Executor 종료 중 인터럽트")
            udpSendExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }

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

    // === 디버깅 및 테스트 메서드들 ===

    /**
     * 테스트용 더미 명령 전송
     */
    fun sendTestCommand() {
        logger.info("테스트 명령 전송 중...")

        val testBitSet = BitSet()
        testBitSet.set(0) // 방위각 축만 설정

        Mono.fromCallable {
            val setDataFrameInstance = ICDService.MultiManualControl.SetDataFrame(
                stx = 0x02,
                cmdOne = 'A',
                axis = testBitSet,
                azimuthAngle = 0.0f,
                azimuthSpeed = 1.0f,
                elevationAngle = 0.0f,
                elevationSpeed = 0.0f,
                tiltAngle = 0.0f,
                tiltSpeed = 0.0f,
                crc16 = 0u,
                etx = 0x03
            )

            val dataToSend = setDataFrameInstance.setDataFrame()
            channel.send(ByteBuffer.wrap(dataToSend), firmwareAddress)

            logger.info("테스트 명령 전송 완료")
            logger.debug("테스트 명령 전송 데이터: {}", JKUtil.JKConvert.Companion.byteArrayToHexString(dataToSend))
        }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                { logger.info("테스트 명령 성공") },
                { error -> logger.error("테스트 명령 실패: {}", error.message, error) }
            )
    }

    /**
     * 강제 재연결 (비상용)
     */
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

    /**
     * 아키텍처 정보
     */
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

    // === 안정화 시간 추적 클래스 (기존 호환성) ===
    private class StableTimeTracker {
        var startTime: Long? = null
    }
}