package com.ruoyi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import com.ruoyi.project.gen.tools.ai.AiDatabaseTableTool;
import com.ruoyi.project.gen.tools.ai.ConfigTool;
import com.ruoyi.project.gen.tools.ai.DataOperationTool;
import com.ruoyi.project.gen.tools.ai.DataSourceTool;
import com.ruoyi.project.gen.tools.ai.DictTool;
import com.ruoyi.project.gen.tools.ai.FileOperationTool;
import com.ruoyi.project.gen.tools.ai.MenuTool;

/**
 * 启动程序
 *
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@MapperScan(basePackages = {"com.ruoyi.project.**.mapper"})
@EnableAsync
public class RuoYiApplication
{
    public static void main(String[] args)
    {
        ConfigurableApplicationContext context = SpringApplication.run(RuoYiApplication.class, args);
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        System.out.println("(♥◠‿◠)ﾉﾞ  magic启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                "http://localhost:" + port + contextPath + "doc.html\n" +
                "http://127.0.0.1:" + port + contextPath + "doc.html");
    }

    /**
     * map 工具构建
     * @param openMeteoService
     * @param configTool
     * @param dataOperationTool
     * @param dataSourceTool
     * @param dictTool
     * @param fileOperationTool
     * @param menuTool
     * @return
     */
    @Bean
    public ToolCallbackProvider mcpTools(AiDatabaseTableTool openMeteoService,
    ConfigTool configTool,
    DataOperationTool dataOperationTool,
    DataSourceTool dataSourceTool,
    DictTool dictTool,
    FileOperationTool fileOperationTool,
    MenuTool menuTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(openMeteoService,configTool,dataOperationTool,dataSourceTool,dictTool,fileOperationTool,menuTool)
                .build();
    }


}
