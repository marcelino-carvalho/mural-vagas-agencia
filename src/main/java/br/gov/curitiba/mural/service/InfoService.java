package br.gov.curitiba.mural.service;

import br.gov.curitiba.mural.domain.InfoConfig;
import br.gov.curitiba.mural.dto.InfoDTO;
import br.gov.curitiba.mural.repository.InfoConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço responsável por gerenciar as informações institucionais, cursos,
 * processos seletivos e títulos personalizados exibidos nas TVs e painéis.
 */
@Service
public class InfoService {

    private static final long CONFIG_ID = 1L;

    private final InfoConfigRepository repository;
    private final VagaSseService sseService;

    public InfoService(InfoConfigRepository repository, VagaSseService sseService) {
        this.repository = repository;
        this.sseService = sseService;
    }

    /**
     * Recupera as configurações e informações institucionais atuais.
     * Retorna um DTO populado com os dados existentes ou uma instância com valores padrão caso ainda não exista.
     *
     * @return DTO com informações institucionais
     */
    @Transactional(readOnly = true)
    public InfoDTO getInfos() {
        return repository.findById(CONFIG_ID)
                .map(this::toDto)
                .orElseGet(InfoDTO::new);
    }

    /**
     * Atualiza e persiste as informações institucionais, notificando as telas via Server-Sent Events.
     *
     * @param novosDados Novos dados enviados pelo painel administrativo
     */
    @Transactional
    public void atualizarInfos(InfoDTO novosDados) {
        InfoDTO dto = normalizar(novosDados);
        repository.save(toEntity(dto));
        sseService.notifyChange();
    }

