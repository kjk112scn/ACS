package com.gtlsystems.acs_api.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, exchange: ServerWebExchange): Mono<ResponseEntity<Map<String, Any>>> {
        logger.error("요청 처리 중 오류 발생: ${ex.message}", ex)

        val errorDetails = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "path" to exchange.request.path.value(),
            "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error" to HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            "message" to (ex.message ?: "알 수 없는 오류"),
            "requestId" to exchange.request.id
        )

        return Mono.just(ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Map<String, Any>>> {
        logger.error("HTTP 상태 오류: ${ex.statusCode} - ${ex.reason}", ex)

        // Spring Boot 3.x 버전용 코드
        val errorDetails = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "path" to exchange.request.path.value(),
            "status" to ex.statusCode.value(),
            "error" to ex.statusCode.toString(),
            "message" to (ex.reason ?: "알 수 없는 오류"),
            "requestId" to exchange.request.id
        )

        return Mono.just(ResponseEntity(errorDetails, ex.statusCode))
    }

    // Spring Boot 2.x 버전용 대체 메서드 (필요한 경우 주석 해제)
    /*
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Map<String, Any>>> {
        logger.error("HTTP 상태 오류: ${ex.status} - ${ex.reason}", ex)

        val errorDetails = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "path" to exchange.request.path.value(),
            "status" to ex.status.value(),
            "error" to ex.status.reasonPhrase,
            "message" to (ex.reason ?: "알 수 없는 오류"),
            "requestId" to exchange.request.id
        )

        return Mono.just(ResponseEntity(errorDetails, ex.status))
    }
    */
}