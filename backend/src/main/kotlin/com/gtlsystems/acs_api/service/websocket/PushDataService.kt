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
	 * ✅ 실시간 데이터 생성 (클라이언트별 맞춤 데이터)
	 */
	fun generateRealtimeData(clientId: String): String {
		return try {
			// DataStoreService에서 최신 데이터 가져오기
			val currentData = dataStoreService.getLatestData()
			val isUdpConnected = dataStoreService.isUdpConnected()

			val trackingStatus = PushData.TRACKING_STATUS

			// ✅ 하드웨어 에러 로그 처리 및 클라이언트별 데이터 생성
			val errorData = try {
				hardwareErrorLogService.processAntennaData(currentData)
				hardwareErrorLogService.getClientData(clientId)
			} catch (e: Exception) {
				logger.warn("하드웨어 에러 로그 처리 실패: {}", e.message)
				null
			}

			val currentMstId = dataStoreService.getCurrentTrackingMstId()
			val currentDetailId = dataStoreService.getCurrentTrackingDetailId()
			val nextMstId = dataStoreService.getNextTrackingMstId()
			val nextDetailId = dataStoreService.getNextTrackingDetailId()

			val cmdAz = PushData.CMD.cmdAzimuthAngle
			val cmdEl = PushData.CMD.cmdElevationAngle
			val cmdTrain = PushData.CMD.cmdTrainAngle

			// 필수 데이터만 포함하여 처리 시간 최소화
			val dataWithInfo = mapOf(
				"data" to currentData,
				"trackingStatus" to trackingStatus,
				"serverTime" to GlobalData.Time.serverTime,
				"resultTimeOffsetCalTime" to GlobalData.Time.resultTimeOffsetCalTime,
				"cmdAzimuthAngle" to cmdAz,
				"cmdElevationAngle" to cmdEl,
				"cmdTrainAngle" to cmdTrain,
				"udpConnected" to isUdpConnected,
				"lastUdpUpdateTime" to dataStoreService.getLastUdpUpdateTime().toString(),
				// ✅ mstId와 detailId 정보 추가
				"currentTrackingMstId" to currentMstId,
				"currentTrackingDetailId" to currentDetailId,
				"nextTrackingMstId" to nextMstId,
				"nextTrackingDetailId" to nextDetailId,
				// ✅ 에러 데이터 추가 (클라이언트별 맞춤)
				"errorData" to errorData
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
	fun clientConnected(clientId: String): String {
		val count = activeClients.incrementAndGet()
		logger.info("📈 클라이언트 연결. 활성: {}, 클라이언트: {}", count, clientId)

		// 즉시 최신 데이터 반환
		return generateRealtimeData(clientId)
	}
	
	/**
	 * ✅ 팝업 상태 설정
	 */
	fun setPopupState(clientId: String, isOpen: Boolean): com.gtlsystems.acs_api.service.hardware.PopupResponse? {
		return hardwareErrorLogService.setPopupState(clientId, isOpen)
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
	fun startSimulation() = clientConnected("legacy-client")
	fun stopSimulation() = clientDisconnected()
	
	// === 기존 호환성 메서드 (클라이언트 ID 없이 호출) ===
	fun generateRealtimeData(): String = generateRealtimeData("legacy-client")
	fun clientConnected(): String = clientConnected("legacy-client")
}