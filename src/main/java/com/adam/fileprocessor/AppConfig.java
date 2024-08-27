package com.adam.fileprocessor;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@EnableJdbcRepositories
public class AppConfig  {

    @Autowired
    private HikariDataSource dataSource;

    @Bean
    public DataSourceInfoProvider dataSourceInfoProvider() {
        return new DataSourceInfoProvider() {

            @Override
            public DataSourceInfo getDataSourceInfo() {
                return new DataSourceInfo(
                        dataSource.getJdbcUrl(),
                        dataSource.getMaximumPoolSize(),
                        dataSource.getConnectionTimeout(),
                        new HikariDataSourcePoolMetadata(dataSource).getActive()
                );
            }
        };
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DataSourceInfo {
        private String url;
        private int maxPoolSize;
        private long connectiontimeout;
        private Integer activeConnection;
    }

    public static interface DataSourceInfoProvider {
        DataSourceInfo getDataSourceInfo();
    }
}
