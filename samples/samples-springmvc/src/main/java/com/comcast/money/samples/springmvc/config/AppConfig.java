package com.comcast.money.samples.springmvc.config;

import com.comcast.money.core.concurrent.TraceFriendlyThreadPoolExecutor;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;

@Configuration
@ComponentScan(basePackages = {"com.comcast.money.samples.springmvc", "com.comcast.money.spring"})
public class AppConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService nestedServiceExecutor() {
        return TraceFriendlyThreadPoolExecutor.newCachedThreadPool();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService rootServiceExecutor(){
        return TraceFriendlyThreadPoolExecutor.newCachedThreadPool();
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        final DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        defaultAdvisorAutoProxyCreator.setProxyTargetClass(true);
        return defaultAdvisorAutoProxyCreator;
    }
}
