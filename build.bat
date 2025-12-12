@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM 获取服务名称参数（可选）
set SERVICE_NAME=%~1

echo ========================================
echo   RuoYi-Magic 打包脚本
echo ========================================
if defined SERVICE_NAME (
    echo   服务名称: %SERVICE_NAME%
) else (
    echo   服务名称: 默认
)
echo ========================================
echo.

cd /d %~dp0

REM 清理旧的 JAR 文件
echo [1/4] 正在清理旧的 JAR 文件...
if exist target\ruoyi-*.jar (
    del /q target\ruoyi-*.jar 2>nul
    echo       已删除旧的 JAR 文件
) else (
    echo       没有旧的 JAR 文件需要清理
)

if exist target\ruoyi-*.jar.original (
    del /q target\ruoyi-*.jar.original 2>nul
    echo       已删除旧的 .original 文件
)

echo.

REM 执行 Maven 打包
echo [2/4] 正在执行 Maven 打包...
echo.
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo   打包失败！请检查错误信息
    echo ========================================
    pause
    exit /b 1
)

echo.

REM 重命名 JAR 文件（添加服务名和时间戳）
echo [3/4] 正在重命名 JAR 文件...

REM 生成时间戳 (格式: YYYYMMDD-HHmmss)
for /f "delims=" %%i in ('powershell -Command "Get-Date -Format 'yyyyMMdd-HHmmss'"') do set TIMESTAMP=%%i

REM 查找并重命名 JAR 文件
for %%f in (target\ruoyi-*.jar) do (
    if not "%%~xf"==".original" (
        set ORIGINAL_JAR=%%f
        set JAR_NAME=%%~nf
        
        REM 构建新的 JAR 文件名
        if defined SERVICE_NAME (
            set NEW_JAR=target\ruoyi-!SERVICE_NAME!-!TIMESTAMP!.jar
        ) else (
            set NEW_JAR=target\ruoyi-!TIMESTAMP!.jar
        )
        
        REM 重命名文件
        move /y "!ORIGINAL_JAR!" "!NEW_JAR!" >nul
        echo       已重命名: !NEW_JAR!
    )
)

echo.

REM 显示打包结果
echo [4/4] 打包完成！
echo.
echo ========================================
echo   生成的 JAR 文件:
echo ========================================
for %%f in (target\ruoyi-*.jar) do (
    if not "%%~xf"==".original" (
        echo   %%f
        echo   大小: %%~zf 字节
    )
)
echo ========================================
echo.

pause
