package com.gtlsystems.acs_api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.model.PushReadStatusData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

@Service
class PushReadStatusService(private val objectMapper: ObjectMapper) {
    private val logger = LoggerFactory.getLogger(PushReadStatusService::class.java)

    // 최신 데이터를 저장할 AtomicReference
    private val latestData = AtomicReference<PushReadStatusData.ReadData>(PushReadStatusData.ReadData())

    // 최신 JSON 문자열을 저장할 AtomicReference
    private val latestJsonData = AtomicReference<String>()

    // 최신 값을 유지하는 Sink 사용
    private val sink = Sinks.many().replay().latest<String>()

    // 시뮬레이션 데이터 생성 주기 설정 (필요에 따라 조정)
    val readStatusDataFlux: Flux<String> = Flux.interval(Duration.ofMillis(100))
        .map {
            // 항상 최신 데이터 사용
            val currentData = latestData.get()
            try {
                val jsonData = objectMapper.writeValueAsString(currentData)
                latestJsonData.set(jsonData) // 최신 JSON 저장
                logger.debug("Generated data: ${jsonData.take(50)}...")
                jsonData
            } catch (e: Exception) {
                logger.error("Error serializing data: ${e.message}", e)
                latestJsonData.get() // 오류 시 마지막 유효한 JSON 반환
            }
        }
        .doOnNext { data ->
            if (data != null) {
                sink.tryEmitNext(data)
            }
        }
        .doOnCancel {
            logger.info("No more subscribers for data flux")
        }

    // 새 데이터 업데이트 메서드
    fun updateData(newData: PushReadStatusData.ReadData) {
        // 기존 데이터와 새 데이터를 병합하여 null 값 방지
        val currentData = latestData.get()

        // 새 데이터의 null이 아닌 필드만 업데이트
        val mergedData = PushReadStatusData.ReadData(
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
            rtdOne = newData.rtdOne ?: currentData.rtdOne,
            rtdTwo = newData.rtdTwo ?: currentData.rtdTwo,
            mainBoardProtocolStatusBits = newData.mainBoardProtocolStatusBits ?: currentData.mainBoardProtocolStatusBits,
            mainBoardStatusBits = newData.mainBoardStatusBits ?: currentData.mainBoardStatusBits,
            mainBoardMCOnOffBits = newData.mainBoardMCOnOffBits ?: currentData.mainBoardMCOnOffBits,
            mainBoardReserveBits = newData.mainBoardReserveBits ?: currentData.mainBoardReserveBits,
            azimuthBoardServoStatusBits = newData.azimuthBoardServoStatusBits ?: currentData.azimuthBoardServoStatusBits,
            azimuthBoardStatusBits = newData.azimuthBoardStatusBits ?: currentData.azimuthBoardStatusBits,
            elevationBoardServoStatusBits = newData.elevationBoardServoStatusBits ?: currentData.elevationBoardServoStatusBits,
            elevationBoardStatusBits = newData.elevationBoardStatusBits ?: currentData.elevationBoardStatusBits,
            tiltBoardServoStatusBits = newData.tiltBoardServoStatusBits ?: currentData.tiltBoardServoStatusBits,
            tiltBoardStatusBits = newData.tiltBoardStatusBits ?: currentData.tiltBoardStatusBits,
            feedSBoardStatusBits = newData.feedSBoardStatusBits ?: currentData.feedSBoardStatusBits,
            feedXBoardStatusBits = newData.feedXBoardStatusBits ?: currentData.feedXBoardStatusBits,
            currentSBandLNA_LHCP = newData.currentSBandLNA_LHCP ?: currentData.currentSBandLNA_LHCP,
            currentSBandLNA_RHCP = newData.currentSBandLNA_RHCP ?: currentData.currentSBandLNA_RHCP,
            currentXBandLNA_LHCP = newData.currentXBandLNA_LHCP ?: currentData.currentXBandLNA_LHCP,
            currentXBandLNA_RHCP = newData.currentXBandLNA_RHCP ?: currentData.currentXBandLNA_RHCP,
            rssiSBandLNA_LHCP = newData.rssiSBandLNA_LHCP ?: currentData.rssiSBandLNA_LHCP,
            rssiSBandLNA_RHCP = newData.rssiSBandLNA_RHCP ?: currentData.rssiSBandLNA_RHCP,
            rssiXBandLNA_LHCP = newData.rssiXBandLNA_LHCP ?: currentData.rssiXBandLNA_LHCP,
            rssiXBandLNA_RHCP = newData.rssiXBandLNA_RHCP ?: currentData.rssiXBandLNA_RHCP
        )

        // 병합된 데이터로 업데이트
        latestData.set(mergedData)

        try {
            // 새 JSON 생성 및 발행
            val jsonData = objectMapper.writeValueAsString(mergedData)
            latestJsonData.set(jsonData)
            publish(jsonData)
        } catch (e: Exception) {
            logger.error("Error publishing updated data: ${e.message}", e)
        }
    }

    fun publish(readStatusDataJson: String) {
        try {
            if (readStatusDataJson.isBlank()) {
                logger.warn("Skipping empty data")
                return
            }

            logger.debug("Publishing data: ${readStatusDataJson.take(50)}...")
            val result = sink.tryEmitNext(readStatusDataJson)

            if (result.isFailure()) {
                if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                    logger.debug("No subscribers available, but data is buffered")
                } else {
                    logger.error("Failed to emit data: $result")
                }
            }
        }
        catch (e: Exception) {
            logger.error("Error publishing data: ${e.message}", e)
        }
    }

    fun getReadStatusDataStream(): Flux<String> {
        logger.debug("Creating read status data stream")

        // 구독 시 항상 최신 데이터로 시작
        return sink.asFlux()
            .switchIfEmpty(Flux.defer {
                // sink가 비어있으면 최신 데이터 사용
                val latest = latestJsonData.get()
                if (latest != null) {
                    Flux.just(latest)
                } else {
                    // 아직 데이터가 없으면 시뮬레이션 데이터 사용
                    readStatusDataFlux.take(1)
                }
            })
            .mergeWith(readStatusDataFlux)
            .distinctUntilChanged() // 중복 데이터 제거
            .doOnSubscribe {
                logger.info("New subscriber to read status data stream")
            }
            .doOnCancel {
                logger.info("Subscriber cancelled read status data stream")
            }
    }
    fun startSimulation() {
        logger.info("Starting data simulation")
        // 시뮬레이션 데이터 생성 시작 로직
        // 예: readStatusDataFlux를 구독하여 데이터 생성 시작
        readStatusDataFlux.subscribe()
    }
}
