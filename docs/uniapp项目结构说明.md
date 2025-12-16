# uniapp项目结构说明

> **项目名称：** 记账小工具前端APP  
> **技术栈：** uniapp + Vue3 + uView UI  
> **开发工具：** HBuilderX  
> **发布平台：** 微信小程序、H5、APP

---

## 一、项目目录结构

```
bill-app-uniapp/
├── pages/                        # 页面目录
│   ├── index/                    # 首页模块
│   │   └── index.vue            # 首页（收支概览）
│   ├── record/                   # 记账模块
│   │   ├── add.vue              # 记账页面
│   │   ├── list.vue             # 账单列表
│   │   └── detail.vue           # 账单详情
│   ├── stat/                     # 统计模块
│   │   ├── index.vue            # 统计首页
│   │   ├── category.vue         # 分类统计
│   │   └── trend.vue            # 收支趋势
│   ├── user/                     # 用户模块
│   │   ├── login.vue            # 登录页面
│   │   ├── register.vue         # 注册页面
│   │   └── profile.vue          # 个人中心
│   ├── family/                   # 家庭组模块
│   │   ├── index.vue            # 家庭组管理
│   │   ├── create.vue           # 创建家庭组
│   │   └── join.vue             # 加入家庭组
│   ├── budget/                   # 预算模块
│   │   ├── index.vue            # 预算列表
│   │   └── setting.vue          # 预算设置
│   └── setting/                  # 设置模块
│       ├── index.vue            # 设置首页
│       ├── account.vue          # 账户管理
│       ├── category.vue         # 分类管理
│       └── reminder.vue         # 提醒设置
├── components/                   # 组件目录
│   ├── chart/                    # 图表组件
│   │   ├── pie-chart.vue        # 饼图组件
│   │   ├── line-chart.vue       # 折线图组件
│   │   └── bar-chart.vue        # 柱状图组件
│   ├── record/                   # 记账相关组件
│   │   ├── category-select.vue  # 分类选择器
│   │   ├── account-select.vue   # 账户选择器
│   │   ├── date-select.vue      # 日期选择器
│   │   └── amount-input.vue     # 金额输入键盘
│   ├── common/                   # 通用组件
│   │   ├── custom-navbar.vue    # 自定义导航栏
│   │   ├── empty-state.vue      # 空状态组件
│   │   └── loading.vue          # 加载组件
│   └── family/                   # 家庭组组件
│       ├── member-list.vue      # 成员列表
│       └── invite-code.vue      # 邀请码
├── static/                       # 静态资源
│   ├── images/                   # 图片资源
│   │   ├── logo.png
│   │   ├── avatar.png
│   │   └── icons/               # 分类图标
│   ├── css/                      # 全局样式
│   │   └── common.css
│   └── font/                     # 字体文件
├── store/                        # Pinia状态管理
│   ├── index.js                 # Store入口
│   ├── modules/
│   │   ├── user.js              # 用户状态
│   │   ├── family.js            # 家庭组状态
│   │   ├── category.js          # 分类状态
│   │   └── account.js           # 账户状态
├── utils/                        # 工具函数
│   ├── request.js               # HTTP请求封装
│   ├── storage.js               # 本地存储封装
│   ├── auth.js                  # 认证工具
│   ├── date.js                  # 日期工具
│   ├── number.js                # 数字格式化
│   └── validator.js             # 表单验证
├── api/                          # API接口
│   ├── user.js                  # 用户相关接口
│   ├── family.js                # 家庭组接口
│   ├── record.js                # 账单接口
│   ├── category.js              # 分类接口
│   ├── account.js               # 账户接口
│   ├── budget.js                # 预算接口
│   └── stat.js                  # 统计接口
├── config/                       # 配置文件
│   ├── index.js                 # 全局配置
│   ├── api-config.js            # API配置
│   └── theme.js                 # 主题配置
├── mixins/                       # 混入
│   ├── auth.js                  # 认证混入
│   └── page.js                  # 页面混入
├── App.vue                       # 应用入口
├── main.js                       # 主入口文件
├── manifest.json                 # 应用配置
├── pages.json                    # 页面路由配置
├── uni.scss                      # uni-app全局样式变量
└── package.json                  # 依赖配置
```

---

## 二、pages.json 配置示例

