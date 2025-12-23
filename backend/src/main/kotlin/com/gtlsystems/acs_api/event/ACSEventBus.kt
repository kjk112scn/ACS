package com.gtlsystems.acs_api.event

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.java

/**
 * 애플리케이션 전체에서 사용되는 이벤트 버스입니다.
 * 이벤트 발행 및 구독 기능을 제공합니다.
 */
@Component
class ACSEventBus(private val applicationEventPublisher: ApplicationEventPublisher) {
    val logger = LoggerFactory.getLogger(javaClass)

    // 이벤트 타입별 Sink 저장
    val sinks = ConcurrentHashMap<Class<out ACSEvent>, Sinks.Many<ACSEvent>>()

    /**
     * 이벤트를 발행합니다.
     * Spring의 ApplicationEventPublisher를 통해 이벤트를 발행하고,
     * Reactor의 Sink를 통해 reactive 스트림으로도 이벤트를 전달합니다.
     */
    fun publish(event: ACSEvent) {
        try {
            // Spring 이벤트 시스템을 통한 발행
            applicationEventPublisher.publishEvent(event)

            // Reactive 스트림을 통한 발행
            val eventClass = event::class.java
            val sink = sinks.computeIfAbsent(eventClass.superclass as Class<out ACSEvent>) {
                Sinks.many().multicast().onBackpressureBuffer()
            }

            val result = sink.tryEmitNext(event)
            if (result.isFailure) {
                logger.warn("이벤트 발행 실패: $event, 이유: $result")
            } else {
                logger.debug("이벤트 발행됨: $event")
            }
        } catch (e: Exception) {
            logger.error("이벤트 발행 중 오류 발생: ${e.message}", e)
        }
    }

    /**
     * 특정 타입의 이벤트를 구독합니다.
     * Reactive 스트림을 통해 이벤트를 수신할 수 있습니다.
     */
    fun <T : ACSEvent> subscribe(eventClass: Class<T>): Flux<T> {
        // 이벤트 클래스의 실제 타입을 찾기 위한 로직
        val actualEventClass = if (eventClass.superclass == ACSEvent::class.java) {
            eventClass
        } else {
            // 상위 클래스 중에서 ACSEvent의 직접 하위 클래스를 찾음
            var currentClass: Class<*>? = eventClass
            var result: Class<*>? = null

            while (currentClass != null && currentClass != Any::class.java) {
                if (currentClass.superclass == ACSEvent::class.java) {
                    result = currentClass
                    break
                }
                currentClass = currentClass.superclass
            }

            result ?: eventClass // 찾지 못한 경우 원래 클래스 사용
        }

        // 적절한 싱크를 가져오거나 생성
        val sink = sinks.computeIfAbsent(actualEventClass as Class<out ACSEvent>) {
            Sinks.many().multicast().onBackpressureBuffer()
        }

        return sink.asFlux()
            .filter { event ->
                // 이벤트가 구독한 타입의 인스턴스인지 확인
                eventClass.isInstance(event)
            }
            .map { it as T }
            .doOnSubscribe { logger.debug("이벤트 구독 시작: ${eventClass.simpleName}") }
            .doOnCancel { logger.debug("이벤트 구독 취소: ${eventClass.simpleName}") }
    }
}

// 편의를 위한 인라인 확장 함수 (클래스 외부에 정의)
inline fun <reified T : ACSEvent> ACSEventBus.subscribeToType(): Flux<T> {
    return subscribe(T::class.java)
}
