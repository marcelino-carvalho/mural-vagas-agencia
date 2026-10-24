package br.gov.curitiba.mural;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada principal da aplicação Spring Boot.
 * Inicializa o contexto do Spring, pools de conexões e componentes do sistema
 * do Mural de Vagas da Agência do Trabalhador de Curitiba.
 */
@SpringBootApplication
public class MuralApplication {

    public static void main(String[] args) {
        SpringApplication.run(MuralApplication.class, args);
    }
}
