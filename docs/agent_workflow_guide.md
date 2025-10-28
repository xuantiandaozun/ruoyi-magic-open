# Agent工作流功能使用指南

## 概述

本系统基于LangChain4j框架实现了智能Agent工作流功能，支持多种工作流模式和工具调用。

## 功能特性

### 1. 工作流类型

- **传统顺序工作流** (`sequential`): 传统的步骤顺序执行模式
- **LangChain4j Agent工作流** (`langchain4j_agent`): 基于LangChain4j的智能Agent模式
- **条件工作流** (`conditional`): 基于条件的分支执行模式
- **循环工作流** (`loop`): 循环执行的工作流模式

### 2. 支持的工具

- **GitHub趋势工具** (`github_trending`): 查询GitHub热门项目
- **数据库查询工具** (`database_query`): 执行数据库查询
- **文件操作工具** (`file_operation`): 文件读写操作
- **网络请求工具** (`http_request`): HTTP请求调用

## 核心组件

### 1. LangChain4j工具注册表 (`LangChain4jToolRegistry`)

负责管理和注册所有可用的工具：

```java
@Component
public class LangChain4jToolRegistry {
    // 注册工具
    public void registerTool(String name, Object tool);
    
    // 获取工具规范
    public ToolSpecification getToolSpecification(String toolName);
    
    // 执行工具
    public String executeTool(String toolName, String arguments);
}
```

### 2. LangChain4j Agent服务 (`LangChain4jAgentService`)

提供Agent创建和工作流执行功能：

```java
@Service
public class LangChain4jAgentService {
    // 创建简单Agent
    public Map<String, Object> createSimpleAgent(ChatModel chatModel, List<String> toolNames, Map<String, Object> input);
    
    // 创建顺序工作流
    public Map<String, Object> executeSequentialWorkflow(List<UntypedAgent> agents, AgenticScope scope);
    
    // 创建循环工作流
    public Map<String, Object> executeLoopWorkflow(UntypedAgent agent, AgenticScope scope, int maxIterations);
}
```

### 3. Agent工作流执行服务 (`AgenticWorkflowExecutionServiceImpl`)

核心的工作流执行引擎，支持多种执行模式：

```java
@Service
public class AgenticWorkflowExecutionServiceImpl {
    // 执行工作流
    public Map<String, Object> executeWorkflow(Long workflowId, Map<String, Object> inputData);
    
    // 使用LangChain4j Agent执行
    private Map<String, Object> executeWithLangChain4jAgent(AiWorkflow workflow, List<AiWorkflowStep> steps, Map<String, Object> inputData);
    
    // 使用传统方式执行
    private Map<String, Object> executeWithTraditionalAgent(List<AiWorkflowStep> steps, Map<String, Object> inputData);
}
```

## 数据库设计

### 工作流表 (`ai_workflow`)

```sql
CREATE TABLE `ai_workflow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `workflow_name` varchar(100) NOT NULL COMMENT '工作流名称',
  `workflow_type` varchar(50) DEFAULT 'sequential' COMMENT '工作流类型',
  `enabled` char(1) DEFAULT '1' COMMENT '启用状态',
  -- 其他字段...
);
```

### 工作流步骤表 (`ai_workflow_step`)

```sql
CREATE TABLE `ai_workflow_step` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `workflow_id` bigint(20) NOT NULL COMMENT '工作流ID',
  `step_name` varchar(100) NOT NULL COMMENT '步骤名称',
  `step_order` int(11) NOT NULL COMMENT '步骤顺序',
  `model_config_id` bigint(20) DEFAULT NULL COMMENT 'AI模型配置ID',
  `system_prompt` text COMMENT '系统提示词',
  `input_variable` varchar(100) DEFAULT NULL COMMENT '输入变量名',
  `output_variable` varchar(100) DEFAULT NULL COMMENT '输出变量名',
  `tool_type` varchar(50) DEFAULT NULL COMMENT '工具类型',
  `tool_enabled` char(1) DEFAULT 'N' COMMENT '是否启用工具',
  -- 其他字段...
);
```

## API接口

### 1. 工作流执行接口

```http
POST /ai/workflow/execute
Content-Type: application/json

{
  "workflowId": 1,
  "inputData": {
    "userInput": "请查询Java语言的GitHub热门项目"
  }
}
```

### 2. 测试接口

#### 测试LangChain4j Agent工作流

```http
POST /ai/workflow/test/langchain4j
Content-Type: application/json

{
  "workflowId": 1,
  "userInput": "请分析Java语言的GitHub趋势",
  "systemPrompt": "你是一个GitHub趋势分析专家"
}
```

#### 测试GitHub趋势工具

```http
POST /ai/workflow/test/github-trending
Content-Type: application/json

