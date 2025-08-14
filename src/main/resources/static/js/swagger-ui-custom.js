// Swagger UI 커스텀 JavaScript
(function() {
    'use strict';
    
    // 페이지 로드 완료 후 실행
    function initLanguageSelector() {
        // 언어 선택 드롭다운 생성
        createLanguageSelector();
        
        // 언어 정보 박스 생성
        createLanguageInfo();
        
        // 초기 언어 설정
        setCurrentLanguage('ko');
    }
    
    // 언어 선택 드롭다운 생성
    function createLanguageSelector() {
        // 이미 존재하는지 확인
        if (document.getElementById('language-selector')) {
            return;
        }
        
        const selector = document.createElement('div');
        selector.id = 'language-selector';
        selector.className = 'language-selector';
        selector.innerHTML = `
            <label for="languageSelect">🌐 언어 선택:</label>
            <select id="languageSelect" onchange="changeLanguage()">
                <option value="ko">🇰🇷 한국어</option>
                <option value="en">🇺🇸 English</option>
            </select>
        `;
        
        // 스타일 적용
        selector.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            background: white;
            border: 2px solid #3b4151;
            border-radius: 8px;
            padding: 10px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        `;
        
        // 페이지에 추가
        document.body.appendChild(selector);
        
        // select 스타일 적용
        const select = selector.querySelector('select');
        select.style.cssText = `
            padding: 8px 12px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 14px;
            background: white;
            cursor: pointer;
        `;
        
        // label 스타일 적용
        const label = selector.querySelector('label');
        label.style.cssText = `
            font-weight: bold;
            margin-right: 8px;
            color: #3b4151;
        `;
    }
    
    // 언어 정보 박스 생성
    function createLanguageInfo() {
        // 이미 존재하는지 확인
        if (document.getElementById('language-info')) {
            return;
        }
        
        const info = document.createElement('div');
        info.id = 'language-info';
        info.className = 'language-info';
        info.innerHTML = `
            <h4>📚 언어별 API 문서</h4>
            <p><span class="flag">🇰🇷</span> 한국어: <a href="/v3/api-docs" target="_blank">OpenAPI Spec</a></p>
            <p><span class="flag">🇺🇸</span> English: <a href="/v3/api-docs-english" target="_blank">OpenAPI Spec</a></p>
            <p><small>언어를 변경하면 해당 언어의 API 설명이 표시됩니다.</small></p>
        `;
        
        // 스타일 적용
        info.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            z-index: 9998;
            background: #f8f9fa;
            border: 1px solid #dee2e6;
            border-radius: 6px;
            padding: 15px;
            max-width: 300px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        `;
        
        // 페이지에 추가
        document.body.appendChild(info);
        
        // h4 스타일 적용
        const h4 = info.querySelector('h4');
        h4.style.cssText = `
            margin: 0 0 10px 0;
            color: #495057;
            font-size: 16px;
        `;
        
        // p 스타일 적용
        const paragraphs = info.querySelectorAll('p');
        paragraphs.forEach(p => {
            p.style.cssText = `
                margin: 5px 0;
                font-size: 14px;
                color: #6c757d;
            `;
        });
        
        // flag 스타일 적용
        const flags = info.querySelectorAll('.flag');
        flags.forEach(flag => {
            flag.style.cssText = `
                font-size: 20px;
                margin-right: 8px;
            `;
        });
    }
    
    // 언어 변경 함수 (전역 함수로 등록)
    window.changeLanguage = function() {
        const select = document.getElementById('languageSelect');
        const newLanguage = select.value;
        
        if (newLanguage !== getCurrentLanguage()) {
            setCurrentLanguage(newLanguage);
            reloadSwaggerUI(newLanguage);
            updateLanguageInfo(newLanguage);
        }
    };
    
    // 현재 언어 가져오기
    function getCurrentLanguage() {
        return localStorage.getItem('swagger-language') || 'ko';
    }
    
    // 현재 언어 설정
    function setCurrentLanguage(language) {
        localStorage.setItem('swagger-language', language);
        
        // select 값 업데이트
        const select = document.getElementById('languageSelect');
        if (select) {
            select.value = language;
        }
    }
    
    // Swagger UI 다시 로드
    function reloadSwaggerUI(language) {
        const url = language === 'ko' ? '/v3/api-docs' : '/v3/api-docs-english';
        
        // Swagger UI 인스턴스가 있는지 확인
        if (window.ui) {
            // 기존 UI 제거
            const swaggerContainer = document.getElementById('swagger-ui');
            if (swaggerContainer) {
                swaggerContainer.innerHTML = '';
            }
            
            // 새로운 URL로 다시 로드
            window.ui.specActions.updateUrl(url);
            window.ui.specActions.download(url);
        }
    }
    
    // 언어 정보 업데이트
    function updateLanguageInfo(language) {
        const infoDiv = document.getElementById('language-info');
        if (!infoDiv) return;
        
        if (language === 'ko') {
            infoDiv.innerHTML = `
                <h4>📚 한국어 API 문서</h4>
                <p><span class="flag">🇰🇷</span> 한국어: <a href="/v3/api-docs" target="_blank">OpenAPI Spec</a></p>
                <p><span class="flag">🇺🇸</span> English: <a href="/v3/api-docs-english" target="_blank">OpenAPI Spec</a></p>
                <p><small>현재 한국어로 API 설명이 표시됩니다.</small></p>
            `;
        } else {
            infoDiv.innerHTML = `
                <h4>📚 English API Documentation</h4>
                <p><span class="flag">🇰🇷</span> 한국어: <a href="/v3/api-docs" target="_blank">OpenAPI Spec</a></p>
                <p><span class="flag">🇺🇸</span> English: <a href="/v3/api-docs-english" target="_blank">OpenAPI Spec</a></p>
                <p><small>Currently displaying API descriptions in English.</small></p>
            `;
        }
        
        // 스타일 다시 적용
        const h4 = infoDiv.querySelector('h4');
        h4.style.cssText = `
            margin: 0 0 10px 0;
            color: #495057;
            font-size: 16px;
        `;
        
        const paragraphs = infoDiv.querySelectorAll('p');
        paragraphs.forEach(p => {
            p.style.cssText = `
                margin: 5px 0;
                font-size: 14px;
                color: #6c757d;
            `;
        });
        
        const flags = infoDiv.querySelectorAll('.flag');
        flags.forEach(flag => {
            flag.style.cssText = `
                font-size: 20px;
                margin-right: 8px;
            `;
        });
    }
    
    // Swagger UI 로드 완료 감지
    function waitForSwaggerUI() {
        if (document.getElementById('swagger-ui')) {
            // Swagger UI가 로드된 후 언어 선택기 초기화
            setTimeout(initLanguageSelector, 1000);
        } else {
            // 아직 로드되지 않았으면 다시 시도
            setTimeout(waitForSwaggerUI, 100);
        }
    }
    
    // 페이지 로드 시 실행
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', waitForSwaggerUI);
    } else {
        waitForSwaggerUI();
    }
    
})(); 