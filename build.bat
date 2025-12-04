@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ========================================
echo   RuoYi-Magic 打包脚本
echo ========================================
echo.

cd /d %~dp0

REM 清理旧的 JAR 文件
echo [1/3] 正在清理旧的 JAR 文件...
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
echo [2/3] 正在执行 Maven 打包...
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

REM 显示打包结果
echo [3/3] 打包完成！
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
