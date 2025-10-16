# LangChain4j Agentic AI 完整指南

## 重要说明

本模块（langchain4j-agentic）需要被视为实验性质，在未来版本中可能会发生变化。

## 1. Agentic系统概述

### 1.1 什么是Agentic系统

虽然对AI Agent没有普遍认可的定义，但有几种新兴模式展示了如何协调和组合多个AI服务的能力，以创建能够完成更复杂任务的AI应用。这些模式通常被称为"agentic systems"或"agentic AI"。

根据Anthropic研究人员最近发表的文章，这些Agentic系统架构可以分为两大类：
- **Workflows（工作流）**
- **Pure Agents（纯Agent）**

### 1.2 LangChain4j中的Agent

langchain4j-agentic模块提供了一组抽象和工具，帮助你构建工作流和纯agentic AI应用程序。

---

## 2. Agent基础

### 2.1 定义Agent

Agent执行特定任务或一组任务。Agent通过接口定义，类似于普通的AI服务，但需要添加`@Agent`注解。

```java
public interface CreativeWriter {

    @UserMessage("""
            You are a creative writer.
            Generate a draft of a story no more than
            3 sentences long around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
    @Agent("Generates a story based on the given topic")
    String generateStory(@V("topic") String topic);
}
```

**最佳实践：**
- 在`@Agent`注解中提供简短描述，特别是在纯agentic模式中使用时
- 描述也可以通过builder的`description`方法以编程方式提供

### 2.2 Agent命名

Agent必须有一个唯一标识的名称。名称可以：
- 在`@Agent`注解中指定
- 通过builder的`name`方法以编程方式指定
- 如果未指定，则使用被`@Agent`注解的方法名

### 2.3 构建Agent

```java
CreativeWriter creativeWriter = AgenticServices
        .agentBuilder(CreativeWriter.class)
        .chatModel(myChatModel)
        .outputName("story")
        .build();
```

### 2.4 Agent与AI Service的区别

1. Agent本质上是普通的AI服务，提供相同的功能
2. 主要区别是`outputName`参数：
   - 指定共享变量的名称
   - Agent调用结果会存储在这个变量中
   - 使agentic系统中的其他agent可以使用

`outputName`也可以直接在`@Agent`注解中声明：

```java
@Agent(outputName = "story", description = "Generates a story based on the given topic")
```

---

## 3. AgenticScope

### 3.1 什么是AgenticScope

AgenticScope是参与agentic系统的agent之间共享的数据集合。

**用途：**
- 存储共享变量
- 由一个agent写入以传递结果
- 由另一个agent读取以获取所需信息
- 自动注册所有agent的调用序列及其响应

### 3.2 创建和使用

- 在主agent调用时自动创建
- 在必要时通过回调以编程方式提供
- 允许agent有效协作，按需共享信息和结果

---

## 4. 工作流模式

### 4.1 顺序工作流（Sequential Workflow）

#### 概念
多个agent按顺序调用，每个agent的输出作为下一个agent的输入。

#### 示例场景
创作故事 → 针对受众编辑 → 针对风格编辑

#### AudienceEditor定义

```java
public interface AudienceEditor {

    @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better align
        with the target audience of {{audience}}.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
    @Agent("Edits a story to better fit a given audience")
    String editStory(@V("story") String story, @V("audience") String audience);
}
```

#### StyleEditor定义

```java
public interface StyleEditor {

    @UserMessage("""
        You are a professional editor.
        Analyze and rewrite the following story to better fit and be more coherent with the {{style}} style.
        Return only the story and nothing else.
        The story is "{{story}}".
        """)
    @Agent("Edits a story to better fit a given style")
    String editStory(@V("story") String story, @V("style") String style);
}
```

**注意：**
- 输入参数使用`@V`注解标注变量名
- 参数值从AgenticScope共享变量中获取
- 如果使用`-parameters`编译选项，可以省略`@V`注解

#### 创建顺序工作流

```java
CreativeWriter creativeWriter = AgenticServices
        .agentBuilder(CreativeWriter.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

AudienceEditor audienceEditor = AgenticServices
        .agentBuilder(AudienceEditor.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

StyleEditor styleEditor = AgenticServices
        .agentBuilder(StyleEditor.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

UntypedAgent novelCreator = AgenticServices
        .sequenceBuilder()
        .subAgents(creativeWriter, audienceEditor, styleEditor)
        .outputName("story")
        .build();

Map<String, Object> input = Map.of(
        "topic", "dragons and wizards",
        "style", "fantasy",
        "audience", "young adults"
);

String story = (String) novelCreator.invoke(input);
```

#### UntypedAgent接口

```java
public interface UntypedAgent {
    @Agent
    Object invoke(Map<String, Object> input);
}
```

#### 使用类型化接口

定义接口：

```java
public interface NovelCreator {

    @Agent
    String createNovel(@V("topic") String topic, @V("audience") String audience, @V("style") String style);
}
```

创建和使用：

