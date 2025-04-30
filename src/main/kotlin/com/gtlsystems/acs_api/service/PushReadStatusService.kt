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
    private val icdService = ICDService.ReadStatus.GetDataFrame()

    // 시뮬레이션 센서 데이터 생성 (실제로는 센서로부터 데이터를 읽어오는 로직)
    val readStatusDataFlux: Flux<String> = Flux.interval(Duration.ofMillis(100))
        .map { PushReadStatusData.ReadData() } // 기본값으로 ReadData 객체 생성
        .map { objectMapper.writeValueAsString(it) }
        .doOnCancel { println("No more subscribers for simulated sensor data.")
        }
    fun publish(readStatusDataJson: String) {
        sink.tryEmitNext(readStatusDataJson).orThrow()
    }

    fun getReadStatusDataStream(): Flux<String> {
        return sink.asFlux().mergeWith(readStatusDataFlux)
    }
}