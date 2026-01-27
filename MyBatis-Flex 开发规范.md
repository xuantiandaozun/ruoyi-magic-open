# MyBatis-Flex 开发规范

本文档旨在为喜欢**直接使用字符串列名、拒绝繁琐静态生成类**的开发者提供一套业务层开发规范。文档涵盖了从业务层接口设计到底层 Mapper 操作的全方位指南。

## 目录

- [1. 业务层标准模式 (IService)](#1-业务层标准模式-iservice)
  - [1.1 接口定义与实现](#11-接口定义与实现)
  - [1.2 IService 常用方法](#12-iservice-常用方法)
- [2. BaseMapper 核心增删改](#2-basemapper-核心增删改)
  - [2.1 新增数据 (Insert)](#21-新增数据-insert)
  - [2.2 删除数据 (Delete)](#22-删除数据-delete)
  - [2.3 更新数据 (Update)](#23-更新数据-update)
  - [2.4 高级更新 (UpdateWrapper & UpdateChain)](#24-高级更新-updatewrapper--updatechain)
- [3. QueryWrapper 核心详解](#3-querywrapper-核心详解)
  - [3.1 基础构建与投影](#31-基础构建与投影)
  - [3.2 动态条件与模糊查询](#32-动态条件与模糊查询)
  - [3.3 IN 查询全解](#33-in-查询全解)
  - [3.4 嵌套逻辑 (OR/AND)](#34-嵌套逻辑-orand)
- [4. 连表查询 (Join)](#4-连表查询-join)
- [5. 自动映射 (Auto Mapping)](#5-自动映射-auto-mapping)
  - [5.1 基础与 DTO 映射](#51-基础与-dto-映射)
  - [5.2 嵌套对象与集合映射](#52-嵌套对象与集合映射)
- [6. 分页查询](#6-分页查询)
  - [6.1 简单分页](#61-简单分页)
  - [6.2 分页陷阱与优化](#62-分页陷阱与优化)
- [7. 部分字段更新 - UpdateEntity](#7-部分字段更新---updateentity)
- [8. 最佳实践总结](#8-最佳实践总结)

---

## 1. 业务层标准模式 (IService)

在企业级开发中，**推荐继承 `IService` 接口**而非直接在 Controller 中调用 `Db` 类或 Mapper。它提供了标准的 CRUD 能力，简化了重复代码。

### 1.1 接口定义与实现

**接口定义**：

```java
public interface IAccountService extends IService<Account> {
    // 自定义业务方法
    List<Account> findVipAccounts();
}
```

**实现类**：

```java
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements IAccountService {
    @Override
    public List<Account> findVipAccounts() {
        // 使用字符串构建条件
        return list(QueryWrapper.create().eq("type", "VIP").ge("age", 18));
    }
}
```

### 1.2 IService 常用方法

`IService` 接口屏蔽了底层细节，提供了极简的 API。

```java
@Autowired
private IAccountService service;

// --- 1. 保存 (Save) ---
service.save(entity);               // 插入 (忽略 null)
service.saveOrUpdate(entity);       // ID 存在则更新，否则插入
service.saveBatch(list, 500);       // 批量插入，每批 500 条

// --- 2. 删除 (Remove) ---
service.removeById(1);              // 根据 ID 删除
service.removeByIds(ids);           // 批量删除
service.removeByMap(Map.of("status", 0)); // 根据 Map 删除
service.remove(QueryWrapper.create().gt("age", 100)); // 根据条件删除

// --- 3. 更新 (Update) ---
service.updateById(entity);         // 根据 ID 更新 (忽略 null)
// 根据条件更新：UPDATE table SET status=1 WHERE name LIKE '%Test%'
Account updateEntity = new Account();
updateEntity.setStatus(1);
service.update(updateEntity, QueryWrapper.create().like("name", "Test"));

// --- 4. 查询 (Query) ---
Account user = service.getById(1);  // 查单条
// 查列表：WHERE age >= 18 ORDER BY id DESC
List<Account> list = service.list(
    QueryWrapper.create().ge("age", 18).orderBy("id", false)
);
long count = service.count(QueryWrapper.create().eq("status", 1)); // 查数量
boolean exists = service.exists(QueryWrapper.create().eq("mobile", "13800000000")); // 判断存在
```

---

## 2. BaseMapper 核心增删改

`BaseMapper` 是底层核心接口，当 `IService` 无法满足特殊需求（如 SQL 函数更新）时，可直接使用 Mapper。

### 2.1 新增数据 (Insert)

```java
Account account = new Account();
account.setUserName("Flex");

// 1. 普通插入 (不忽略 null，使用数据库默认值需小心)
mapper.insert(account);

// 2. 选择性插入 (推荐，自动忽略 null，让数据库默认值生效)
mapper.insertSelective(account);

// 3. 批量插入 (高性能，基于第一条数据构建 SQL)
mapper.insertBatch(accountList);
```

**高级用法：插入 SQL 函数**

```java
// 场景：插入数据时，birthday 字段使用数据库函数 now()
Account newAccount = UpdateWrapper.of(new Account())
    .set("user_name", "Michael")
    .setRaw("birthday", "now()") // setRaw 允许传入 SQL 片段
    .toEntity();

mapper.insert(newAccount);
// SQL: INSERT INTO tb_account(user_name, birthday) VALUES (?, now())
```

### 2.2 删除数据 (Delete)

```java
// 1. 根据 ID 删除
mapper.deleteById(100);
mapper.deleteBatchByIds(Arrays.asList(100, 101));

// 2. 根据 QueryWrapper 删除 (最灵活)
QueryWrapper query = QueryWrapper.create().ge("age", 100);
mapper.deleteByQuery(query);
```

### 2.3 更新数据 (Update)

```java
// 1. 根据 ID 更新 (忽略 null)
mapper.update(account);

// 2. 根据 ID 更新 (强制更新 null)
mapper.update(account, false);

// 3. 根据 QueryWrapper 更新
Account updateData = new Account();
updateData.setStatus(1);
// UPDATE table SET status = 1 WHERE age > 18
mapper.updateByQuery(updateData, QueryWrapper.create().gt("age", 18));
```

### 2.4 高级更新 (UpdateWrapper & UpdateChain)

当需要执行 `age = age + 1` 或链式调用时，使用此功能。

**场景 1：UpdateChain 链式更新 (推荐)**

```java
UpdateChain.of(Account.class)
    .set("user_name", "张三")        // 设置参数 (预编译，安全)
    .setRaw("money", "money + 100")  // 设置 SQL 片段 (余额+100)
    .where("id").eq(1)
    .update();
```

**场景 2：UpdateWrapper 手动构建**

```java
Account account = UpdateEntity.of(Account.class, 100);
UpdateWrapper wrapper = UpdateWrapper.of(account);
wrapper.setRaw("age", "age + 1");
mapper.update(account);
```

> **⚠️ 安全警告：set() vs setRaw()**
>
> - **`set("name", "张三")`**：生成 SQL `name = ?`，参数安全。
> - **`setRaw("name", "张三")`**：生成 SQL `name = 张三`，直接拼接，**有 SQL 注入风险**！仅用于 `now()`、`age + 1` 等确定性的 SQL 片段。

---

## 3. QueryWrapper 核心详解

`QueryWrapper` 是构建 SQL 的核心。本章节**完全采用字符串列名风格**，代码简洁且符合 SQL 直觉。

### 3.1 基础构建与投影

```java
// 1. 指定列查询
QueryWrapper query = QueryWrapper.create()
    .select("id", "user_name", "dept_id") // 支持 AS: "dept_id AS deptId"
    .from("tb_account")
    .where("status = ?", 1);

// 2. 排序与去重
query.orderBy("create_time", false); // DESC
query.groupBy("dept_id");
```

### 3.2 动态条件与模糊查询

所有条件方法支持第三个 `boolean` 参数，避免大量 `if-else`。

```java
String keyword = ""; // 假设前端传参为空
Integer minAge = 18;

QueryWrapper query = QueryWrapper.create()
    .from("tb_account")
    .eq("status", 1)
    // --- 动态 SQL ---
    // 只有当 keyword 有值时，才拼接 LIKE
    .like("user_name", keyword, StringUtil::hasText) 
    // 只有当 minAge != null 时，才拼接 GE
    .ge("age", minAge, minAge != null); 
```

### 3.3 IN 查询全解

```java
// 1. 数组 / 可变参数
query.in("status", 1, 2, 3);

// 2. 集合 (最常用)
List<Integer> ids = Arrays.asList(10, 20);
query.in("dept_id", ids);

// 3. 子查询 (嵌套 QueryWrapper)
// SQL: ... dept_id IN (SELECT id FROM dept WHERE type = 'IT')
QueryWrapper subQuery = QueryWrapper.create().select("id").from("dept").eq("type", "IT");
query.in("dept_id", subQuery);
```

### 3.4 嵌套逻辑 (OR/AND)

默认链式调用是 `AND`。如果需要 `OR` 或括号，请使用 Lambda。

```java
// SQL: WHERE status = 1 AND (age > 60 OR age < 18)
query.eq("status", 1)
     .and(q -> q.gt("age", 60).or().lt("age", 18));
```

---

## 4. 连表查询 (Join)

使用字符串别名清晰管理多表关系。

```java
QueryWrapper query = QueryWrapper.create()
    .select("u.id", "u.user_name", "d.dept_name")
    .from("tb_account").as("u") // 主表别名 u
    // 左连接
    .leftJoin("tb_dept").as("d").on("u.dept_id = d.id")
    // 带条件的连接
    .innerJoin("tb_role").as("r").on("u.role_id = r.id", needRoleInfo)
    .where("u.age > ?", 18);
    
List<Account> list = mapper.selectListByQuery(query);
```

---

## 5. 自动映射 (Auto Mapping)

MyBatis-Flex 能够自动将结果集映射到 **DTO**、**VO** 或 **嵌套对象**，无需 ResultMap。

### 5.1 基础与 DTO 映射

如果查询结果列名与 DTO 属性名不一致，使用 `as` 进行匹配。

```java
// DTO 类：public class AccountDTO { private int maxAge; }

QueryWrapper query = QueryWrapper.create()
    .select(
        "dept_id", 
        "MAX(age) AS maxAge" // 别名匹配 DTO 属性
    )
    .from("tb_account")
    .groupBy("dept_id");

// 自动映射到 AccountDTO
List<AccountDTO> dtos = service.listAs(query, AccountDTO.class);
```

### 5.2 嵌套对象与集合映射

**场景**：`AccountVO` 包含 `List<Book> books` (一对多)。

```java
// 1. 编写查询，包含两张表所需字段
QueryWrapper query = QueryWrapper.create()
    .select(
        "a.id", "a.user_name",   // 账户信息
        "b.id", "b.title"        // 图书信息
    )
    .from("tb_account").as("a")
    .leftJoin("tb_book").as("b").on("a.id = b.account_id");

// 2. 执行映射
// Flex 会自动识别并去重，将 books 填充到集合中
List<AccountVO> vos = service.listAs(query, AccountVO.class);
```

> **注意**：如果多表存在重名字段（如 `id`），建议显式指定 `select("a.id", "b.id")`，以确保映射准确。

---

## 6. 分页查询

### 6.1 简单分页

```java
// 1. 构建分页对象 (页码, 每页大小)
Page<Account> page = new Page<>(1, 10);

// 2. 构建条件
QueryWrapper query = QueryWrapper.create().eq("status", 1).orderBy("id", false);

// 3. 执行分页
Page<Account> result = service.page(page, query);
```

### 6.2 分页陷阱与优化

**重要**：如果 `QueryWrapper` 包含 `leftJoin` 且 `where` 条件使用了副表字段，Flex 默认的 Count 优化（移除 Join）会导致报错。

```java
QueryWrapper query = QueryWrapper.create()
    .from("user").as("u")
    .leftJoin("dept").as("d").on(...)
    .where("d.region = 'CN'"); // 条件依赖副表 d

Page<Account> page = new Page<>(1, 10);
// ⚠️ 必须关闭优化，否则 Count SQL 会去掉 dept 表，导致 'Unknown column d.region'
page.setOptimizeCountQuery(false); 

service.page(page, query);
```

---

## 7. 部分字段更新 - UpdateEntity

**核心场景**：只想更新某几个字段，且其中某些字段需要被**置为 NULL**。

### 7.1 基本用法

```java
// 1. 创建 UpdateEntity (必须指定 ID)
Account account = UpdateEntity.of(Account.class, 100);

// 2. 设置变更
account.setUserName("新名字");
account.setAge(null); // 显式更新为 NULL

// 3. 执行更新 (未调用 set 的字段保持不变)
service.updateById(account);
// SQL: UPDATE tb_account SET user_name=?, age=NULL WHERE id=100
```

### 7.2 常见应用场景

#### 解除用户绑定

```java
/**
 * 解除用户仓库绑定
 */
@Transactional
public int unbindUserFromWarehouse(Long warehouseId) {
    BizWarehouse warehouse = UpdateEntity.of(BizWarehouse.class, warehouseId);
    warehouse.setUserId(null);  // 将用户ID设置为null
    warehouse.setUpdateTime(new Date());
    return updateById(warehouse) ? 1 : 0;
}
```

#### 部分状态更新

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

### 7.3 UpdateEntity vs 普通更新的对比

| 比较项 | 普通更新 | UpdateEntity |
|--------|---------|--------------|
| **创建方式** | `new Entity()` | `UpdateEntity.of(Entity.class, id)` |
| **字段更新** | 所有字段 | 仅设置过的字段 |
| **null处理** | 未设置的字段默认为null | null值会被更新到数据库 |
| **使用场景** | 全量更新 | 部分字段更新 |
| **安全性** | 容易误删数据 | 更安全，不会意外修改 |

---

## 8. 最佳实践总结

| 场景 | 推荐方案 | 关键点 |
| :--- | :--- | :--- |
| **业务层开发** | 继承 `IService` | 避免直接用 `Db` 类，保持代码分层规范。 |
| **构建 SQL** | `QueryWrapper` (字符串风格) | 直接用 `"user_name"`，简单直观，无需生成静态类。 |
| **动态条件** | `eq("col", val, bool)` | 利用第三个参数控制条件，消灭 `if-else`。 |
| **复杂更新** | `UpdateChain` | **严禁**在 `setRaw` 中拼接用户输入，防止 SQL 注入。 |
| **复杂分页** | `page.setOptimizeCountQuery(false)` | 涉及 Join 筛选时必须关闭 Count 优化。 |
| **部分更新** | `UpdateEntity` | 不要用 `new Entity()` 更新，否则未赋值字段可能被误置空。 |
| **结果映射** | `listAs(DTO.class)` | 善用别名和自动映射，减少 DTO 转换工作量。 |

### Service 层 API 使用规范

在 Service 层有三种方式，**不要混淆**：

```java
// ✅ 方式1: 简单查询用 QueryWrapper + 继承的 list()/getOne()/page()
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    public List<User> selectUsers(String status) {
        QueryWrapper qw = QueryWrapper.create().eq("status", status);
        return list(qw);  // 继承的方法
    }
    
    public User selectById(Long id) {
        return getOne(QueryWrapper.create().eq("id", id));  // 继承的方法
    }
    
    public Page<User> selectPage(Integer pageNum, Integer pageSize, User user) {
        Page<User> page = new Page<>(pageNum, pageSize);
        QueryWrapper qw = QueryWrapper.create().eq("status", user.getStatus());
        return page(page, qw);  // 继承的方法
    }
}

// ✅ 方式2: 复杂多表查询用 QueryWrapper 的 from/join/select 构建
public List<User> selectUserWithDept(String status) {
    QueryWrapper qw = QueryWrapper.create()
        .select("u.*", "d.dept_name")
        .from("sys_user").as("u")
        .leftJoin("sys_dept").as("d").on("u.dept_id = d.id")
        .where("u.status = ?", status);
    return list(qw);
}

// ✅ 方式3: 最复杂的 SQL 直接写 SQL（用 Db 静态方法）
String sql = "SELECT u.*, d.dept_name FROM sys_user u LEFT JOIN sys_dept d ON u.dept_id = d.id WHERE u.status = ?";
List<Row> result = Db.selectListBySql(sql, "0");

// ❌ 错误: 不要混用两种 API
// Db.selectOneByQuery(User.class, qw);  // 错误！应该用 getOne(qw)
// 直接使用 Db 进行简单查询 // 应该用继承的方法
```

**关键原则**：
- Service 继承 `ServiceImpl<Mapper, Entity>` 后，用继承的方法：`list()`, `getOne()`, `page()`, `save()`, `updateById()`, `update()`, `removeById()`, `remove()`
- `QueryWrapper` 用来构建条件，配合继承的方法使用
- 仅当 SQL 特别复杂时才用 `Db.selectListBySql()` 等静态方法
- **不要在 Service 层混用** `Db.selectListByQuery()` 和 `list()` 等调用
