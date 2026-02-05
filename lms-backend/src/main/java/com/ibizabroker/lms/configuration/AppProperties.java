package com.ibizabroker.lms.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds custom application properties under the `app.*` prefix.
 * Declaring these properties improves IDE tooling (spring-boot metadata)
 * and centralizes defaults.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Async async = new Async();
    private Api api = new Api();
    private String allowedOrigins;

    public Async getAsync() {
        return async;
    }

    public void setAsync(Async async) {
        this.async = async;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public static class Async {
        private int corePoolSize = 4;
        private int maxPoolSize = 8;
        private int queueCapacity = 200;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Api {
        private String devUrl;
        private String prodUrl;

        public String getDevUrl() {
            return devUrl;
        }

        public void setDevUrl(String devUrl) {
            this.devUrl = devUrl;
        }

        public String getProdUrl() {
            return prodUrl;
        }

        public void setProdUrl(String prodUrl) {
            this.prodUrl = prodUrl;
        }
    }
}
