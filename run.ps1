# ACS API 서버 실행 스크립트
# UTF-8 인코딩으로 한글이 제대로 표시됩니다

Write-Host "ACS API 서버를 시작합니다..." -ForegroundColor Green

# 환경 변수 설정
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# JAR 파일 실행
java "-Dfile.encoding=UTF-8" "-Dsun.stdout.encoding=UTF-8" "-Dsun.stderr.encoding=UTF-8" -jar build\libs\acs_api-0.0.1-SNAPSHOT.jar
