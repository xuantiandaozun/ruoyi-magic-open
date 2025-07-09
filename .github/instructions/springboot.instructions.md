# RuoYi-Magic 项目代码生成规范

## 项目基础架构
- 基于 RuoYi 框架的企业级应用快速开发平台
- Spring Boot 3.2.2 + Java 17
- MyBatis-Flex 1.10.9 作为 ORM 框架
- Sa-Token 1.42.0 进行权限认证
<<<<<<< HEAD
<<<<<<< HEAD
- MySQL 8.x + HikariCP 连接池
=======
- MySQL 8.x + Druid 连接池
>>>>>>> 2fe1861 (first commit)
=======
- MySQL 8.x + HikariCP 连接池
>>>>>>> af4fee7 (refactor(系统优化): 重构数据库连接池为HikariCP并优化启动性能)
- Redis 缓存和会话管理
- Spring AI 1.0.0-SNAPSHOT + DeepSeek-Chat 模型

## 包结构规范
- 系统管理: com.ruoyi.project.system
- 代码生成: com.ruoyi.project.gen  
- 监控模块: com.ruoyi.project.monitor
- 通用模块: com.ruoyi.common
- 核心配置: com.ruoyi.framework

## 命名约定
- 实体类以 Sys 为前缀（如 SysUser, SysRole, SysMenu, SysDept）
- Service 接口使用 I 前缀，实现类使用 Impl 后缀
- Mapper 接口以 Mapper 结尾
- Controller 以 Controller 结尾
- 配置类以 Config 结尾
- 工具类以 Utils 结尾

## MyBatis-Flex 使用规范

**重要原则：禁止编写 XML 文件，所有 SQL 直接在 Java 代码中实现**

### MyBatis-Flex 核心包导入

**重要：MyBatis-Flex 是较新的框架，必须导入正确的包**

```java
// 核心查询包
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.QueryColumn;

// 分页包
import com.mybatisflex.core.paginate.Page;

// Db + Row 工具包
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;

// Service 基础包
import com.mybatisflex.spring.service.IService;
import com.mybatisflex.spring.service.impl.ServiceImpl;

// Mapper 基础包
import com.mybatisflex.core.BaseMapper;

// 实体注解包
import com.mybatisflex.annotation.Table;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.KeyType;

// 更新工具包
import com.mybatisflex.core.update.UpdateEntity;

// 多数据源注解包
import com.mybatisflex.annotation.UseDataSource;
```

### 实体类规范
- 继承 BaseEntity 基础实体类
- 使用 @Table 注解指定表名
- 使用 @Id 注解标注主键字段，主键类型使用 KeyType.Auto
- 使用 @Column 注解映射特殊字段名
- 时间字段使用 Date 类型而不是 LocalDateTime
- 逻辑删除字段使用 @Column(isLogicDelete = true)
- 乐观锁字段使用 @Column(version = true)

### 实体类规范
- 继承 BaseEntity 基础实体类
- 使用 @Table 注解指定表名
- 使用 @Id 注解标注主键字段，主键类型使用 KeyType.Auto
- 使用 @Column 注解映射特殊字段名
- 时间字段使用 Date 类型而不是 LocalDateTime
- 逻辑删除字段使用 @Column(isLogicDelete = true)
- 乐观锁字段使用 @Column(version = true)

### Mapper 接口规范
- 继承 BaseMapper<T> 接口
- 不要编写任何 XML 文件
- 不要使用 @Select、@Insert、@Update、@Delete 注解
- 保持 Mapper 接口绝对简洁，只继承 BaseMapper 即可
- 所有复杂查询在 Service 层使用 Db + Row 工具实现

### QueryWrapper 使用规范

**重要：不要使用静态字段方式（如 SYS_USER.USER_NAME），使用 QueryColumn 和 from() 方法**

```java
// 正确的 QueryWrapper 写法
QueryWrapper query = QueryWrapper.create()
    .from("sys_user")
    .where(new QueryColumn("user_name").like(userName, StrUtil.isNotEmpty(userName)))
    .and(new QueryColumn("status").eq(status, ObjectUtil.isNotNull(status)))
    .and(new QueryColumn("create_time").between(startTime, endTime, startTime != null && endTime != null))
    .orderBy(new QueryColumn("create_time").desc());

// 复杂条件查询模板
QueryWrapper query = QueryWrapper.create()
    .from("表名")
    .where(new QueryColumn("字段名").条件(值, 条件判断))
    .and(new QueryColumn("字段名").条件(值, 条件判断))
    .orderBy(new QueryColumn("字段名").desc());
```

### Service 层规范 - 单表操作使用 IService 方法

继承 IService<T> 接口和 ServiceImpl<M, T> 实现类，所有单表操作使用 IService 提供的方法：

