package com.ruoyi.framework.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

   
    @Autowired
    private ChatClient.Builder chatClientBuilder;

 

    @Bean
    public ChatClient customChatClient() {

        return chatClientBuilder.build();
    }

}
