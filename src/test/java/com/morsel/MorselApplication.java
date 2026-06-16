package com.morsel;

import org.springframework.boot.SpringApplication;

public class MorselApplication {

    static void main(String[] args) {
        SpringApplication.from(Application::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
