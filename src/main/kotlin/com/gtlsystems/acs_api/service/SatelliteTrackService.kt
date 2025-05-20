package com.gtlsystems.acs_api.service

import com.gtlsystems.acs_api.algorithm.satellitetracker.impl.OrekitCalculator
import com.gtlsystems.acs_api.model.GlobalData
import com.gtlsystems.acs_api.model.SatelliteTrackingData
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import com.gtlsystems.acs_api.algorithm.axistransformation.CoordinateTransformer
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * 위성 추적 서비스
 * 위성의 위치를 계산하고 추적 정보를 제공합니다.
 */
@Service
class SatelliteTrackService(private val orekitCalculator: OrekitCalculator) {

    // 밀리초를 포함하는 사용자 정의 포맷터 생성
    private val timeFormatterWithMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    private val timeOnlyFormatterWithMillis = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    private val logger = LoggerFactory.getLogger(javaClass)

    // 위성 TLE 데이터 캐시
    private val satelliteTleCache = ConcurrentHashMap<String, Pair<String, String>>()
    private val trackingData= SatelliteTrackingData.Tracking
    private val locationData= GlobalData.Location

    // 위성 추적 마스터 및 세부 데이터 저장소 (실제로는 데이터베이스를 사용할 것입니다)
    private val ephemerisTrackMstStorage = mutableListOf<Map<String, Any?>>()
    private val ephemerisTrackDtlStorage = mutableListOf<Map<String, Any?>>()

    @PostConstruct
    fun init() {
      /*  val satelliteName = "AQUA"
        val tle1 = "1 27424U 02022A   25139.85285278  .00000972  00000+0  20704-3 0  9993"
        val tle2 =  "2 27424  98.3764  97.8511 0002005  96.0297 292.2463 14.61250812225859"
        EphemerisDesignationTrack(tle1,tle2,satelliteName)*/
        //satelliteTest()
    }

    fun satelliteTest() {
        try {

            calculateRotatorAngleTable(
                standardAzimuth = 177.796609998884,
                standardElevation = 46.4621529680836,
                tiltAngle = -6.98,
                rotatorStepDegrees = 356.62
            )
        } catch (e: Exception) {
            logger.error("satellite_Test 실행 중 오류 발생: ${e.message}", e)
        }
    }
    fun calculateRotatorAngleTable(
        standardAzimuth: Double,
        standardElevation: Double,
        tiltAngle: Double,
        rotatorStepDegrees: Double = 30.0
    ) {
        val table = CoordinateTransformer.generateRotatorAngleTable(
            standardAzimuth, standardElevation, tiltAngle, rotatorStepDegrees
        )

        logger.info("회전체 각도에 따른 방위각/고도각 변화 테이블")
        logger.info("표준 좌표: Az=${standardAzimuth}°, El=${standardElevation}°, 기울기=${tiltAngle}°")
        logger.info("─────────────────────────────────────────────")
        logger.info("│ 회전체 각도 │   방위각   │   고도각   │")
        logger.info("─────────────────────────────────────────────")

        table.forEach { (rotatorAngle, az, el) ->
            logger.info("│ ${String.format("%20.8f", rotatorAngle)}° │ ${String.format("%20.8f", az)}° │ ${String.format("%20.8f", el)}° │")
        }

        logger.info("─────────────────────────────────────────────")
    }
    fun SaveCSvFileSatelliteTrack()
    {
        val aquaId = "AQUA"
        val aquaTleLine1 = "1 27424U 02022A   25134.85411318  .00000946  00000-0  20168-3 0  9990"
        val aquaTleLine2 = "2 27424  98.3761  92.7913 0001892 100.4476 287.5765 14.61239902225127"
        addSatelliteTle(aquaId, aquaTleLine1, aquaTleLine2)
        // 위성 추적 스케줄 생성
        val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
            tleLine1 = aquaTleLine1,
            tleLine2 = aquaTleLine2,
            startDate = trackingData.startDate,
            durationDays = trackingData.durationDays.toInt(),
            minElevation = trackingData.minElevationAngle,
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            altitude = locationData.altitude,
            trackingIntervalMs = trackingData.msInterval
        )
        logger.info("위성 추적 스케줄 생성 완료: 총 ${schedule.trackingPasses.size}개 패스")

        // 3. 추적 데이터 파일로 저장
        val outputDir = "tracking_data/aqua_test"
        val filePrefix = "AQUA_20250514"

        logger.info("추적 데이터 파일 저장 시작: 출력 디렉토리=${outputDir}")

        val savedFiles = orekitCalculator.saveAllTrackingData(schedule, outputDir, filePrefix)

