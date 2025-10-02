package com.gtlsystems.acs_api.service.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.PushData
import com.gtlsystems.acs_api.service.datastore.DataStoreService
import com.gtlsystems.acs_api.service.hardware.HardwareErrorLogService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

@Service
class PushDataService(
	private val objectMapper: ObjectMapper,
	private val dataStoreService: DataStoreService,
	private val hardwareErrorLogService: HardwareErrorLogService
) {

	private val logger = LoggerFactory.getLogger(PushDataService::class.java)

	// === 클라이언트 카운트 관리만 ===
	private val activeClients = AtomicInteger(0)

	/**
	 * ✅ 실시간 데이터 생성 (mstId 포함 + 하드웨어 로그 포함)
	 */
	fun generateRealtimeData(): String {
		return try {
			// DataStoreService에서 최신 데이터 가져오기
			val currentData = dataStoreService.getLatestData()
			val isUdpConnected = dataStoreService.isUdpConnected()

			// ✅ Sun Track 상태 로깅 추가
			val trackingStatus = PushData.TRACKING_STATUS
			if (trackingStatus.sunTrackTrackingState != null) {
				logger.debug("☀️ WebSocket 전송 - Sun Track 추적 상태: {}", trackingStatus.sunTrackTrackingState)
			}

			// ✅ 하드웨어 에러 로그 처리 (예외로 전체 흐름이 깨지지 않도록 보호)
			try {
				val currentData = dataStoreService.getLatestData() // ✅ PushData.ReadData 직접 사용
				logger.debug("🔍 하드웨어 에러 로그 처리 - elevationBoardStatusBits: {}", currentData.elevationBoardStatusBits)
				hardwareErrorLogService.processAntennaData(currentData) // ✅ 타입 안전한 전달
			} catch (e: Exception) {
				logger.warn("하드웨어 에러 로그 처리 실패: {}", e.message)
			}

			// ✅ 하드웨어 에러 로그 수집 (예외 보호)
			val hardwareErrorLogs = try {
				val logs = hardwareErrorLogService.getAllErrorLogs()
				logger.info("📋 하드웨어 에러 로그 수집: {}개", logs.size)
				if (logs.isNotEmpty()) {
					logger.info("📋 첫 번째 로그: {}", logs.first())
				}
				logs
			} catch (e: Exception) {
				logger.warn("하드웨어 에러 로그 조회 실패: {}", e.message)
				emptyList()
			}

			// 필수 데이터만 포함하여 처리 시간 최소화
			val dataWithInfo = mapOf(
				"data" to currentData,
				"trackingStatus" to trackingStatus,
				"serverTime" to GlobalData.Time.serverTime,
				"resultTimeOffsetCalTime" to GlobalData.Time.resultTimeOffsetCalTime,
				"cmdAzimuthAngle" to PushData.CMD.cmdAzimuthAngle,
				"cmdElevationAngle" to PushData.CMD.cmdElevationAngle,
				"cmdTrainAngle" to PushData.CMD.cmdTrainAngle,
				"udpConnected" to isUdpConnected,
				"lastUdpUpdateTime" to dataStoreService.getLastUdpUpdateTime().toString(),
				// ✅ mstId 정보 추가
				"currentTrackingMstId" to dataStoreService.getCurrentTrackingMstId(),
				"nextTrackingMstId" to dataStoreService.getNextTrackingMstId(),
				// ✅ 하드웨어 에러 로그 추가
				"hardwareErrorLogs" to hardwareErrorLogs
			)

			val jsonData = objectMapper.writeValueAsString(dataWithInfo)
			"""{"topic":"read","data":$jsonData}"""

		} catch (e: Exception) {
			logger.error("❌ 실시간 데이터 생성 오류: {}", e.message, e)
			"""{"topic":"error","message":"데이터 생성 실패: ${e.message}"}"""
		}
	}

	/**
	 * ✅ 클라이언트 연결 알림
	 */
	fun clientConnected(): String {
		val count = activeClients.incrementAndGet()
		logger.info("📈 클라이언트 연결. 활성: {}", count)

		// 즉시 최신 데이터 반환
		return generateRealtimeData()
	}

	/**
	 * ✅ 클라이언트 해제 알림
	 */
	fun clientDisconnected() {
		val count = activeClients.decrementAndGet()
		logger.info("📉 클라이언트 해제. 활성: {}", count)
	}

	/**
	 * ✅ 활성 클라이언트 수 반환
	 */
	fun getActiveClientCount(): Int = activeClients.get()

	/**
	 * ✅ 서비스 상태 정보
	 */
	fun getServiceStats(): Map<String, Any> {
		return mapOf(
			"activeClients" to activeClients.get(),
			"dataStoreConnected" to dataStoreService.isUdpConnected(),
			"lastUdpUpdateTime" to dataStoreService.getLastUdpUpdateTime().toString(),
			"serviceRole" to "Data Generation Only",
			"features" to listOf(
				"Real-time Data Generation",
				"Client Count Management",
				"DataStore Integration",
				"Hardware Error Logging"
			)
		)
	}

	/**
	 * ✅ 상태 리포트
	 */
	fun getStatusReport(): String {
		val stats = getServiceStats()

		return buildString {
			appendLine("=== PushDataService 상태 (데이터 생성 전용) ===")
			appendLine("🔗 활성 클라이언트: ${stats["activeClients"]}")
			appendLine("📡 DataStore 연결: ${stats["dataStoreConnected"]}")
			appendLine("⏰ 마지막 UDP 업데이트: ${stats["lastUdpUpdateTime"]}")
			appendLine("🎯 역할: ${stats["serviceRole"]}")
			appendLine("🔋 상태: 데이터 생성 준비됨")
		}
	}

	// === 기존 호환성 메서드들 (Controller에서 호출) ===
	fun startSimulation() = clientConnected()
	fun stopSimulation() = clientDisconnected()
}