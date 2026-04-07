package com.sliit.smartcampus;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartCampusApplication {

    public static void main(String[] args) {
        applyDotenv("./");
        applyDotenv("../");
        SpringApplication.run(SmartCampusApplication.class, args);
    }

    private static void applyDotenv(String directory) {
        Dotenv dotenv = Dotenv.configure()
                .directory(directory)
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        dotenv.entries().forEach(e -> {
            String v = e.getValue();
            if (v != null && !v.isBlank()) {
                System.setProperty(e.getKey(), v);
            }
        });
    }
}
