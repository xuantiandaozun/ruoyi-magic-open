import { NodeSSH } from 'node-ssh'

const ssh = new NodeSSH()
await ssh.connect({
  host: '47.109.155.176',
  port: 22,
  username: 'root',
  password: '@ThMWux7KTZBden',
})

const cmds = [
  "grep -E 'Started RuoYiApplication|MiniAppBillAccountController|BillMiniAppBaseController|Application run failed' /root/logs/ruoyi-blue.log | tail -10",
  "cd /root && printf 'status\\nexit\\n' | ./ruoyi-proxy-linux cli 2>/dev/null | grep -E '当前JAR|蓝色环境|绿色环境'",
]

for (const cmd of cmds) {
  console.log('\n===', cmd, '===')
  const r = await ssh.execCommand(cmd, { cwd: '/root' })
  console.log(r.stdout || r.stderr)
}

ssh.dispose()
