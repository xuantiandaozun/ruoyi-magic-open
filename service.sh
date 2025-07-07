#!/bin/bash

# 若依项目服务控制脚本
# 脚本和jar包放在同一目录下使用

# ==================== 配置区域 ====================
# 应用配置
APP_NAME="ruoyi"
APP_JAR="ruoyi.jar"  # jar包文件名
APP_PORT=8080              # 应用端口
SPRING_PROFILE="dev"      # 环境配置: dev/test/prod

# JVM参数配置 (兼容Java 17+)
JAVA_OPTS="-server \
-Xms1g \
-Xmx2g \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=./logs/ \
-Xlog:gc*:./logs/gc.log:time,tags \
-Dfile.encoding=UTF-8 \
-Djava.security.egd=file:/dev/./urandom"

# ==================== 脚本逻辑 ====================
# 获取脚本所在目录
APP_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$APP_HOME/$APP_NAME.pid"
LOG_FILE="$APP_HOME/logs/$APP_NAME.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

# 启动服务
start() {
    echo -e "${BLUE}正在启动 $APP_NAME...${NC}"
    
    # 检查是否已运行
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${YELLOW}$APP_NAME 已经在运行 (PID: $PID)${NC}"
        return 1
    fi
    
    # 检查jar文件
    if [ ! -f "$APP_HOME/$APP_JAR" ]; then
        echo -e "${RED}错误: 找不到jar文件 $APP_HOME/$APP_JAR${NC}"
        return 1
    fi
    
    # 创建日志目录
    mkdir -p "$APP_HOME/logs"
    
    # 启动应用
    cd "$APP_HOME"
    nohup java $JAVA_OPTS \
        -Dspring.profiles.active=$SPRING_PROFILE \
        -Dserver.port=$APP_PORT \
        -jar "$APP_JAR" \
        > "$LOG_FILE" 2>&1 &
    
    echo $! > "$PID_FILE"
    
    # 等待启动
    sleep 3
    PID=$(get_pid)
    if [ -n "$PID" ]; then
        echo -e "${GREEN}$APP_NAME 启动成功!${NC}"
        echo -e "${GREEN}PID: $PID${NC}"
        echo -e "${GREEN}端口: $APP_PORT${NC}"
        echo -e "${GREEN}环境: $SPRING_PROFILE${NC}"
        echo -e "${GREEN}访问地址: http://localhost:$APP_PORT${NC}"
        echo -e "${GREEN}日志文件: $LOG_FILE${NC}"
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
        echo "日志: $LOG_FILE"
        
        # 显示内存使用情况
        if command -v ps >/dev/null 2>&1; then
            MEM=$(ps -p $PID -o rss= 2>/dev/null | awk '{print int($1/1024)}' 2>/dev/null)
            if [ -n "$MEM" ] && [ "$MEM" -gt 0 ]; then
                echo "内存使用: ${MEM}MB"
            fi
        fi
        return 0
    else
        echo -e "${RED}$APP_NAME 未运行${NC}"
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

# 显示配置信息
info() {
    echo -e "${BLUE}=== $APP_NAME 配置信息 ===${NC}"
    echo "应用名称: $APP_NAME"
    echo "JAR文件: $APP_JAR"
    echo "运行端口: $APP_PORT"
    echo "运行环境: $SPRING_PROFILE"
    echo "工作目录: $APP_HOME"
    echo "PID文件: $PID_FILE"
    echo "日志文件: $LOG_FILE"
    echo "JVM参数: $JAVA_OPTS"
}

# 显示帮助
help() {
    echo -e "${BLUE}若依项目服务控制脚本${NC}"
    echo ""
    echo "用法: $0 {start|stop|restart|status|logs|info|help}"
    echo ""
    echo "命令说明:"
    echo "  start   - 启动服务"
    echo "  stop    - 停止服务"
    echo "  restart - 重启服务"
    echo "  status  - 查看运行状态"
    echo "  logs    - 实时查看日志"
    echo "  info    - 显示配置信息"
    echo "  help    - 显示帮助信息"
    echo ""
    echo "注意事项:"
    echo "1. 请确保脚本和jar包在同一目录"
    echo "2. 首次使用请给脚本执行权限: chmod +x $0"
    echo "3. 可在脚本顶部配置区域修改端口、环境等参数"
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