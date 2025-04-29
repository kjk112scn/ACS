package com.gtlsystems.acs_api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.model.PushReadStatusData
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@Service
class PushReadStatusService (private val objectMapper: ObjectMapper) {
    private val sink = Sinks.many().multicast().onBackpressureBuffer<String>()
    private val counter = AtomicInteger(0)

    // 시뮬레이션 센서 데이터 생성 (실제로는 센서로부터 데이터를 읽어오는 로직)
    val sensorDataFlux: Flux<String> = Flux.interval(Duration.ofMillis(100))
        .map { PushReadStatusData.ReadData(value = counter.incrementAndGet()) }
        .map { objectMapper.writeValueAsString(it) }
        .doOnCancel { println("No more subscribers for sensor data.") }

    fun publish(sensorDataJson: String) {
        sink.tryEmitNext(sensorDataJson).orThrow()
    }

    fun getSensorDataStream(): Flux<String> {
        return sink.asFlux().mergeWith(sensorDataFlux)
    }
}