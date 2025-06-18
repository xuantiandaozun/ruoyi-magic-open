package com.ruoyi.project.gen.config;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * resolve the request timeout issue
 */

@AutoConfiguration
public class RestConfiguration {

	@Bean
	public RestClient.Builder createRestClient() {
		RequestConfig requestConfig = RequestConfig.custom()
				.setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
				.setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES))
				.build();

		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		return RestClient.builder().requestFactory(requestFactory);
	}

}
