package com.lawfirm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置
 * 配置超时时间和连接池
 */
@Configuration
public class RestTemplateConfig {

    @Value("${llm.timeout:30000}")
    private int timeout;

    @Value("${llm.connect-timeout:10000}")
    private int connectTimeout;

    /**
     * 配置RestTemplate Bean
     * 设置连接超时和读取超时
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(timeout);

        return new RestTemplate(factory);
    }
}
