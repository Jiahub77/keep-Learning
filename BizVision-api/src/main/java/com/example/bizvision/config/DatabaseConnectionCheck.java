package com.example.bizvision.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

//@Component
public class DatabaseConnectionCheck implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String database = jdbcTemplate.queryForObject(
                "SELECT DATABASE()", String.class);

        System.out.println("数据库连接成功，当前库：" + database);
    }
}