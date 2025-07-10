package de.unistuttgart.iste.meitrex.tutor_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
@Slf4j
public class TutorServiceApplication {

    public static void main(String[] args) {
        Arrays.stream(args).map(arg -> "Received argument: " + arg).forEach(log::info);
        SpringApplication.run(TutorServiceApplication.class, args);
    }

}