```java
NovelCreator novelCreator = AgenticServices
        .sequenceBuilder(NovelCreator.class)
        .subAgents(creativeWriter, audienceEditor, styleEditor)
        .outputName("story")
        .build();

String story = novelCreator.createNovel("dragons and wizards", "young adults", "fantasy");
```

---

### 4.2 循环工作流（Loop Workflow）

#### 概念
通过重复调用agent来迭代优化结果，直到满足特定条件。

#### StyleScorer定义

```java
public interface StyleScorer {

    @UserMessage("""
            You are a critical reviewer.
            Give a review score between 0.0 and 1.0 for the following
            story based on how well it aligns with the style '{{style}}'.
            Return only the score and nothing else.
            
            The story is: "{{story}}"
            """)
    @Agent("Scores a story based on how well it aligns with a given style")
    double scoreStyle(@V("story") String story, @V("style") String style);
}
```

#### 创建循环工作流

```java
StyleEditor styleEditor = AgenticServices
        .agentBuilder(StyleEditor.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

StyleScorer styleScorer = AgenticServices
        .agentBuilder(StyleScorer.class)
        .chatModel(BASE_MODEL)
        .outputName("score")
        .build();

UntypedAgent styleReviewLoop = AgenticServices
        .loopBuilder()
        .subAgents(styleScorer, styleEditor)
        .maxIterations(5)
        .exitCondition( agenticScope -> agenticScope.readState("score", 0.0) >= 0.8)
        .build();
```

#### 退出条件配置

**默认行为：**
- 每次agent调用后都评估退出条件
- 条件满足后立即退出循环

**在循环结束时测试：**

```java
UntypedAgent styleReviewLoop = AgenticServices
        .loopBuilder()
        .subAgents(styleScorer, styleEditor)
        .maxIterations(5)
        .testExitAtLoopEnd(true)
        .exitCondition( (agenticScope, loopCounter) -> {
            double score = agenticScope.readState("score", 0.0);
            return loopCounter <= 3 ? score >= 0.8 : score >= 0.6;
        })
        .build();
```

**使用循环计数器：**
- `BiPredicate<AgenticScope, Integer>`接收当前循环迭代计数
- 可以根据迭代次数调整退出标准

#### 组合循环到顺序工作流

```java
public interface StyledWriter {

    @Agent
    String writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
}

CreativeWriter creativeWriter = AgenticServices
        .agentBuilder(CreativeWriter.class)
        .chatModel(BASE_MODEL)
        .outputName("story")
        .build();

StyledWriter styledWriter = AgenticServices
        .sequenceBuilder(StyledWriter.class)
        .subAgents(creativeWriter, styleReviewLoop)
        .outputName("story")
        .build();

String story = styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");
```

---

### 4.3 并行工作流（Parallel Workflow）

#### 概念
同时调用多个可以独立工作的agent，然后组合它们的输出。

#### 示例Agent定义

**FoodExpert：**

```java
public interface FoodExpert {

    @UserMessage("""
        You are a great evening planner.
        Propose a list of 3 meals matching the given mood.
        The mood is {{mood}}.
        For each meal, just give the name of the meal.
        Provide a list with the 3 items and nothing else.
        """)
    @Agent
    List<String> findMeal(@V("mood") String mood);
}
```

**MovieExpert：**

```java
public interface MovieExpert {

    @UserMessage("""
        You are a great evening planner.
        Propose a list of 3 movies matching the given mood.
        The mood is {mood}.
        Provide a list with the 3 items and nothing else.
        """)
    @Agent
    List<String> findMovie(@V("mood") String mood);
}
```

#### 创建并行工作流

```java
FoodExpert foodExpert = AgenticServices
        .agentBuilder(FoodExpert.class)
        .chatModel(BASE_MODEL)
        .outputName("meals")
        .build();

MovieExpert movieExpert = AgenticServices
        .agentBuilder(MovieExpert.class)
        .chatModel(BASE_MODEL)
        .outputName("movies")
        .build();

EveningPlannerAgent eveningPlannerAgent = AgenticServices
        .parallelBuilder(EveningPlannerAgent.class)
        .subAgents(foodExpert, movieExpert)
        .executor(Executors.newFixedThreadPool(2))
        .outputName("plans")
        .output(agenticScope -> {
            List<String> movies = agenticScope.readState("movies", List.of());
            List<String> meals = agenticScope.readState("meals", List.of());

            List<EveningPlan> moviesAndMeals = new ArrayList<>();
            for (int i = 0; i < movies.size(); i++) {
                if (i >= meals.size()) {
                    break;
                }
                moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
            }
            return moviesAndMeals;
        })
        .build();

List<EveningPlan> plans = eveningPlannerAgent.plan("romantic");
```

#### 关键配置

**output方法：**
- 定义如何组合子agent的输出
- 可在任何工作流模式中使用
- 不仅限于并行工作流

**executor方法：**
- 可选提供用于并行执行的Executor
- 如果不提供，默认使用内部缓存线程池

