package br.gov.curitiba.mural.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa uma vaga de emprego disponível no sistema.
 * Mapeia a tabela 'vaga' no banco de dados PostgreSQL.
 */
@Entity
@Table(name = "vaga")
@Getter
@Setter
@NoArgsConstructor
public class Vaga {

    /** Módulo padrão de exibição de vagas convencionais */
    public static final String MODULO_VAGAS = "VAGAS";

    /** Módulo específico para eventos e mutirões de emprego */
    public static final String MODULO_MUTIROES = "MUTIROES";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Título ou cargo da vaga (ex: "Auxiliar Administrativo") */
    private String titulo;

    /** Remuneração oferecida ou faixa salarial */
    private String salario;

    /** Nível de escolaridade exigido */
    private String escolaridade;

    /**
     * Descrição livre da vaga.
     * Mantido para retrocompatibilidade com o banco de dados existente.
     */
    private String descricao;

    /** Categoria da vaga (ex: CONVENCIONAL, SUPERIOR, PCD) */
    private String categoria;

    /** Código interno ou identificador da vaga */
    private String codigo;

    /** Nome da empresa contratante */
    private String empresa;

    /** Módulo ao qual a vaga pertence (VAGAS ou MUTIROES) */
    private String modulo;

    /** Identificador da tela/painel onde será exibida no mutirão (ex: 1, 2 ou 3) */
    private String tela;

    /** Localidade ou bairro do posto de trabalho */
    private String local;

    /** Benefícios concedidos pela empresa */
    private String beneficios;

    /** Atribuições e responsabilidades detalhadas do cargo (até 4000 caracteres) */
    @Column(length = 4000)
    private String atribuicoes;

    /** Data limite de validade do anúncio da vaga */
    private String validade;

    /** Indica se requer experiência prévia ("Sim" ou "Não") */
    private String experiencia;

    /** Horário ou jornada de expediente da vaga (ex: "Seg a Sex, das 08h00 às 18h00") */
    @Column (length = 50)
    private String expediente;
}