```java
import com.mybatisflex.spring.service.IService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.update.UpdateEntity;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    
    // 新增用户
    public boolean saveUser(SysUser user) {
        user.setCreateTime(new Date());
        return this.save(user);
    }
    
    // 批量新增
    public boolean saveUsers(List<SysUser> users) {
        return this.saveBatch(users);
    }
    
    // 根据ID查询
    public SysUser getUserById(Long id) {
        return this.getById(id);
    }
    
    // 根据用户名查询
    public SysUser getUserByUserName(String userName) {
        QueryWrapper query = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").eq(userName, StrUtil.isNotEmpty(userName)));
        return this.getOne(query);
    }
    
    // 条件查询列表
    public List<SysUser> getUserList(String userName, Integer status) {
        QueryWrapper query = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").like(userName, StrUtil.isNotEmpty(userName)))
            .and(new QueryColumn("status").eq(status, ObjectUtil.isNotNull(status)))
            .orderBy(new QueryColumn("create_time").desc());
        return this.list(query);
    }
    
    // 分页查询
    public Page<SysUser> getUserPage(int pageNum, int pageSize, String userName) {
        QueryWrapper query = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").like(userName, StrUtil.isNotEmpty(userName)))
            .orderBy(new QueryColumn("create_time").desc());
        return this.page(new Page<>(pageNum, pageSize), query);
    }
    
    // 复杂条件查询示例
    public Page<SysUser> getUserPageWithCondition(int pageNum, int pageSize, SysUser user) {
        QueryWrapper query = QueryWrapper.create()
            .from("sys_user")
            .where(new QueryColumn("user_name").like(user.getUserName(), StrUtil.isNotEmpty(user.getUserName())))
            .and(new QueryColumn("status").eq(user.getStatus(), ObjectUtil.isNotNull(user.getStatus())))
            .and(new QueryColumn("phonenumber").like(user.getPhonenumber(), StrUtil.isNotEmpty(user.getPhonenumber())))
            .and(new QueryColumn("create_time").between(user.getParams().get("beginTime"), user.getParams().get("endTime"), 
                        user.getParams().get("beginTime") != null && user.getParams().get("endTime") != null))
            .and(new QueryColumn("dept_id").eq(user.getDeptId(), ObjectUtil.isNotNull(user.getDeptId())))
            .orderBy(new QueryColumn("create_time").desc());
        return this.page(new Page<>(pageNum, pageSize), query);
    }
    
    // 更新用户
    public boolean updateUser(SysUser user) {
        user.setUpdateTime(new Date());
        return this.updateById(user);
    }
    
    // 部分字段更新
    public boolean updateUserStatus(Long id, Integer status) {
        SysUser user = UpdateEntity.of(SysUser.class);
        user.setId(id);
        user.setStatus(status);
        user.setUpdateTime(new Date());
        return this.updateById(user);
    }
    
    // 删除用户（逻辑删除）
    public boolean deleteUser(Long id) {
        return this.removeById(id);
    }
    
    // 批量删除
    public boolean deleteUsers(List<Long> ids) {
        return this.removeByIds(ids);
    }
}
```

### 复杂查询规范
- 使用 QueryWrapper 构建查询条件
- 复杂 SQL 使用 Db + Row 工具类
- 原生 SQL 使用 Db.selectListBySql() 方法
- 分页复杂查询使用 Db.paginate() 方法
- Row 可以直接转换为实体类：row.toEntity(Account.class)

## Sa-Token 权限控制规范

### 认证相关
- 登录验证使用 StpUtil.login(userId)
- 注销使用 StpUtil.logout()
- 权限校验使用 @SaCheckPermission 注解
- 角色校验使用 @SaCheckRole 注解

### 会话管理
- Token 获取使用 StpUtil.getTokenValue()
- 用户信息获取使用 StpUtil.getLoginId()
- 会话超时配置在 application.yml 中设置

## 多数据源使用规范

### 数据源配置
- 主数据源配置在 application.yml 的 spring.datasource
- 多数据源配置在 flex.datasource
- 使用 @DataSource("数据源名称") 注解切换数据源
- 数据源动态管理使用 DynamicDataSourceManager

### 切换规则
- 方法级别使用 @DataSource 注解
- 支持在 Service 和 Mapper 层进行数据源切换
- 数据源名称要与配置文件中的 key 保持一致

## Controller 层规范

### 基础规范
- 继承 BaseController 基础控制器
- 使用 @RestController 注解
- 统一返回 AjaxResult 包装类
- 请求路径使用 @RequestMapping 族注解

### 权限控制
- 接口权限使用 @PreAuthorize 注解
- 数据权限使用 @DataScope 注解
- 操作日志使用 @Log 注解记录