```json
{
  "pages": [
    {
      "path": "pages/index/index",
      "style": {
        "navigationBarTitleText": "记账本",
        "enablePullDownRefresh": true
      }
    },
    {
      "path": "pages/record/add",
      "style": {
        "navigationBarTitleText": "记一笔",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/record/list",
      "style": {
        "navigationBarTitleText": "账单列表",
        "enablePullDownRefresh": true
      }
    },
    {
      "path": "pages/stat/index",
      "style": {
        "navigationBarTitleText": "统计分析"
      }
    },
    {
      "path": "pages/user/login",
      "style": {
        "navigationBarTitleText": "登录",
        "navigationStyle": "custom"
      }
    },
    {
      "path": "pages/user/register",
      "style": {
        "navigationBarTitleText": "注册"
      }
    },
    {
      "path": "pages/user/profile",
      "style": {
        "navigationBarTitleText": "个人中心"
      }
    }
  ],
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#1989FA",
    "backgroundColor": "#ffffff",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/index/index",
        "iconPath": "static/images/tabbar/home.png",
        "selectedIconPath": "static/images/tabbar/home-active.png",
        "text": "首页"
      },
      {
        "pagePath": "pages/record/add",
        "iconPath": "static/images/tabbar/add.png",
        "selectedIconPath": "static/images/tabbar/add-active.png",
        "text": "记账"
      },
      {
        "pagePath": "pages/stat/index",
        "iconPath": "static/images/tabbar/stat.png",
        "selectedIconPath": "static/images/tabbar/stat-active.png",
        "text": "统计"
      },
      {
        "pagePath": "pages/user/profile",
        "iconPath": "static/images/tabbar/user.png",
        "selectedIconPath": "static/images/tabbar/user-active.png",
        "text": "我的"
      }
    ]
  },
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarTitleText": "记账本",
    "navigationBarBackgroundColor": "#FFFFFF",
    "backgroundColor": "#F7F8FA"
  }
}
```

---

## 三、核心文件示例

### 3.1 main.js（入口文件）

```javascript
import { createSSRApp } from 'vue'
import App from './App.vue'
import store from './store'
import uviewPlus from 'uview-plus'

export function createApp() {
  const app = createSSRApp(App)
  
  // 使用状态管理
  app.use(store)
  
  // 使用uView UI
  app.use(uviewPlus)
  
  return {
    app
  }
}
```

### 3.2 utils/request.js（HTTP请求封装）

```javascript
import config from '@/config'
import { getToken, removeToken } from '@/utils/auth'

// 基础配置
const baseURL = config.baseURL
const timeout = config.timeout || 30000

// 请求拦截器
function request(options = {}) {
  // 获取token
  const token = getToken()
  
  // 构建请求配置
  const requestOptions = {
    url: baseURL + options.url,
    method: options.method || 'GET',
    timeout: timeout,
    header: {
      'Content-Type': 'application/json',
      ...options.header
    },
    data: options.data || {}
  }
  
  // 添加token
  if (token) {
    requestOptions.header['Authorization'] = 'Bearer ' + token
  }
  
  return new Promise((resolve, reject) => {
    uni.request({
      ...requestOptions,
      success: (res) => {
        const data = res.data
        
        // 请求成功
        if (data.code === 200) {
          resolve(data.data)
        }
        // token过期
        else if (data.code === 401) {
          removeToken()
          uni.showToast({
            title: '登录已过期，请重新登录',
            icon: 'none'
          })
          setTimeout(() => {
            uni.navigateTo({
              url: '/pages/user/login'
            })
          }, 1500)
          reject(data)
        }
        // 其他错误
        else {
          uni.showToast({
            title: data.msg || '请求失败',
            icon: 'none'
          })
          reject(data)
        }
      },
      fail: (err) => {
        uni.showToast({
          title: '网络请求失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

// GET请求
export function get(url, data = {}) {
  return request({
    url,
    method: 'GET',
    data
  })
}

// POST请求
export function post(url, data = {}) {
  return request({
    url,
    method: 'POST',
    data
  })
}

// PUT请求
export function put(url, data = {}) {
  return request({
    url,
    method: 'PUT',
    data
  })
}

// DELETE请求
export function del(url, data = {}) {
  return request({
    url,
    method: 'DELETE',
    data
  })
}

export default request
```

