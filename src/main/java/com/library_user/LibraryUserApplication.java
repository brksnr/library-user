package com.library_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibraryUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryUserApplication.class, args);
	}

}
