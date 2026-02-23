package com.lyj.dada;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lyj.dada.mapper")
public class DadaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DadaApplication.class, args);
    }

}
