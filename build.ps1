Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPrefix = 'ruoyi'

Write-Host '========================================'
Write-Host '  RuoYi-Magic 打包部署脚本'
Write-Host '========================================'
Write-Host ''

Set-Location -LiteralPath $scriptDir

Write-Host '[1/3] 正在清理旧的 JAR 文件...'
Remove-Item -LiteralPath (Join-Path $scriptDir "target\$jarPrefix-*.jar") -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath (Join-Path $scriptDir "target\$jarPrefix-*.jar.original") -Force -ErrorAction SilentlyContinue
Write-Host '      清理完成'
Write-Host ''

Write-Host '[2/3] 正在执行 Maven 打包...'
Write-Host ''
& mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host '[ERROR] 打包失败，请检查错误信息' -ForegroundColor Red
    exit $LASTEXITCODE
}

$builtJar = Get-ChildItem -LiteralPath (Join-Path $scriptDir 'target') -Filter "$jarPrefix-*.jar" -File |
    Where-Object { $_.Extension -eq '.jar' } |
    Sort-Object LastWriteTime |
    Select-Object -Last 1

if (-not $builtJar) {
    Write-Host '[ERROR] 未找到打包生成的 JAR 文件' -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host "      JAR: $($builtJar.FullName)"
Write-Host ''

Write-Host '[3/3] 正在部署到服务器...'
Write-Host ''
& node (Join-Path $scriptDir 'deploy\index.mjs') $builtJar.FullName
if ($LASTEXITCODE -ne 0) {
    Write-Host ''
    Write-Host '[ERROR] 部署失败，请检查错误信息' -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host ''
Write-Host '========================================'
Write-Host "  打包部署完成：$($builtJar.Name)"
Write-Host '========================================'
Write-Host ''
