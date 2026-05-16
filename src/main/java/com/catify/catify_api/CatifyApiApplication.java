package com.catify.catify_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatifyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatifyApiApplication.class, args);
		System.out.println("Catify API is running...");
	}

}
