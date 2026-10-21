package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Tudo com @ é uma anotação, é como se fosse um adesivo na classe ou um metodo que dá superpoderes para ela. Esta associação no caso, diz para o SpringBoot para ler o arquivo application.properties e vasculhar todas as classes dentro do pacote com.example.demo procurando outros adesivos para ligar tudo sozinho

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}
