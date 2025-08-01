#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RuoYi Backend Auto Deploy Script
Python version of deploy.bat
"""

import os
import sys
import subprocess
import glob
import shutil
from pathlib import Path

# Configuration
SERVER_HOST = "47.109.155.176"
SERVER_PORT = "22"
SERVER_USER = "root"
SERVER_PASSWORD = "@ThMWux7KTZBden"
SERVER_PATH = "/root/java"
LOCAL_JAR_PATH = "target"

# Maven Configuration
MAVEN_PATH = r"D:\apache-maven-3.9.6\bin\mvn.cmd"  # 可以修改为你的Maven路径
# 如果Maven已添加到PATH环境变量，也可以直接使用 "mvn"
# MAVEN_PATH = "mvn"

def print_step(step_num, description):
    """Print step information"""
    print(f"\n步骤 {step_num}: {description}...")

def print_header():
    """Print script header"""
    print("=" * 50)
    print("           若依后端自动部署脚本")
    print("=" * 50)
    print()

def clean_old_jars():
    """Clean old jar files"""
    print_step(1, "清理旧的构建文件")
    
    target_dir = Path(LOCAL_JAR_PATH)
    if target_dir.exists():
        jar_files = list(target_dir.glob("*.jar"))
        for jar_file in jar_files:
            jar_file.unlink()
            print(f"已删除: {jar_file.name}")
        if jar_files:
            print("已清理旧的jar文件")
        else:
            print("未找到旧的jar文件")
    else:
        print("目标目录不存在")

def maven_package():
    """Execute Maven packaging"""
    print_step(2, "执行Maven打包")
    
    try:
        # 检查Maven路径是否存在
        if MAVEN_PATH != "mvn" and not os.path.exists(MAVEN_PATH):
            print(f"Maven路径不存在: {MAVEN_PATH}")
            print("请检查配置中的MAVEN_PATH设置")
            return False
        
        print(f"使用Maven: {MAVEN_PATH}")
        result = subprocess.run(
            [MAVEN_PATH, "clean", "package", "-DskipTests"],
            check=True,
            capture_output=True,
            text=True
        )
        print("Maven打包成功")
        return True
    except subprocess.CalledProcessError as e:
        print("Maven打包失败，请检查代码错误")
        print(f"错误信息: {e.stderr}")
        return False
    except FileNotFoundError:
        print(f"未找到Maven命令: {MAVEN_PATH}")
        print("请检查以下设置:")
        print("1. 确保Maven已正确安装")
        print("2. 修改脚本中的MAVEN_PATH配置")
        print("3. 或将Maven添加到PATH环境变量")
        return False

def find_jar_file():
    """Find generated jar file"""
    print_step(3, "查找生成的jar文件")
    
    target_dir = Path(LOCAL_JAR_PATH)
    jar_files = list(target_dir.glob("ruoyi-*.jar"))
    
    if not jar_files:
        print("未找到生成的jar文件")
        return None
    
    # Get the most recent jar file
    jar_file = max(jar_files, key=lambda x: x.stat().st_mtime)
    print(f"找到jar文件: {jar_file.name}")
    return jar_file

def upload_file(jar_file):
    """Upload jar file to server using scp"""
    print_step(4, "上传jar文件到服务器")
    
    # Try different scp commands, prioritize automatic password input
    scp_commands = [
        # First try pscp with password (automatic)
        ["pscp", "-pw", SERVER_PASSWORD, "-P", SERVER_PORT, str(jar_file), f"{SERVER_USER}@{SERVER_HOST}:{SERVER_PATH}/"],
        # Then try sshpass + scp (automatic)
        ["sshpass", "-p", SERVER_PASSWORD, "scp", "-P", SERVER_PORT, "-o", "StrictHostKeyChecking=no", str(jar_file), f"{SERVER_USER}@{SERVER_HOST}:{SERVER_PATH}/"],
        # Last resort: standard scp (may need manual password)
        ["scp", "-P", SERVER_PORT, "-o", "StrictHostKeyChecking=no", str(jar_file), f"{SERVER_USER}@{SERVER_HOST}:{SERVER_PATH}/"]
    ]
    
    for cmd in scp_commands:
        try:
            if cmd[0] == "pscp":
                print("使用pscp命令（自动输入密码）")
                result = subprocess.run(cmd, check=True, capture_output=True, text=True)
            elif cmd[0] == "sshpass":
                print("使用sshpass+scp命令（自动输入密码）")
                result = subprocess.run(cmd, check=True, capture_output=True, text=True)
            else:
                print("使用scp命令（如果没有配置SSH密钥，可能需要手动输入密码）")
                result = subprocess.run(cmd, check=True)
            
            print("文件上传成功")
            return True
            
        except subprocess.CalledProcessError as e:
            print(f"使用{cmd[0]}上传失败: {e}")
            continue
        except FileNotFoundError:
            print(f"未找到{cmd[0]}命令")
            continue
    
    print("文件上传失败，请检查:")
    print("1. 网络连接和服务器配置")
    print("2. 安装以下工具之一:")
    print("   - PuTTY工具包 (推荐): https://www.putty.org/")
    print("   - sshpass工具: 在Linux/WSL中使用 apt install sshpass")
    print("   - 或配置SSH密钥认证")
    return False

def deploy_on_server():
    """Execute deployment commands on server"""
    print_step(5, "在服务器上执行部署命令")
    
    # Try different ssh commands, prioritize automatic password input
    deploy_command = f"cd {SERVER_PATH} && chmod +x service.sh && sh service.sh deploy"
    ssh_commands = [
        # First try plink with password (automatic)
        ["plink", "-pw", SERVER_PASSWORD, "-P", SERVER_PORT, f"{SERVER_USER}@{SERVER_HOST}", deploy_command],
        # Then try sshpass + ssh (automatic)
        ["sshpass", "-p", SERVER_PASSWORD, "ssh", "-p", SERVER_PORT, "-o", "StrictHostKeyChecking=no", f"{SERVER_USER}@{SERVER_HOST}", deploy_command],
        # Last resort: standard ssh (may need manual password)
        ["ssh", "-p", SERVER_PORT, "-o", "StrictHostKeyChecking=no", f"{SERVER_USER}@{SERVER_HOST}", deploy_command]
    ]
    
    for cmd in ssh_commands:
        try:
            if cmd[0] == "plink":
                print("使用plink命令（自动输入密码）")
                result = subprocess.run(cmd, check=True, capture_output=True, text=True)
            elif cmd[0] == "sshpass":
                print("使用sshpass+ssh命令（自动输入密码）")
                result = subprocess.run(cmd, check=True, capture_output=True, text=True)
            else:
                print("使用ssh命令（如果没有配置SSH密钥，可能需要手动输入密码）")
                result = subprocess.run(cmd, check=True)
            
            print("远程部署命令执行成功")
            return True
            
        except subprocess.CalledProcessError as e:
            print(f"使用{cmd[0]}执行远程命令失败: {e}")
            continue
        except FileNotFoundError:
            print(f"未找到{cmd[0]}命令")
            continue
    
    print("远程部署命令执行失败")
    return False

def print_completion():
    """Print completion message"""
    print()
    print("=" * 50)
    print("            部署完成！")
    print("=" * 50)
    print(f"应用访问地址: http://{SERVER_HOST}:8080")
    print()

def main():
    """Main function"""
    try:
        print_header()
        
        # Step 1: Clean old files
        clean_old_jars()
        
        # Step 2: Maven package
        if not maven_package():
            input("按回车键退出...")
            sys.exit(1)
        
        # Step 3: Find jar file
        jar_file = find_jar_file()
        if not jar_file:
            input("按回车键退出...")
            sys.exit(1)
        
        # Step 4: Upload file
        if not upload_file(jar_file):
            input("按回车键退出...")
            sys.exit(1)
        
        # Step 5: Deploy on server
        if not deploy_on_server():
            input("按回车键退出...")
            sys.exit(1)
        
        # Completion
        print_completion()
        
    except KeyboardInterrupt:
        print("\n部署被用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n意外错误: {e}")
        input("按回车键退出...")
        sys.exit(1)

if __name__ == "__main__":
    main()