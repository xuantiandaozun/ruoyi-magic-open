package com.example;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VolcEngineFunctionCallChat {

    // 用于解析 get_current_weather 函数参数的类
    public static class WeatherArgs {
        @JsonProperty("location")
        private String location;

        @JsonProperty("unit")
        private String unit;

        // Jackson 需要默认构造函数
        public WeatherArgs() {
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    // 用于定义工具函数参数模式的类 (类似于Python中的parameters字典结构)
    public static class FunctionParameterSchema {
        public String type;
        public Map<String, Object> properties;
        public List<String> required;

        public FunctionParameterSchema(String type, Map<String, Object> properties, List<String> required) {
            this.type = type;
            this.properties = properties;
            this.required = required;
        }

        public String getType() {
            return type;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public List<String> getRequired() {
            return required;
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 工具函数实现: get_current_weather
    public static String getCurrentWeather(String location, String unit) {
        // 此处应为实际调用天气查询 API 的逻辑
        // 这里是示例，返回模拟的天气数据
        String currentUnit = (unit == null || unit.isEmpty()) ? "摄氏度" : unit;
        System.out.println(String.format("调用工具 get_current_weather: location=%s, unit=%s", location, currentUnit));
        return String.format("%s今天天气晴朗，温度 25 %s。", location, currentUnit);
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("ARK_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("错误: ARK_API_KEY 环境变量未设置。");
            return;
        }

        ArkService service = ArkService.builder()
                .apiKey(apiKey)
                .build();

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.builder().role(ChatMessageRole.USER).content("北京和上海今天的天气如何？").build());

        // 步骤 1: 定义工具
        Map<String, Object> locationProperty = new HashMap<>();
        locationProperty.put("type", "string");
        locationProperty.put("description", "地点的位置信息，例如上海，北京");

        Map<String, Object> unitProperty = new HashMap<>();
        unitProperty.put("type", "string");
        unitProperty.put("enum", Arrays.asList("摄氏度", "华氏度"));
        unitProperty.put("description", "温度单位");

        Map<String, Object> schemaProperties = new HashMap<>();
        schemaProperties.put("location", locationProperty);
        schemaProperties.put("unit", unitProperty);

        FunctionParameterSchema functionParams = new FunctionParameterSchema(
                "object",
                schemaProperties,
                Collections.singletonList("location") // 'location' 是必需参数
        );

        List<ChatTool> tools = Collections.singletonList(
                new ChatTool(
                        "function", // 工具类型
                        new ChatFunction.Builder()
                                .name("get_current_weather")
                                .description("获取指定地点的天气信息")
                                .parameters(functionParams) // 工具函数的参数模式
                                .build()));

        String modelId = "doubao-1-5-pro-32k-250115";

        while (true) {
            // 步骤 2: 发起模型请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(modelId)
                    .messages(messages)
                    .tools(tools)
                    .build();

            ChatCompletionResult completionResult;
            try {
                completionResult = service.createChatCompletion(request);
            } catch (Exception e) {
                System.err.println("调用 Ark API 时发生错误: " + e.getMessage());
                e.printStackTrace();
                break;
            }

            if (completionResult == null || completionResult.getChoices() == null
                    || completionResult.getChoices().isEmpty()) {
                System.err.println("从模型收到空的或无效的响应。");
                break;
            }

            ChatCompletionChoice choice = completionResult.getChoices().get(0);
            ChatMessage responseMessage = choice.getMessage();

            // 展示模型中间过程的回复内容
            System.out.println("模型回复: " + responseMessage.stringContent());

            // 将模型的回复（含函数调用请求）添加到消息历史中
            messages.add(responseMessage);
            if (choice.getFinishReason() == null || !"tool_calls".equalsIgnoreCase(choice.getFinishReason())) {
                // 模型最终总结，没有调用工具意愿，或者发生错误等其他终止原因
                break;
            }

            List<ChatToolCall> toolCalls = responseMessage.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                // 如果 finish_reason 是 "tool_calls"，但 toolCalls 为空，这可能是一个异常情况
                System.err.println("警告: Finish reason 是 'tool_calls' 但未在消息中找到 tool_calls。");
                break;
            }

            for (ChatToolCall toolCall : toolCalls) {
                String toolName = toolCall.getFunction().getName();
                if ("get_current_weather".equals(toolName)) {
                    // 步骤 3：调用外部工具
                    String argumentsJson = toolCall.getFunction().getArguments();
                    WeatherArgs tool_args;
                    try {
                        tool_args = objectMapper.readValue(argumentsJson, WeatherArgs.class);
                    } catch (JsonProcessingException e) {
                        System.err.println("解析 get_current_weather 参数时出错: " + argumentsJson + " - " + e.getMessage());
                        // 将错误信息作为工具结果回填
                        messages.add(ChatMessage.builder()
                                .role(ChatMessageRole.TOOL)
                                .content("解析参数时出错: " + e.getMessage())
                                .toolCallId(toolCall.getId())
                                .build());
                        continue;
                    }

                    String toolResult = getCurrentWeather(tool_args.getLocation(), tool_args.getUnit());
                    System.out.println("工具执行结果 (" + toolCall.getId() + "): " + toolResult);

                    // 步骤 4：回填工具结果，并获取模型总结回复
                    messages.add(ChatMessage.builder()
                            .role(ChatMessageRole.TOOL)
                            .content(toolResult)
                            .toolCallId(toolCall.getId()) // 关联函数调用 ID
                            .build());
                }
            }
        }

        service.shutdownExecutor();
        System.out.println("会话结束。");
    }
}