    /**
     * Normaliza e valida campos de texto, cores hexadecimais e listas nulas.
     */
    private InfoDTO normalizar(InfoDTO entrada) {
        InfoDTO dto = (entrada == null) ? new InfoDTO() : entrada;

        dto.setCursos(safeList(dto.getCursos()));
        dto.setProcessos(safeList(dto.getProcessos()));
        dto.setCronograma(safeList(dto.getCronograma()));
        dto.setProcessoVagas(safeList(dto.getProcessoVagas()));
        dto.setMutiraoTituloTela1(nvl(dto.getMutiraoTituloTela1()));
        dto.setMutiraoTituloTela2(nvl(dto.getMutiraoTituloTela2()));
        dto.setMutiraoTituloTela3(nvl(dto.getMutiraoTituloTela3()));
        dto.setMutiraoTemaCor(normalizarCor(dto.getMutiraoTemaCor()));
        dto.setMutiraoRodapeCor(normalizarCor(dto.getMutiraoRodapeCor()));

        // Retrocompatibilidade: se a lista de processos estiver vazia mas houver empresa definida em campos avulsos
        if (dto.getProcessos().isEmpty() && (hasText(dto.getEmpresaNome(), dto.getEmpresaData()) || !dto.getProcessoVagas().isEmpty())) {
            dto.getProcessos().add(new InfoDTO.ProcessoItem(dto.getEmpresaNome(), dto.getEmpresaData(), dto.getProcessoVagas()));
        }

        if (!dto.getProcessos().isEmpty()) {
            InfoDTO.ProcessoItem principal = dto.getProcessos().get(0);
            dto.setEmpresaNome(nvl(principal.getEmpresa()));
            dto.setEmpresaData(nvl(principal.getData()));
            dto.setProcessoVagas(safeList(principal.getTexto()));
        } else {
            dto.setEmpresaNome("");
            dto.setEmpresaData("");
            dto.setProcessoVagas(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Converte a entidade persistida {@link InfoConfig} para o formato {@link InfoDTO}.
     */
    private InfoDTO toDto(InfoConfig entity) {
        InfoDTO dto = new InfoDTO();
        dto.setCursos(deserializeCursos(entity.getCursosJson()));
        dto.setProcessos(deserializeProcessos(entity.getProcessosJson()));
        dto.setCronograma(deserializeCronograma(entity.getCronogramaJson()));
        dto.setProcessoVagas(deserializeTextoLista(entity.getProcessoVagasJson()));
        dto.setEmpresaNome(nvl(entity.getEmpresaNome()));
        dto.setEmpresaData(nvl(entity.getEmpresaData()));
        dto.setMutiraoTituloTela1(nvl(entity.getMutiraoTituloTela1()));
        dto.setMutiraoTituloTela2(nvl(entity.getMutiraoTituloTela2()));
        dto.setMutiraoTituloTela3(nvl(entity.getMutiraoTituloTela3()));
        dto.setMutiraoTemaCor(normalizarCor(entity.getMutiraoTemaCor()));
        dto.setMutiraoRodapeCor(normalizarCor(entity.getMutiraoRodapeCor()));

        if ((dto.getEmpresaNome().isBlank() && dto.getEmpresaData().isBlank() && dto.getProcessoVagas().isEmpty()) && !dto.getProcessos().isEmpty()) {
            InfoDTO.ProcessoItem principal = dto.getProcessos().get(0);
            dto.setEmpresaNome(nvl(principal.getEmpresa()));
            dto.setEmpresaData(nvl(principal.getData()));
            dto.setProcessoVagas(safeList(principal.getTexto()));
        }

        return dto;
    }

    /**
     * Converte o DTO {@link InfoDTO} para a entidade {@link InfoConfig} pronta para ser persistida.
     */
    private InfoConfig toEntity(InfoDTO dto) {
        InfoConfig entity = new InfoConfig();
        entity.setId(CONFIG_ID);
        entity.setCursosJson(serializeCursos(dto.getCursos()));
        entity.setProcessosJson(serializeProcessos(dto.getProcessos()));
        entity.setCronogramaJson(serializeCronograma(dto.getCronograma()));
        entity.setEmpresaNome(dto.getEmpresaNome());
        entity.setEmpresaData(dto.getEmpresaData());
        entity.setProcessoVagasJson(serializeTextoLista(dto.getProcessoVagas()));
        entity.setMutiraoTituloTela1(dto.getMutiraoTituloTela1());
        entity.setMutiraoTituloTela2(dto.getMutiraoTituloTela2());
        entity.setMutiraoTituloTela3(dto.getMutiraoTituloTela3());
        entity.setMutiraoTemaCor(dto.getMutiraoTemaCor());
        entity.setMutiraoRodapeCor(dto.getMutiraoRodapeCor());
        return entity;
    }

    private <T> List<T> safeList(List<T> source) {
        return source == null ? new ArrayList<>() : source;
    }

    private boolean hasText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String normalizarCor(String value) {
        String cor = nvl(value).trim();
        if (cor.matches("^#[0-9a-fA-F]{6}$")) {
            return cor;
        }
        return "#1f4f8a";
    }

    private String enc(String value) {
        return URLEncoder.encode(nvl(value), StandardCharsets.UTF_8);
    }

    private String dec(String value) {
        try {
            return URLDecoder.decode(nvl(value), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    // --- Métodos de Serialização / Deserialização com suporte aos dados persistidos existentes ---

    private String serializeCursos(List<InfoDTO.Curso> cursos) {
        List<String> out = new ArrayList<>();
        for (InfoDTO.Curso c : safeList(cursos)) {
            out.add(enc(c == null ? "" : c.getNome()) + "|" + enc(c == null ? "" : c.getDetalhe()));
        }
        return String.join(";", out);
    }

    private List<InfoDTO.Curso> deserializeCursos(String raw) {
        List<InfoDTO.Curso> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(";", -1)) {
            if (item.isBlank()) continue;
            String[] parts = item.split("\\|", -1);
            String nome = parts.length > 0 ? dec(parts[0]) : "";
            String detalhe = parts.length > 1 ? dec(parts[1]) : "";
            if (!nome.isBlank() || !detalhe.isBlank()) {
                out.add(new InfoDTO.Curso(nome, detalhe));
            }
        }
        return out;
    }

    private String serializeProcessos(List<InfoDTO.ProcessoItem> processos) {
        List<String> out = new ArrayList<>();
        for (InfoDTO.ProcessoItem p : safeList(processos)) {
            String empresa = enc(p == null ? "" : p.getEmpresa());
            String data = enc(p == null ? "" : p.getData());
            String texto = serializeTextoLista(p == null ? new ArrayList<>() : p.getTexto());
            out.add(empresa + "|" + data + "|" + texto);
        }
        return String.join(";", out);
    }

    private List<InfoDTO.ProcessoItem> deserializeProcessos(String raw) {
        List<InfoDTO.ProcessoItem> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(";", -1)) {
            if (item.isBlank()) continue;
            String[] parts = item.split("\\|", -1);
            String empresa = parts.length > 0 ? dec(parts[0]) : "";
            String data = parts.length > 1 ? dec(parts[1]) : "";
            List<String> texto = parts.length > 2 ? deserializeTextoLista(parts[2]) : new ArrayList<>();
            if (!empresa.isBlank() || !data.isBlank() || !texto.isEmpty()) {
                out.add(new InfoDTO.ProcessoItem(empresa, data, texto));
            }
        }
        return out;
    }

    private String serializeCronograma(List<InfoDTO.CronogramaItem> cronograma) {
        List<String> out = new ArrayList<>();
        for (InfoDTO.CronogramaItem c : safeList(cronograma)) {
            out.add(enc(c == null ? "" : c.getData()) + "|" + enc(c == null ? "" : c.getEvento()));
        }
        return String.join(";", out);
    }

    private List<InfoDTO.CronogramaItem> deserializeCronograma(String raw) {
        List<InfoDTO.CronogramaItem> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(";", -1)) {
            if (item.isBlank()) continue;
            String[] parts = item.split("\\|", -1);
            String data = parts.length > 0 ? dec(parts[0]) : "";
            String evento = parts.length > 1 ? dec(parts[1]) : "";
            if (!data.isBlank() || !evento.isBlank()) {
                out.add(new InfoDTO.CronogramaItem(data, evento));
            }
        }
        return out;
    }

    private String serializeTextoLista(List<String> texto) {
        List<String> out = new ArrayList<>();
        for (String linha : safeList(texto)) {
            if (linha != null) {
                out.add(enc(linha));
            }
        }
        return String.join(",", out);
    }

    private List<String> deserializeTextoLista(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(",", -1)) {
            if (item.isBlank()) continue;
            out.add(dec(item));
        }
        return out;
    }
}
