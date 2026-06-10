import { NodeSSH } from 'node-ssh'
import { execSync } from 'node:child_process'
import path from 'node:path'
import fs from 'node:fs'

const config = {
  host: '47.109.155.176',
  port: 22,
  username: 'root',
  password: '@ThMWux7KTZBden',
  remoteDir: '/root',
  remoteCli: '/root/ruoyi-proxy-linux',
}

const REQUIRED_CLASSES = [
  'BOOT-INF/classes/com/ruoyi/project/miniapp/controller/bill/BillMiniAppBaseController.class',
  'BOOT-INF/classes/com/ruoyi/project/miniapp/controller/bill/MiniAppBillAccountController.class',
  'BOOT-INF/classes/com/ruoyi/project/miniapp/service/impl/MiniAppContentSecurityService.class',
]

function verifyJar(jarPath) {
  const listing = execSync(`jar tf "${jarPath}"`, { encoding: 'utf8' })
  const missing = REQUIRED_CLASSES.filter((item) => !listing.includes(item))
  if (missing.length > 0) {
    throw new Error(`JAR 校验失败，缺少: ${missing.join(', ')}`)
  }
  return fs.statSync(jarPath).size
}

async function main() {
  const jarArg = process.argv[2]
  if (!jarArg) {
    console.error('缺少 JAR 文件路径参数')
    process.exit(1)
  }

  const localJar = path.resolve(process.cwd(), jarArg)
  if (!fs.existsSync(localJar)) {
    console.error(`未找到 JAR 文件: ${localJar}`)
    process.exit(1)
  }

  const localSize = verifyJar(localJar)
  console.log(`本地 JAR 校验通过，大小 ${localSize} 字节`)

  const remoteJar = path.posix.join(config.remoteDir, path.basename(localJar))
  const ssh = new NodeSSH()

  try {
    console.log('正在连接服务器...')
    await ssh.connect({
      host: config.host,
      port: config.port,
      username: config.username,
      password: config.password,
    })

    console.log(`正在上传 JAR 到 ${remoteJar} ...`)
    await ssh.putFile(localJar, remoteJar)

    const sizeCheck = await ssh.execCommand(`stat -c%s ${remoteJar}`)
    const remoteSize = Number(sizeCheck.stdout.trim())
    if (remoteSize !== localSize) {
      throw new Error(`远端 JAR 大小不一致: local=${localSize}, remote=${remoteSize}`)
    }
    console.log('远端文件大小校验通过')

    const classCheck = await ssh.execCommand(
      `jar tf ${remoteJar} | grep -E 'BillMiniAppBaseController|MiniAppBillAccountController|MiniAppContentSecurityService'`,
    )
    for (const required of REQUIRED_CLASSES.map((item) => item.split('/').pop())) {
      if (!classCheck.stdout.includes(required)) {
        throw new Error(`远端 JAR 缺少关键类: ${required}`)
      }
    }
    console.log('远端 JAR 类文件校验通过')

    console.log('正在执行 deploy-lowmem ...')
    const command = `chmod +x ${config.remoteCli} && cd ${config.remoteDir} && printf 'deploy-lowmem\\nexit\\n' | ${config.remoteCli} cli`
    const result = await ssh.execCommand(command, { cwd: config.remoteDir })

    if (result.stdout) {
      console.log(result.stdout)
    }
    if (result.stderr) {
      console.error(result.stderr)
    }
    if (result.code !== 0) {
      console.error(`部署失败，远端退出码: ${result.code}`)
      process.exit(1)
    }

    console.log('低内存模式部署成功')
  } catch (error) {
    console.error('部署失败：' + error.message)
    process.exit(1)
  } finally {
    ssh.dispose()
  }
}

main()