---

### 4.4 条件工作流（Conditional Workflow）

#### 概念
根据特定条件调用不同的agent。

#### CategoryRouter定义

```java
public interface CategoryRouter {

    @UserMessage("""
        Analyze the following user request and categorize it as 'legal', 'medical' or 'technical'.
        In case the request doesn't belong to any of those categories categorize it as 'unknown'.
        Reply with only one of those words and nothing else.
        The user request is: '{{request}}'.
        """)
    @Agent("Categorizes a user request")
    RequestCategory classify(@V("request") String request);
}
```

**RequestCategory枚举：**

```java
public enum RequestCategory {
    LEGAL, MEDICAL, TECHNICAL, UNKNOWN
}
```

#### MedicalExpert定义

```java
public interface MedicalExpert {

    @UserMessage("""
        You are a medical expert.
        Analyze the following user request under a medical point of view and provide the best possible answer.
        The user request is {{request}}.
        """)
    @Agent("A medical expert")
    String medical(@V("request") String request);
}
```

#### 创建条件工作流

```java
CategoryRouter routerAgent = AgenticServices
        .agentBuilder(CategoryRouter.class)
        .chatModel(BASE_MODEL)
        .outputName("category")
        .build();

MedicalExpert medicalExpert = AgenticServices
        .agentBuilder(MedicalExpert.class)
        .chatModel(BASE_MODEL)
        .outputName("response")
        .build();
LegalExpert legalExpert = AgenticServices
        .agentBuilder(LegalExpert.class)
        .chatModel(BASE_MODEL)
        .outputName("response")
        .build();
TechnicalExpert technicalExpert = AgenticServices
        .agentBuilder(TechnicalExpert.class)
        .chatModel(BASE_MODEL)
        .outputName("response")
        .build();

UntypedAgent expertsAgent = AgenticServices.conditionalBuilder()
        .subAgents( agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.MEDICAL, medicalExpert)
        .subAgents( agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.LEGAL, legalExpert)
        .subAgents( agenticScope -> agenticScope.readState("category", RequestCategory.UNKNOWN) == RequestCategory.TECHNICAL, technicalExpert)
        .build();

ExpertRouterAgent expertRouterAgent = AgenticServices
        .sequenceBuilder(ExpertRouterAgent.class)
        .subAgents(routerAgent, expertsAgent)
        .outputName("response")
        .build();

String response = expertRouterAgent.ask("I broke my leg what should I do");
```

---

## 5. 异步Agent

### 5.1 同步 vs 异步执行

**默认行为（同步）：**
- 所有agent调用在同一线程中执行
- 每个agent完成后才执行下一个

**异步执行：**
- agent在单独的线程中执行
- 主流程不等待agent完成就继续
- 结果在AgenticScope中可用
- 当其他agent需要该结果时才会阻塞等待

### 5.2 配置异步Agent

```java
FoodExpert foodExpert = AgenticServices
        .agentBuilder(FoodExpert.class)
        .chatModel(BASE_MODEL)
        .async(true)
        .outputName("meals")
        .build();

MovieExpert movieExpert = AgenticServices
        .agentBuilder(MovieExpert.class)
        .chatModel(BASE_MODEL)
        .async(true)
        .outputName("movies")
        .build();

EveningPlannerAgent eveningPlannerAgent = AgenticServices
        .sequenceBuilder(EveningPlannerAgent.class)
        .subAgents(foodExpert, movieExpert)
        .executor(Executors.newFixedThreadPool(2))
        .outputName("plans")
        .output(agenticScope -> {
            List<String> movies = agenticScope.readState("movies", List.of());
            List<String> meals = agenticScope.readState("meals", List.of());

            List<EveningPlan> moviesAndMeals = new ArrayList<>();
            for (int i = 0; i < movies.size(); i++) {
                if (i >= meals.size()) {
                    break;
                }
                moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
            }
            return moviesAndMeals;
        })
        .build();

List<EveningPlan> plans = eveningPlannerAgent.plan("romantic");
```

**优势：**
- 独立的agent可以同时执行
- 即使在顺序工作流中也能实现并行

---

## 6. 错误处理

### 6.1 错误场景

在复杂的agentic系统中可能出现的问题：
- Agent无法产生结果
- 外部工具不可用
- Agent执行期间发生意外错误

### 6.2 ErrorContext

```java
record ErrorContext(String agentName, AgenticScope agenticScope, AgentInvocationException exception) { }
```

### 6.3 ErrorRecoveryResult

三种可能的恢复结果：

1. **ErrorRecoveryResult.throwException()**
   - 默认行为
   - 将异常传播到根调用者

2. **ErrorRecoveryResult.retry()**
   - 重试agent调用
   - 可以先执行纠正措施

3. **ErrorRecoveryResult.result(Object result)**
   - 忽略问题
   - 返回提供的结果作为失败agent的输出

### 6.4 配置错误处理器

