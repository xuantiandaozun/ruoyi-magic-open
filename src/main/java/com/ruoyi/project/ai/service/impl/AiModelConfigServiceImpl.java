package com.ruoyi.project.ai.service.impl;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiModelPrice;
import com.ruoyi.project.ai.mapper.AiModelConfigMapper;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiModelPriceService;
import com.ruoyi.project.system.domain.SysConfig;
import com.ruoyi.project.system.service.ISysConfigService;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

@Service
@UseDataSource("MASTER")
public class AiModelConfigServiceImpl extends ServiceImpl<AiModelConfigMapper, AiModelConfig>
        implements IAiModelConfigService {

    private static final String OPENROUTER_PROVIDER = "openrouter";
    private static final String OPENROUTER_MODELS_URL = "https://openrouter.ai/api/v1/models";
    private static final String OPENROUTER_MODELS_COUNT_URL = "https://openrouter.ai/api/v1/models/count";
    private static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String OPENROUTER_KEY_REF = "config:ai.openrouter.apiKey";
    private static final String OPENROUTER_COUNT_CONFIG_KEY = "ai.openrouter.freeModels.lastCount";
    private static final String OPENROUTER_API_KEY_CONFIG_KEY = "ai.openrouter.apiKey";

    private final IAiModelPriceService modelPriceService;
    private final ISysConfigService sysConfigService;

    @Value("${openrouter.free-model-sync.user-agent:RuoyiMagic/1.0}")
    private String openRouterUserAgent;

    @Value("${openrouter.free-model-sync.skip-when-count-unchanged:true}")
    private boolean skipWhenCountUnchanged;

    public AiModelConfigServiceImpl(IAiModelPriceService modelPriceService, ISysConfigService sysConfigService) {
        this.modelPriceService = modelPriceService;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public List<AiModelConfig> listEnabledByProviderAndCapability(String provider, String capability) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("provider").eq(provider))
            .and(new QueryColumn("capability").eq(capability))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("del_flag").eq("0"));
        return list(qw);
    }

    @Override
    public List<AiModelConfig> listEnabledByCapability(String capability) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("capability").eq(capability))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"));
        return list(qw);
    }

    @Override
    public AiModelConfig getDefaultByProviderAndCapability(String provider, String capability) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("provider").eq(provider))
            .and(new QueryColumn("capability").eq(capability))
            .and(new QueryColumn("is_default").eq("Y"))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"));
        return getOne(qw);
    }

    @Override
    public AiModelConfig getEnabledByModel(String model) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("model").eq(model))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("status").eq("0"))
            .and(new QueryColumn("del_flag").eq("0"));
        return getOne(qw);
    }

    @Override
    @Transactional
    public boolean setDefault(Long configId) {
        AiModelConfig target = getById(configId);
        if (target == null) {
            return false;
        }
        
        // 如果目标配置已经是默认配置，直接返回成功
        if ("Y".equals(target.getIsDefault())) {
            return true;
        }
        
        // 将所有其他默认配置置为N（排除当前配置）
        QueryWrapper others = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("is_default").eq("Y"))
            .and(new QueryColumn("del_flag").eq("0"))
            .and(new QueryColumn("id").ne(configId)); // 排除当前配置
            
        list(others).forEach(cfg -> {
            cfg.setIsDefault("N");
            updateById(cfg);
        });
        
        // 设置目标配置为默认
        target.setIsDefault("Y");
        return updateById(target);
    }

    @Override
    @Transactional
    public Map<String, Object> syncOpenRouterFreeModels() {
        ensureOpenRouterApiKeyConfig();
        Integer remoteCount = fetchOpenRouterModelCount();
        String lastCount = sysConfigService.selectConfigByKey(OPENROUTER_COUNT_CONFIG_KEY);
        if (skipWhenCountUnchanged && remoteCount != null && String.valueOf(remoteCount).equals(lastCount)) {
            Map<String, Object> skipped = new LinkedHashMap<>();
            skipped.put("skipped", true);
            skipped.put("reason", "model_count_unchanged");
            skipped.put("remoteCount", remoteCount);
            skipped.put("freeTotal", listOpenRouterFreeConfigs().size());
            skipped.put("syncedAt", new Date());
            return skipped;
        }

        JSONArray remoteModels = fetchOpenRouterModels();
        List<JSONObject> freeModels = new ArrayList<>();
        for (Object item : remoteModels) {
            if (!(item instanceof JSONObject model)) {
                continue;
            }
            if (isFreeModel(model)) {
                freeModels.add(model);
            }
        }

        int inserted = 0;
        int updated = 0;
        int disabled = 0;
        Set<String> syncedModelIds = freeModels.stream()
            .map(model -> model.getStr("id"))
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toSet());

        for (JSONObject remoteModel : freeModels) {
            String modelId = remoteModel.getStr("id");
            if (StrUtil.isBlank(modelId)) {
                continue;
            }

            AiModelConfig config = findOpenRouterConfig(modelId);
            boolean exists = config != null;
            if (!exists) {
                config = new AiModelConfig();
                config.setProvider(OPENROUTER_PROVIDER);
                config.setModel(modelId);
                config.setApiKeyRef(OPENROUTER_KEY_REF);
                config.setIsDefault("N");
                config.setCreateBy("system");
            }

            applyRemoteModel(config, remoteModel);
            if (exists) {
                updateById(config);
                updated++;
            } else {
                save(config);
                inserted++;
            }
            upsertFreeModelPrice(config, remoteModel);
        }

        List<AiModelConfig> existingFreeConfigs = listOpenRouterFreeConfigs();
        for (AiModelConfig config : existingFreeConfigs) {
            if (!syncedModelIds.contains(config.getModel()) && "Y".equals(config.getEnabled())) {
                config.setEnabled("N");
                config.setStatus("1");
                config.setUpdateBy("system");
                config.setRemark("OpenRouter 免费模型同步未返回，已自动停用");
                updateById(config);
                disabled++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("remoteTotal", remoteModels.size());
        result.put("freeTotal", freeModels.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("disabled", disabled);
        result.put("remoteCount", remoteCount);
        result.put("syncedAt", new Date());
        saveOpenRouterModelCount(remoteCount);
        return result;
    }

    private Integer fetchOpenRouterModelCount() {
        try {
            HttpResponse<String> response = sendOpenRouterGet(OPENROUTER_MODELS_COUNT_URL);
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONObject data = body.getJSONObject("data");
            return data == null ? null : data.getInt("count");
        } catch (Exception e) {
            return null;
        }
    }

    private JSONArray fetchOpenRouterModels() {
        try {
            HttpResponse<String> response = sendOpenRouterGet(OPENROUTER_MODELS_URL);
            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray data = body.getJSONArray("data");
            if (data == null) {
                throw new IllegalStateException("OpenRouter 模型接口响应缺少 data 字段");
            }
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenRouter 模型接口请求被中断", e);
        } catch (Exception e) {
            throw new IllegalStateException("OpenRouter 模型接口请求失败", e);
        }
    }

    private HttpResponse<String> sendOpenRouterGet(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/json")
            .header("User-Agent", openRouterUserAgent)
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenRouter 接口返回异常: status=" + response.statusCode());
        }
        return response;
    }

    private void saveOpenRouterModelCount(Integer remoteCount) {
        if (remoteCount == null) {
            return;
        }
        QueryWrapper qw = QueryWrapper.create()
            .from("sys_config")
            .where(new QueryColumn("config_key").eq(OPENROUTER_COUNT_CONFIG_KEY));
        SysConfig config = sysConfigService.getOne(qw);
        if (config == null) {
            config = new SysConfig();
            config.setConfigName("OpenRouter免费模型数量缓存");
            config.setConfigKey(OPENROUTER_COUNT_CONFIG_KEY);
            config.setConfigType("N");
            config.setCreateBy("system");
            config.setDelFlag("0");
        }
        config.setConfigValue(String.valueOf(remoteCount));
        config.setUpdateBy("system");
        config.setRemark("OpenRouter 免费模型同步任务自动维护");
        if (config.getConfigId() == null) {
            sysConfigService.save(config);
        } else {
            sysConfigService.updateById(config);
        }
        sysConfigService.resetConfigCache();
    }

    private void ensureOpenRouterApiKeyConfig() {
        QueryWrapper qw = QueryWrapper.create()
            .from("sys_config")
            .where(new QueryColumn("config_key").eq(OPENROUTER_API_KEY_CONFIG_KEY));
        SysConfig config = sysConfigService.getOne(qw);
        if (config != null) {
            return;
        }
        config = new SysConfig();
        config.setConfigName("OpenRouter API Key");
        config.setConfigKey(OPENROUTER_API_KEY_CONFIG_KEY);
        config.setConfigValue("");
        config.setConfigType("N");
        config.setCreateBy("system");
        config.setUpdateBy("system");
        config.setRemark("OpenRouter 免费模型与调用链路使用的 API Key，请在系统参数中维护");
        config.setDelFlag("0");
        sysConfigService.save(config);
        sysConfigService.resetConfigCache();
    }

    private boolean isFreeModel(JSONObject model) {
        String modelId = model.getStr("id");
        if ("openrouter/free".equals(modelId) || StrUtil.endWith(modelId, ":free")) {
            return true;
        }
        JSONObject pricing = model.getJSONObject("pricing");
        if (pricing == null) {
            return false;
        }
        return isZeroPrice(pricing.getStr("prompt"))
            && isZeroPrice(pricing.getStr("completion"))
            && isZeroOrBlankPrice(pricing.getStr("request"))
            && isZeroOrBlankPrice(pricing.getStr("image"));
    }

    private boolean isZeroOrBlankPrice(String value) {
        return StrUtil.isBlank(value) || isZeroPrice(value);
    }

    private boolean isZeroPrice(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        try {
            return new BigDecimal(value).compareTo(BigDecimal.ZERO) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private AiModelConfig findOpenRouterConfig(String modelId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("provider").eq(OPENROUTER_PROVIDER))
            .and(new QueryColumn("model").eq(modelId))
            .and(new QueryColumn("del_flag").eq("0"));
        return getOne(qw);
    }

    private List<AiModelConfig> listOpenRouterFreeConfigs() {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_config")
            .where(new QueryColumn("provider").eq(OPENROUTER_PROVIDER))
            .and(new QueryColumn("free_available").eq("Y"))
            .and(new QueryColumn("del_flag").eq("0"));
        return list(qw);
    }

    private void applyRemoteModel(AiModelConfig config, JSONObject remoteModel) {
        JSONObject architecture = remoteModel.getJSONObject("architecture");
        JSONArray inputModalities = architecture == null ? null : architecture.getJSONArray("input_modalities");
        JSONArray outputModalities = architecture == null ? null : architecture.getJSONArray("output_modalities");
        JSONObject topProvider = remoteModel.getJSONObject("top_provider");

        config.setCapability(resolveCapability(outputModalities));
        config.setEndpoint(OPENROUTER_BASE_URL);
        config.setExtraParams(buildExtraParams(remoteModel));
        config.setContextWindow(firstNonNull(remoteModel.getInt("context_length"),
            topProvider == null ? null : topProvider.getInt("context_length")));
        config.setMaxOutputTokens(topProvider == null ? null : topProvider.getInt("max_completion_tokens"));
        config.setSupportsStream("Y");
        config.setSupportsVision(contains(inputModalities, "image") ? "Y" : "N");
        config.setSupportsCache("N");
        config.setFreeAvailable("Y");
        config.setEnabled("Y");
        config.setStatus("0");
        config.setDelFlag("0");
        config.setUpdateBy("system");
        config.setRemark("OpenRouter 免费模型自动同步: " + remoteModel.getStr("name", config.getModel()));
    }

    private String resolveCapability(JSONArray outputModalities) {
        if (contains(outputModalities, "image")) {
            return "image";
        }
        return "chat";
    }

    private String buildExtraParams(JSONObject remoteModel) {
        JSONObject extra = new JSONObject();
        extra.set("syncSource", "openrouter_free_models");
        extra.set("openrouterId", remoteModel.getStr("id"));
        extra.set("name", remoteModel.getStr("name"));
        extra.set("description", remoteModel.getStr("description"));
        extra.set("canonicalSlug", remoteModel.getStr("canonical_slug"));
        extra.set("created", remoteModel.get("created"));
        extra.set("pricing", remoteModel.get("pricing"));
        extra.set("architecture", remoteModel.get("architecture"));
        extra.set("topProvider", remoteModel.get("top_provider"));
        extra.set("supportedParameters", remoteModel.get("supported_parameters"));
        extra.set("perRequestLimits", remoteModel.get("per_request_limits"));
        extra.set("links", remoteModel.get("links"));
        extra.set("syncedAt", new Date());
        return JSONUtil.toJsonStr(extra);
    }

    private void upsertFreeModelPrice(AiModelConfig config, JSONObject remoteModel) {
        AiModelPrice price = findOpenRouterPrice(config.getModel());
        boolean exists = price != null;
        if (!exists) {
            price = new AiModelPrice();
            price.setProvider(OPENROUTER_PROVIDER);
            price.setModelName(config.getModel());
            price.setCurrency("USD");
            price.setEffectiveTime(new Date());
            price.setCreateBy("system");
        }
        price.setModelConfigId(config.getId());
        price.setInputPricePer1mTokens(BigDecimal.ZERO);
        price.setOutputPricePer1mTokens(BigDecimal.ZERO);
        price.setCachedInputPricePer1mTokens(BigDecimal.ZERO);
        price.setImagePricePerUnit(BigDecimal.ZERO);
        price.setEnabled("Y");
        price.setSourceUrl(OPENROUTER_MODELS_URL);
        price.setUpdateBy("system");
        price.setRemark("OpenRouter 免费模型价格自动同步: " + remoteModel.getStr("name", config.getModel()));
        price.setDelFlag("0");
        if (exists) {
            modelPriceService.updateById(price);
        } else {
            modelPriceService.save(price);
        }
    }

    private AiModelPrice findOpenRouterPrice(String modelId) {
        QueryWrapper qw = QueryWrapper.create()
            .from("ai_model_price")
            .where(new QueryColumn("provider").eq(OPENROUTER_PROVIDER))
            .and(new QueryColumn("model_name").eq(modelId))
            .and(new QueryColumn("enabled").eq("Y"))
            .and(new QueryColumn("del_flag").eq("0"));
        return modelPriceService.getOne(qw);
    }

    private boolean contains(JSONArray array, String value) {
        return array != null && array.stream().anyMatch(item -> value.equals(String.valueOf(item)));
    }

    private Integer firstNonNull(Integer first, Integer second) {
        return first != null ? first : second;
    }
}
