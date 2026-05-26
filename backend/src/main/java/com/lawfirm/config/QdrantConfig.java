package com.lawfirm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {
    private boolean enabled = false;
    private String host = "localhost";
    private int port = 6333;
    private int grpcPort = 6334;
    private String collectionName = "lawfirm-knowledge";
    private int vectorSize = 1024;
    private String distance = "Cosine";
    private int timeoutSeconds = 10;

    public String getHttpUrl() {
        return String.format("http://%s:%d", host, port);
    }

    public String getGrpcUrl() {
        return String.format("%s:%d", host, grpcPort);
    }
}
