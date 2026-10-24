package br.gov.curitiba.mural.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade de configuração institucional exibida nos painéis da TV e mural.
 * Mapeia os dados da tabela 'info_config', armazenando cronogramas, cursos e processos seletivos.
 */
@Entity
@Table(name = "info_config")
@Getter
@Setter
@NoArgsConstructor
public class InfoConfig {

    /** Identificador único da configuração (registro singleton com ID fixo = 1) */
    @Id
    private Long id;

    /** Cursos de capacitação profissional ofertados */
    @Lob
    @Column(name = "cursos_json")
    private String cursosJson;

    /** Processos seletivos em andamento na agência */
    @Lob
    @Column(name = "processos_json")
    private String processosJson;

    /** Cronograma de eventos e mutirões */
    @Lob
    @Column(name = "cronograma_json")
    private String cronogramaJson;

    /** Nome da empresa principal em destaque no processo seletivo */
    @Column(name = "empresa_nome")
    private String empresaNome;

    /** Data agendada para o processo seletivo da empresa */
    @Column(name = "empresa_data")
    private String empresaData;

    /** Lista de vagas específicas do processo seletivo */
    @Lob
    @Column(name = "processo_vagas_json")
    private String processoVagasJson;

    /** Título personalizado para o painel 1 do mutirão */
    @Column(name = "mutirao_titulo_tela1")
    private String mutiraoTituloTela1;

    /** Título personalizado para o painel 2 do mutirão */
    @Column(name = "mutirao_titulo_tela2")
    private String mutiraoTituloTela2;

    /** Título personalizado para o painel 3 do mutirão */
    @Column(name = "mutirao_titulo_tela3")
    private String mutiraoTituloTela3;

    /** Cor temática principal do painel de mutirões (formato hexadecimal) */
    @Column(name = "mutirao_tema_cor")
    private String mutiraoTemaCor;

    /** Cor temática do rodapé do painel de mutirões (formato hexadecimal) */
    @Column(name = "mutirao_rodape_cor")
    private String mutiraoRodapeCor;
}
