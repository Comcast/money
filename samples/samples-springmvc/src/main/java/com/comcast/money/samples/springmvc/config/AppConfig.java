package com.comcast.money.samples.springmvc.config;

import java.util.concurrent.ExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;

import com.comcast.money.api.TraceFriendlyExecutors;
import com.comcast.money.spring.TracedMethodInterceptor;
import com.comcast.money.spring.TracedMethodAdvisor;
import com.comcast.money.spring.SpringTracer;

@Configuration
@ComponentScan(basePackages = {"com.comcast.money.samples.springmvc", "com.comcast.money.spring"})
public class AppConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService asyncRootService() {
        return TraceFriendlyExecutors.newCachedThreadPool();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService asyncNestedService(){
        return TraceFriendlyExecutors.newCachedThreadPool();
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        final DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator = new DefaultAdvisorAutoProxyCreator();
        defaultAdvisorAutoProxyCreator.setProxyTargetClass(true);
        return defaultAdvisorAutoProxyCreator;
    }
}
