package com.hillcommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.hillcommerce.modules.**.mapper")
public class HillCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HillCommerceApplication.class, args);
    }

    @Bean
    DefaultIdentifierGenerator identifierGenerator() {
        return new DefaultIdentifierGenerator();
    }
}
