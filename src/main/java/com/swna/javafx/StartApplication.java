package com.swna.javafx;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class StartApplication {
    public static void main(String[] args) {
        Application.launch(JavafxApplication.class, args);
    }
}