```java
UntypedAgent novelCreator = AgenticServices.sequenceBuilder()
        .subAgents(creativeWriter, audienceEditor, styleEditor)
        .errorHandler(errorContext -> {
            if (errorContext.agentName().equals("generateStory") &&
                    errorContext.exception() instanceof MissingArgumentException mEx && mEx.argumentName().equals("topic")) {
                errorContext.agenticScope().writeState("topic", "dragons and wizards");
                errorRecoveryCalled.set(true);
                return ErrorRecoveryResult.retry();
            }
            return ErrorRecoveryResult.throwException();
        })
        .outputName("story")
        .build();
```

**示例说明：**
- 检测到缺少"topic"参数
- 在AgenticScope中提供缺失的参数
- 重试agent调用

---

## 7. 可观察性

### 7.1 监听器类型

1. **beforeAgentInvocation**
   - 在agent调用之前立即通知
   - 接收AgentRequest参数

2. **afterAgentInvocation**
   - 在agent完成任务并返回结果后立即通知
   - 接收AgentResponse参数

### 7.2 配置监听器

```java
CreativeWriter creativeWriter = AgenticServices.agentBuilder(CreativeWriter.class)
    .chatModel(baseModel())
    .outputName("story")
    .beforeAgentInvocation(request -> System.out.println("Invoking CreativeWriter with topic: " + request.inputs().get("topic")))
    .afterAgentInvocation(response -> System.out.println("CreativeWriter generated this story: " + response.output()))
    .build();
```

### 7.3 关键信息

**AgentRequest和AgentResponse包含：**
- Agent名称
- 输入/输出
- AgenticScope实例

**注意事项：**
- 监听器在执行agent的同一线程中调用
- 监听器是同步的
- 不应执行长时间阻塞操作

---

## 8. 声明式API

### 8.1 概述

使用注解以更简洁、可读的方式定义工作流。

### 8.2 并行工作流声明式示例

```java
public interface EveningPlannerAgent {

    @ParallelAgent(outputName = "plans", subAgents = {
            @SubAgent(type = FoodExpert.class, outputName = "meals"),
            @SubAgent(type = MovieExpert.class, outputName = "movies")
    })
    List<EveningPlan> plan(@V("mood") String mood);

    @ParallelExecutor
    static Executor executor() {
        return Executors.newFixedThreadPool(2);
    }

    @Output
    static List<EveningPlan> createPlans(@V("movies") List<String> movies, @V("meals") List<String> meals) {
        List<EveningPlan> moviesAndMeals = new ArrayList<>();
        for (int i = 0; i < movies.size(); i++) {
            if (i >= meals.size()) {
                break;
            }
            moviesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
        }
        return moviesAndMeals;
    }
}
```

### 8.3 创建Agentic系统

```java
EveningPlannerAgent eveningPlannerAgent = AgenticServices
        .createAgenticSystem(EveningPlannerAgent.class, BASE_MODEL);
List<EveningPlan> plans = eveningPlannerAgent.plan("romantic");
```

### 8.4 自定义ChatModel

```java
public interface FoodExpert {

    @UserMessage("""
        You are a great evening planner.
        Propose a list of 3 meals matching the given mood.
        The mood is {{mood}}.
        For each meal, just give the name of the meal.
        Provide a list with the 3 items and nothing else.
        """)
    @Agent
    List<String> findMeal(@V("mood") String mood);

    @ChatModelSupplier
    static ChatModel chatModel() {
        return FOOD_MODEL;
    }
}
```

### 8.5 可用的配置注解

| 注解名称 | 描述 | 参数要求 |
|---------|------|---------|
| @ChatModelSupplier | 返回此agent使用的ChatModel | 无参数 |
| @ChatMemorySupplier | 返回此agent使用的ChatMemory | 无参数 |
| @ChatMemoryProviderSupplier | 返回此agent使用的ChatMemoryProvider | 需要Object作为memoryId |
| @ContentRetrieverSupplier | 返回此agent使用的ContentRetriever | 无参数 |
| @BeforeAgentInvocation | 在agent调用前通知 | 需要AgentRequest参数 |
| @AfterAgentInvocation | 在agent调用完成后通知 | 需要AgentResponse参数 |
| @RetrievalAugmentorSupplier | 返回此agent使用的RetrievalAugmentor | 无参数 |
| @ToolsSupplier | 返回此agent使用的工具或工具集 | 可返回Object或Object[] |
| @ToolProviderSupplier | 返回此agent使用的ToolProvider | 无参数 |

### 8.6 条件工作流声明式示例