        logger.info("추적 데이터 파일 저장 완료: 총 ${savedFiles.size}개 파일 생성")
        logger.info("생성된 파일 목록:")
        savedFiles.forEach { filePath ->
            logger.info("- $filePath")
        }
    }


    /**
     * TLE 데이터로 위성 궤도 추적
     * 위성 이름이 제공되지 않으면 TLE에서 추출
     */
    fun EphemerisDesignationTrack(
        tleLine1: String,
        tleLine2: String,
        satelliteName: String? = null
    ): Pair<List<Map<String, Any?>>, List<Map<String, Any?>>> {
        try {
            // TLE에서 위성 ID 추출
            val satelliteId = tleLine1.substring(2, 7).trim()

            // 위성 이름이 제공되지 않은 경우 ID에서 추출
            val actualSatelliteName = satelliteName ?: getSatelliteNameFromId(satelliteId)

            logger.info("$actualSatelliteName 위성의 궤도 추적 시작")

            // 추적 기간 설정 (오늘 00시부터 내일 00시까지)
            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            val tomorrow = today.plusDays(1)

            // 추적 스케줄을 위한 마스터 리스트 생성
            val ephemerisTrackMst = mutableListOf<Map<String, Any?>>()

            // 추적 좌표를 위한 세부 리스트 생성
            val ephemerisTrackDtl = mutableListOf<Map<String, Any?>>()

            // 위성 추적 스케줄 생성
            val schedule = orekitCalculator.generateSatelliteTrackingSchedule(
                tleLine1 = tleLine1,
                tleLine2 = tleLine2,
                startDate = today,
                durationDays = 1,
                minElevation = trackingData.minElevationAngle,
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                altitude = locationData.altitude,
                trackingIntervalMs = trackingData.msInterval
            )

            logger.info("위성 추적 스케줄 생성 완료: ${schedule.trackingPasses.size}개 패스")

            // 생성 메타데이터를 위한 현재 날짜와 사용자 정보
            val creationDate = ZonedDateTime.now()
            val creator = "System"

            // 스케줄 정보로 마스터 리스트 채우기
            schedule.trackingPasses.forEachIndexed { index, pass ->
                val mstId = index + 1

                ephemerisTrackMst.add(mapOf(
                    "No" to mstId,
                    "SatelliteID" to satelliteId,
                    "SatelliteName" to actualSatelliteName,
                    "StartTime" to pass.startTime,
                    "EndTime" to pass.endTime,
                    "Duration" to pass.getDurationString(),
                    "MaxElevation" to pass.maxElevation,
                    "MaxAzRate" to pass.maxAzimuthRate,
                    "MaxElRate" to pass.maxElevationRate,
                    "MaxAzAccel" to pass.maxAzimuthAccel,
                    "MaxElAccel" to pass.maxElevationAccel,
                    "CreationDate" to creationDate,
                    "Creator" to creator
                ))

                // 추적 좌표로 세부 리스트 채우기
                pass.trackingData.forEachIndexed { dtlIndex, data ->
                    ephemerisTrackDtl.add(mapOf(
                        "No" to (dtlIndex + 1),
                        "MstId" to mstId,  // 마스터 리스트의 No 값을 MstId로 사용
                        "Time" to data.timestamp,
                        "Azimuth" to data.azimuth,
                        "Elevation" to data.elevation,
                        "Range" to data.range,
                        "Altitude" to data.altitude
                    ))
                }
            }

            logger.info("위성 궤도 추적 데이터 생성 완료: ${ephemerisTrackMst.size}개 스케줄 항목과 ${ephemerisTrackDtl.size}개 좌표 포인트")

            // 저장소에 데이터 저장
            ephemerisTrackMstStorage.clear()
            ephemerisTrackDtlStorage.clear()
            ephemerisTrackMstStorage.addAll(ephemerisTrackMst)
            ephemerisTrackDtlStorage.addAll(ephemerisTrackDtl)

            return Pair(ephemerisTrackMst, ephemerisTrackDtl)

        } catch (e: Exception) {
            logger.error("위성 궤도 추적 중 오류 발생: ${e.message}", e)
            throw e
        }
    }

    /**
     * 모든 위성 추적 마스터 데이터 조회
     */
    fun getAllEphemerisTrackMst(): List<Map<String, Any?>> {
        return ephemerisTrackMstStorage.toList()
    }

    /**
     * 특정 마스터 ID에 해당하는 세부 추적 데이터 조회
     */
    fun getEphemerisTrackDtlByMstId(mstId: Int): List<Map<String, Any?>> {
        return ephemerisTrackDtlStorage.filter { it["MstId"] == mstId }
    }

    /**
     * 위성 ID로부터 위성 이름을 가져오는 헬퍼 함수
     * 실제 애플리케이션에서는 데이터베이스를 조회할 것입니다
     */
    private fun getSatelliteNameFromId(satelliteId: String): String {
        // 이것은 임시 구현입니다 - 실제 애플리케이션에서는 ID를 기반으로
        // 데이터베이스나 다른 소스에서 이름을 조회할 것입니다
        return when (satelliteId) {
            "27424" -> "AQUA"
            "25544" -> "ISS"
            "43013" -> "NOAA-20"
            else -> "Satellite-$satelliteId"
        }
    }

    /**
     * 위성 TLE 데이터를 캐시에 추가합니다.
     */
    fun addSatelliteTle(satelliteId: String, tleLine1: String, tleLine2: String) {
        satelliteTleCache[satelliteId] = Pair(tleLine1, tleLine2)
        logger.info("위성 TLE 데이터가 캐시에 추가되었습니다. 위성 ID: $satelliteId")
    }

    /**
     * 위성 TLE 데이터를 캐시에서 가져옵니다.
     */
    fun getSatelliteTle(satelliteId: String): Pair<String, String>? {
        return satelliteTleCache[satelliteId]
    }

    /**
     * 위성 TLE 데이터를 캐시에서 삭제합니다.
     */
    fun removeSatelliteTle(satelliteId: String) {
        satelliteTleCache.remove(satelliteId)
        logger.info("위성 TLE 데이터가 캐시에서 삭제되었습니다. 위성 ID: $satelliteId")
    }

    /**
     * 캐시된 모든 위성 ID 목록을 반환합니다.
     */
    fun getAllSatelliteIds(): List<String> {
        return satelliteTleCache.keys.toList()
    }

}