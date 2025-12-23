package com.gtlsystems.acs_api.config

import jakarta.annotation.PostConstruct
import org.orekit.data.DataContext
import org.orekit.data.DirectoryCrawler
import org.orekit.frames.FramesFactory
import org.orekit.time.TimeScalesFactory
import org.orekit.utils.IERSConventions
import org.orekit.time.TimeScale
import org.orekit.bodies.CelestialBody
import org.orekit.bodies.OneAxisEllipsoid
import org.orekit.frames.Frame
import org.orekit.utils.Constants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileOutputStream
import java.net.JarURLConnection
import java.nio.file.Files
import java.util.zip.ZipInputStream

@Configuration
class OrekitConfig {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${orekit.data.path:./orekit-data}")
    private lateinit var orekitDataPath: String

    @PostConstruct
    fun initializeOrekit() {
        try {
            logger.info("Orekit 데이터 초기화 시작...")

            // 이미 초기화되었는지 확인
            val existingProviders = DataContext.getDefault().dataProvidersManager.providers
            if (existingProviders.isNotEmpty()) {
                logger.info("Orekit 데이터가 이미 초기화되어 있습니다.")
                return
            }

            // 1. 클래스패스에서 orekit-data-main 찾기
            val success = tryLoadFromClasspath()

            if (!success) {
                // 2. 외부 파일에서 찾기 (백업 방법)
                tryLoadFromExternalFile()
            }

            // ✅ 초기화 검증 - Bean 참조 제거
            verifyOrekitDataSimple()

        } catch (e: Exception) {
            logger.error("Orekit 데이터 초기화 실패: ${e.message}", e)
            throw RuntimeException("Orekit 초기화 실패", e)
        }
    }

    // ✅ 기존 verifyOrekitData 메서드를 간단하게 수정
    private fun verifyOrekitDataSimple() {
        try {
            logger.info("Orekit 데이터 검증 시작...")

            // ✅ Bean 참조 없이 직접 TimeScale 생성
            val utc = TimeScalesFactory.getUTC()
            logger.info("UTC TimeScale 초기화 성공")

            // ✅ SimpleEOP = true로 설정하여 순환 의존성 방지
            val ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true)
            logger.info("UT1 TimeScale 초기화 성공")

            val itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true)
            logger.info("ITRF 프레임 초기화 성공")

            val celestialBodies = DataContext.getDefault().celestialBodies
            val sun = celestialBodies.sun
            logger.info("태양 천체 객체 초기화 성공")

            logger.info("Orekit 데이터 검증 완료")

        } catch (e: Exception) {
            logger.warn("Orekit 데이터 검증 중 경고: ${e.message}", e)
        }
    }

    // ✅ 기존 verifyOrekitData 메서드는 제거하거나 주석 처리
    /*
    private fun verifyOrekitData() {
        // 이 메서드에서 Bean을 참조하면서 순환 의존성 발생
        // 주석 처리 또는 삭제
    }
    */

    private fun tryLoadFromClasspath(): Boolean {
        return try {
            logger.info("클래스패스에서 orekit-data-main 로드 시도...")

            val classLoader = javaClass.classLoader
            val orekitDataUrl = classLoader.getResource("orekit-data-main")

            if (orekitDataUrl == null) {
                logger.warn("클래스패스에서 orekit-data-main을 찾을 수 없습니다.")
                return false
            }

            val orekitDataDir = if (orekitDataUrl.protocol == "jar") {
                val tempDir = Files.createTempDirectory("orekit-data").toFile()
                tempDir.deleteOnExit()
                copyResourcesFromJar("orekit-data-main", tempDir)
                tempDir
            } else {
                File(orekitDataUrl.toURI())
            }

            DataContext.getDefault().dataProvidersManager.addProvider(
                DirectoryCrawler(orekitDataDir)
            )

            logger.info("클래스패스에서 Orekit 데이터 로드 성공: ${orekitDataDir.absolutePath}")
            true

        } catch (e: Exception) {
            logger.warn("클래스패스에서 Orekit 데이터 로드 실패: ${e.message}")
            false
        }
    }

    private fun tryLoadFromExternalFile() {
        logger.info("외부 파일에서 orekit 데이터 로드 시도...")
        // 필요시 구현
    }

    private fun copyResourcesFromJar(resourcePath: String, targetDir: File) {
        val classLoader = javaClass.classLoader
        val jarUrl = classLoader.getResource(resourcePath) ?: return
        val jarConnection = jarUrl.openConnection() as JarURLConnection
        val jarFile = jarConnection.jarFile

        val entries = jarFile.entries()
        val prefix = "$resourcePath/"

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.startsWith(prefix) && !entry.isDirectory) {
                val destFile = File(targetDir, entry.name.substring(prefix.length))
                destFile.parentFile?.mkdirs()

                classLoader.getResourceAsStream(entry.name)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    // ✅ UTC 시간 척도 Bean
    @Bean
    fun utcTimeScale(): TimeScale {
        return TimeScalesFactory.getUTC()
    }

    // ✅ UT1 시간 척도 Bean (EOP 사용 여부에 따라 다르게 설정)
    @Bean
    fun ut1TimeScale(): TimeScale {
        return TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true)
    }

    // ✅ ITRF 프레임 Bean
    @Bean
    fun earthFrame(): Frame {
        return FramesFactory.getITRF(IERSConventions.IERS_2010, true)
    }

    // ✅ 지구 모델 Bean
    @Bean
    fun earthModel(earthFrame: Frame): OneAxisEllipsoid {
        return OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            earthFrame
        )
    }

    // ✅ 태양 천체 Bean
    @Bean
    fun sun(): CelestialBody {
        return DataContext.getDefault().celestialBodies.sun
    }

    // ✅ Orekit 초기화 상태 Bean
    @Bean
    fun orekitInitializationStatus(): OrekitInitializationStatus {
        val providers = DataContext.getDefault().dataProvidersManager.providers
        return OrekitInitializationStatus(
            isInitialized = providers.isNotEmpty(),
            dataProvidersCount = providers.size
        )
        }
    data class OrekitInitializationStatus(
        val isInitialized: Boolean,
        val dataProvidersCount: Int
    )
}
