#!/bin/bash

# 若依项目服务控制脚本 - Java 17 + AppCDS
# 脚本和jar包放在同一目录下使用

# ==================== 配置区域 ====================
# 应用配置
APP_NAME="ruoyi"
APP_JAR_PATTERN="ruoyi-*.jar"     # jar包文件名模式
APP_PORT=8080                     # 应用端口
SPRING_PROFILE="prod"              # 环境配置: dev/test/prod

# 部署配置
KEEP_HISTORY_JARS=2               # 保留历史jar文件数量（包含当前使用的）
STARTUP_TIMEOUT=30                # 启动超时时间（秒）

# AppCDS配置
ENABLE_CDS=true                   # 是否启用AppCDS加速
WORKDIR_PREFIX="work"             # 工作目录前缀

# JVM参数配置 (Java 17优化)
BASE_JVM_OPTS="-server \
-Xms256m \
-Xmx1200m \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:+UseStringDeduplication \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=./logs/ \
-Xlog:gc*:./logs/gc.log:time,tags \
-XX:+ShowCodeDetailsInExceptionMessages \
-Dfile.encoding=UTF-8 \
-Djava.security.egd=file:/dev/./urandom"

# AppCDS参数 (Java 17)
CDS_JVM_OPTS="-Xlog:cds:./logs/cds.log:time,tags"

# ==================== 脚本逻辑 ====================
# 获取脚本所在目录
APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$APP_HOME/$APP_NAME.pid"
LOG_FILE="$APP_HOME/logs/$APP_NAME.log"
CURRENT_JAR_FILE="$APP_HOME/.current_jar"
BACKUP_JAR_FILE="$APP_HOME/.backup_jar"

# 获取jar对应的工作目录
get_jar_workdir() {
    local jar_file="$1"
    if [ -z "$jar_file" ]; then
        echo ""
        return 1
    fi
    # 提取jar文件名（无扩展名）作为目录名
    local jar_name=$(basename "$jar_file" .jar)
    echo "$APP_HOME/${WORKDIR_PREFIX}_$jar_name"
}

# 获取jar对应的CDS文件
get_jar_cds() {
    local jar_file="$1"
    if [ -z "$jar_file" ]; then
        echo ""
        return 1
    fi
    local workdir=$(get_jar_workdir "$jar_file")
    echo "$workdir/app.jsa"
}

