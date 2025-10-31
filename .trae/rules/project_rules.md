# RuoYi-Magic 项目开发规则

## 1. 测试与部署规则

1. 任何开发或者修改完成之后不要尝试启动开发服务器来测试功能，或者修改是否生效，而是让用户来测试。

## 2. MyBatis-Flex 使用规范

### 2.1 Service 层方法使用规范
- **优先使用 MyBatis-Flex 提供的封装方法**，避免自定义方法名
- 基础 CRUD 操作必须使用以下标准方法：
  - 查询单个：`getById(id)` 替代 `selectXxxById(id)`
  - 插入：`save(entity)` 替代 `insertXxx(entity)`
  - 更新：`updateById(entity)` 替代 `updateXxx(entity)`
  - 删除单个：`removeById(id)` 替代 `deleteXxxById(id)`
  - 删除多个：`removeByIds(ids)` 替代 `deleteXxxByIds(ids)`
  - 批量操作：`saveBatch(list)` 替代自定义批量方法

### 2.2 返回值处理规范
- MyBatis-Flex 封装方法返回 `boolean` 类型，不是 `int` 类型
- 更新返回值判断：`if (result)` 替代 `if (result > 0)`
- 删除操作同样返回 `boolean` 类型

### 2.3 复杂查询规范
- 简单条件查询使用 `QueryWrapper`
- 复杂业务查询可保留自定义方法，但需在 Mapper 接口中明确声明
- 分页查询使用 `Page<T> page(Page<T> page, QueryWrapper queryWrapper)`

### 2.4 实体类字段映射
- 确保实体类字段名与数据库字段正确映射
- 主键字段统一使用 `id`，避免使用业务相关的字段名如 `sessionId`
- 使用 `@Column` 注解明确字段映射关系

## 3. 代码重构规范

### 3.1 方法替换原则
- 重构时必须保持业务逻辑不变
- 替换方法前先确认新方法的返回值类型和参数要求
- 批量替换时逐个验证，避免遗漏

### 3.2 编译验证
- 每次重构后必须执行 `mvn compile` 验证代码正确性
- 发现编译错误立即修复，不允许带错误提交

### 3.3 业务方法处理
- 非标准 CRUD 的业务方法需要重新实现
- 使用 `QueryWrapper` 构建复杂查询条件
- 保持方法的业务语义清晰

## 4. Spring Boot 3.x 规范

### 4.1 依赖管理
- 使用 Spring Boot 3.3.6 版本
- JDK 版本必须为 17+
- 所有依赖版本与 Spring Boot 版本保持兼容

### 4.2 配置规范
- 使用 `application.yml` 进行配置管理
- 敏感信息使用环境变量或配置文件分离
- 数据源配置使用 HikariCP 连接池

## 5. Sa-Token 安全规范

### 5.1 权限控制
- 所有 Controller 方法必须添加适当的权限注解
- 使用 `@SaCheckPermission` 进行权限校验
- 敏感操作添加角色验证

### 5.2 Token 管理
- Token 有效期设置为 30 天（2592000 秒）
- 支持并发登录，但需要合理控制
- 重要操作需要验证 Token 有效性

## 6. 代码质量规范

### 6.1 异常处理
- 使用统一的异常处理机制
- 数据库操作异常必须妥善处理
- 返回给前端的错误信息要用户友好

### 6.2 日志规范
- 使用 SLF4J + Logback 进行日志记录
- 重要业务操作必须记录日志
- 错误日志包含足够的上下文信息

### 6.3 性能优化
- 避免 N+1 查询问题
- 合理使用缓存机制
- 大数据量操作使用分页处理

## 7. AI 功能集成规范

### 7.1 Spring AI Alibaba 使用
- AI 功能调用必须添加异常处理
- 长时间 AI 操作使用异步处理
- AI 响应结果需要验证和过滤

### 7.2 工具类使用
- 优先使用 Hutool 工具类
- 字符串操作使用 `StrUtil`
- 集合操作使用 `CollUtil`

## 8. 数据库操作最佳实践

### 8.1 事务管理
- 使用 `@Transactional` 注解管理事务
- 只读操作使用 `@Transactional(readOnly = true)`
- 长事务拆分为多个短事务

### 8.2 复杂 SQL 使用 Db 类规范
- **优先使用 Db 类处理复杂 SQL**，避免在 Mapper 中定义过多自定义方法
- 复杂连表查询直接使用 `Db.selectListBySql()` 方法
- 子查询、窗口函数、CTE 等高级 SQL 特性使用 `Db.selectListBySql()`
- 复杂统计查询使用 `Db.selectCount()` 或 `Db.selectObject()`

