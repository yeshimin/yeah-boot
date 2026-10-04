package com.yeshimin.yeahboot.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.yeshimin.yeahboot"})
public class YeahAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(YeahAppApplication.class, args);
    }
}
