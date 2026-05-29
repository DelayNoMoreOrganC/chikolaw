package com.lawfirm.config;

import org.apache.coyote.AbstractProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 线程与连接 tuning，面向约 50 并发用户。
 */
@Configuration
public class TomcatPerformanceConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer(
            LawfirmPerformanceProperties properties) {
        return factory -> factory.addConnectorCustomizers(connector -> {
            if (connector.getProtocolHandler() instanceof AbstractProtocol) {
                AbstractProtocol<?> protocol = (AbstractProtocol<?>) connector.getProtocolHandler();
                LawfirmPerformanceProperties.Tomcat tomcat = properties.getTomcat();
                protocol.setMaxThreads(tomcat.getMaxThreads());
                protocol.setMinSpareThreads(tomcat.getMinSpareThreads());
                protocol.setAcceptCount(tomcat.getAcceptCount());
                protocol.setMaxConnections(tomcat.getMaxConnections());
            }
        });
    }
}