### 3.3 store/modules/user.js（用户状态管理）

```javascript
import { defineStore } from 'pinia'
import { login, getUserInfo } from '@/api/user'
import { setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: ''
  }),
  
  getters: {
    isLogin: (state) => !!state.token,
    userId: (state) => state.userInfo?.userId,
    nickName: (state) => state.userInfo?.nickName,
    avatar: (state) => state.userInfo?.avatar,
    familyId: (state) => state.userInfo?.familyId
  },
  
  actions: {
    // 登录
    async login(loginForm) {
      try {
        const data = await login(loginForm)
        this.token = data.token
        this.userInfo = data.userInfo
        setToken(data.token)
        return data
      } catch (error) {
        throw error
      }
    },
    
    // 获取用户信息
    async getUserInfo() {
      try {
        const data = await getUserInfo()
        this.userInfo = data
        return data
      } catch (error) {
        throw error
      }
    },
    
    // 退出登录
    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
      uni.reLaunch({
        url: '/pages/user/login'
      })
    }
  }
})
```

### 3.4 api/record.js（账单接口）

```javascript
import { get, post, put, del } from '@/utils/request'

// 创建账单
export function addRecord(data) {
  return post('/api/bill/record/add', data)
}

// 账单列表
export function getRecordList(params) {
  return get('/api/bill/record/list', params)
}

// 账单详情
export function getRecordDetail(recordId) {
  return get(`/api/bill/record/${recordId}`)
}

// 更新账单
export function updateRecord(data) {
  return put('/api/bill/record/update', data)
}

// 删除账单
export function deleteRecord(recordId) {
  return del(`/api/bill/record/${recordId}`)
}

// 批量删除账单
export function batchDeleteRecord(recordIds) {
  return del('/api/bill/record/batch', { recordIds })
}
```

---

## 四、核心页面示例

### 4.1 pages/index/index.vue（首页）

```vue
<template>
  <view class="container">
    <!-- 头部概览 -->
    <view class="overview-card">
      <view class="month-info">
        <text class="month-text">{{ currentMonth }}</text>
      </view>
      
      <view class="amount-row">
        <view class="amount-item">
          <text class="label">支出</text>
          <text class="value expense">{{ monthExpense }}</text>
        </view>
        <view class="amount-item">
          <text class="label">收入</text>
          <text class="value income">{{ monthIncome }}</text>
        </view>
      </view>
      
      <view class="balance-row">
        <text class="label">结余</text>
        <text class="value">{{ monthBalance }}</text>
      </view>
    </view>
    
    <!-- 快速记账按钮 -->
    <view class="quick-add-btn" @click="goToAdd">
      <u-icon name="plus" size="24"></u-icon>
      <text class="btn-text">记一笔</text>
    </view>
    
    <!-- 最近账单 -->
    <view class="recent-records">
      <view class="section-title">最近账单</view>
      
      <view class="record-list">
        <view 
          class="record-item" 
          v-for="item in recordList" 
          :key="item.recordId"
          @click="goToDetail(item.recordId)"
        >
          <view class="record-left">
            <text class="category-icon">{{ item.categoryIcon }}</text>
            <view class="record-info">
              <text class="category-name">{{ item.categoryName }}</text>
              <text class="record-time">{{ item.recordDate }} {{ item.createTime }}</text>
            </view>
          </view>
          
          <view class="record-right">
            <text :class="['amount', item.recordType === '0' ? 'expense' : 'income']">
              {{ item.recordType === '0' ? '-' : '+' }}{{ item.amount }}
            </text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview } from '@/api/stat'
import { getRecordList } from '@/api/record'
import dayjs from 'dayjs'

// 概览数据
const overviewData = ref({})
const recordList = ref([])

// 计算属性
const currentMonth = computed(() => {
  return dayjs().format('YYYY年MM月')
})

const monthExpense = computed(() => {
  return overviewData.value.monthExpense?.toFixed(2) || '0.00'
})

const monthIncome = computed(() => {
  return overviewData.value.monthIncome?.toFixed(2) || '0.00'
})

const monthBalance = computed(() => {
  return overviewData.value.monthBalance?.toFixed(2) || '0.00'
})

// 获取概览数据
const loadOverview = async () => {
  try {
    const data = await getOverview()
    overviewData.value = data
  } catch (error) {
    console.error('获取概览数据失败', error)
  }
}

// 获取最近账单
const loadRecordList = async () => {
  try {
    const data = await getRecordList({
      page: 1,
      pageSize: 10
    })
    recordList.value = data.list || []
  } catch (error) {
    console.error('获取账单列表失败', error)
  }
}

// 跳转到记账页面
const goToAdd = () => {
  uni.navigateTo({
    url: '/pages/record/add'
  })
}

// 跳转到详情页面
const goToDetail = (recordId) => {
  uni.navigateTo({
    url: `/pages/record/detail?id=${recordId}`
  })
}

onMounted(() => {
  loadOverview()
  loadRecordList()
})
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #F7F8FA;
  padding: 20rpx;
}

.overview-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20rpx;
  padding: 40rpx;
  color: #fff;
  margin-bottom: 30rpx;
}

.month-info {
  margin-bottom: 30rpx;
}

.month-text {
  font-size: 32rpx;
  font-weight: bold;
}

.amount-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 30rpx;
}

.amount-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.label {
  font-size: 26rpx;
  opacity: 0.8;
  margin-bottom: 10rpx;
}

.value {
  font-size: 40rpx;
  font-weight: bold;
  
  &.expense {
    color: #FF6B6B;
  }
  
  &.income {
    color: #51CF66;
  }
}

.quick-add-btn {
  background: #1989FA;
  border-radius: 50rpx;
  padding: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  margin-bottom: 30rpx;
}

.btn-text {
  margin-left: 10rpx;
  font-size: 32rpx;
  font-weight: bold;
}

.recent-records {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1px solid #f0f0f0;
  
  &:last-child {
    border-bottom: none;
  }
}

.record-left {
  display: flex;
  align-items: center;
}

.category-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.record-info {
  display: flex;
  flex-direction: column;
}

.category-name {
  font-size: 28rpx;
  margin-bottom: 5rpx;
}

.record-time {
  font-size: 24rpx;
  color: #999;
}

.amount {
  font-size: 32rpx;
  font-weight: bold;
  
  &.expense {
    color: #FF6B6B;
  }
  
  &.income {
    color: #51CF66;
  }
}
</style>
```

