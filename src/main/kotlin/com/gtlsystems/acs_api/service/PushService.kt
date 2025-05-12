package com.gtlsystems.acs_api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.GlobalData.CMD.azimuth
import com.gtlsystems.acs_api.model.PushData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.DirectProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

@Service
class PushService(private val objectMapper: ObjectMapper) {
    private val logger = LoggerFactory.getLogger(PushService::class.java)

    // 데이터 처리를 위한 프로세서 (더 효율적인 방식)
    private val readStatusDataProcessor = DirectProcessor.create<String>()
    private val readStatusDataSink = readStatusDataProcessor.sink()

    // 최신 데이터 저장
    private val latestData = AtomicReference(PushData.ReadData())
    private val latestJsonData = AtomicReference<String>()

    // 시뮬레이션 데이터 스트림
    private val readStatusDataFlux = Flux.interval(Duration.ofMillis(50))
        .map {
            try {
                val currentData = latestData.get()
                val jsonData = objectMapper.writeValueAsString(currentData)
                latestJsonData.set(jsonData)
                jsonData
            } catch (e: Exception) {
                logger.error("Error serializing data: ${e.message}", e)
                latestJsonData.get()
            }
        }
        .publishOn(Schedulers.parallel()) // 병렬 처리
    // 데이터 스트림 제공 (최적화)
    fun getReadStatusDataStream(): Flux<String> {
        logger.debug("Creating read status data stream")

        // 구독 시 항상 최신 데이터로 시작하고, 이후 실시간 업데이트
        return Flux.defer {
            // 최신 데이터가 있으면 먼저 전송
            val latest = latestJsonData.get()
            if (latest != null) {
                Flux.just(latest)
            } else {
                Flux.empty()
            }
        }
            .concatWith(readStatusDataProcessor) // 실시간 업데이트 스트림과 연결
            .publishOn(Schedulers.parallel()) // 병렬 처리로 성능 향상
            .map { data ->
                try {
                    // 타임스탬프 추가
                    val jsonNode = objectMapper.readTree(data)
                    val objectNode = jsonNode as ObjectNode
                    objectNode.put("_receivedAt", System.currentTimeMillis())
                    objectMapper.writeValueAsString(objectNode)
                } catch (e: Exception) {
                    data // 오류 시 원본 데이터 반환
                }
            }
            .doOnSubscribe {
                logger.info("New subscriber to read status data stream")
            }
            .doOnCancel {
                logger.info("Subscriber cancelled read status data stream")
            }
    }

    // 일반 토픽 발행 (최적화)
    fun publish(topic: String, message: String) {
        try {
            if (topic == "read") {
                // read 토픽인 경우 직접 프로세서로 전송
                readStatusDataSink.next(message)
            }
            // 다른 토픽 처리는 필요에 따라 추가
        } catch (e: Exception) {
            logger.error("Error publishing to topic $topic: ${e.message}", e)
        }
    }
    // 데이터 업데이트 (최적화)
    fun updateData(newData: PushData.ReadData) {
        // 처리 시간 측정 시작
        val startTime = System.currentTimeMillis()

        // 기존 데이터와 새 데이터를 병합하여 null 값 방지
        val currentData = latestData.get()

        // 새 데이터의 null이 아닌 필드만 업데이트
        val mergedData = PushData.ReadData(
            modeStatusBits = newData.modeStatusBits ?: currentData.modeStatusBits,
            azimuthAngle = newData.azimuthAngle ?: currentData.azimuthAngle,
            elevationAngle = newData.elevationAngle ?: currentData.elevationAngle,
            tiltAngle = newData.tiltAngle ?: currentData.tiltAngle,
            azimuthSpeed = newData.azimuthSpeed ?: currentData.azimuthSpeed,
            elevationSpeed = newData.elevationSpeed ?: currentData.elevationSpeed,
            tiltSpeed = newData.tiltSpeed ?: currentData.tiltSpeed,
            servoDriverAzimuthAngle = newData.servoDriverAzimuthAngle ?: currentData.servoDriverAzimuthAngle,
            servoDriverElevationAngle = newData.servoDriverElevationAngle ?: currentData.servoDriverElevationAngle,
            servoDriverTiltAngle = newData.servoDriverTiltAngle ?: currentData.servoDriverTiltAngle,
            torqueAzimuth = newData.torqueAzimuth ?: currentData.torqueAzimuth,
            torqueElevation = newData.torqueElevation ?: currentData.torqueElevation,
            torqueTilt = newData.torqueTilt ?: currentData.torqueTilt,
            windSpeed = newData.windSpeed ?: currentData.windSpeed,
            windDirection = newData.windDirection ?: currentData.windDirection,
            currentSBandLNA_LHCP = newData.currentSBandLNA_LHCP ?: currentData.currentSBandLNA_LHCP,
            currentSBandLNA_RHCP = newData.currentSBandLNA_RHCP ?: currentData.currentSBandLNA_RHCP,
            currentXBandLNA_LHCP = newData.currentXBandLNA_LHCP ?: currentData.currentXBandLNA_LHCP,
            currentXBandLNA_RHCP = newData.currentXBandLNA_RHCP ?: currentData.currentXBandLNA_RHCP,
            rssiSBandLNA_LHCP = newData.rssiSBandLNA_LHCP ?: currentData.rssiSBandLNA_LHCP,
            rssiSBandLNA_RHCP = newData.rssiSBandLNA_RHCP ?: currentData.rssiSBandLNA_RHCP,
            rssiXBandLNA_LHCP = newData.rssiXBandLNA_LHCP ?: currentData.rssiXBandLNA_LHCP,
            rssiXBandLNA_RHCP = newData.rssiXBandLNA_RHCP ?: currentData.rssiXBandLNA_RHCP,
            // 타임스탬프 추가
           // timestamp = System.currentTimeMillis()
        )
        // 병합된 데이터로 업데이트
        latestData.set(mergedData)

        try {
            // 새 JSON 생성 및 발행
            val readJsonData = objectMapper.writeValueAsString(mergedData)
            latestJsonData.set(readJsonData)

            // 수정 전
            val dataWithTime = mapOf(
                "data" to mergedData,
                "serverTime" to GlobalData.Time.serverTime,
                "resultTimeOffsetCalTime" to GlobalData.Time.resultTimeOffsetCalTime,
                "cmdAzimuthAngle" to PushData.CMD.cmdAzimuthAngle,
                "cmdElevationAngle" to PushData.CMD.cmdElevationAngle,
                "cmdTiltAngle" to PushData.CMD.cmdTiltAngle,
            )
            val readJsonWithTime = objectMapper.writeValueAsString(dataWithTime)
            publish("read", readJsonWithTime)

            // 처리 시간 측정 종료
            val processingTime = System.currentTimeMillis() - startTime
            if (processingTime > 50) {
                logger.warn("Data processing took too long: ${processingTime}ms")
            }
        } catch (e: Exception) {
            logger.error("Error updating data: ${e.message}", e)
        }
    }
    /**
     * 최신 데이터를 반환하는 메서드
     *
     * @return 최신 ReadData 객체
     */
    fun getLatestData(): PushData.ReadData {
        return latestData.get()
    }
    // 시뮬레이션 시작 (필요한 경우)
    fun startSimulation() {
        // 이미 구현되어 있는 경우 사용
    }
}