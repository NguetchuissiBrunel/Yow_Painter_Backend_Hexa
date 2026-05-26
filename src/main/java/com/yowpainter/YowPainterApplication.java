package com.yowpainter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YowPainterApplication {

    public static void main(String[] args) {
        SpringApplication.run(YowPainterApplication.class, args);
    }

}