---

## 五、开发流程

### 5.1 环境准备
1. **安装HBuilderX**：https://www.dcloud.io/hbuilderx.html
2. **安装微信开发者工具**：https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html
3. **注册微信小程序账号**：https://mp.weixin.qq.com/

### 5.2 项目初始化
```bash
# 使用HBuilderX创建uniapp项目
# 选择模板：Vue3/Vite版

# 安装依赖
npm install

# 安装uView UI
npm install uview-plus

# 安装Pinia
npm install pinia

# 安装Day.js
npm install dayjs
```

### 5.3 开发调试
- **H5调试**：在HBuilderX中点击"运行 > 运行到浏览器"
- **小程序调试**：在HBuilderX中点击"运行 > 运行到小程序模拟器 > 微信开发者工具"
- **真机调试**：使用HBuilderX的真机运行功能

### 5.4 打包发布
- **微信小程序**：HBuilderX > 发行 > 小程序-微信
- **H5**：HBuilderX > 发行 > 网站-H5手机版
- **APP**：HBuilderX > 发行 > 原生App-云打包

---

## 六、注意事项

### 6.1 跨平台兼容性
- 使用uni-app的条件编译处理平台差异
- 测试各个平台的兼容性
- 注意小程序的包大小限制（主包2MB，分包20MB）

### 6.2 性能优化
- 图片使用webp格式
- 列表使用虚拟滚动
- 合理使用分包加载
- 减少setData的频率

### 6.3 用户体验
- 添加loading状态
- 完善错误提示
- 支持下拉刷新
- 优化页面加载速度

---

## 七、开发建议

1. **先开发微信小程序版本**，再适配其他平台
2. **使用uView UI组件库**，提高开发效率
3. **做好状态管理**，使用Pinia管理全局状态
4. **封装通用组件**，提高代码复用率
5. **规范代码风格**，使用ESLint + Prettier
6. **做好版本控制**，使用Git管理代码

---

**祝开发顺利！** 🚀
