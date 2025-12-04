# MyBatis-Flex 数据库操作指南

基于 `Db`、`QueryWrapper`、`DbChain` 三个核心类的使用说明，适合喜欢直接写 SQL 的开发者。

> **说明**：本文档侧重于直接使用 `Db`、`QueryWrapper`、`DbChain` 进行数据库操作。如需了解 `IService`、`BaseMapper` 的业务层标准模式，请参阅 [MyBatis-Flex 开发规范.md](./MyBatis-Flex%20开发规范.md)。

## 目录

- [1. 单表基础操作（CRUD）](#1-单表基础操作crud)
- [2. 单表查询操作](#2-单表查询操作)
- [3. 连表查询](#3-连表查询)
  - [3.1 简单连表 - 推荐直接写SQL](#31-简单连表---推荐直接写sql)
  - [3.2 动态连表 - 使用 QueryWrapper](#32-动态连表---使用-querywrapper)
  - [3.3 表别名设置 - 使用 QueryWrapper](#33-表别名设置---使用-querywrapper)
- [4. 复杂查询](#4-复杂查询)
- [5. 分页查询](#5-分页查询)
  - [5.1 简单分页 - 使用 Db.paginate](#51-简单分页---使用-dbpaginate)
  - [5.2 链式分页 - 使用 DbChain](#52-链式分页---使用-dbchain)
  - [5.3 复杂连表分页](#53-复杂连表分页)
  - [5.4 分页方式选择建议](#54-分页方式选择建议)
  - [5.5 分页查询重要优化](#55-分页查询重要优化)
- [6. 事务操作](#6-事务操作)
- [7. 批量操作](#7-批量操作)
- [8. 部分字段更新 - UpdateEntity](#8-部分字段更新---updateentity)
- [9. 推荐使用场景总结](#9-推荐使用场景总结)
- [10. 最佳实践建议](#10-最佳实践建议)

## 1. 单表基础操作（CRUD）

### 1.1 简单插入/更新/删除 - 推荐使用 `Db` 类

#### 插入操作
```java
// 基本插入
Row user = new Row();
user.set("name", "张三");
user.set("age", 25);
user.set("email", "zhangsan@example.com");
int result = Db.insert("user", user);

// 使用SQL插入
int result = Db.insertBySql("INSERT INTO user(name, age) VALUES(?, ?)", "李四", 30);

// 批量插入
List<Row> users = Arrays.asList(user1, user2, user3);
int[] results = Db.insertBatch("user", users);
```

#### 更新操作
```java
// 根据ID更新
Row updateData = new Row();
updateData.set("name", "李四");
updateData.set("age", 26);
int result = Db.updateById("user", updateData.setId("id", 1));

// 使用SQL更新
int result = Db.updateBySql("UPDATE user SET status = ? WHERE age > ?", 1, 18);

// 根据条件更新
Map<String, Object> whereCondition = Map.of("status", 0);
int result = Db.updateByMap("user", updateData, whereCondition);

// 使用 UpdateEntity 进行部分字段更新（包括null字段）
// 适用于只更新某些字段的场景，特别是需要将字段设置为null时
Account account = UpdateEntity.of(Account.class, 100);
account.setUserName(null);  // 将userName字段更新为null
account.setAge(10);  // 更新age字段为10
account.setBalance(99.99);  // 选择性更新
int result = Db.updateById("account", account);
```

#### 删除操作
```java
// 根据ID删除
int result = Db.deleteById("user", "id", 1);

// 根据条件删除
Map<String, Object> condition = Map.of("status", 0);
int result = Db.deleteByMap("user", condition);

// 使用SQL删除
int result = Db.deleteBySql("DELETE FROM user WHERE created_at < ?", lastMonth);
```

### 1.2 链式操作 - 推荐使用 `DbChain`

#### 链式插入
```java
boolean success = DbChain.table("user")
    .set("name", "王五")
    .set("age", 30)
    .set("email", "wangwu@example.com")
    .save();
```

#### 链式更新
```java
boolean updated = DbChain.table("user")
    .set("status", 1)
    .set("updated_at", new Date())
    .where("id = ?", 1)
    .update();
```

#### 链式删除
```java
boolean deleted = DbChain.table("user")
    .where("status = ? AND created_at < ?", 0, lastMonth)
    .remove();
```

## 2. 单表查询操作

### 2.1 简单查询 - 推荐使用 `Db` 类

#### 基础查询
```java
// 根据ID查询
Row user = Db.selectOneById("user", "id", 1);

// 根据条件查询单条
Map<String, Object> condition = Map.of("email", "test@example.com");
Row user = Db.selectOneByMap("user", condition);

// 查询列表
List<Row> users = Db.selectListByMap("user", condition);

// 查询所有
List<Row> allUsers = Db.selectAll("user");
```

#### SQL查询
```java
// 直接SQL查询
String sql = "SELECT * FROM user WHERE status = ? AND age >= ?";
List<Row> users = Db.selectListBySql(sql, 1, 18);

// 查询单个值
Object maxAge = Db.selectObject("SELECT MAX(age) FROM user WHERE status = ?", 1);

// 统计查询
long count = Db.selectCount("SELECT COUNT(*) FROM user WHERE status = ?", 1);
```

### 2.2 链式查询 - 推荐使用 `DbChain`

#### 基础链式查询
```java
// 单条查询
Row user = DbChain.table("user")
    .where("email = ?", "test@example.com")
    .one();

// 可选查询（避免空指针）
Optional<Row> userOpt = DbChain.table("user")
    .where("id = ?", 1)
    .oneOpt();

// 列表查询
List<Row> activeUsers = DbChain.table("user")
    .where("status = ? AND age >= ?", 1, 18)
    .orderBy("created_at DESC")
    .limit(10)
    .list();
```

#### 统计和检查
```java
// 统计记录数
long count = DbChain.table("user")
    .where("status = ?", 1)
    .count();

// 检查是否存在
boolean exists = DbChain.table("user")
    .where("email = ?", "test@example.com")
    .exists();

// 查询单个值
Object maxSalary = DbChain.table("user")
    .select("MAX(salary)")
    .where("dept_id = ?", 1)
    .obj();
```

## 3. 连表查询

### 3.1 简单连表 - 推荐直接写SQL

#### 两表连接
```java
String sql = """
    SELECT u.*, r.role_name 
    FROM user u 
    LEFT JOIN role r ON u.role_id = r.id 
    WHERE u.status = ? AND r.status = ?
    """;
List<Row> result = Db.selectListBySql(sql, 1, 1);
```

#### 多表连接
```java
String complexSql = """
    SELECT 
        u.name, 
        d.dept_name, 
        COUNT(p.id) as project_count
    FROM user u
    LEFT JOIN department d ON u.dept_id = d.id
    LEFT JOIN user_project up ON u.id = up.user_id
    LEFT JOIN project p ON up.project_id = p.id
    WHERE u.status = ? AND d.status = ?
    GROUP BY u.id, d.id
    HAVING COUNT(p.id) > ?
    ORDER BY project_count DESC
    """;
List<Row> complexResult = Db.selectListBySql(complexSql, 1, 1, 0);
```

### 3.2 动态连表 - 使用 `QueryWrapper`

```java
// 构建动态连表查询
QueryWrapper query = QueryWrapper.create()
    .select("u.*", "r.role_name", "d.dept_name")
    .from("user u")
    .leftJoin("role r").on("u.role_id = r.id")
    .leftJoin("department d").on("u.dept_id = d.id")
    .where("u.status = ?", 1);

// 动态添加条件
if (roleId != null) {
    query.and("u.role_id = ?", roleId);
}
if (deptId != null) {
    query.and("u.dept_id = ?", deptId);
}
if (StringUtil.hasText(keyword)) {
    query.and("u.name LIKE ?", "%" + keyword + "%");
}

query.orderBy("u.created_at DESC");
List<Row> users = Db.selectListByQuery(query);
```

### 3.3 表别名设置 - 使用 `QueryWrapper`

在MyBatis-Flex的`QueryWrapper`中，设置表别名有几种方式：

#### 3.3.1 使用 `as()` 方法为主表设置别名

```java
// 为FROM子句中的第一个表设置别名
QueryWrapper query = QueryWrapper.create()
    .select("u.*")
    .from("user")
    .as("u")  // 为user表设置别名u
    .where("u.status = ?", 1);

// 生成SQL: SELECT u.* FROM user u WHERE u.status = ?
```

#### 3.3.2 在 `from()` 方法中直接设置别名

```java
// 方法1：使用QueryTable
QueryWrapper query = QueryWrapper.create()
    .select("u.*")
    .from(new QueryTable("user").as("u"))
    .where("u.status = ?", 1);

// 方法2：多表查询时设置别名
QueryWrapper query = QueryWrapper.create()
    .select("u.*", "r.*")
    .from(new QueryTable("user").as("u"), new QueryTable("role").as("r"));
```

#### 3.3.3 在JOIN操作中设置别名

```java
QueryWrapper query = QueryWrapper.create()
    .select("u.*", "r.role_name")
    .from("user").as("u")  // 主表别名
    .leftJoin(new QueryTable("role").as("r"))  // JOIN表别名
    .on("u.role_id = r.id")
    .where("u.status = ?", 1);

// 或者直接在join方法中使用字符串
QueryWrapper query = QueryWrapper.create()
    .select("u.*", "r.role_name")
    .from("user u") 
    .leftJoin("role r")  // 直接在表名后加别名
    .on("u.role_id = r.id");
```

#### 3.3.4 复杂查询中的表别名设置

```java
QueryWrapper query = QueryWrapper.create()
    .select("u.name", "d.dept_name", "r.role_name")
    .from("user").as("u")  // 主表别名
    .leftJoin("department").as("d").on("u.dept_id = d.id")  // 链式设置别名
    .leftJoin("role r").on("u.role_id = r.id")  // 直接在join中设置
    .where("u.status = ?", 1)
    .and("d.status = ?", 1)
    .orderBy("u.created_at DESC");

// 生成SQL类似：
// SELECT u.name, d.dept_name, r.role_name
// FROM user u
// LEFT JOIN department d ON u.dept_id = d.id
// LEFT JOIN role r ON u.role_id = r.id
// WHERE u.status = ? AND d.status = ?
// ORDER BY u.created_at DESC
```

#### 3.3.5 子查询中的表别名

```java
// 子查询作为表使用时设置别名
QueryWrapper subQuery = QueryWrapper.create()
    .select("dept_id", "COUNT(*) as user_count")
    .from("user")
    .where("status = ?", 1)
    .groupBy("dept_id");

QueryWrapper mainQuery = QueryWrapper.create()
    .select("d.dept_name", "uc.user_count")
    .from("department d")
    .leftJoin(subQuery).as("uc").on("d.id = uc.dept_id");  // 子查询别名
```

#### 3.3.6 在DbChain中使用表别名

由于`DbChain`主要用于单表操作，通常不需要设置别名，但如果需要在WHERE条件中明确指定表名，可以：

```java
// 虽然DbChain主要用于单表，但条件中可以使用表前缀
DbChain.table("user")
    .where("user.status = ? AND user.age > ?", 1, 18)
    .list();
```

#### 3.3.7 表别名设置总结

表别名的设置方式：

1. **主表别名**：使用 `.as("alias")` 方法
2. **FROM中别名**：使用 `QueryTable` 对象或直接在表名后加别名
3. **JOIN中别名**：在JOIN方法中直接指定或使用 `QueryTable.as()`
4. **子查询别名**：子查询作为表时使用 `.as("alias")`

选择哪种方式主要看个人习惯和代码风格，但建议保持一致性。对于复杂查询，使用别名可以让SQL更清晰易读。推荐使用 `.as()` 方法设置别名，这样表名和别名分离，代码更加清晰。

## 4. 复杂查询

### 4.1 子查询

#### IN子查询
```java
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

#### EXISTS子查询
```java
String existsSql = """
    SELECT u.* FROM user u 
    WHERE EXISTS (
        SELECT 1 FROM user_role ur 
        WHERE ur.user_id = u.id AND ur.role_id = ?
    )
    """;
List<Row> usersWithRole = Db.selectListBySql(existsSql, 1);
```

### 4.2 窗口函数查询

#### 排名查询
```java
String rankSql = """
    SELECT 
        name, 
        salary, 
        dept_id,
        ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) as rank_in_dept,
        RANK() OVER (ORDER BY salary DESC) as overall_rank
    FROM user 
    WHERE status = ?
    """;
List<Row> rankedUsers = Db.selectListBySql(rankSql, 1);
```

#### 累计统计
```java
String cumulativeSql = """
    SELECT 
        DATE(created_at) as date,
        COUNT(*) as daily_count,
        SUM(COUNT(*)) OVER (ORDER BY DATE(created_at)) as cumulative_count
    FROM user 
    WHERE created_at >= ?
    GROUP BY DATE(created_at)
    ORDER BY date
    """;
List<Row> stats = Db.selectListBySql(cumulativeSql, startDate);
```

### 4.3 CTE（公用表表达式）查询

```java
String cteSql = """
    WITH dept_stats AS (
        SELECT 
            dept_id,
            COUNT(*) as emp_count,
            AVG(salary) as avg_salary
        FROM user 
        WHERE status = 1
        GROUP BY dept_id
    )
    SELECT 
        d.dept_name,
        ds.emp_count,
        ds.avg_salary,
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

## 5. 分页查询

### 5.1 简单分页 - 使用 `Db.paginate`

在MyBatis-Flex中，分页查询有两种主要实现方式：使用内置的分页功能和手动分页。下面详细介绍这两种方式。

#### 5.1.1 方式一：指定表名的分页查询

这种方式适用于单表查询或者简单的连表查询，需要指定主表名称。

```java
// 构建查询条件
QueryWrapper query = QueryWrapper.create()
    .select("*")
    .from("user")
    .where("status = ?", 1)
    .orderBy("created_at DESC");

// 执行分页查询 - 方式一：指定表名
// 参数说明：表名、页码、每页大小、查询条件
Page<Row> page = Db.paginate("user", 1, 20, query);

System.out.println("总记录数: " + page.getTotalRow());
System.out.println("总页数: " + page.getTotalPage());
System.out.println("当前页: " + page.getPageNumber());
List<Row> users = page.getRecords();
```

#### 5.1.2 方式一在复杂连表查询中的应用

在复杂连表查询中使用方式一时，需要注意以下几点：

1. **主表名称**：必须指定正确的主表名称作为第一个参数
2. **表别名**：如果使用了表别名，查询条件中必须使用一致的别名

```java
// 构建带别名的连表查询
QueryWrapper query = QueryWrapper.create()
    .select("u.*", "d.dept_name", "r.role_name")
    .from("user").as("u")  // 使用as()方法设置别名
    .leftJoin("department").as("d").on("u.dept_id = d.id")
    .leftJoin("role").as("r").on("u.role_id = r.id")
    .where("u.status = ?", 1)
    .orderBy("u.created_at DESC");

// 执行分页查询 - 注意第一个参数是主表名称，不包含别名
Page<Row> page = Db.paginate("user", 1, 20, query);

// 生成的SQL类似：
// SELECT COUNT(*) FROM user u LEFT JOIN department d ON ... LEFT JOIN role r ON ... WHERE ...;
// SELECT u.*, d.dept_name, r.role_name FROM user u LEFT JOIN ... LIMIT 20 OFFSET 0;
```

#### 5.1.3 条件分页
```java
// 根据条件分页
QueryCondition condition = QueryCondition.create()
    .and("status = ?", 1)
    .and("age >= ?", 18);

Page<Row> page = Db.paginate("user", 1, 20, condition);
```

### 5.2 链式分页 - 使用 `DbChain`

#### 基础链式分页
```java
Page<Row> page = new Page<>(1, 20);
Page<Row> result = DbChain.table("user")
    .where("status = ? AND age >= ?", 1, 18)
    .orderBy("salary DESC", "created_at DESC")
    .page(page);
```

#### 已知总数的分页（性能优化）
```java
// 如果已知总数，避免重复统计
Page<Row> pageWithTotal = new Page<>(2, 20, 1000L);
Page<Row> result = DbChain.table("user")
    .where("status = ?", 1)
    .page(pageWithTotal);
```

### 5.3 复杂连表分页

#### 5.3.1 方式二：手动分页（推荐复杂查询使用）

对于非常复杂的连表查询、子查询或特殊SQL，推荐使用手动分页方式，这样可以完全控制SQL的执行。

```java
// 先查询总数
String countSql = """
    SELECT COUNT(*) FROM user u 
    LEFT JOIN department d ON u.dept_id = d.id 
    WHERE u.status = ? AND d.region = ?
    """;
long total = Db.selectCount(countSql, 1, "华东");

// 再查询分页数据
String dataSql = """
    SELECT u.*, d.dept_name 
    FROM user u 
    LEFT JOIN department d ON u.dept_id = d.id 
    WHERE u.status = ? AND d.region = ?
    ORDER BY u.salary DESC 
    LIMIT ? OFFSET ?
    """;

int pageNum = 1, pageSize = 20;
int offset = (pageNum - 1) * pageSize;
List<Row> data = Db.selectListBySql(dataSql, 1, "华东", pageSize, offset);

// 手动构建分页对象
Page<Row> page = new Page<>(pageNum, pageSize, total);
page.setRecords(data);
```

#### 5.3.2 使用QueryWrapper实现手动分页

也可以使用QueryWrapper实现手动分页，这样可以更灵活地构建查询条件：

```java
// 构建查询条件
QueryWrapper query = QueryWrapper.create()
    .select("u.*", "d.dept_name")
    .from("user").as("u")
    .leftJoin("department").as("d").on("u.dept_id = d.id")
    .where("u.status = ?", 1)
    .orderBy("u.created_at DESC");

// 1. 先查询总数
QueryWrapper countQuery = QueryWrapper.create()
    .select("COUNT(*)")
    .from("user").as("u")
    .leftJoin("department").as("d").on("u.dept_id = d.id")
    .where("u.status = ?", 1);
    
long total = Db.selectCountByQuery(countQuery);

// 2. 添加分页参数到数据查询
int pageNum = 1, pageSize = 20;
int offset = (pageNum - 1) * pageSize;
query.limit(pageSize).offset(offset);

// 3. 执行分页数据查询
List<Row> data = Db.selectListByQuery(query);

// 4. 手动构建分页对象
Page<Row> page = new Page<>(pageNum, pageSize, total);
page.setRecords(data);
```

### 5.4 分页方式选择建议

| 分页方式 | 适用场景 | 优点 | 缺点 |
|---------|----------|------|------|
| **方式一：Db.paginate(表名, 页码, 大小, 查询条件)** | 单表查询、简单连表查询 | 代码简洁，自动处理计数和分页 | 对复杂查询支持有限 |
| **方式二：手动分页** | 复杂连表查询、子查询、特殊SQL | 完全控制SQL，灵活性最高 | 代码量较大 |

#### 选择建议：

1. **优先使用方式一**：对于大多数查询场景，特别是单表查询或简单连表查询
2. **复杂查询使用方式二**：当查询包含多表连接、子查询、窗口函数等复杂SQL时

### 5.5 分页查询重要优化

在使用 `QueryWrapper` 进行分页查询时，如果 `QueryWrapper` 使用了左连接（`leftJoin`、`innerJoin` 等），**必须** 设置 `Page` 对象的 `setOptimizeCountQuery(false)`。

#### 问题描述
MyBatis-Flex 默认会在分页查询时自动优化 COUNT 查询语句，以提高性能。当 COUNT 查询被优化时，会自动去掉 JOIN 操作，只统计主表记录数。但是，如果后续在查询条件中包含子表字段的过滤条件，会导致 COUNT 查询与实际数据查询不一致，引发错误。

#### 正确示例
```java
// 构建包含左连接的查询
QueryWrapper queryWrapper = QueryWrapper.create()
    .select("tpdo.*, dc.company_name as deliveryCompanyName")
    .from("third_party_delivery_order").as("tpdo")
    .leftJoin("sys_user").as("su").on("su.user_name = tpdo.create_by and su.status = '0' and su.del_flag = '0'")
    .leftJoin("delivery_company").as("dc").on("dc.user_id = su.user_id and dc.status = '0' and dc.del_flag = '0'")
    .where("tpdo.del_flag = '0'");

// 使用分页方法时必须设置不优化COUNT查询
Page<ThirdPartyDeliveryOrder> pageQuery = new Page<>(pageNum, pageSize);
// 设置不自动优化 COUNT 查询语句。重要：避免连表被优化导致后续子表字段查询条件报错
pageQuery.setOptimizeCountQuery(false);
Page<ThirdPartyDeliveryOrder> page = thirdPartyDeliveryOrderService.page(pageQuery, queryWrapper);
```

#### 错误示例
```java
// ❌ 错误：未设置 setOptimizeCountQuery(false)
// 如果后续有基于 dc.company_name 的查询条件，会报错
Page<ThirdPartyDeliveryOrder> page = new Page<>(pageNum, pageSize);
// page.setOptimizeCountQuery(false); // 忘记设置会导致问题
Page<ThirdPartyDeliveryOrder> result = service.page(page, queryWrapper);
```

#### 适用场景
- 使用 `QueryWrapper` 进行分页查询
- 查询条件包含 JOIN 操作（`leftJoin`、`innerJoin` 等）
- 后续可能添加基于子表字段的查询条件

#### 最佳实践
在 Controller 的分页查询方法中，如果 `QueryWrapper` 包含 JOIN 操作，务必添加 `page.setOptimizeCountQuery(false);` 注释说明原因，确保代码可维护性。

## 6. 事务操作

### 6.1 简单事务

#### 布尔返回值事务
```java
boolean success = Db.tx(() -> {
    // 插入用户
    Row user = new Row();
    user.set("name", "张三");
    user.set("email", "zhangsan@example.com");
    Db.insert("user", user);
    
    // 插入用户角色关系
    Row userRole = new Row();
    userRole.set("user_id", user.get("id"));
    userRole.set("role_id", 1);
    Db.insert("user_role", userRole);
    
    // 返回false或抛异常会回滚
    return true;
});
```

#### 带返回值事务
```java
String result = Db.txWithResult(() -> {
    // 转账操作
    int result1 = Db.updateBySql(
        "UPDATE account SET balance = balance - ? WHERE id = ? AND balance >= ?", 
        100, 1, 100);
    
    if (result1 == 0) {
        throw new RuntimeException("余额不足");
    }
    
    int result2 = Db.updateBySql(
        "UPDATE account SET balance = balance + ? WHERE id = ?", 
        100, 2);
    
    if (result2 == 0) {
        throw new RuntimeException("转入账户不存在");
    }
    
    return "转账成功";
});
```

### 6.2 事务传播行为

```java
// 需要新事务
String result = Db.txWithResult(() -> {
    // 这里的操作会在新事务中执行，与外层事务隔离
    return processInNewTransaction();
}, Propagation.REQUIRES_NEW);

// 必须在事务中
boolean success = Db.tx(() -> {
    // 如果当前没有事务，会抛出异常
    return processInRequiredTransaction();
}, Propagation.MANDATORY);
```

## 7. 批量操作

### 7.1 批量插入

#### 普通批量插入
```java
List<Row> users = new ArrayList<>();
for (int i = 0; i < 1000; i++) {
    Row user = new Row();
    user.set("name", "用户" + i);
    user.set("age", 20 + i % 50);
    user.set("email", "user" + i + "@example.com");
    users.add(user);
}

// 普通批量插入
int[] results = Db.insertBatch("user", users);
```

#### 高效批量插入
```java
// 根据第一条记录的字段进行批量插入（性能更好）
int result = Db.insertBatchWithFirstRowColumns("user", users);

// 指定批次大小
int[] results = Db.insertBatch("user", users, 500);
```

### 7.2 批量更新

#### 使用BatchArgsSetter
```java
List<Integer> userIds = Arrays.asList(1, 2, 3, 4, 5);
List<Integer> newStatus = Arrays.asList(1, 1, 0, 1, 0);

String sql = "UPDATE user SET status = ? WHERE id = ?";
int[] results = Db.updateBatch(sql, new BatchArgsSetter() {
    @Override
    public int getBatchSize() {
        return userIds.size();
    }
    
    @Override
    public Object[] getSqlArgs(int index) {
        return new Object[]{newStatus.get(index), userIds.get(index)};
    }
});
```

#### 批量操作工具方法
```java
// 自定义批量操作
List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
int[] results = Db.executeBatch(ids, 100, RowMapper.class, 
    (mapper, id) -> mapper.updateBySql("UPDATE user SET last_login = NOW() WHERE id = ?", id));
```

### 7.3 大数据量处理

```java
// 分批处理大量数据
public void processBigData(List<Row> bigDataList) {
    int batchSize = 1000;
    
    Db.tx(() -> {
        for (int i = 0; i < bigDataList.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, bigDataList.size());
            List<Row> batch = bigDataList.subList(i, endIndex);
            
            Db.insertBatchWithFirstRowColumns("big_table", batch);
            
            // 每处理一批数据后可以输出进度
            System.out.println("已处理: " + endIndex + "/" + bigDataList.size());
        }
        return true;
    });
}
```

## 8. 部分字段更新 - UpdateEntity

### 8.1 UpdateEntity 概述

在实际业务中，我们经常需要只更新实体的某些字段，而保留其他字段不变。这种场景下，不能简单地创建一个新实体对象并设置要更新的字段，因为其他字段会被设置为 `null`。

MyBatis-Flex 提供的 `UpdateEntity` 工具类可以解决这个问题，它能够准确地记录哪些字段被设置过，只更新被设置过的字段，包括那些被设置为 `null` 的字段。

### 8.2 基本用法

#### 8.2.1 创建 UpdateEntity 实例

```java
// 方式一：指定ID创建（推荐）
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, 100);

// 方式二：创建后再设置ID
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class);
warehouse.setId(100);
```

#### 8.2.2 设置要更新的字段

```java
// 创建 UpdateEntity 实例
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, 100);

// 设置要更新的字段
warehouse.setUserId(null);  // 将userId字段更新为null
warehouse.setUpdateTime(new Date());  // 更新updateTime字段
warehouse.setStatus("0");  // 更新status字段

// 未设置的字段不会被更新
// 例如：如果没有调用 setWarehouseName()，该字段不会被修改
```

#### 8.2.3 执行更新

```java
// 使用继承的 updateById 方法
boolean result = updateById(warehouse);

// 或使用 Db 的方法
int result = Db.updateById(BizWarehouse.class, warehouse);
```

### 8.3 常见应用场景

#### 8.3.1 解除用户绑定

```java
/**
 * 解除用户仓库绑定
 */
@Transactional
public int unbindUserFromWarehouse(Long warehouseId) {
    // 创建 UpdateEntity，只更新userId和updateTime字段
    BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, warehouseId);
    warehouse.setUserId(null);  // 将用户ID设置为null
    warehouse.setUpdateTime(new Date());  // 更新修改时间
    
    return updateById(warehouse) ? 1 : 0;
}
```

#### 8.3.2 部分状态更新

```java
/**
 * 更新订单状态和备注
 */
@Transactional
public int updateOrderStatus(Long orderId, String status, String remark) {
    Order order = UpdateEntity.of(Order.class, orderId);
    order.setStatus(status);
    order.setRemark(remark);
    order.setUpdateTime(new Date());
    
    return updateById(order) ? 1 : 0;
}
```

#### 8.3.3 清空可选字段

```java
/**
 * 清空用户的联系方式
 */
@Transactional
public int clearUserContact(Long userId) {
    SysUser user = UpdateEntity.of(SysUser.class, userId);
    user.setPhone(null);  // 清空手机号
    user.setEmail(null);  // 清空邮箱
    user.setUpdateTime(new Date());
    
    return updateById(user) ? 1 : 0;
}
```

#### 8.3.4 批量字段更新

```java
/**
 * 更新多个字段
 */
@Transactional
public int updateUserProfile(Long userId, String nickName, String avatar, Integer age) {
    SysUser user = UpdateEntity.of(SysUser.class, userId);
    user.setNickName(nickName);
    user.setAvatar(avatar);
    user.setAge(age);
    user.setUpdateTime(new Date());
    
    return updateById(user) ? 1 : 0;
}
```

### 8.4 注意事项

#### 8.4.1 主键必须指定

```java
// ✅ 正确：指定主键
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, 100);
warehouse.setUserId(null);

// ❌ 错误：未指定主键，更新会失败
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class);
warehouse.setUserId(null);
// 必须调用 warehouse.setId(100) 或在创建时指定
```

#### 8.4.2 UpdateEntity 只能用于 updateById

```java
// ✅ 正确：使用 updateById
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, 100);
warehouse.setUserId(null);
updateById(warehouse);

// ❌ 错误：不能与 QueryWrapper 的 update() 方法配合
// UpdateEntity 是为了精确控制更新字段，与条件查询更新不兼容
```

#### 8.4.3 所有字段都未设置的情况

```java
// 虽然可以创建，但通常没有意义
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, 100);
// 没有调用任何 setter 方法
updateById(warehouse);  // 这会导致没有任何字段被更新
```

### 8.5 UpdateEntity vs 普通更新的对比

#### 普通更新方式（全字段更新）

```java
/**
 * 普通方式：更新所有字段
 * 问题：所有未设置的字段都会被设置为null
 */
BizWarehouse warehouse = new BizWarehouse();
warehouse.setId(100);
warehouse.setUserId(null);
warehouse.setUpdateTime(new Date());
// 其他字段如 warehouseName, status 等都会被设置为null

updateById(warehouse);  // 导致这些字段被意外清空
```

#### UpdateEntity 方式（部分字段更新）

```java
/**
 * UpdateEntity 方式：只更新指定字段
 * 优点：只有明确设置过的字段才会被更新
 */
BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, 100);
warehouse.setUserId(null);
warehouse.setUpdateTime(new Date());
// 其他字段保持不变

updateById(warehouse);  // 只更新userId和updateTime，其他字段不动
```

| 比较项 | 普通更新 | UpdateEntity |
|--------|---------|--------------|
| **创建方式** | `new Entity()` | `UpdateEntity.of(Entity.class, id)` |
| **字段更新** | 所有字段 | 仅设置过的字段 |
| **null处理** | 未设置的字段默认为null | null值会被更新到数据库 |
| **使用场景** | 全量更新 | 部分字段更新 |
| **安全性** | 容易误删数据 | 更安全，不会意外修改 |

### 8.6 最佳实践

```java
// ✅ 推荐：在Service层使用UpdateEntity进行部分更新
@Service
@Transactional
public class BizWarehouseServiceImpl extends ServiceImpl<BizWarehouseMapper, BizWarehouse> {
    
    /**
     * 只更新仓库的用户绑定信息
     */
    public int bindUserToWarehouse(Long warehouseId, Long userId) {
        BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, warehouseId);
        warehouse.setUserId(userId);
        warehouse.setUpdateTime(new Date());
        return updateById(warehouse) ? 1 : 0;
    }
    
    /**
     * 清空仓库的用户绑定
     */
    public int unbindUserFromWarehouse(Long warehouseId) {
        BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, warehouseId);
        warehouse.setUserId(null);  // 允许将字段更新为null
        warehouse.setUpdateTime(new Date());
        return updateById(warehouse) ? 1 : 0;
    }
    
    /**
     * 只更新仓库状态
     */
    public int updateWarehouseStatus(Long warehouseId, String status) {
        BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, warehouseId);
        warehouse.setStatus(status);
        warehouse.setUpdateTime(new Date());
        return updateById(warehouse) ? 1 : 0;
    }
}
```

## 9. 推荐使用场景总结

| 操作类型 | 推荐方案 | 适用场景 | 优势 |
|---------|----------|----------|------|
| **简单CRUD** | `Db` 类静态方法 | 单表增删改查 | 代码简洁，性能好 |
| **链式操作** | `DbChain` | 需要链式调用的场景 | API统一，可读性好 |
| **动态查询** | `QueryWrapper` | 条件动态变化 | 灵活构建查询条件 |
| **复杂SQL** | 直接写SQL + `Db.selectListBySql` | 连表、子查询、窗口函数 | SQL可控，性能最优 |
| **简单分页** | `Db.paginate` 或 `DbChain.page` | 单表分页 | 自动处理分页逻辑 |
| **复杂分页** | 手动分页 | 连表分页、复杂统计 | 灵活控制查询逻辑 |
| **部分字段更新** | `UpdateEntity` | 只更新某些字段，包括设置为null | 精确控制，避免数据意外修改 |
| **批量操作** | `Db` 批量方法 | 大量数据处理 | 性能优化 |
| **事务处理** | `Db.tx` / `Db.txWithResult` | 需要事务保证 | 简化事务管理 |

## 10. 最佳实践建议

### 9.1 性能优化

```java
// ✅ 推荐：使用批量操作
List<Row> users = prepareUserData();
Db.insertBatchWithFirstRowColumns("user", users);

// ❌ 避免：循环单条操作
for (Row user : users) {
    Db.insert("user", user);  // 性能差
}
```

### 9.2 SQL安全

```java
// ✅ 推荐：使用参数化查询
String keyword = userInput;
List<Row> users = Db.selectListBySql(
    "SELECT * FROM user WHERE name LIKE ?", 
    "%" + keyword + "%"
);

// ❌ 避免：SQL拼接
String sql = "SELECT * FROM user WHERE name LIKE '%" + keyword + "%'";  // SQL注入风险
```

### 9.3 事务管理

```java
// ✅ 推荐：保持事务范围小
boolean success = Db.tx(() -> {
    // 只包含需要事务保证的核心操作
    Db.insert("order", order);
    Db.update("inventory", inventory);
    return true;
});

// 非事务操作放在事务外
sendNotification(order);  // 发送通知不需要事务
```

### 9.4 分页优化

```java
// ✅ 推荐：已知总数时避免重复查询
Page<Row> page = new Page<>(pageNum, pageSize, knownTotal);
Page<Row> result = DbChain.table("user")
    .where("status = ?", 1)
    .page(page);

// ✅ 推荐：复杂查询使用手动分页
long total = Db.selectCount(countSql, params);
List<Row> data = Db.selectListBySql(dataSql + " LIMIT ? OFFSET ?", 
    ArrayUtil.concat(params, new Object[]{pageSize, offset}));
```

### 9.5 错误处理

```java
// ✅ 推荐：适当的异常处理
try {
    boolean success = Db.tx(() -> {
        // 业务操作
        return processBusinessLogic();
    });
    
    if (!success) {
        log.warn("业务处理失败");
        return Result.fail("操作失败");
    }
    
} catch (Exception e) {
    log.error("数据库操作异常", e);
    return Result.error("系统异常");
}
```

### 9.6 代码组织

```java
// ✅ 推荐：将复杂SQL抽取为常量或方法
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

### 9.7 部分字段更新最佳实践

```java
// ✅ 推荐：使用 UpdateEntity 进行部分字段更新
@Transactional
public int updateUserStatus(Long userId, String status) {
    SysUser user = UpdateEntity.of(SysUser.class, userId);
    user.setStatus(status);
    user.setUpdateTime(new Date());
    return updateById(user) ? 1 : 0;
}

// ✅ 推荐：需要设置字段为null时必须使用 UpdateEntity
@Transactional
public int clearUserPhone(Long userId) {
    SysUser user = UpdateEntity.of(SysUser.class, userId);
    user.setPhone(null);  // 这样才能真正将字段更新为null
    user.setUpdateTime(new Date());
    return updateById(user) ? 1 : 0;
}

// ❌ 避免：使用普通对象进行部分更新
@Transactional
public int updateUserStatusWrong(Long userId, String status) {
    SysUser user = new SysUser();
    user.setId(userId);
    user.setStatus(status);
    user.setUpdateTime(new Date());
    // 其他字段会被设置为null，导致数据被意外清空
    return updateById(user) ? 1 : 0;
}
```