{
  "workflowId": 1,
  "language": "java",
  "period": "daily"
}
```

## 使用示例

### 1. 创建简单的GitHub趋势查询工作流

1. 创建工作流：
```sql
INSERT INTO `ai_workflow` (`workflow_name`, `workflow_type`, `enabled`) 
VALUES ('GitHub趋势查询', 'langchain4j_agent', '1');
```

2. 创建工作流步骤：
```sql
INSERT INTO `ai_workflow_step` (
  `workflow_id`, `step_name`, `step_order`, `model_config_id`, 
  `system_prompt`, `tool_type`, `tool_enabled`
) VALUES (
  1, 'GitHub趋势查询', 1, 1,
  '你是一个GitHub趋势分析专家，请查询并分析热门项目。',
  'github_trending', 'Y'
);
```

3. 执行工作流：
```java
Map<String, Object> inputData = new HashMap<>();
inputData.put("userInput", "请查询Java语言的热门项目");

Map<String, Object> result = workflowExecutionService.executeWorkflow(1L, inputData);
```

### 2. 创建多步骤顺序工作流

1. 创建包含多个步骤的工作流
2. 每个步骤可以配置不同的AI模型和工具
3. 通过`input_variable`和`output_variable`连接步骤间的数据流

## 配置说明

### 1. AI模型配置

在`ai_model_config`表中配置AI模型：

```sql
INSERT INTO `ai_model_config` (
  `config_name`, `provider`, `model`, `api_key`, `endpoint`, `enabled`
) VALUES (
  'OpenAI GPT-3.5', 'openai', 'gpt-3.5-turbo', 'your-api-key', 'https://api.openai.com/v1', 'Y'
);
```

### 2. 智能工具配置

**重要说明**: 从v2.0版本开始，工具参数由AI自动决定，无需手动配置。

AI会根据以下信息自动确定工具参数：
- 用户的提示词内容
- 上下文信息和变量
- 工作流的执行环境

**关键注意事项**: 在用户提示词中，必须**显式指定工具名称**，AI才能正确调用相应的工具。

#### 正确的提示词写法示例：
✅ **正确写法**（显式指定工具名称）：
```
请使用 github_trending 查询今天上榜的热门仓库信息，选择2-3个最有意思的项目，用生动有趣的方式写一篇技术分析文章。重点突出项目的实用价值、创新点和学习意义。
```

❌ **错误写法**（没有显式指定工具名称）：
```
请分析今天的GitHub热门项目，选择2-3个最有意思的项目，用生动有趣的方式写一篇技术分析文章。重点突出项目的实用价值、创新点和学习意义。
```

支持的智能工具类型：
- `github_trending`: GitHub趋势查询（AI自动确定语言、时间范围等）
- `database_query`: 数据库查询（AI自动生成SQL语句）
- `file_operation`: 文件操作（AI自动处理路径和操作类型）

**旧版本兼容性**: 如果您使用的是v1.x版本，仍需要手动配置工具参数：
```json
{
  "language": "java",
  "period": "daily", 
  "limit": 10
}
```

## 扩展开发

### 1. 添加新工具

1. 实现工具类：
```java
@Component
public class CustomTool {
    @Tool("执行自定义操作")
    public String executeCustomAction(@P("参数") String parameter) {
        // 工具逻辑
        return "执行结果";
    }
}
```

2. 注册工具：
```java
@PostConstruct
public void init() {
    toolRegistry.registerTool("custom_tool", customTool);
}
```

### 2. 添加新的工作流类型

1. 在`LangChain4jAgentService`中添加新的工作流创建方法
2. 在`AgenticWorkflowExecutionServiceImpl`中添加对应的执行逻辑
3. 更新数据字典配置

## 注意事项

1. **API密钥安全**: 确保AI模型的API密钥安全存储
2. **工具权限**: 谨慎配置工具权限，避免安全风险
3. **性能优化**: 对于复杂工作流，注意性能优化和资源管理
4. **错误处理**: 完善的错误处理和日志记录
5. **测试验证**: 充分测试各种工作流场景

## 故障排除

### 常见问题

1. **工具调用失败**: 检查工具注册和AI模型配置（v2.0+无需检查参数配置）
2. **AI模型连接失败**: 验证API密钥和端点配置
3. **工作流执行超时**: 调整超时设置和优化提示词
4. **数据流传递错误**: 检查变量名配置和数据格式

### 日志查看

查看应用日志获取详细的执行信息：

```bash
tail -f logs/ruoyi-magic.log | grep -E "(Agent|Workflow|Tool)"
```