# 获取jar对应的解压文件
get_jar_extracted() {
    local jar_file="$1"
    if [ -z "$jar_file" ]; then
        echo ""
        return 1
    fi
    local workdir=$(get_jar_workdir "$jar_file")
    echo "$workdir/$jar_file"
}

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 自动查找jar文件
find_jar_file() {
    local jar_files=($(find "$APP_HOME" -maxdepth 1 -name "$APP_JAR_PATTERN" -type f 2>/dev/null | sort -V))
    
    if [ ${#jar_files[@]} -eq 0 ]; then
        echo ""
        return 1
    elif [ ${#jar_files[@]} -eq 1 ]; then
        echo "$(basename "${jar_files[0]}")"
        return 0
    else
        # 多个jar文件时，选择最新的（按文件名排序，时间戳格式会自然排序）
        echo "$(basename "${jar_files[-1]}")"
        return 0
    fi
}

# 获取当前使用的jar文件
get_current_jar() {
    local jar_file=$(find_jar_file)
    if [ -z "$jar_file" ]; then
        echo -e "${RED}错误: 未找到匹配的jar文件 ($APP_JAR_PATTERN)${NC}" >&2
        return 1
    fi
    echo "$jar_file"
}

# 记录当前使用的jar文件
save_current_jar() {
    local jar_file="$1"
    echo "$jar_file" > "$CURRENT_JAR_FILE"
}

# 读取当前使用的jar文件
read_current_jar() {
    if [ -f "$CURRENT_JAR_FILE" ] && [ -s "$CURRENT_JAR_FILE" ]; then
        cat "$CURRENT_JAR_FILE"
    else
        echo ""
    fi
}

# 保存备份jar信息
save_backup_jar() {
    local jar_file="$1"
    echo "$jar_file" > "$BACKUP_JAR_FILE"
}

# 读取备份jar信息
read_backup_jar() {
    if [ -f "$BACKUP_JAR_FILE" ] && [ -s "$BACKUP_JAR_FILE" ]; then
        cat "$BACKUP_JAR_FILE"
    else
        echo ""
    fi
}

# 获取所有jar文件（按时间排序）
get_all_jars() {
    find "$APP_HOME" -maxdepth 1 -name "$APP_JAR_PATTERN" -type f 2>/dev/null | sort -V
}

# 清理历史jar文件和相关目录
cleanup_old_jars() {
    local jar_files=($(get_all_jars))
    local jar_count=${#jar_files[@]}
    
    if [ $jar_count -le $KEEP_HISTORY_JARS ]; then
        echo -e "${CYAN}当前jar文件数量: $jar_count，无需清理${NC}"
        return 0
    fi
    
    local current_jar=$(read_current_jar)
    local backup_jar=$(read_backup_jar)
    
    local remove_count=$((jar_count - KEEP_HISTORY_JARS))
    echo -e "${YELLOW}清理旧的jar文件和工作目录，保留最新的 $KEEP_HISTORY_JARS 个...${NC}"
    
    for ((i=0; i<remove_count; i++)); do
        local old_jar=$(basename "${jar_files[i]}")
        
        # 不删除当前使用的和备份的jar
        if [ "$old_jar" = "$current_jar" ] || [ "$old_jar" = "$backup_jar" ]; then
            echo -e "${CYAN}跳过正在使用的jar: $old_jar${NC}"
            continue
        fi
        
        echo -e "${CYAN}删除旧jar: $old_jar${NC}"
        rm -f "${jar_files[i]}"
        
        # 删除对应的工作目录
        local old_workdir=$(get_jar_workdir "$old_jar")
        if [ -d "$old_workdir" ]; then
            echo -e "${CYAN}删除工作目录: $old_workdir${NC}"
            rm -rf "$old_workdir"
        fi
    done
    
    echo -e "${GREEN}清理完成，已删除 $remove_count 个旧jar文件及相关目录${NC}"
}

# 清理不需要的工作目录（保留当前和备份）
cleanup_work_dirs() {
    local current_jar=$(read_current_jar)
    local backup_jar=$(read_backup_jar)
    
    # 找到所有工作目录
    local work_dirs=($(find "$APP_HOME" -maxdepth 1 -name "${WORKDIR_PREFIX}_*" -type d 2>/dev/null))
    
    for work_dir in "${work_dirs[@]}"; do
        local dir_name=$(basename "$work_dir")
        local jar_name="${dir_name#${WORKDIR_PREFIX}_}.jar"
        
        # 检查对应的jar是否还存在，或者是否是当前/备份jar
        if [ ! -f "$APP_HOME/$jar_name" ] && [ "$jar_name" != "$current_jar" ] && [ "$jar_name" != "$backup_jar" ]; then
            echo -e "${CYAN}清理无用工作目录: $work_dir${NC}"
            rm -rf "$work_dir"
        fi
    done
}

# 检查服务是否正常启动
check_service_health() {
    local timeout=${1:-$STARTUP_TIMEOUT}
    local count=0
    
    echo -e "${BLUE}检查服务健康状态...${NC}"
    
    while [ $count -lt $timeout ]; do
        local pid=$(get_pid)
        if [ -n "$pid" ]; then
            # 检查端口是否可用
            if command -v nc >/dev/null 2>&1; then
                if nc -z localhost $APP_PORT 2>/dev/null; then
                    echo -e "${GREEN}服务启动成功并可正常访问${NC}"
                    return 0
                fi
            elif [ $count -gt 10 ]; then
                # 如果没有nc命令，等待10秒后认为启动成功
                echo -e "${GREEN}服务启动成功${NC}"
                return 0
            fi
        else
            echo -e "${RED}服务进程已退出${NC}"
            return 1
        fi
        
        sleep 1
        count=$((count + 1))
        if [ $((count % 5)) -eq 0 ]; then
            echo -e "${CYAN}等待中... ($count/${timeout}s)${NC}"
        fi
    done
    
    echo -e "${RED}服务启动超时${NC}"
    return 1
}

# 获取进程ID
get_pid() {
    if [ -f "$PID_FILE" ] && [ -s "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 $PID 2>/dev/null; then
            echo $PID
        else
            rm -f "$PID_FILE"
            echo ""
        fi
    else
        echo ""
    fi
}

# 检查CDS归档文件是否存在且有效
check_cds_archive() {
    if [ "$ENABLE_CDS" != "true" ]; then
        return 1
    fi
    
    local current_jar=$(read_current_jar)
    if [ -z "$current_jar" ]; then
        return 1
    fi
    
    local cds_file=$(get_jar_cds "$current_jar")
    if [ -f "$cds_file" ] && [ -s "$cds_file" ]; then
        # 检查CDS文件是否比JAR文件新
        if [ "$cds_file" -nt "$APP_HOME/$current_jar" ]; then
            return 0
        else
            echo -e "${YELLOW}检测到JAR文件更新，需要重新生成CDS归档${NC}"
            return 1
        fi
    else
        return 1
    fi
}

# 解压JAR文件
extract_jar() {
    local jar_file="$1"
    if [ -z "$jar_file" ]; then
        echo -e "${RED}错误: 未指定jar文件${NC}"
        return 1
    fi
    
    echo -e "${CYAN}正在解压JAR文件: $jar_file${NC}"
    
    local workdir=$(get_jar_workdir "$jar_file")
    local extracted_jar=$(get_jar_extracted "$jar_file")
    
    # 清理旧的解压目录
    if [ -d "$workdir" ]; then
        rm -rf "$workdir"
    fi
    
    # 创建工作目录
    mkdir -p "$workdir"
    
    # 使用Spring Boot的自解压功能
    cd "$APP_HOME"
    java -Djarmode=tools -jar "$jar_file" extract --destination "$workdir"
    
    if [ $? -eq 0 ] && [ -f "$extracted_jar" ]; then
        echo -e "${GREEN}JAR文件解压成功 -> $workdir${NC}"
        return 0
    else
        echo -e "${RED}JAR文件解压失败，将使用普通模式启动${NC}"
        return 1
    fi
}

# 生成CDS归档文件
generate_cds() {
    local jar_file="$1"
    if [ -z "$jar_file" ]; then
        echo -e "${RED}错误: 未指定jar文件${NC}"
        return 1
    fi
    
    echo -e "${CYAN}正在为 $jar_file 生成AppCDS归档文件...${NC}"
    
    local workdir=$(get_jar_workdir "$jar_file")
    local extracted_jar=$(get_jar_extracted "$jar_file")
    local cds_file=$(get_jar_cds "$jar_file")
    
    # 确保解压目录存在
    if [ ! -f "$extracted_jar" ]; then
        if ! extract_jar "$jar_file"; then
            return 1
        fi
    fi
    
    # 删除旧的CDS文件
    rm -f "$cds_file"
    
    # 创建日志目录
    mkdir -p "$APP_HOME/logs"
    
    # 检查可用内存，动态调整参数
    echo -e "${BLUE}检查系统内存...${NC}"
    local available_mem=512  # 默认值
    if command -v free >/dev/null 2>&1; then
        available_mem=$(free -m | awk 'NR==2{print $7}')
        echo -e "${BLUE}系统可用内存: ${available_mem}MB${NC}"
    fi
    
    # 根据可用内存调整CDS构建参数
    if [ "$available_mem" -lt 800 ]; then
        echo -e "${YELLOW}内存紧张，使用最小配置${NC}"
        local CDS_BUILD_OPTS="-server -Xms128m -Xmx384m -XX:+UseSerialGC -Dfile.encoding=UTF-8"
    elif [ "$available_mem" -lt 1500 ]; then
        echo -e "${YELLOW}内存适中，使用保守配置${NC}"
        local CDS_BUILD_OPTS="-server -Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8"
    else
        echo -e "${GREEN}内存充足，使用标准配置${NC}"
        local CDS_BUILD_OPTS="-server -Xms256m -Xmx768m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Dfile.encoding=UTF-8"
    fi
    
    cd "$APP_HOME"
    echo -e "${BLUE}开始生成CDS归档...${NC}"
    
    # 添加超时保护，并记录详细日志
    timeout 300s java $CDS_BUILD_OPTS \
        -Xlog:cds:"$APP_HOME/logs/cds-$jar_file.log":time,tags \
        -XX:ArchiveClassesAtExit="$cds_file" \
        -Dspring.context.exit=onRefresh \
        -Dspring.profiles.active=$SPRING_PROFILE \
        -jar "$extracted_jar" \
        > "$APP_HOME/logs/cds-generation-$jar_file.log" 2>&1
    
    local exit_code=$?
    
    if [ $exit_code -eq 124 ]; then
        echo -e "${RED}CDS生成超时，应用可能启动过慢${NC}"
        return 1
    elif [ $exit_code -eq 137 ]; then
        echo -e "${RED}CDS生成被系统杀死，内存不足${NC}"
        echo -e "${YELLOW}建议: 关闭其他进程或增加系统内存${NC}"
        return 1
    elif [ $exit_code -ne 0 ]; then
        echo -e "${RED}CDS生成失败，退出码: $exit_code${NC}"
        echo -e "${YELLOW}查看详细日志: $APP_HOME/logs/cds-generation-$jar_file.log${NC}"
        return 1
    fi
    
    if [ -f "$cds_file" ] && [ -s "$cds_file" ]; then
        CDS_SIZE=$(du -h "$cds_file" | cut -f1)
        echo -e "${GREEN}AppCDS归档生成成功! (大小: $CDS_SIZE)${NC}"
        return 0
    else
        echo -e "${RED}AppCDS归档文件不存在或为空${NC}"
        return 1
    fi
}

# 启动服务
start() {
    echo -e "${BLUE}正在启动 $APP_NAME...${NC}"
    
    # 检查是否已运行
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${YELLOW}$APP_NAME 已经在运行 (PID: $PID)${NC}"
        return 1
    fi
    
    # 获取要使用的jar文件
    local current_jar=$(read_current_jar)
    if [ -z "$current_jar" ] || [ ! -f "$APP_HOME/$current_jar" ]; then
        # 如果没有记录或文件不存在，使用最新的jar
        current_jar=$(get_current_jar) || return 1
        save_current_jar "$current_jar"
    fi
    
    echo -e "${CYAN}使用JAR文件: $current_jar${NC}"
    
    # 检查jar文件
    if [ ! -f "$APP_HOME/$current_jar" ]; then
        echo -e "${RED}错误: 找不到jar文件 $APP_HOME/$current_jar${NC}"
        return 1
    fi
    
    # 创建日志目录
    mkdir -p "$APP_HOME/logs"
    
    # 构建启动命令
    local JAVA_OPTS="$BASE_JVM_OPTS"
    local JAR_FILE="$APP_HOME/$current_jar"
    local STARTUP_MODE="普通模式"
    
    # 尝试使用AppCDS
    if [ "$ENABLE_CDS" = "true" ]; then
        local cds_file=$(get_jar_cds "$current_jar")
        local extracted_jar=$(get_jar_extracted "$current_jar")
        
        if check_cds_archive; then
            # 使用现有的CDS归档
            echo -e "${GREEN}使用AppCDS加速启动${NC}"
            JAVA_OPTS="$BASE_JVM_OPTS -Xlog:cds:./logs/cds-runtime.log:time,tags -XX:SharedArchiveFile=$cds_file"
            JAR_FILE="$extracted_jar"
            STARTUP_MODE="AppCDS加速模式"
        else
            # 自动生成CDS归档
            echo -e "${YELLOW}生成AppCDS归档...${NC}"
            if extract_jar "$current_jar" && generate_cds "$current_jar"; then
                echo -e "${GREEN}使用AppCDS加速启动${NC}"
                JAVA_OPTS="$BASE_JVM_OPTS -Xlog:cds:./logs/cds-runtime.log:time,tags -XX:SharedArchiveFile=$cds_file"
                JAR_FILE="$extracted_jar"
                STARTUP_MODE="AppCDS加速模式"
            else
                echo -e "${YELLOW}使用普通模式启动${NC}"
            fi
        fi
    fi
    
    # 启动应用
    cd "$APP_HOME"
    nohup java $JAVA_OPTS \
        -Dspring.profiles.active=$SPRING_PROFILE \
        -Dserver.port=$APP_PORT \
        -jar "$JAR_FILE" \
        > "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_FILE"
    
    # 等待启动
    echo -e "${BLUE}等待应用启动...${NC}"
    for i in {1..20}; do
        sleep 1
        PID=$(get_pid)
        if [ -n "$PID" ]; then
            # 检查端口是否可用
            if command -v nc >/dev/null 2>&1; then
                if nc -z localhost $APP_PORT 2>/dev/null; then
                    break
                fi
            elif [ $i -gt 10 ]; then
                break
            fi
        else
            echo -e "${RED}$APP_NAME 启动失败，请查看日志: $LOG_FILE${NC}"
            rm -f "$PID_FILE"
            return 1
        fi
        echo -n "."
    done
    echo ""
    
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${GREEN}$APP_NAME 启动成功!${NC}"
        echo -e "${GREEN}PID: $PID${NC}"
        echo -e "${GREEN}JAR: $current_jar${NC}"
        echo -e "${GREEN}端口: $APP_PORT${NC}"
        echo -e "${GREEN}模式: $STARTUP_MODE${NC}"
        echo -e "${GREEN}访问地址: http://localhost:$APP_PORT${NC}"
        
        if [ "$STARTUP_MODE" = "AppCDS加速模式" ]; then
            echo -e "${CYAN}🚀 使用AppCDS加速，启动速度已优化${NC}"
        fi
        return 0
    else
        echo -e "${RED}$APP_NAME 启动失败，请查看日志: $LOG_FILE${NC}"
        rm -f "$PID_FILE"
        return 1
    fi
}

# 停止服务
stop() {
    echo -e "${BLUE}正在停止 $APP_NAME...${NC}"
    
    PID=$(get_pid)
    if [ -z "$PID" ]; then
        echo -e "${YELLOW}$APP_NAME 未运行${NC}"
        return 1
    fi
    
    # 优雅停止
    kill $PID
    
    # 等待进程结束
    for i in {1..15}; do
        if ! kill -0 $PID 2>/dev/null; then
            rm -f "$PID_FILE"
            echo -e "${GREEN}$APP_NAME 已停止${NC}"
            return 0
        fi
        sleep 1
    done
    
    # 强制停止
    echo -e "${YELLOW}强制停止 $APP_NAME...${NC}"
    kill -9 $PID 2>/dev/null
    rm -f "$PID_FILE"
    echo -e "${GREEN}$APP_NAME 已强制停止${NC}"
}

# 重启服务
restart() {
    echo -e "${BLUE}正在重启 $APP_NAME...${NC}"
    stop
    sleep 2
    start
}

# 查看状态
status() {
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${GREEN}$APP_NAME 正在运行${NC}"
        echo "PID: $PID"
        echo "端口: $APP_PORT"
        echo "环境: $SPRING_PROFILE"
        
        # 显示当前使用的jar文件
        local current_jar=$(read_current_jar)
        if [ -n "$current_jar" ]; then
            echo "当前JAR: $current_jar"
        fi
        
        # 显示备份jar文件
        local backup_jar=$(read_backup_jar)
        if [ -n "$backup_jar" ] && [ -f "$APP_HOME/$backup_jar" ]; then
            echo "备份JAR: $backup_jar"
        fi
        
        # 检查AppCDS状态
        local cds_file=$(get_jar_cds "$current_jar")
        if [ "$ENABLE_CDS" = "true" ] && [ -f "$cds_file" ]; then
            CDS_SIZE=$(du -h "$cds_file" 2>/dev/null | cut -f1)
            echo "AppCDS: ✅ 已启用 (归档: $CDS_SIZE)"
        else
            echo "AppCDS: ❌ 未启用"
        fi
        
        # 显示内存使用情况
        if command -v ps >/dev/null 2>&1; then
            MEM=$(ps -p $PID -o rss= 2>/dev/null | awk '{print int($1/1024)}' 2>/dev/null)
            if [ -n "$MEM" ] && [ "$MEM" -gt 0 ]; then
                echo "内存: ${MEM}MB"
            fi
        fi
        return 0
    else
        echo -e "${RED}$APP_NAME 未运行${NC}"
        
        # 即使未运行也显示jar信息
        local current_jar=$(read_current_jar)
        if [ -n "$current_jar" ]; then
            echo "记录的JAR: $current_jar"
        fi
        
        local backup_jar=$(read_backup_jar)
        if [ -n "$backup_jar" ] && [ -f "$APP_HOME/$backup_jar" ]; then
            echo "备份JAR: $backup_jar"
        fi
        
        return 1
    fi
}

# 查看日志
logs() {
    if [ -f "$LOG_FILE" ]; then
        echo -e "${BLUE}显示 $APP_NAME 日志 (按 Ctrl+C 退出):${NC}"
        tail -f "$LOG_FILE"
    else
        echo -e "${RED}日志文件不存在: $LOG_FILE${NC}"
    fi
}

# 自动部署新版本
deploy() {
    echo -e "${BLUE}=== 开始自动部署 ===${NC}"
    
    # 获取最新的jar文件
    local new_jar=$(get_current_jar) || return 1
    local current_jar=$(read_current_jar)
    
    echo -e "${CYAN}检测到最新jar: $new_jar${NC}"
    
    # 检查是否有新版本
    if [ "$new_jar" = "$current_jar" ]; then
        echo -e "${YELLOW}当前已是最新版本，无需部署${NC}"
        return 0
    fi
    
    # 备份当前版本信息
    if [ -n "$current_jar" ] && [ -f "$APP_HOME/$current_jar" ]; then
        save_backup_jar "$current_jar"
        echo -e "${CYAN}已备份当前版本: $current_jar${NC}"
    fi
    
    # 检查新jar文件是否存在
    if [ ! -f "$APP_HOME/$new_jar" ]; then
        echo -e "${RED}错误: 新jar文件不存在 $new_jar${NC}"
        return 1
    fi
    
    # 检查当前服务状态
    local was_running=false
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        was_running=true
        echo -e "${CYAN}检测到服务正在运行，将在停止服务后构建CDS${NC}"
    fi
    
    # 步骤1: 如果服务未运行，可以预先构建AppCDS
    if [ "$ENABLE_CDS" = "true" ] && [ "$was_running" = "false" ]; then
        echo -e "${YELLOW}步骤1: 预构建新版本AppCDS归档...${NC}"
        
        if extract_jar "$new_jar" && generate_cds "$new_jar"; then
            echo -e "${GREEN}新版本AppCDS归档构建完成${NC}"
        else
            echo -e "${YELLOW}AppCDS构建失败，将使用普通模式启动${NC}"
        fi
    elif [ "$was_running" = "true" ]; then
        echo -e "${CYAN}服务运行中，跳过预构建步骤以避免内存冲突${NC}"
    fi
    
    # 步骤2: 停止当前服务
    if [ "$was_running" = "true" ]; then
        echo -e "${YELLOW}步骤2: 停止当前服务...${NC}"
        stop
        sleep 3  # 等待进程完全结束，释放内存
    else
        echo -e "${CYAN}当前服务未运行，跳过停止步骤${NC}"
    fi
    
    # 步骤3: 启动新版本服务
    echo -e "${YELLOW}步骤3: 启动新版本服务...${NC}"
    save_current_jar "$new_jar"
    
    if start; then
        echo -e "${YELLOW}步骤4: 检查服务健康状态...${NC}"
        if check_service_health; then
            echo -e "${YELLOW}步骤5: 清理历史文件...${NC}"
            cleanup_old_jars
            cleanup_work_dirs
            
            echo -e "${GREEN}✅ 部署成功!${NC}"
            echo -e "${GREEN}新版本: $new_jar${NC}"
            echo -e "${GREEN}访问地址: http://localhost:$APP_PORT${NC}"
            
            if [ "$ENABLE_CDS" = "true" ]; then
                echo -e "${CYAN}🚀 使用AppCDS加速启动${NC}"
            fi
            return 0
        else
            echo -e "${RED}服务健康检查失败，开始回滚...${NC}"
            if rollback; then
                echo -e "${YELLOW}已回滚到备份版本${NC}"
            else
                echo -e "${RED}回滚也失败了，请手动检查${NC}"
            fi
            return 1
        fi
    else
        echo -e "${RED}新版本启动失败，开始回滚...${NC}"
        if rollback; then
            echo -e "${YELLOW}已回滚到备份版本${NC}"
        else
            echo -e "${RED}回滚也失败了，请手动检查${NC}"
        fi
        return 1
    fi
}

# 回滚到备份版本
rollback() {
    echo -e "${YELLOW}=== 开始回滚操作 ===${NC}"
    
    local backup_jar=$(read_backup_jar)
    if [ -z "$backup_jar" ] || [ ! -f "$APP_HOME/$backup_jar" ]; then
        echo -e "${RED}错误: 没有可用的备份jar文件${NC}"
        return 1
    fi
    
    echo -e "${CYAN}回滚到备份版本: $backup_jar${NC}"
    
    # 停止当前服务
    stop
    sleep 2
    
    # 更新当前jar记录
    save_current_jar "$backup_jar"
    
    # 清理CDS文件，强制重新生成
    local cds_file=$(get_jar_cds "$backup_jar")
    if [ -f "$cds_file" ]; then
        rm -f "$cds_file"
        echo -e "${YELLOW}已清理CDS归档，将重新生成${NC}"
    fi
    
    # 启动备份版本
    if start; then
        echo -e "${GREEN}回滚成功!${NC}"
        return 0
    else
        echo -e "${RED}回滚失败${NC}"
        return 1
    fi
}

# 手动构建AppCDS归档
build_cds() {
    echo -e "${BLUE}=== 构建AppCDS归档 ===${NC}"
    
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${RED}请先停止应用再生成CDS归档${NC}"
        return 1
    fi
    
    # 获取要使用的jar文件
    local current_jar=$(read_current_jar)
    if [ -z "$current_jar" ] || [ ! -f "$APP_HOME/$current_jar" ]; then
        current_jar=$(get_current_jar) || return 1
        save_current_jar "$current_jar"
    fi
    
    echo -e "${CYAN}使用JAR文件: $current_jar${NC}"
    
    if [ ! -f "$APP_HOME/$current_jar" ]; then
        echo -e "${RED}错误: 找不到jar文件 $APP_HOME/$current_jar${NC}"
        return 1
    fi
    
    if extract_jar "$current_jar" && generate_cds "$current_jar"; then
        echo -e "${GREEN}AppCDS归档构建完成! 下次启动将自动使用加速模式${NC}"
    else
        echo -e "${RED}AppCDS归档构建失败${NC}"
        return 1
    fi
}

# 清理AppCDS文件
clean_cds() {
    echo -e "${BLUE}=== 清理AppCDS文件 ===${NC}"
    
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${RED}请先停止应用再清理CDS文件${NC}"
        return 1
    fi
    
    local cleaned=false
    
    # 清理所有工作目录
    local work_dirs=($(find "$APP_HOME" -maxdepth 1 -name "${WORKDIR_PREFIX}_*" -type d 2>/dev/null))
    
    for work_dir in "${work_dirs[@]}"; do
        echo -e "${CYAN}删除工作目录: $work_dir${NC}"
        rm -rf "$work_dir"
        cleaned=true
    done
    
    # 清理CDS日志文件
    if ls "$APP_HOME/logs/cds-"*.log >/dev/null 2>&1; then
        rm -f "$APP_HOME/logs/cds-"*.log
        echo -e "${GREEN}已删除CDS日志文件${NC}"
        cleaned=true
    fi
    
    if [ "$cleaned" = "true" ]; then
        echo -e "${GREEN}AppCDS文件清理完成${NC}"
    else
        echo -e "${YELLOW}没有找到需要清理的AppCDS文件${NC}"
    fi
}

# 列出可用的jar文件
list_jars() {
    echo -e "${BLUE}=== 可用的JAR文件 ===${NC}"
    
    local jar_files=($(find "$APP_HOME" -maxdepth 1 -name "$APP_JAR_PATTERN" -type f 2>/dev/null | sort -V))
    
    if [ ${#jar_files[@]} -eq 0 ]; then
        echo -e "${RED}未找到匹配的jar文件 ($APP_JAR_PATTERN)${NC}"
        return 1
    fi
    
    local current_jar=$(read_current_jar)
    local backup_jar=$(read_backup_jar)
    
    for jar_file in "${jar_files[@]}"; do
        local jar_name=$(basename "$jar_file")
        local file_size=$(du -h "$jar_file" | cut -f1)
        local file_date=$(stat -c "%Y" "$jar_file" 2>/dev/null | xargs -I{} date -d @{} "+%Y-%m-%d %H:%M:%S" 2>/dev/null)
        
        local status=""
        if [ "$jar_name" = "$current_jar" ]; then
            status="${CYAN}<-- 当前使用${NC}"
        elif [ "$jar_name" = "$backup_jar" ]; then
            status="${YELLOW}<-- 备份版本${NC}"
        fi
        
        if [ "$jar_name" = "$current_jar" ]; then
            echo -e "${GREEN}✓ $jar_name${NC} (${file_size}) [$file_date] $status"
        else
            echo "  $jar_name (${file_size}) [$file_date] $status"
        fi
    done
}

# 显示配置信息
info() {
    echo -e "${BLUE}=== $APP_NAME 配置信息 ===${NC}"
    echo "应用名称: $APP_NAME"
    echo "JAR匹配: $APP_JAR_PATTERN"
    echo "运行端口: $APP_PORT"
    echo "运行环境: $SPRING_PROFILE"
    echo "工作目录: $APP_HOME"
    echo "保留历史: $KEEP_HISTORY_JARS 个jar文件"
    echo ""
    
    echo -e "${CYAN}=== JAR文件状态 ===${NC}"
    local current_jar=$(read_current_jar)
    if [ -n "$current_jar" ] && [ -f "$APP_HOME/$current_jar" ]; then
        echo "当前JAR: $current_jar ✅"
        
        # 显示当前jar的工作目录状态
        local workdir=$(get_jar_workdir "$current_jar")
        if [ -d "$workdir" ]; then
            echo "工作目录: $(basename "$workdir") ✅"
        else
            echo "工作目录: 未创建"
        fi
        
        # 显示当前jar的CDS状态
        local cds_file=$(get_jar_cds "$current_jar")
        if [ -f "$cds_file" ]; then
            local CDS_SIZE=$(du -h "$cds_file" 2>/dev/null | cut -f1)
            echo "CDS归档: ✅ 已生成 (大小: $CDS_SIZE)"
        else
            echo "CDS归档: ❌ 未生成"
        fi
    else
        local latest_jar=$(find_jar_file)
        if [ -n "$latest_jar" ]; then
            echo "当前JAR: 未设置"
            echo "最新JAR: $latest_jar"
        else
            echo -e "${RED}当前JAR: 未找到匹配文件${NC}"
        fi
    fi
    
    local backup_jar=$(read_backup_jar)
    if [ -n "$backup_jar" ] && [ -f "$APP_HOME/$backup_jar" ]; then
        echo "备份JAR: $backup_jar ✅"
    else
        echo "备份JAR: 无"
    fi
    
    # 显示所有jar文件统计
    local jar_files=($(get_all_jars))
    echo "总jar数: ${#jar_files[@]}"
    
    # 显示工作目录统计
    local work_dirs=($(find "$APP_HOME" -maxdepth 1 -name "${WORKDIR_PREFIX}_*" -type d 2>/dev/null))
    echo "工作目录数: ${#work_dirs[@]}"
    
    echo ""
    echo -e "${CYAN}=== AppCDS配置 ===${NC}"
    echo "AppCDS启用: $ENABLE_CDS"
    echo "工作目录前缀: $WORKDIR_PREFIX"
    echo "独立目录结构: 每个jar使用独立的工作目录和CDS归档"
    
    # 显示系统内存信息
    if command -v free >/dev/null 2>&1; then
        echo ""
        echo -e "${CYAN}=== 系统内存状态 ===${NC}"
        local total_mem=$(free -m | awk 'NR==2{print $2}')
        local used_mem=$(free -m | awk 'NR==2{print $3}')
        local available_mem=$(free -m | awk 'NR==2{print $7}')
        echo "总内存: ${total_mem}MB"
        echo "已用内存: ${used_mem}MB"
        echo "可用内存: ${available_mem}MB"
    fi
}

# 显示帮助
help() {
    echo -e "${BLUE}若依项目服务控制脚本 - Java 17 + AppCDS${NC}"
    echo ""
    echo "用法: $0 {start|stop|restart|status|logs|deploy|rollback|build-cds|clean-cds|list-jars|info|help}"
    echo ""
    echo "基础命令:"
    echo "  start     - 启动服务 (使用记录的jar版本)"
    echo "  stop      - 停止服务"
    echo "  restart   - 重启服务"
    echo "  status    - 查看运行状态"
    echo "  logs      - 实时查看日志"
    echo ""
    echo "部署命令:"
    echo "  deploy    - 自动部署最新jar (智能CDS构建->停止->启动->检查->清理)"
    echo "  rollback  - 回滚到备份版本"
    echo ""
    echo "AppCDS命令:"
    echo "  build-cds - 手动构建AppCDS归档文件"
    echo "  clean-cds - 清理AppCDS相关文件"
    echo ""
    echo "其他命令:"
    echo "  list-jars - 列出可用的JAR文件"
    echo "  info      - 显示配置信息"
    echo "  help      - 显示帮助信息"
    echo ""
    echo -e "${CYAN}自动部署流程:${NC}"
    echo "1. 上传新的ruoyi-YYYYMMDD-HHMMSS.jar文件"
    echo "2. 运行: $0 deploy"
    echo "3. 脚本自动完成："
    echo "   - 服务未运行时：预构建CDS -> 启动新版本"
    echo "   - 服务运行中时：停止服务 -> 构建CDS -> 启动新版本"
    echo "   - 健康检查 -> 清理旧文件"
    echo "4. 如果失败会自动回滚到备份版本"
    echo "5. 智能内存管理，避免OOM问题"
    echo ""
    echo -e "${CYAN}JAR文件管理:${NC}"
    echo "• 支持 ruoyi-YYYYMMDD-HHMMSS.jar 格式"
    echo "• 每个jar使用独立的工作目录和CDS归档"
    echo "• 自动保留最新的 $KEEP_HISTORY_JARS 个jar文件"
    echo "• 部署失败时自动回滚到备份版本"
    echo "• 使用 list-jars 查看所有可用文件"
    echo ""
    echo -e "${CYAN}AppCDS优化说明:${NC}"
    echo "• AppCDS可以显著提升应用启动速度(20-40%)"
    echo "• 每个jar版本使用独立的CDS归档，避免冲突"
    echo "• 智能内存检测，根据可用内存调整构建参数"
    echo "• 超时保护和错误检测，提高成功率"
    echo "• 部署时避免内存竞争，服务运行中不预构建CDS"
    echo ""
    echo -e "${CYAN}内存优化特性:${NC}"
    echo "• 动态内存配置：根据系统可用内存调整CDS构建参数"
    echo "• 冲突避免：服务运行时跳过预构建，避免内存竞争"
    echo "• 超时保护：5分钟超时，防止进程卡死"
    echo "• 错误识别：区分OOM、超时等不同失败原因"
}

# 主程序
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs|log)
        logs
        ;;
    deploy)
        deploy
        ;;
    rollback)
        rollback
        ;;
    build-cds|cds)
        build_cds
        ;;
    clean-cds|clean)
        clean_cds
        ;;
    list-jars|list)
        list_jars
        ;;
    info)
        info
        ;;
    help|--help|-h)
        help
        ;;
    *)
        echo -e "${RED}错误: 无效的命令 '$1'${NC}"
        echo ""
        help
        exit 1
        ;;
esac

exit $?