package com.becnotificationsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BeCNotificationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeCNotificationSystemApplication.class, args);
    }

}
