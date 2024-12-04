package com.ba.escuchador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class GPISwiftV5Application extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(GPISwiftV5Application.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application){
		return application.sources(GPISwiftV5Application.class);
	}
}