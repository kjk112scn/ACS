package com.gtlsystems.acs_api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import org.slf4j.LoggerFactory
import jakarta.annotation.PostConstruct
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Service
class PushService(
    private val objectMapper: ObjectMapper,
    private val dataStoreService: DataStoreService
) {
    private val logger = LoggerFactory.getLogger(PushService::class.java)

    // 싱크 생성 (WebSocket으로 데이터 전송)
    private val sink = Sinks.many().multicast().onBackpressureBuffer<String>()

    // 활성 클라이언트 수
    private val activeClients = AtomicInteger(0)

    // 마지막으로 전송된 JSON 데이터
    private val latestJsonData = AtomicReference<String>("")

    @PostConstruct
    fun init() {
        startPeriodicDataTransmission()
        logger.info("푸시 서비스 초기화 완료")
    }

    // 주기적 데이터 전송 시작
    private fun startPeriodicDataTransmission() {
        // 전송 간격 설정 (기본값: 100ms)
        val interval = 100L // 필요에 따라 조정 가능

        Flux.interval(Duration.ofMillis(interval))
            .subscribe {
                try {
                    // 활성 클라이언트가 있을 때만 데이터 전송
                    if (activeClients.get() > 0) {
                        sendLatestData()
                    }
                } catch (e: Exception) {
                    logger.error("주기적 데이터 전송 중 오류 발생: ${e.message}", e)
                }
            }
    }

    // 최신 데이터 전송
    private fun sendLatestData() {
        try {
            // DataStoreService에서 최신 데이터 가져오기
            val currentData = dataStoreService.getLatestData()

            // UDP 연결 상태 확인
            val isUdpConnected = dataStoreService.isUdpConnected()

            // 데이터와 상태 정보를 포함한 맵 생성
            val dataWithInfo = mapOf(
                "data" to currentData,
                "serverTime" to GlobalData.Time.serverTime,
                "resultTimeOffsetCalTime" to GlobalData.Time.resultTimeOffsetCalTime,
                "cmdAzimuthAngle" to PushData.CMD.cmdAzimuthAngle,
                "cmdElevationAngle" to PushData.CMD.cmdElevationAngle,
                "cmdTiltAngle" to PushData.CMD.cmdTiltAngle,
                "udpConnected" to isUdpConnected,
                "lastUdpUpdateTime" to dataStoreService.getLastUdpUpdateTime().toString()
            )

            // JSON으로 변환하여 발행
            val jsonData = objectMapper.writeValueAsString(dataWithInfo)
            latestJsonData.set(jsonData)
            publish("read", jsonData)

        } catch (e: Exception) {
            logger.error("데이터 전송 중 오류: ${e.message}", e)
        }
    }

    // 데이터 발행
    fun publish(topic: String, message: String) {
        try {
            val fullMessage = """{"topic":"$topic","message":$message}"""
            sink.tryEmitNext(fullMessage)
        } catch (e: Exception) {
            logger.error("데이터 발행 중 오류 발생: ${e.message}", e)
        }
    }

    // 데이터 스트림 가져오기 (WebSocketHandler에서 사용)
    fun getDataStream(): Flux<String> {
        return sink.asFlux()
    }

    // 클라이언트 연결 시 호출
    fun clientConnected() {
        activeClients.incrementAndGet()
        logger.info("클라이언트 연결됨. 현재 활성 클라이언트: ${activeClients.get()}")

        // 연결 즉시 최신 데이터 전송
        val lastData = latestJsonData.get()
        if (lastData.isNotEmpty()) {
            publish("read", lastData)
        }
    }

    // 클라이언트 연결 해제 시 호출
    fun clientDisconnected() {
        val count = activeClients.decrementAndGet()
        logger.info("클라이언트 연결 해제됨. 현재 활성 클라이언트: $count")
    }

    // 전송 간격 설정
    fun setTransmissionInterval(intervalMs: Long) {
        if (intervalMs > 0) {
            // GlobalData.DataTransmission.transmissionInterval = intervalMs
            logger.info("데이터 전송 간격 설정: ${intervalMs}ms")
        } else {
            logger.warn("유효하지 않은 전송 주기: ${intervalMs}ms. 양수 값만 허용됩니다.")
        }
    }

    // 시뮬레이션 시작 (기존 코드와의 호환성 유지)
    fun startSimulation() {
        // 클라이언트 연결 시 호출되는 메서드
        clientConnected()
    }

    // 시뮬레이션 중지 (기존 코드와의 호환성 유지)
    fun stopSimulation() {
        // 클라이언트 연결 해제 시 호출되는 메서드
        clientDisconnected()
    }

    // 기존 코드와의 호환성을 위한 메서드
    fun getReadStatusDataStream(): Flux<String> {
        return getDataStream()
    }
}