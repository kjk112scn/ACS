package com.gtlsystems.acs_api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.model.PushData.CMD
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

@Service
class PushService(private val objectMapper: ObjectMapper) {
    private val logger = LoggerFactory.getLogger(PushService::class.java)

    // 최신 데이터를 저장할 AtomicReference
    private val latestData = AtomicReference<PushData.ReadData>(PushData.ReadData())

    // 최신 JSON 문자열을 저장할 AtomicReference
    private val latestJsonData = AtomicReference<String>()

    // 최신 값을 유지하는 Sink 사용
    private val sink = Sinks.many().replay().latest<String>()


    // 시뮬레이션 데이터 생성 주기 설정 (필요에 따라 조정)
    val readStatusDataFlux: Flux<String> = Flux.interval(Duration.ofMillis(100)).onBackpressureLatest()
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
                // sink.tryEmitNext(data)
            }
        }
        .doOnCancel {
            logger.info("No more subscribers for data flux")
        }

    // 새 데이터 업데이트 메서드
    fun updateData(newData: PushData.ReadData) {
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
            rtdOne = newData.rtdOne ?: currentData.rtdOne,
            rtdTwo = newData.rtdTwo ?: currentData.rtdTwo,
            mainBoardProtocolStatusBits = newData.mainBoardProtocolStatusBits
                ?: currentData.mainBoardProtocolStatusBits,
            mainBoardStatusBits = newData.mainBoardStatusBits ?: currentData.mainBoardStatusBits,
            mainBoardMCOnOffBits = newData.mainBoardMCOnOffBits ?: currentData.mainBoardMCOnOffBits,
            mainBoardReserveBits = newData.mainBoardReserveBits ?: currentData.mainBoardReserveBits,
            azimuthBoardServoStatusBits = newData.azimuthBoardServoStatusBits
                ?: currentData.azimuthBoardServoStatusBits,
            azimuthBoardStatusBits = newData.azimuthBoardStatusBits ?: currentData.azimuthBoardStatusBits,
            elevationBoardServoStatusBits = newData.elevationBoardServoStatusBits
                ?: currentData.elevationBoardServoStatusBits,
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
            val readJsonData = objectMapper.writeValueAsString(mergedData)
            latestJsonData.set(readJsonData)
            publish("read", readJsonData)

            val cmdJsonData = objectMapper.writeValueAsString(CMD)
            publish("cmd", cmdJsonData)

        } catch (e: Exception) {
            logger.error("Error publishing updated data: ${e.message}", e)
        }
    }

    fun publish(topic: String = "data", dataJson: String) {
        try {
            if (dataJson.isBlank()) {
                logger.warn("Skipping empty data for topic: $topic")
                return
            }
            // 토픽과 데이터를 포함하는 래퍼 객체 생성
            val wrappedData = mapOf(
                "topic" to topic,
                "timestamp" to System.currentTimeMillis(),
                "data" to dataJson
            )
            // 래퍼 객체를 JSON으로 변환
            val wrappedJson = objectMapper.writeValueAsString(wrappedData)
            //logger.debug("Publishing $topic data: ${dataJson.take(50)}...")
            val result = sink.tryEmitNext(wrappedJson)
            if (result.isFailure) {
                if (result == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
                    logger.debug("No subscribers available for $topic, but data is buffered")
                } else {
                    logger.error("Failed to emit $topic data: $result")
                }
            }
        } catch (e: Exception) {
            logger.error("Error publishing $topic data: ${e.message}", e)
        }
    }

    /**
     * 객체를 직접 발행합니다. 항상 토픽을 포함하도록 합니다.
     * 이 함수는 토픽 없이 직접 데이터 객체를 발행하는 코드를 대체합니다.
     */
    fun publishObject(topic: String = "data", data: Any) {
        try {
            if (!topic.isBlank()) {
                // 객체를 JSON으로 변환
                val dataJson = objectMapper.writeValueAsString(data)

                // publish 함수를 호출하여 토픽과 함께 발행
                publish(topic, dataJson)
            } else {
                logger.warn("Skipping empty topic for object publishing")
            }
        } catch (e: Exception) {
            logger.error("Error publishing object for topic $topic: ${e.message}", e)
        }
    }

    /**
     * 기존 코드와의 호환성을 위한 오버로드된 함수
     */
    fun publish(readStatusDataJson: String) {
        publish("read", readStatusDataJson)
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