```java
public interface ExpertsAgent {

    @ConditionalAgent(outputName = "response", subAgents = {
            @SubAgent(type = MedicalExpert.class, outputName = "response"),
            @SubAgent(type = TechnicalExpert.class, outputName = "response"),
            @SubAgent(type = LegalExpert.class, outputName = "response")
    })
    String askExpert(@V("request") String request);

    @ActivationCondition(MedicalExpert.class)
    static boolean activateMedical(@V("category") RequestCategory category) {
        return category == RequestCategory.MEDICAL;
    }

    @ActivationCondition(TechnicalExpert.class)
    static boolean activateTechnical(@V("category") RequestCategory category) {
        return category == RequestCategory.TECHNICAL;
    }

    @ActivationCondition(LegalExpert.class)
    static boolean activateLegal(@V("category") RequestCategory category) {
        return category == RequestCategory.LEGAL;
    }
}
```

### 8.7 混合编程和声明式风格

可以混合使用两种风格：
- 部分使用注解配置agent
- 部分使用builder配置
- 声明式定义的agent可用作编程式系统的子agent

```java
public interface CreativeWriter {

    @UserMessage("""
            You are a creative writer.
            Generate a draft of a story long no more than 3 sentence around the given topic.
            Return only the story and nothing else.
            The topic is {{topic}}.
            """)
    @Agent(description = "Generate a story based on the given topic", outputName = "story")
    String generateStory(@V("topic") String topic);

    @ChatModelSupplier
    static ChatModel chatModel() {
        return baseModel();
    }
}

UntypedAgent novelCreator = AgenticServices.sequenceBuilder()
        .subAgents(CreativeWriter.class, AudienceEditor.class)
        .outputName("story")
        .build();
```

---

## 9. 记忆与上下文工程

### 9.1 为Agent提供记忆

#### 定义带记忆的Agent

```java
public interface MedicalExpertWithMemory {

    @UserMessage("""
        You are a medical expert.
        Analyze the following user request under a medical point of view and provide the best possible answer.
        The user request is {{request}}.
        """)
    @Agent("A medical expert")
    String medical(@MemoryId String memoryId, @V("request") String request);
}
```

#### 配置记忆提供者

```java
MedicalExpertWithMemory medicalExpert = AgenticServices
        .agentBuilder(MedicalExpertWithMemory.class)
        .chatModel(BASE_MODEL)
        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
        .outputName("response")
        .build();
```

### 9.2 Agentic系统中的上下文问题

**问题场景：**
```java
String response1 = expertRouterAgent.ask("1", "I broke my leg, what should I do?");
String legalResponse1 = expertRouterAgent.ask("1", "Should I sue my neighbor who caused this damage?");
```

第二个问题路由到法律专家，但法律专家没有第一次对话的上下文。

### 9.3 上下文摘要解决方案

#### 定义ContextSummarizer

```java
public interface ContextSummarizer {

    @UserMessage("""
        Create a very short summary, 2 sentences at most, of the
        following conversation between an AI agent and a user.

        The user conversation is: '{{it}}'.
        """)
    String summarize(String conversation);
}
```

#### 为Agent提供上下文

```java
LegalExpertWithMemory legalExpert = AgenticServices
        .agentBuilder(LegalExpertWithMemory.class)
        .chatModel(BASE_MODEL)
        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
        .context(agenticScope -> contextSummarizer.summarize(agenticScope.contextAsConversation()))
        .outputName("response")
        .build();
```

### 9.4 上下文如何工作

框架自动重写用户消息以包含摘要的上下文：

```
"Considering this context \"The user asked about what to do after breaking their leg, and the AI provided medical advice on immediate actions like immobilizing the leg, applying ice, and seeking medical attention.\"
You are a legal expert.
Analyze the following user request under a legal point of view and provide the best possible answer.
The user request is Should I sue my neighbor who caused this damage?."
```

### 9.5 简化的上下文摘要

```java
LegalExpertWithMemory legalExpert = AgenticServices
        .agentBuilder(LegalExpertWithMemory.class)
        .chatModel(BASE_MODEL)
        .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
        .summarizedContext("medical", "technical")
        .outputName("response")
        .build();
```

**说明：**
- 内部使用ContextSummarizer agent
- 使用与定义agent相同的chat model
- 可以指定要摘要的agent名称（varargs）
- 如果不指定，则摘要所有agent的上下文

---

## 10. AgenticScope注册表和持久化

### 10.1 AgenticScope生命周期

**无状态执行：**
- 不使用记忆时
- AgenticScope在执行结束时自动丢弃
- 状态不会持久化

**有状态执行：**
- 使用记忆时
- AgenticScope保存在内部注册表中
- 永久保留以支持有状态和对话式交互

### 10.2 清理AgenticScope

当不再需要特定ID的AgenticScope时，必须显式驱逐：

```java
// 根agent需要实现AgenticScopeAccess接口
agent.evictAgenticScope(memoryId);
```

### 10.3 自定义持久化层

#### AgenticScopeStore接口

实现SPI接口以插入自定义持久化层：

**编程方式设置：**

```java
AgenticScopePersister.setStore(new MyAgenticScopeStore());
```

**Service Provider接口方式：**

创建文件：`META-INF/services/dev.langchain4j.agentic.scope.AgenticScopeStore`

内容：实现类的完全限定名

---