### 参数校验
- 使用 @Validated 注解进行参数校验
- 分组校验使用 groups 参数
- 自定义校验规则继承 ConstraintValidator

## 代码生成规范

### 生成器配置
- 支持多数据源代码生成
- 表前缀统一使用 sys_
- 生成模板支持 CRUD、树形表、主子表
- 生成的代码要符合项目规范

### AI 智能建表
- 使用 DeepSeek 模型进行表结构设计
- 表名使用下划线命名法
- 字段名规范：id、create_time、update_time、create_by、update_by
- 必须包含逻辑删除字段 del_flag

## 缓存使用规范

### Redis 缓存
- 缓存 key 使用项目前缀：ruoyi:
- 用户会话缓存：ruoyi:login:token:
- 系统配置缓存：ruoyi:sys:config:
- 字典数据缓存：ruoyi:sys:dict:
- 使用 @Cacheable、@CacheEvict、@CachePut 注解

### 缓存策略
- 热点数据设置合理的过期时间
- 大数据量使用分页缓存
- 缓存更新使用双写一致性策略

## 异常处理规范

### 全局异常处理
- 使用 GlobalExceptionHandler 统一处理异常
- 业务异常继承 ServiceException
- 参数校验异常返回具体错误信息
- 系统异常记录详细日志

### 自定义异常
- 业务异常使用 ServiceException
- 权限异常使用 NotPermissionException  
- 数据源异常使用 DataSourceException

## 日志规范

### 操作日志
- 使用 @Log 注解记录业务操作
- 日志标题描述具体操作内容
- 操作类型：新增、修改、删除、查询、导入、导出
- 记录操作人、操作时间、操作内容

### 系统日志
- 使用 SLF4J + Logback 记录系统日志
- 错误日志记录完整堆栈信息
- 关键业务节点记录 INFO 级别日志
- 调试信息使用 DEBUG 级别

## 前端交互规范

### API 响应格式
- 成功响应：AjaxResult.success()
- 失败响应：AjaxResult.error()
- 分页响应：TableDataInfo 包装
- 列表响应统一使用 List<T> 格式

### 参数传递
- 查询参数使用 @RequestParam
- 路径参数使用 @PathVariable
- 请求体使用 @RequestBody
- 文件上传使用 MultipartFile

## 数据权限规范

### 权限范围
- 全部数据权限：1
- 自定义数据权限：2  
- 部门数据权限：3
- 部门及以下数据权限：4
- 仅本人数据权限：5

### 实现方式
- 使用 @DataScope 注解标注需要数据权限的方法
- 在 SQL 中使用 ${params.dataScope} 插入权限条件
- 权限过滤在 DataScopeAspect 切面中实现

## 国际化支持

### 配置规范
- 国际化资源文件放在 resources/i18n 目录
- 使用 MessageUtils.message(key) 获取国际化文本
- 前端错误信息支持多语言显示

## 定时任务规范

### Quartz 任务  
- 任务类继承 QuartzJobBean
- 使用 @Component 注解注册为 Bean
- 任务执行状态要记录日志
- 异常任务要有重试机制

## 监控规范

### 系统监控
- 使用 Actuator 提供健康检查端点
<<<<<<< HEAD
<<<<<<< HEAD
=======
- Druid 监控页面：/druid/index.html
>>>>>>> 2fe1861 (first commit)
=======
>>>>>>> af4fee7 (refactor(系统优化): 重构数据库连接池为HikariCP并优化启动性能)
- 系统信息监控：CPU、内存、磁盘使用率
- 在线用户监控和管理

## 代码质量规范

### 代码风格
- 使用 4 空格缩进
- 方法长度不超过 50 行
- 类长度不超过 500 行
- 复杂方法要添加注释说明

### 测试规范
- 重要业务逻辑要编写单元测试
- 使用 JUnit 5 + Mockito 框架
- 测试覆盖率要求达到 70% 以上

## 部署配置

### 环境配置
- 开发环境：application-dev.yml
- 测试环境：application-test.yml  
- 生产环境：application-prod.yml
- 使用 @Profile 注解区分环境

### Docker 部署
- 项目支持 Docker 容器化部署
- 提供 Dockerfile 和 docker-compose.yml
- 环境变量外部化配置

## AI 功能集成

### Spring AI 使用
- 使用 DeepSeek-Chat 模型
- AI 建表功能在代码生成模块中实现
- 模型配置在 application.yml 中设置
- 提供 AI 辅助代码生成功能

### 智能建表
- 通过自然语言描述生成表结构
- 支持字段类型推断和约束设置
- 生成的表结构符合项目规范
- 自动生成对应的实体类和 CRUD 代码