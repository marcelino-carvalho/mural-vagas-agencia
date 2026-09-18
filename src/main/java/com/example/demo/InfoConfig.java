package com.example.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "info_config")
public class InfoConfig {

    @Id
    private Long id;

    @Lob
    @Column(name = "cursos_json")
    private String cursosJson;

    @Lob
    @Column(name = "processos_json")
    private String processosJson;

    @Lob
    @Column(name = "cronograma_json")
    private String cronogramaJson;

    @Column(name = "empresa_nome")
    private String empresaNome;

    @Column(name = "empresa_data")
    private String empresaData;

    @Lob
    @Column(name = "processo_vagas_json")
    private String processoVagasJson;

    @Column(name = "mutirao_titulo_tela1")
    private String mutiraoTituloTela1;

    @Column(name = "mutirao_titulo_tela2")
    private String mutiraoTituloTela2;

    @Column(name = "mutirao_titulo_tela3")
    private String mutiraoTituloTela3;

    @Column(name = "mutirao_tema_cor")
    private String mutiraoTemaCor;

    @Column(name = "mutirao_rodape_cor")
    private String mutiraoRodapeCor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCursosJson() {
        return cursosJson;
    }

    public void setCursosJson(String cursosJson) {
        this.cursosJson = cursosJson;
    }

    public String getProcessosJson() {
        return processosJson;
    }

    public void setProcessosJson(String processosJson) {
        this.processosJson = processosJson;
    }

    public String getCronogramaJson() {
        return cronogramaJson;
    }

    public void setCronogramaJson(String cronogramaJson) {
        this.cronogramaJson = cronogramaJson;
    }

    public String getEmpresaNome() {
        return empresaNome;
    }

    public void setEmpresaNome(String empresaNome) {
        this.empresaNome = empresaNome;
    }

    public String getEmpresaData() {
        return empresaData;
    }

    public void setEmpresaData(String empresaData) {
        this.empresaData = empresaData;
    }

    public String getProcessoVagasJson() {
        return processoVagasJson;
    }

    public void setProcessoVagasJson(String processoVagasJson) {
        this.processoVagasJson = processoVagasJson;
    }

    public String getMutiraoTituloTela1() {
        return mutiraoTituloTela1;
    }

    public void setMutiraoTituloTela1(String mutiraoTituloTela1) {
        this.mutiraoTituloTela1 = mutiraoTituloTela1;
    }

    public String getMutiraoTituloTela2() {
        return mutiraoTituloTela2;
    }

    public void setMutiraoTituloTela2(String mutiraoTituloTela2) {
        this.mutiraoTituloTela2 = mutiraoTituloTela2;
    }

    public String getMutiraoTituloTela3() {
        return mutiraoTituloTela3;
    }

    public void setMutiraoTituloTela3(String mutiraoTituloTela3) {
        this.mutiraoTituloTela3 = mutiraoTituloTela3;
    }

    public String getMutiraoTemaCor() {
        return mutiraoTemaCor;
    }

    public void setMutiraoTemaCor(String mutiraoTemaCor) {
        this.mutiraoTemaCor = mutiraoTemaCor;
    }

    public String getMutiraoRodapeCor() {
        return mutiraoRodapeCor;
    }

    public void setMutiraoRodapeCor(String mutiraoRodapeCor) {
        this.mutiraoRodapeCor = mutiraoRodapeCor;
    }
}