## 11. 纯Agentic AI

### 11.1 概述

在纯agentic模式中，系统不使用确定性工作流，而是让agent根据上下文和之前交互的结果自主决策。

### 11.2 Supervisor Agent

langchain4j-agentic提供开箱即用的supervisor agent：
- 可以提供一组子agent
- 自主生成计划
- 决定下一个调用哪个agent
- 判断任务是否完成

### 11.3 示例：银行Agent系统

#### WithdrawAgent

```java
public interface WithdrawAgent {

    @SystemMessage("""
            You are a banker that can only withdraw US dollars (USD) from a user account,
            """)
    @UserMessage("""
            Withdraw {{amount}} USD from {{user}}'s account and return the new balance.
            """)
    @Agent("A banker that withdraw USD from an account")
    String withdraw(@V("user") String user, @V("amount") Double amount);
}
```

#### CreditAgent

```java
public interface CreditAgent {
    @SystemMessage("""
        You are a banker that can only credit US dollars (USD) to a user account,
        """)
    @UserMessage("""
        Credit {{amount}} USD to {{user}}'s account and return the new balance.
        """)
    @Agent("A banker that credit USD to an account")
    String credit(@V("user") String user, @V("amount") Double amount);
}
```

#### ExchangeAgent

```java
public interface ExchangeAgent {
    @UserMessage("""
            You are an operator exchanging money in different currencies.
            Use the tool to exchange {{amount}} {{originalCurrency}} into {{targetCurrency}}
            returning only the final amount provided by the tool as it is and nothing else.
            """)
    @Agent("A money exchanger that converts a given amount of money from the original to the target currency")
    Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency);
}
```

### 11.4 工具定义

#### BankTool

```java
public class BankTool {

    private final Map<String, Double> accounts = new HashMap<>();

    void createAccount(String user, Double initialBalance) {
        if (accounts.containsKey(user)) {
            throw new RuntimeException("Account for user " + user + " already exists");
        }
        accounts.put(user, initialBalance);
    }

    double getBalance(String user) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        return balance;
    }

    @Tool("Credit the given user with the given amount and return the new balance")
    Double credit(@P("user name") String user, @P("amount") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance + amount;
        accounts.put(user, newBalance);
        return newBalance;
    }

    @Tool("Withdraw the given amount with the given user and return the new balance")
    Double withdraw(@P("user name") String user, @P("amount") Double amount) {
        Double balance = accounts.get(user);
        if (balance == null) {
            throw new RuntimeException("No balance found for user " + user);
        }
        Double newBalance = balance - amount;
        accounts.put(user, newBalance);
        return newBalance;
    }
}
```

#### ExchangeTool

```java
public class ExchangeTool {

    @Tool("Exchange the given amount of money from the original to the target currency")
    Double exchange(@P("originalCurrency") String originalCurrency, @P("amount") Double amount, @P("targetCurrency") String targetCurrency) {
        // Invoke a REST service to get the exchange rate
    }
}
```

### 11.5 创建Supervisor Agent

```java
BankTool bankTool = new BankTool();
bankTool.createAccount("Mario", 1000.0);
bankTool.createAccount("Georgios", 1000.0);

WithdrawAgent withdrawAgent = AgenticServices
        .agentBuilder(WithdrawAgent.class)
        .chatModel(BASE_MODEL)
        .tools(bankTool)
        .build();
CreditAgent creditAgent = AgenticServices
        .agentBuilder(CreditAgent.class)
        .chatModel(BASE_MODEL)
        .tools(bankTool)
        .build();

ExchangeAgent exchangeAgent = AgenticServices
        .agentBuilder(ExchangeAgent.class)
        .chatModel(BASE_MODEL)
        .tools(new ExchangeTool())
        .build();

SupervisorAgent bankSupervisor = AgenticServices
        .supervisorBuilder()
        .chatModel(PLANNER_MODEL)
        .subAgents(withdrawAgent, creditAgent, exchangeAgent)
        .responseStrategy(SupervisorResponseStrategy.SUMMARY)
        .build();
```

### 11.6 SupervisorAgent接口

```java
public interface SupervisorAgent {
    @Agent
    String invoke(@V("request") String request);
}
```

### 11.7 工作原理

**用户请求：**
```java
bankSupervisor.invoke("Transfer 100 EUR from Mario's account to Georgios' one")
```

**Supervisor生成的计划（AgentInvocation序列）：**

```java
public record AgentInvocation(String agentName, Map<String, String> arguments) {}
```

**示例计划：**

```
AgentInvocation{agentName='exchange', arguments={originalCurrency=EUR, amount=100, targetCurrency=USD}}

AgentInvocation{agentName='withdraw', arguments={user=Mario, amount=115.0}}

AgentInvocation{agentName='credit', arguments={user=Georgios, amount=115.0}}

AgentInvocation{agentName='done', arguments={response=The transfer of 100 EUR from Mario's account to Georgios' account has been completed. Mario's balance is 885.0 USD, and Georgios' balance is 1115.0 USD. The conversion rate was 1.15 EUR to USD.}}
```

