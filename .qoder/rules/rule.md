---
trigger: always_on
alwaysApply: true
---




# 项目开发规范

## 后端规范

### Controller层
- 使用 `@RestController` 和 `@RequestMapping` 注解
- 继承 `BaseController` 获取通用方法（提供分页、响应封装等通用功能）
- 使用 `@RequiresPermissions` 进行权限控制
- 统一返回 `TableDataInfo` 或 `AjaxResult`
- 分页接口必须使用 `TableDataInfo`（通过 `BaseController` 的 `getDataTable()` 方法返回）
- 遵循 RESTful 风格：GET查询、POST新增、PUT修改、DELETE删除

### Service层
- 使用 MyBatis-Flex 进行数据库操作
- 查询使用 `QueryWrapper` 构建条件
- 批量操作使用 `updateBatch`、`insertBatch`
- 分页使用 `Page.of(pageNum, pageSize)`
- 关联查询使用 `leftJoin`、`innerJoin`
- **外键关联数据查询规范**：
  - 当需要展示外键关联对象的字段时，必须在 SQL 层面使用左连接（`leftJoin`）完成数据聚合
  - 禁止在代码中循环调用服务查询关联数据，避免 N+1 查询问题和并发安全隐患
  - 关联字段使用 `as` 别名映射到实体的冗余字段上（如 `b.title as blogName`）
  - 别名设置方式：`.from("table").as("u").leftJoin("join_table").as("b").on("连接条件")`

### 实体类
- 使用 `@Table` 指定表名
- 使用 `@Id` 标识主键（ID默认为数据库自增，无需手动设置）
- 使用 `@Column` 映射字段
- 继承 `BaseEntity` 获取通用字段（createBy、createTime、updateBy、updateTime 由 MyBatis-Flex 自动填充，无需手动处理）
- **外键冷备字段规范**：
  - 当列表需要显示外键关联对象的名称不是 ID 时，应在实体中添加 `@Column(ignore = true)` 的冗余字段
  - 字段名称颜色推荐：外键表为 `表名Name`（如 `blogName`、`deptName`、`roleName`）
  - 此类字段由后端通过左连接查询填充，不不存储到数据库

### 工具类使用
- 优先使用 Hutool 工具类库
- 字符串操作使用 `StrUtil`（如：`StrUtil.isBlank()`、`StrUtil.format()` 等）
- 对象复制使用 `BeanUtil.copyProperties()`
- 集合操作使用 `CollUtil`
- 日期处理使用 `DateUtil`
- 其他常用工具：`Convert`（类型转换）、`ObjectUtil`（对象判空）等

### MyBatis-Flex 左连接查询示例
```java
// Service 或 Controller 中：
QueryWrapper qw = QueryWrapper.create()
    .select("u.*", "b.title as blogName")  // 选择字段，外键表字段使用 as 加别名
    .from("user").as("u")                   // 主表设置别名
    .leftJoin("blog").as("b")               // 幾合表设置别名
    .on("u.blog_id = b.blog_id")            // 设置连接条件
    .where("u.del_flag = 0");               // 添加查询条件

Page<User> page = userService.page(new Page<>(pageNum, pageSize), qw);
return getDataTable(page);  // 会自动填充 blogName 字段
```



## 前端规范

### Vue组件
- 使用 `export default` 导出组件
- `name` 属性必填，用于路由缓存
- `data` 使用函数返回对象
- API调用统一在 `api` 目录管理

### HTTP请求拦截器（request.js）
- 所有API请求统一通过 `request.js` 发起
- 响应拦截器统一处理返回数据：
  - 成功响应：`code === 200` 时返回 `res.data`（业务数据）
  - 错误处理：`code !== 200` 时根据错误码进行处理
    - `401`：未授权，跳转登录页
    - `500`：服务器错误,提示错误信息
    - 其他错误码：显示后端返回的 `msg` 信息
  - 统一错误提示使用 `Message.error()` 或 `$modal.msgError()`
- 请求拦截器自动添加 `Authorization` token 到请求头
- 业务代码中直接使用 `res` 获取数据，无需再判断 `code`

### 表格操作
- 使用 `el-table` 展示数据
- 使用 `pagination` 组件分页
- 搜索表单使用 `el-form` + `queryParams`
- 操作按钮统一使用 `el-button`
- **列表字段显示规则**：
  - 主表ID字段（如 `id`、`userId` 等）不显示在列表中
  - 关联表ID字段（如 `deptId`、`roleId` 等）处理规则：
    - 优先显示冗余字段（如 `deptName`、`roleName`）
    - 如果后端已返回对应的关联字段名称，则显示该字段
    - 如果既没有冗余字段，后端也未返回关联数据，则不显示该ID字段

### 表单提交
- 使用 `rules` 定义验证规则
- 提交前调用 `$refs.form.validate()`
- 成功后使用 `$modal.msgSuccess()` 提示
- 失败使用 `$modal.msgError()` 提示

### 界面布局规范
- **避免写死尺寸**：不使用固定的 `px` 值设置宽度和高度
- **使用相对单位**：优先使用 `%`、`vh`、`vw`、`flex` 等相对单位
- **弹性布局**：使用 `flex` 布局实现自适应
- **响应式设计**：
  - 容器使用 `height: 100%` 或 `calc(100vh - xxx)` 实现高度自适应
  - 表格高度使用 `:height="tableHeight"` 动态计算
  - 对话框宽度使用百分比（如 `width="80%"`）或 `max-width` 限制最大宽度
- **栅格系统**：使用 Element UI 的 `el-row` 和 `el-col` 实现响应式栅格布局
- **避免固定尺寸场景**：
  - 表单项不设置固定宽度，使用 `el-col` 的 `:span` 属性控制比例
  - 卡片、面板使用 `height: 100%` 填充父容器
  - 内容区域使用 `overflow: auto` 实现内容溢出滚动

## 命名规范
- 文件名：小驼峰 `userInfo.vue`
- 组件名：大驼峰 `UserInfo`
- 方法名：小驼峰 `getUserList`
- 常量：大写下划线 `MAX_COUNT`
