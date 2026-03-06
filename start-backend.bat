@echo off
:: Switch Windows console to UTF-8 so Vietnamese characters display correctly
chcp 65001 >nul

cd /d C:\Users\Admin\library-management-system\lms-backend
set SPRING_PROFILES_ACTIVE=dev
set DB_PASSWORD=
:: JAVA_TOOL_OPTIONS ensures UTF-8 even when run from other terminals / IDEs
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8
mvn spring-boot:run
pause