最后一个调用是特殊的"done"调用，表示任务完成。

---

## 12. Supervisor设计和自定义

### 12.1 响应策略（SupervisorResponseStrategy）

```java
public enum SupervisorResponseStrategy {
    SCORED,   // 使用scorer agent评分选择最佳响应
    SUMMARY,  // 总是返回摘要
    LAST      // 总是返回最后一个agent的响应（默认）
}
```

#### 配置响应策略

```java
AgenticServices.supervisorBuilder()
        .responseStrategy(SupervisorResponseStrategy.SCORED)
        .build();
```

#### SCORED策略

使用单独的agent对两个可能的响应评分：
1. Supervisor生成的摘要
2. 最后调用的agent的响应

**示例评分结果：**
```
ResponseScore{finalResponse=0.3, summary=1.0}
```

根据评分选择返回哪个响应。

### 12.2 上下文生成策略（SupervisorContextStrategy）

```java
public enum SupervisorContextStrategy {
    CHAT_MEMORY,                      // 仅使用本地聊天记忆（默认）
    SUMMARIZATION,                    // 摘要子agent的对话
    CHAT_MEMORY_AND_SUMMARIZATION     // 结合两者
}
```

#### 配置上下文策略

```java
AgenticServices.supervisorBuilder()
        .contextGenerationStrategy(SupervisorContextStrategy.SUMMARIZATION)
        .build();
```

### 12.3 Supervisor架构图

```
用户请求 → Supervisor Agent
            ↓
       生成计划
            ↓
      调用子Agent
            ↓
      收集响应
            ↓
    响应策略评估 → 最终响应
```

### 12.4 为Supervisor提供上下文

上下文是约束、策略或偏好，用于指导计划生成。

#### 构建时配置

```java
SupervisorAgent bankSupervisor = AgenticServices
        .supervisorBuilder()
        .chatModel(PLANNER_MODEL)
        .supervisorContext("Policies: prefer internal tools; currency USD; no external APIs")
        .subAgents(withdrawAgent, creditAgent, exchangeAgent)
        .responseStrategy(SupervisorResponseStrategy.SUMMARY)
        .build();
```

#### 调用时提供（类型化Supervisor）

```java
public interface SupervisorAgent {
    @Agent
    String invoke(@V("request") String request, @V("supervisorContext") String supervisorContext);
}

// 调用时覆盖构建时的值
bankSupervisor.invoke(
        "Transfer 100 EUR from Mario's account to Georgios' one",
        "Policies: convert to USD first; use bank tools only; no external APIs"
);
```

#### 调用时提供（非类型化Supervisor）

```java
Map<String, Object> input = Map.of(
        "request", "Transfer 100 EUR from Mario's account to Georgios' one",
        "supervisorContext", "Policies: convert to USD first; use bank tools only; no external APIs"
);

String result = (String) bankSupervisor.invoke(input);
```

**优先级：**
- 如果同时提供，调用时的值会覆盖构建时的值
- 上下文存储在AgenticScope的"supervisorContext"变量中

---

## 13. 非AI Agent

### 13.1 概念

非AI agent执行不需要自然语言处理的任务，如：
- 调用REST API
- 执行命令
- 执行计算

虽然更类似于工具，但在agentic系统中建模为agent更方便。

### 13.2 示例：ExchangeOperator

```java
public class ExchangeOperator {

    @Agent(value = "A money exchanger that converts a given amount of money from the original to the target currency",
            outputName = "exchange")
    public Double exchange(@V("originalCurrency") String originalCurrency, @V("amount") Double amount, @V("targetCurrency") String targetCurrency) {
        // invoke the REST API to perform the currency exchange
    }
}
```

### 13.3 使用非AI Agent

```java
WithdrawAgent withdrawAgent = AgenticServices
        .agentBuilder(WithdrawAgent.class)
        .chatModel(BASE_MODEL)
        .tools(bankTool)
        .build();
CreditAgent creditAgent = AgenticServices
        .agentBuilder(CreditAgent.class)
        .chatModel(BASE_MODEL)
        .tools(bankTool)
        .build();

SupervisorAgent bankSupervisor = AgenticServices
        .supervisorBuilder()
        .chatModel(PLANNER_MODEL)
        .subAgents(withdrawAgent, creditAgent, new ExchangeOperator())
        .build();
```

### 13.4 定义规则

**任何Java类都可以是agent，只要：**
- 有且仅有一个方法被`@Agent`注解标注

### 13.5 Agent Action

使用`agentAction`工厂方法创建简单的agent，用于：
- 读取AgenticScope状态
- 执行小操作

**示例：类型转换**

```java
UntypedAgent editor = AgenticServices.sequenceBuilder()
        .subAgents(
                scorer,
                AgenticServices.agentAction(agenticScope -> 
                    agenticScope.writeState("score", 
                        Double.parseDouble(agenticScope.readState("score", "0.0")))),
                reviewer)
        .build();
```

