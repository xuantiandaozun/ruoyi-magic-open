package com.ruoyi.framework.config.magic.swagger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.BaseDefinition;
import org.ssssssss.magicapi.core.model.DataType;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.Header;
import org.ssssssss.magicapi.core.model.Path;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.impl.RequestMagicDynamicRegistry;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class MagicApiOpenApiCustomizer implements OpenApiCustomizer {

    private final MagicApiSpringDocProperties properties;

    @Autowired(required = false)
    private RequestMagicDynamicRegistry requestMagicDynamicRegistry;

    @Autowired(required = false)
    private MagicResourceService magicResourceService;

    public MagicApiOpenApiCustomizer(MagicApiSpringDocProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (requestMagicDynamicRegistry == null || magicResourceService == null) {
            return;
        }

        try {
            List<ApiInfo> apiInfos = requestMagicDynamicRegistry.mappings();

            for (ApiInfo apiInfo : apiInfos) {
                processApiInfo(openApi, apiInfo);
            }
        } catch (Exception e) {
            // 日志记录错误，但不影响应用启动
            System.err.println("处理Magic API文档时出错: " + e.getMessage());
        }
    }

    private void processApiInfo(OpenAPI openApi, ApiInfo apiInfo) {
        String path = buildApiPath(apiInfo);
        String method = apiInfo.getMethod().toLowerCase();

        // 创建Operation
        Operation operation = new Operation();
        operation.setOperationId(apiInfo.getId());
        operation.setSummary(StringUtils.defaultIfBlank(apiInfo.getName(), "Magic API接口"));
        operation.setDescription(StringUtils.defaultIfBlank(apiInfo.getDescription(), apiInfo.getName()));

        // 添加标签 - 支持分组层级显示
        addGroupTags(operation, apiInfo);

        // 处理参数
        addParameters(operation, apiInfo);

        // 处理RequestBody
        addRequestBody(operation, apiInfo, openApi);

        // 处理响应
        addResponses(operation, apiInfo, openApi);

        // 添加到OpenAPI
        PathItem pathItem = openApi.getPaths().get(path);
        if (pathItem == null) {
            pathItem = new PathItem();
            openApi.getPaths().put(path, pathItem);
        }

        setOperationByMethod(pathItem, method, operation);
    }

    private String buildApiPath(ApiInfo apiInfo) {
        StringBuilder pathBuilder = new StringBuilder();

        // 添加API前缀
        String apiPrefix = properties.getApiPrefix();
        if (StringUtils.isNotBlank(apiPrefix)) {
            pathBuilder.append(apiPrefix);
        }

        // 添加分组路径
        String groupPath = "";
        if (magicResourceService != null) {
            groupPath = magicResourceService.getGroupPath(apiInfo.getGroupId());
        }

        if (StringUtils.isNotBlank(groupPath)) {
            // 确保分组路径以斜杠开头
            if (!groupPath.startsWith("/")) {
                pathBuilder.append("/");
            }
            pathBuilder.append(groupPath);
        }

        // 添加API路径
        String apiPath = apiInfo.getPath();
        if (StringUtils.isNotBlank(apiPath)) {
            // 确保API路径以斜杠开头
            if (!apiPath.startsWith("/")) {
                pathBuilder.append("/");
            }
            pathBuilder.append(apiPath);
        }

        // 清理多余的斜杠
        String finalPath = pathBuilder.toString();
        finalPath = finalPath.replaceAll("/+", "/"); // 将多个连续斜杠替换为单个斜杠

        // 确保路径以斜杠开头
        if (!finalPath.startsWith("/")) {
            finalPath = "/" + finalPath;
        }

        return finalPath;
    }

    /**
     * 添加分组标签，只使用第一级分组作为标签，子分组信息加入接口名称并支持按二级分组排序
     */
    private void addGroupTags(Operation operation, ApiInfo apiInfo) {
        if (magicResourceService != null) {
            try {
                // 获取完整的分组层级名称
                String fullGroupName = magicResourceService.getGroupName(apiInfo.getGroupId());
                if (StringUtils.isNotBlank(fullGroupName)) {
                    // 移除开头的斜杠
                    fullGroupName = fullGroupName.startsWith("/") ? fullGroupName.substring(1) : fullGroupName;

                    // 分割路径获取各级分组
                    String[] pathParts = fullGroupName.split("/");

                    // 只使用第一级分组作为标签
                    String firstLevelGroup = pathParts[0];
                    operation.addTagsItem(firstLevelGroup);

                    // 构建接口名称，支持按二级分组排序
                    String originalSummary = operation.getSummary();
                    String newSummary;

                    if (pathParts.length > 1) {
                        // 有子分组时，使用二级分组名称作为排序前缀
                        String secondLevelGroup = pathParts[1];
                        StringBuilder subGroupPath = new StringBuilder();

                        // 构建完整的子分组路径（从第二级开始）
                        for (int i = 1; i < pathParts.length; i++) {
                            if (i > 1) {
                                subGroupPath.append(" > ");
                            }
                            subGroupPath.append(pathParts[i]);
                        }

                        // 使用二级分组名称作为排序前缀，完整路径作为显示内容
                        newSummary = secondLevelGroup + " - [" + subGroupPath.toString() + "] " + originalSummary;
                    } else {
                        // 没有子分组时，直接使用原始名称
                        newSummary = originalSummary;
                    }

                    operation.setSummary(newSummary);
                    return;
                }

                // 如果 getGroupName 返回空，则尝试使用 getGroup 获取当前分组名称
                Group group = magicResourceService.getGroup(apiInfo.getGroupId());
                String groupName = group != null ? group.getName() : "默认分组";
                operation.addTagsItem(groupName);
            } catch (Exception e) {
                operation.addTagsItem("默认分组");
            }
        } else {
            operation.addTagsItem("Magic API");
        }
    }

    private void addParameters(Operation operation, ApiInfo apiInfo) {
        String method = apiInfo.getMethod().toLowerCase();
        boolean isPostLikeMethod = "post".equals(method) || "put".equals(method) || "patch".equals(method);

        // 对于POST/PUT/PATCH请求，参数应该放在RequestBody中，而不是作为query参数
        // 只有GET/DELETE等方法才将参数作为query参数
        if (apiInfo.getParameters() != null && !isPostLikeMethod) {
            for (org.ssssssss.magicapi.core.model.Parameter param : apiInfo.getParameters()) {
                Parameter parameter = new Parameter();
                parameter.setName(param.getName());
                parameter.setIn("query");
                parameter.setRequired(param.isRequired());
                parameter.setDescription(param.getDescription());

                Schema schema = new Schema();
                schema.setType(convertDataType(param.getDataType()));
                if (param.getValue() != null) {
                    schema.setExample(param.getValue());
                }
                parameter.setSchema(schema);

                operation.addParametersItem(parameter);
            }
        }

        // 路径参数 - 总是添加
        if (apiInfo.getPaths() != null) {
            for (Path pathParam : apiInfo.getPaths()) {
                Parameter parameter = new Parameter();
                parameter.setName(pathParam.getName());
                parameter.setIn("path");
                parameter.setRequired(true);
                parameter.setDescription(pathParam.getDescription());

                Schema schema = new Schema();
                schema.setType(convertDataType(pathParam.getDataType()));
                if (pathParam.getValue() != null) {
                    schema.setExample(pathParam.getValue());
                }
                parameter.setSchema(schema);

                operation.addParametersItem(parameter);
            }
        }

        // Header参数 - 总是添加
        if (apiInfo.getHeaders() != null) {
            for (Header header : apiInfo.getHeaders()) {
                Parameter parameter = new Parameter();
                parameter.setName(header.getName());
                parameter.setIn("header");
                parameter.setRequired(header.isRequired());
                parameter.setDescription(header.getDescription());

                Schema schema = new Schema();
                schema.setType(convertDataType(header.getDataType()));
                if (header.getValue() != null) {
                    schema.setExample(header.getValue());
                }
                parameter.setSchema(schema);

                operation.addParametersItem(parameter);
            }
        }
    }

    /**
     * 将Hutool JSON对象中的值类型转换为OpenAPI Schema类型
     * 
     * @param value a value from JSONObject or JSONArray
     * @return a string representing the schema type
     */
    private String convertJsonValueType(Object value) {
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Integer || value instanceof Long) {
            return "integer";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof JSONArray) {
            return "array";
        }
        if (value instanceof JSONObject) {
            return "object";
        }
        return "string";
    }

    private void addRequestBody(Operation operation, ApiInfo apiInfo, OpenAPI openApi) {
        String method = apiInfo.getMethod().toLowerCase();
        boolean shouldHaveRequestBody = "post".equals(method) || "put".equals(method) || "patch".equals(method);

        if (!shouldHaveRequestBody) {
            return; // GET、DELETE等方法不需要RequestBody
        }

        // 优先处理 requestBodyDefinition（结构化定义）
        BaseDefinition requestBodyDef = apiInfo.getRequestBodyDefinition();
        if (requestBodyDef != null && !CollectionUtils.isEmpty(requestBodyDef.getChildren())) {
            RequestBody requestBody = new RequestBody();
            requestBody.setRequired(true);
            requestBody.setDescription(StringUtils.defaultIfBlank(requestBodyDef.getDescription(), "请求体"));

            Content content = new Content();
            MediaType mediaType = new MediaType();

            Schema schema = buildSchemaFromDefinition(requestBodyDef, apiInfo, openApi, "Request");
            Object exampleObject = buildExampleFromDefinition(requestBodyDef);
            if (exampleObject != null) {
                schema.setExample(exampleObject);
            }

            mediaType.setSchema(schema);
            content.addMediaType("application/json", mediaType);
            requestBody.setContent(content);
            operation.setRequestBody(requestBody);
            return;
        }

        // 处理简单的requestBody字符串（使用Hutool进行优化）
        if (StringUtils.isNotBlank(apiInfo.getRequestBody())) {
            RequestBody requestBody = new RequestBody();
            requestBody.setRequired(true);
            requestBody.setDescription("请求体");

            Content content = new Content();
            MediaType mediaType = new MediaType();
            Schema schema = new Schema();
            Object bodyObj = parseJsonSafely(apiInfo.getRequestBody());

            if (bodyObj instanceof JSONArray) {
                schema.setType("array");
                JSONArray list = (JSONArray) bodyObj;
                // 如果数组不为空，且第一个元素是JSONObject，则尝试构建items的schema
                if (!list.isEmpty() && list.get(0) instanceof JSONObject) {
                    ObjectSchema itemSchema = new ObjectSchema();
                    JSONObject firstItem = (JSONObject) list.get(0);
                    for (Map.Entry<String, Object> entry : firstItem.entrySet()) {
                        Schema<?> propertySchema = new Schema<>();
                        propertySchema.setType(convertJsonValueType(entry.getValue())); // 优化：根据值的类型推断Schema类型
                        propertySchema.setExample(entry.getValue());
                        itemSchema.addProperty(entry.getKey(), propertySchema);
                    }
                    schema.setItems(itemSchema);
                } else {
                    // 否则，回退到原有逻辑
                    schema.setItems(new Schema().type("object"));
                }
            } else { // 包括 JSONObject 和其他无法解析的字符串
                schema.setType("object");
            }

            if (bodyObj != null) {
                schema.setExample(bodyObj);
            }

            mediaType.setSchema(schema);
            content.addMediaType("application/json", mediaType);
            requestBody.setContent(content);
            operation.setRequestBody(requestBody);
            return;
        }

        // 处理POST请求的参数，将其作为JSON请求体的属性
        if (apiInfo.getParameters() != null && !apiInfo.getParameters().isEmpty()) {
            RequestBody requestBody = new RequestBody();
            requestBody.setRequired(true);
            requestBody.setDescription("请求体");

            Content content = new Content();
            MediaType mediaType = new MediaType();

            ObjectSchema schema = new ObjectSchema();
            List<String> requiredFields = new ArrayList<>();
            Map<String, Object> exampleObject = new HashMap<>();

            for (org.ssssssss.magicapi.core.model.Parameter param : apiInfo.getParameters()) {
                Schema propertySchema = new Schema();
                propertySchema.setType(convertDataType(param.getDataType()));
                propertySchema.setDescription(param.getDescription());

                // 设置属性示例值
                Object exampleValue = param.getValue();
                if (exampleValue == null) {
                    // 根据数据类型生成默认示例值
                    exampleValue = generateDefaultExample(param.getDataType());
                }
                propertySchema.setExample(exampleValue);

                // 添加到整体示例对象中
                exampleObject.put(param.getName(), exampleValue);

                schema.addProperty(param.getName(), propertySchema);

                if (param.isRequired()) {
                    requiredFields.add(param.getName());
                }
            }

            if (!requiredFields.isEmpty()) {
                schema.setRequired(requiredFields);
            }

            // 只设置schema的example
            schema.setExample(exampleObject);

            mediaType.setSchema(schema);
            content.addMediaType("application/json", mediaType);
            requestBody.setContent(content);
            operation.setRequestBody(requestBody);
            return;
        }
        // 如果POST请求没有任何RequestBody定义和参数，创建一个空的对象RequestBody
        RequestBody requestBody = new RequestBody();
        requestBody.setRequired(false); // 这种情况保持false
        requestBody.setDescription("请求体");

        Content content = new Content();
        MediaType mediaType = new MediaType();

        Schema schema = new Schema();
        schema.setType("object");

        // 只设置schema的example
        schema.setExample(new HashMap<>());

        mediaType.setSchema(schema);
        content.addMediaType("application/json", mediaType);
        requestBody.setContent(content);
        operation.setRequestBody(requestBody);
    }

    private Schema<?> buildSchemaFromDefinition(BaseDefinition definition, ApiInfo apiInfo, OpenAPI openApi,
            String suffix) {
        if (definition == null) {
            return new ObjectSchema();
        }

        DataType dataType = definition.getDataType();

        if (dataType == DataType.Object
                || (dataType == DataType.Any && !CollectionUtils.isEmpty(definition.getChildren()))) {
            String schemaName = generateSchemaName(definition, apiInfo, suffix);

            if (openApi.getComponents().getSchemas() != null
                    && openApi.getComponents().getSchemas().containsKey(schemaName)) {
                // 修正点 1
                return new Schema<>().$ref("#/components/schemas/" + schemaName);
            }

            ObjectSchema objectSchema = new ObjectSchema();
            objectSchema.setDescription(definition.getDescription());
            List<String> requiredFields = new ArrayList<>();

            if (!CollectionUtils.isEmpty(definition.getChildren())) {
                for (BaseDefinition child : definition.getChildren()) {
                    String childSuffix = suffix + StringUtils.capitalize(child.getName());
                    Schema<?> propertySchema = buildSchemaFromDefinition(child, apiInfo, openApi, childSuffix);
                    objectSchema.addProperty(child.getName(), propertySchema);
                    if (child.isRequired()) {
                        requiredFields.add(child.getName());
                    }
                }
            }
            if (!requiredFields.isEmpty()) {
                objectSchema.setRequired(requiredFields);
            }

            openApi.getComponents().addSchemas(schemaName, objectSchema);
            // 修正点 2
            return new Schema<>().$ref("#/components/schemas/" + schemaName);

        } else if (dataType == DataType.Array) {
            ArraySchema arraySchema = new ArraySchema();
            arraySchema.setDescription(definition.getDescription());

            if (!CollectionUtils.isEmpty(definition.getChildren())) {
                BaseDefinition itemDefinition = definition.getChildren().get(0);
                String itemSuffix = suffix + "Item";
                Schema<?> itemSchema = buildSchemaFromDefinition(itemDefinition, apiInfo, openApi, itemSuffix);
                arraySchema.setItems(itemSchema);
            } else {
                arraySchema.setItems(new Schema<>().type("object"));
            }
            return arraySchema;

        } else {
            Schema<?> schema = new Schema<>();
            schema.setType(convertDataType(dataType));
            schema.setDescription(definition.getDescription());
            if (definition.getValue() != null) {
                schema.setExample(definition.getValue());
            }
            return schema;
        }
    }

    private String generateSchemaName(BaseDefinition definition, ApiInfo apiInfo, String suffix) {
        // 基础名称始终从API路径生成，以保证API间的隔离
        String baseName = Arrays.stream(apiInfo.getPath().split("[^a-zA-Z0-9]"))
                .filter(s -> !s.isEmpty())
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());

        // 直接组合基础名称和携带上下文的后缀
        // suffix现在可能是 "Response", "ResponseData", "ResponseDataItem" 等
        String finalName = baseName + suffix;

        // 如果名称为空（例如路径是'/'），则提供一个默认名称
        if (finalName.isEmpty()) {
            return "DefaultObject" + suffix;
        }

        return finalName;
    }

    /**
     * 从结构化定义生成示例对象
     */
    private Object buildExampleFromDefinition(BaseDefinition definition) {
        if (definition == null) {
            return null;
        }

        if (definition.getDataType() == DataType.Array) {
            List<Object> exampleArray = new ArrayList<>();

            if (!CollectionUtils.isEmpty(definition.getChildren())) {
                // 为数组生成一个示例项
                Object itemExample = buildExampleFromDefinition(definition.getChildren().get(0));
                if (itemExample != null) {
                    exampleArray.add(itemExample);
                }
            }

            return exampleArray;
        } else if (definition.getDataType() == DataType.Object || definition.getDataType() == DataType.Any) {
            Map<String, Object> exampleObject = new HashMap<>();

            if (!CollectionUtils.isEmpty(definition.getChildren())) {
                for (BaseDefinition child : definition.getChildren()) {
                    Object childExample = buildExampleFromDefinition(child);
                    if (childExample != null) {
                        exampleObject.put(child.getName(), childExample);
                    }
                }
            }

            return exampleObject;
        } else {
            // 基本类型，优先使用定义的值，否则使用默认示例
            if (definition.getValue() != null && !definition.getValue().toString().trim().isEmpty()) {
                return definition.getValue();
            } else {
                return generateDefaultExample(definition.getDataType());
            }
        }
    }

    private void addResponses(Operation operation, ApiInfo apiInfo, OpenAPI openApi) {
        ApiResponses responses = new ApiResponses();

        // 默认200响应
        ApiResponse response200 = new ApiResponse();
        response200.setDescription("成功");

        Content content = new Content();
        MediaType mediaType = new MediaType();

        // 处理响应体定义
        BaseDefinition responseBodyDef = apiInfo.getResponseBodyDefinition();
        if (responseBodyDef != null && !CollectionUtils.isEmpty(responseBodyDef.getChildren())) {
            Schema schema = buildSchemaFromDefinition(responseBodyDef, apiInfo, openApi, "Response");
            mediaType.setSchema(schema);
        } else {
            // 处理简单响应体
            Schema schema = new Schema();
            schema.setType("object");

            if (StringUtils.isNotBlank(apiInfo.getResponseBody())) {
                try {
                    Object responseObj = parseJsonSafely(apiInfo.getResponseBody());
                    if (responseObj instanceof List) {
                        schema.setType("array");
                        schema.setItems(new Schema().type("object"));
                    }
                    schema.setExample(responseObj);
                } catch (Exception e) {
                    schema.setExample(apiInfo.getResponseBody());
                }
            }

            mediaType.setSchema(schema);
        }

        content.addMediaType("application/json", mediaType);
        response200.setContent(content);
        responses.addApiResponse("200", response200);

        // 添加错误响应
        ApiResponse response500 = new ApiResponse();
        response500.setDescription("服务器错误");
        responses.addApiResponse("500", response500);

        operation.setResponses(responses);
    }

    private void setOperationByMethod(PathItem pathItem, String method, Operation operation) {
        switch (method) {
            case "get":
                pathItem.setGet(operation);
                break;
            case "post":
                pathItem.setPost(operation);
                break;
            case "put":
                pathItem.setPut(operation);
                break;
            case "delete":
                pathItem.setDelete(operation);
                break;
            case "patch":
                pathItem.setPatch(operation);
                break;
            case "head":
                pathItem.setHead(operation);
                break;
            case "options":
                pathItem.setOptions(operation);
                break;
        }
    }

    private String convertDataType(DataType dataType) {
        if (dataType == null) {
            return "string";
        }

        switch (dataType) {
            case String:
                return "string";
            case Boolean:
                return "boolean";
            case Array:
                return "array";
            case Object:
            case Any:
                return "object";
            case Integer:
            case Long:
            case Float:
            case Double:
            case Date:
            default:
                return "string";
        }
    }

    private Object parseJsonSafely(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSONUtil.parse(json);
        } catch (Exception e) {
            // 解析失败时返回原始字符串，以作兼容
            return json;
        }
    }

    /**
     * 根据数据类型生成默认示例值
     */
    private Object generateDefaultExample(DataType dataType) {
        if (dataType == null) {
            return "string";
        }

        switch (dataType) {
            case String:
                return "string";
            case Boolean:
                return true;
            case Array:
                return new ArrayList<>();
            case Object:
            case Any:
                return new HashMap<>();
            case Integer:
            case Long:
                return 0;
            case Float:
            case Double:
                return 0.0;
            case Date:
                return "2023-01-01";
            default:
                return "string";
        }
    }
}