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

## 9. 开发流程规范

### 9.1 代码提交
- 提交信息要清晰描述修改内容
- 重大重构需要详细的提交说明

### 9.2 版本管理
- 遵循语义化版本规范
- 重要功能变更需要更新版本号
- 保持 CHANGELOG 更新