---

## 14. Human-in-the-Loop

### 14.1 概念

允许agentic系统：
- 请求用户输入缺失信息
- 在执行某些操作前获得批准

可以作为特殊的非AI agent实现。

### 14.2 基本实现

```java
public record HumanInTheLoop(Consumer<String> requestWriter, Supplier<String> responseReader) {

    @Agent("An agent that asks the user for missing information")
    public String askUser(String request) {
        requestWriter.accept(request);
        return responseReader.get();
    }
}
```

**组件：**
- **requestWriter**: Consumer，将AI请求转发给用户
- **responseReader**: Supplier，获取用户响应（可能阻塞）

### 14.3 完整示例：星座运势系统

#### AstrologyAgent定义

```java
public interface AstrologyAgent {
    @SystemMessage("""
        You are an astrologist that generates horoscopes based on the user's name and zodiac sign.
        """)
    @UserMessage("""
        Generate the horoscope for {{name}} who is a {{sign}}.
        """)
    @Agent("An astrologist that generates horoscopes based on the user's name and zodiac sign.")
    String horoscope(@V("name") String name, @V("sign") String sign);
}
```

#### 创建系统

```java
AstrologyAgent astrologyAgent = AgenticServices
        .agentBuilder(AstrologyAgent.class)
        .chatModel(BASE_MODEL)
        .build();

HumanInTheLoop humanInTheLoop = AgenticServices
        .humanInTheLoopBuilder()
        .description("An agent that asks the zodiac sign of the user")
        .outputName("sign")
        .requestWriter(request -> {
            System.out.println(request);
            System.out.print("> ");
        })
        .responseReader(() -> System.console().readLine())
        .build();

SupervisorAgent horoscopeAgent = AgenticServices
        .supervisorBuilder()
        .chatModel(PLANNER_MODEL)
        .subAgents(astrologyAgent, humanInTheLoop)
        .build();
```

#### 执行流程

**用户调用：**
```java
horoscopeAgent.invoke("My name is Mario. What is my horoscope?")
```

**系统输出：**
```
What is your zodiac sign?
> 
```

等待用户输入，然后使用该信息生成运势。

### 14.4 异步建议

**推荐配置为异步：**
- 用户可能需要时间提供答案
- 不依赖用户输入的agent可以继续执行

```java
HumanInTheLoop humanInTheLoop = AgenticServices
        .humanInTheLoopBuilder()
        .description("An agent that asks the zodiac sign of the user")
        .outputName("sign")
        .async(true)  // 配置为异步
        .requestWriter(...)
        .responseReader(...)
        .build();
```

**注意：**
- Supervisor总是强制所有agent阻塞执行
- 这允许计划考虑AgenticScope的完整状态
- 在supervisor中配置HumanInTheLoop为异步可能无效

---

## 15. A2A集成

### 15.1 概述

langchain4j-agentic-a2a模块提供与A2A协议的无缝集成：
- 使用远程A2A服务器agent
- 与本地定义的agent混合使用

### 15.2 非类型化A2A Agent

```java
UntypedAgent creativeWriter = AgenticServices
        .a2aBuilder(A2A_SERVER_URL)
        .inputNames("topic")
        .outputName("story")
        .build();
```

**说明：**
- agent能力描述从A2A服务器的agent card自动检索
- 由于card不提供输入参数名称，需要使用`inputNames`显式指定

### 15.3 类型化A2A Agent

#### 定义本地接口

```java
public interface A2ACreativeWriter {

    @Agent
    String generateStory(@V("topic") String topic);
}
```

#### 创建A2A Agent

```java
A2ACreativeWriter creativeWriter = AgenticServices
        .a2aBuilder(A2A_SERVER_URL, A2ACreativeWriter.class)
        .outputName("story")
        .build();
```

**优势：**
- 类型安全
- 输入名称自动从方法参数派生

### 15.4 与本地Agent混合

A2A agent可以：
- 像本地agent一样使用
- 在定义工作流时混合使用
- 作为supervisor的子agent

### 15.5 要求

远程A2A agent必须返回Task类型。

---

## 总结

LangChain4j Agentic模块提供了构建复杂AI系统的完整工具集：

### 核心能力
1. **工作流模式**: 顺序、循环、并行、条件
2. **执行控制**: 同步/异步、错误处理
3. **记忆管理**: ChatMemory、上下文工程
4. **可观察性**: 监听器、日志
5. **灵活架构**: 编程式+声明式API

### 高级特性
1. **Pure Agentic AI**: Supervisor agent自主决策
2. **Human-in-the-Loop**: 用户交互
3. **非AI Agent**: 集成传统工具
4. **A2A集成**: 远程agent调用

### 设计原则
- Agent之间通过AgenticScope共享数据
- 支持混合编程和声明式风格
- 灵活的持久化和状态管理
- 可扩展的错误处理机制