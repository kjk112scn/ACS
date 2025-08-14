package com.gtlsystems.acs_api.controller.system

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/swagger-ui")
class SwaggerUiController {

    @GetMapping("/index.html")
    fun getCustomSwaggerUi(): ResponseEntity<String> {
        val resource = ClassPathResource("static/swagger-ui/index.html")
        val htmlContent = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(htmlContent)
    }

    @GetMapping("/swagger-ui.css")
    fun getSwaggerUiCss(): ResponseEntity<String> {
        val resource = ClassPathResource("static/swagger-ui/swagger-ui.css")
        val cssContent = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        
        return ResponseEntity.ok()
            .contentType(MediaType.valueOf("text/css"))
            .body(cssContent)
    }

    @GetMapping("/swagger-ui-bundle.js")
    fun getSwaggerUiBundleJs(): ResponseEntity<String> {
        val resource = ClassPathResource("static/swagger-ui/swagger-ui-bundle.js")
        val jsContent = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        
        return ResponseEntity.ok()
            .contentType(MediaType.valueOf("application/javascript"))
            .body(jsContent)
    }

    @GetMapping("/swagger-ui-standalone-preset.js")
    fun getSwaggerUiStandalonePresetJs(): ResponseEntity<String> {
        val resource = ClassPathResource("static/swagger-ui/swagger-ui-standalone-preset.js")
        val jsContent = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        
        return ResponseEntity.ok()
            .contentType(MediaType.valueOf("application/javascript"))
            .body(jsContent)
    }
    
    // 영어 API 문서를 위한 별도 엔드포인트
    @GetMapping("/english")
    fun getEnglishSwaggerUi(): ResponseEntity<String> {
        val resource = ClassPathResource("static/swagger-ui/index.html")
        var htmlContent = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        
        // 영어로 초기화하도록 수정
        htmlContent = htmlContent.replace(
            "initSwaggerUI('/v3/api-docs');",
            "initSwaggerUI('/v3/api-docs-english');"
        )
        
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(htmlContent)
    }
} 