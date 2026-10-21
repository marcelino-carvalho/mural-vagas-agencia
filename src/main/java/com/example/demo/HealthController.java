package com.example.demo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// por meio desta Anotação (@RestController), o @SpringBootApplication ao encontrá-la ja percebe q a classe HealthController é como se fosse o recepcionista da web. Tudo que os métodos dessa classe devolverem (return) será enviado diretamente pela internet para quem fez o pedido HTTP

@RestController
public class HealthController {

    @GetMapping(value = "/api/health", produces = MediaType.TEXT_PLAIN_VALUE)
    public String health() {
        return "ok";
    }
}
