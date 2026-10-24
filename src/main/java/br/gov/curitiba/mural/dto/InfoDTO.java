package br.gov.curitiba.mural.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de transferência de dados para as informações institucionais,
 * cursos e processos seletivos exibidos nos murais e na TV.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfoDTO {

    /** Lista de cursos profissionalizantes disponíveis */
    private List<Curso> cursos = new ArrayList<>();

    /** Lista de processos seletivos programados */
    private List<ProcessoItem> processos = new ArrayList<>();

    /** Nome da empresa para exibição direta/retrocompatibilidade */
    private String empresaNome = "";

    /** Data do evento ou processo da empresa */
    private String empresaData = "";

    /** Vagas ofertadas no processo seletivo em destaque */
    private List<String> processoVagas = new ArrayList<>();

    /** Cronograma de datas e eventos da agência */
    private List<CronogramaItem> cronograma = new ArrayList<>();

    /** Título do cabeçalho da Tela 1 de mutirões */
    private String mutiraoTituloTela1 = "";

    /** Título do cabeçalho da Tela 2 de mutirões */
    private String mutiraoTituloTela2 = "";

    /** Título do cabeçalho da Tela 3 de mutirões */
    private String mutiraoTituloTela3 = "";

    /** Cor hexadecimal principal do tema dos mutirões */
    private String mutiraoTemaCor = "#1f4f8a";

    /** Cor hexadecimal do rodapé dos mutirões */
    private String mutiraoRodapeCor = "#6889ec";

    /**
     * Representa um curso ofertado pela agência.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Curso {
        private String nome;
        private String detalhe;
    }

    /**
     * Representa um processo seletivo organizado na agência.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessoItem {
        private String empresa;
        private String data;
        private List<String> texto = new ArrayList<>();
    }

    /**
     * Representa um item da agenda de datas/cronograma institucional.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CronogramaItem {
        private String data;
        private String evento;
    }
}
