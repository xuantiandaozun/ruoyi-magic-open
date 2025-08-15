package com.ruoyi.project.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Hutool AI配置类
 * 用于管理多种AI模型的配置信息
 * 
 * @author ruoyi-magic
 * @date 2024-12-15
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
public class HutoolAiConfig {
    
    /**
     * 默认AI服务类型
     */
    private Default defaultConfig = new Default();
    
    /**
     * 豆包AI配置
     */
    private Doubao doubao = new Doubao();
    
    /**
     * OpenAI配置
     */
    private OpenAi openai = new OpenAi();
    
    /**
     * DeepSeek配置
     */
    private DeepSeek deepseek = new DeepSeek();
    
    /**
     * 默认配置
     */
    public static class Default {
        /**
         * 默认使用的AI服务类型：DOUBAO、OPENAI、DEEPSEEK
         */
        private String type = "DOUBAO";
        
        /**
         * 请求超时时间（秒）
         */
        private int timeout = 30;
        
        /**
         * 最大重试次数
         */
        private int maxRetries = 3;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
        
        public int getMaxRetries() {
            return maxRetries;
        }
        
        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }
    
    /**
     * 豆包AI配置
     */
    public static class Doubao {
        /**
         * API密钥
         */
        private String apiKey;
        
        /**
         * API端点
         */
        private String endpoint = "https://ark.cn-beijing.volces.com/api/v3";
        
        /**
         * 模型名称
         */
        private String model = "ep-20241212105607-kcmvs";
        
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 温度参数（0.0-2.0）
         */
        private double temperature = 0.7;
        
        /**
         * 最大Token数
         */
        private int maxTokens = 4096;
        
        /**
         * 视觉模型名称
         */
        private String visionModel = "ep-20241212105607-kcmvs";
        
        /**
         * 向量化模型名称
         */
        private String embeddingModel = "ep-20241212105607-kcmvs";
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public String getVisionModel() {
            return visionModel;
        }
        
        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }
        
        public String getEmbeddingModel() {
            return embeddingModel;
        }
        
        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }
    }
    
    /**
     * OpenAI配置
     */
    public static class OpenAi {
        /**
         * API密钥
         */
        private String apiKey;
        
        /**
         * API端点
         */
        private String endpoint = "https://api.openai.com/v1";
        
        /**
         * 模型名称
         */
        private String model = "gpt-3.5-turbo";
        
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 温度参数（0.0-2.0）
         */
        private double temperature = 0.7;
        
        /**
         * 最大Token数
         */
        private int maxTokens = 4096;
        
        /**
         * 组织ID
         */
        private String organization;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public String getOrganization() {
            return organization;
        }
        
        public void setOrganization(String organization) {
            this.organization = organization;
        }
    }
    
    /**
     * DeepSeek配置
     */
    public static class DeepSeek {
        /**
         * API密钥
         */
        private String apiKey;
        
        /**
         * API端点
         */
        private String endpoint = "https://api.deepseek.com/v1";
        
        /**
         * 模型名称
         */
        private String model = "deepseek-chat";
        
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 温度参数（0.0-2.0）
         */
        private double temperature = 0.7;
        
        /**
         * 最大Token数
         */
        private int maxTokens = 4096;
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double getTemperature() {
            return temperature;
        }
        
        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
    
    // Getters and Setters
    public Default getDefaultConfig() {
        return defaultConfig;
    }
    
    public void setDefaultConfig(Default defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
    
    public Doubao getDoubao() {
        return doubao;
    }
    
    public void setDoubao(Doubao doubao) {
        this.doubao = doubao;
    }
    
    public OpenAi getOpenai() {
        return openai;
    }
    
    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }
    
    public DeepSeek getDeepseek() {
        return deepseek;
    }
    
    public void setDeepseek(DeepSeek deepseek) {
        this.deepseek = deepseek;
    }
}