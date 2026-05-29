package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lawfirm.performance")
public class LawfirmPerformanceProperties {

    private Tomcat tomcat = new Tomcat();

    @Data
    public static class Tomcat {
        private int maxThreads = 200;
        private int minSpareThreads = 20;
        private int acceptCount = 100;
        private int maxConnections = 10000;
    }
}