#### 8.2.1 连表查询规范
```java
// ✅ 推荐：直接使用 SQL 进行复杂连表查询
String sql = """
    SELECT u.*, d.dept_name, r.role_name 
    FROM user u 
    LEFT JOIN department d ON u.dept_id = d.id 
    LEFT JOIN role r ON u.role_id = r.id 
    WHERE u.status = ? AND d.status = ?
    ORDER BY u.created_at DESC
    """;
List<Row> result = Db.selectListBySql(sql, 1, 1);
```

#### 8.2.2 子查询规范
```java
// ✅ 推荐：使用 Db 类处理 IN 子查询
String subQuerySql = """
    SELECT u.* FROM user u 
    WHERE u.dept_id IN (
        SELECT d.id FROM department d 
        WHERE d.region = ? AND d.status = ?
    ) 
    AND u.salary > (
        SELECT AVG(salary) FROM user WHERE dept_id = u.dept_id
    )
    """;
List<Row> result = Db.selectListBySql(subQuerySql, "华东", 1);
```

#### 8.2.3 窗口函数规范
```java
// ✅ 推荐：使用 Db 类处理窗口函数查询
String rankSql = """
    SELECT 
        name, salary, dept_id,
        ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) as rank_in_dept,
        RANK() OVER (ORDER BY salary DESC) as overall_rank
    FROM user 
    WHERE status = ?
    """;
List<Row> rankedUsers = Db.selectListBySql(rankSql, 1);
```

#### 8.2.4 CTE（公用表表达式）规范
```java
// ✅ 推荐：使用 Db 类处理 CTE 查询
String cteSql = """
    WITH dept_stats AS (
        SELECT dept_id, COUNT(*) as emp_count, AVG(salary) as avg_salary
        FROM user WHERE status = 1 GROUP BY dept_id
    )
    SELECT d.dept_name, ds.emp_count, ds.avg_salary,
        CASE 
            WHEN ds.avg_salary > 8000 THEN '高薪部门'
            WHEN ds.avg_salary > 5000 THEN '中薪部门'
            ELSE '低薪部门'
        END as salary_level
    FROM dept_stats ds
    JOIN department d ON ds.dept_id = d.id
    ORDER BY ds.avg_salary DESC
    """;
List<Row> deptAnalysis = Db.selectListBySql(cteSql);
```

#### 8.2.5 复杂分页查询规范
```java
// ✅ 推荐：复杂查询使用手动分页
// 1. 先查询总数
String countSql = """
    SELECT COUNT(*) FROM user u 
    LEFT JOIN department d ON u.dept_id = d.id 
    WHERE u.status = ? AND d.region = ?
    """;
long total = Db.selectCount(countSql, 1, "华东");

// 2. 再查询分页数据
String dataSql = """
    SELECT u.*, d.dept_name 
    FROM user u 
    LEFT JOIN department d ON u.dept_id = d.id 
    WHERE u.status = ? AND d.region = ?
    ORDER BY u.salary DESC 
    LIMIT ? OFFSET ?
    """;
int offset = (pageNum - 1) * pageSize;
List<Row> data = Db.selectListBySql(dataSql, 1, "华东", pageSize, offset);
```

#### 8.2.6 SQL 代码组织规范
```java
// ✅ 推荐：将复杂 SQL 抽取为常量
public class UserDao {
    private static final String COMPLEX_QUERY_SQL = """
        SELECT u.*, d.dept_name, r.role_name
        FROM user u
        LEFT JOIN department d ON u.dept_id = d.id
        LEFT JOIN role r ON u.role_id = r.id
        WHERE u.status = ? AND u.created_at >= ?
        ORDER BY u.created_at DESC
        """;
    
    public List<Row> findActiveUsersWithDetails(Date startDate) {
        return Db.selectListBySql(COMPLEX_QUERY_SQL, 1, startDate);
    }
}
```

### 8.3 SQL 优化
- 避免使用 `SELECT *`
- 合理使用索引
- 复杂查询优先考虑性能

## 9. 管理端接口调用规范

### 9.1 前端API调用规范

#### 9.1.1 request.js 响应处理机制
- **理解自动数据提取**：`request.js` 会自动提取 `res.data.data` 作为返回值
- **后端响应结构**：后端统一返回格式为 `{code, msg, data}`
- **前端接收数据**：前端API函数直接返回 `data` 部分，无需再次访问 `.data` 属性

```javascript
// ❌ 错误：重复访问 .data
const result = await generateImage(params);
const images = result.data.images; // 错误！result 已经是 data 部分

// ✅ 正确：直接使用返回的数据
const result = await generateImage(params);
const images = result.images; // 正确！
```

