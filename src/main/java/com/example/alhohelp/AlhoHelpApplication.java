package com.example.alhohelp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class AlhoHelpApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlhoHelpApplication.class, args);
	}

}
