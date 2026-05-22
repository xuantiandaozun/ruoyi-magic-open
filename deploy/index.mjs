import { NodeSSH } from 'node-ssh'
import path from 'node:path'
import fs from 'node:fs'

const config = {
  host: '47.109.155.176',
  port: 22,
  username: 'root',
  password: '@ThMWux7KTZBden',
  remoteDir: '/root',
  remoteCli: '/root/ruoyi-proxy-linux'
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

  const remoteJar = path.posix.join(config.remoteDir, path.basename(localJar))
  const ssh = new NodeSSH()

  try {
    console.log('正在连接服务器...')
    await ssh.connect({
      host: config.host,
      port: config.port,
      username: config.username,
      password: config.password
    })

    console.log(`正在上传 JAR 到 ${remoteJar} ...`)
    await ssh.putFile(localJar, remoteJar)

    console.log('正在执行远端 deploy ...')
    const command = `chmod +x ${config.remoteCli} && cd ${config.remoteDir} && printf 'deploy\\nexit\\n' | ${config.remoteCli} cli`
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

    console.log('后台 JAR 上传并触发 deploy 成功')
  } catch (error) {
    console.error('部署失败：' + error.message)
    process.exit(1)
  } finally {
    ssh.dispose()
  }
}

main()