#### 9.1.2 响应数据验证规范
- **必须验证数据存在性**：调用API后必须验证返回数据的结构
- **类型检查**：验证关键字段的数据类型
- **容错处理**：对可能缺失的字段提供默认值

```javascript
// ✅ 推荐的响应处理方式
try {
  const response = await apiFunction(params);
  
  // 验证响应数据结构
  if (!response) {
    throw new Error('接口返回数据为空');
  }
  
  // 验证关键字段
  if (!response.images || !Array.isArray(response.images)) {
    throw new Error('返回数据格式错误：images字段缺失或非数组');
  }
  
  // 处理业务逻辑
  processData(response.images);
  
} catch (error) {
  console.error('API调用失败:', error);
  // 用户友好的错误提示
  ElMessage.error('操作失败，请稍后重试');
}
```

#### 9.1.3 API函数定义规范
- **统一使用 request 工具**：所有API调用必须使用项目的 `request.js`
- **明确请求方法**：GET、POST、PUT、DELETE 方法要与后端接口保持一致
- **参数传递规范**：POST请求使用 `data`，GET请求使用 `params`

```javascript
// ✅ 标准API函数定义
import request from '@/utils/request'

// GET请求示例
export function getSupportedModels() {
  return request({
    url: '/ai/image/models',
    method: 'get'
  })
}

// POST请求示例
export function generateImage(data) {
  return request({
    url: '/ai/image/generate',
    method: 'post',
    data
  })
}
```

### 9.2 Vue组件接口调用规范

#### 9.2.1 异步操作处理
- **使用 async/await**：优先使用 async/await 处理异步操作
- **加载状态管理**：长时间操作必须显示加载状态
- **防重复提交**：避免用户重复点击导致的多次请求

```javascript
// ✅ 推荐的异步处理方式
const loading = ref(false);

const handleSubmit = async () => {
  if (loading.value) return; // 防重复提交
  
  loading.value = true;
  try {
    const result = await apiFunction(formData.value);
    // 处理成功结果
    ElMessage.success('操作成功');
  } catch (error) {
    console.error('操作失败:', error);
    ElMessage.error('操作失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};
```

#### 9.2.2 数据响应式处理
- **合理使用 ref/reactive**：根据数据结构选择合适的响应式API
- **数据初始化**：为响应式数据提供合理的初始值
- **数据更新时机**：在合适的生命周期钩子中调用API

```javascript
// ✅ 推荐的响应式数据管理
const formData = reactive({
  prompt: '',
  model: '',
  size: ''
});

const images = ref([]);
const supportedModels = ref([]);

// 组件挂载时加载基础数据
onMounted(async () => {
  try {
    const models = await getSupportedModels();
    supportedModels.value = models || [];
  } catch (error) {
    console.error('加载模型列表失败:', error);
  }
});
```

### 9.3 错误处理规范

#### 9.3.1 分层错误处理
- **API层错误**：在API函数中处理网络错误和HTTP状态码错误
- **业务层错误**：在组件中处理业务逻辑错误和数据格式错误
- **用户界面错误**：向用户显示友好的错误信息

#### 9.3.2 错误信息规范
- **开发环境**：详细的错误信息用于调试
- **生产环境**：用户友好的错误提示
- **错误日志**：重要错误必须记录到控制台

```javascript
// ✅ 推荐的错误处理方式
const handleError = (error, userMessage = '操作失败') => {
  // 开发环境显示详细错误
  if (process.env.NODE_ENV === 'development') {
    console.error('详细错误信息:', error);
  }
  
  // 生产环境显示用户友好信息
  ElMessage.error(userMessage);
  
  // 记录错误日志
  console.error(`${userMessage}:`, error.message || error);
};
```

### 9.4 性能优化规范

#### 9.4.1 请求优化
- **避免重复请求**：相同参数的请求应该缓存结果
- **请求合并**：多个相关请求可以考虑合并
- **分页加载**：大数据量使用分页或虚拟滚动

#### 9.4.2 数据缓存
- **基础数据缓存**：字典数据、配置数据等可以缓存
- **用户数据缓存**：用户相关数据合理缓存
- **缓存失效策略**：设置合理的缓存过期时间

## 10. 开发流程规范

### 10.1 代码提交
- 提交信息要清晰描述修改内容
- 重大重构需要详细的提交说明

### 10.2 版本管理
- 遵循语义化版本规范
- 重要功能变更需要更新版本号
- 保持 CHANGELOG 更新