package com.example.smartshelfx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class  SmartshelfxApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartshelfxApplication.class, args);
	}

}
