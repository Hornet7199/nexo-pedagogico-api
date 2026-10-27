package com.nexo.pedagogicoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PedagogicoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedagogicoApiApplication.class, args);
    }
}
