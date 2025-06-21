package com.xhx.userservice.config;

import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author master
 */
@Configuration
public class SeataDataSourceProxyConfig {
    @Bean("dataSource")
    public DataSource dataSource(@Qualifier("shardingSphereDataSource") DataSource shardingSphereDataSource) {
        return new DataSourceProxy(shardingSphereDataSource);
    }
} 