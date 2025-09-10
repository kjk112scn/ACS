package com.gtlsystems.acs_api.repository.interfaces.settings

import com.gtlsystems.acs_api.settings.entity.Setting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SettingsRepository : JpaRepository<Setting, Long> {
    
    /**
     * 설정 키로 설정 조회
     */
    fun findByKey(key: String): Setting?
    
    /**
     * 시스템 설정 여부로 조회
     */
    fun findByIsSystemSetting(isSystemSetting: Boolean): List<Setting>
    
    /**
     * 설정 키가 존재하는지 확인
     */
    fun existsByKey(key: String): Boolean
    
    /**
     * 설정 키로 삭제
     */
    //fun deleteByKey(key: String)
    
    /**
     * 설정 키 목록으로 조회
     */
    fun findByKeyIn(keys: List<String>): List<Setting>
}