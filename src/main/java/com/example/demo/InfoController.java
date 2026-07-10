package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/infos")
public class InfoController {

    private static final long CONFIG_ID = 1L;

    private final InfoConfigRepository repository;
    private final VagaSseService sseService;
    private final PanelAccessWindow accessWindow;

    public InfoController(InfoConfigRepository repository, VagaSseService sseService, PanelAccessWindow accessWindow) {
        this.repository = repository;
        this.sseService = sseService;
        this.accessWindow = accessWindow;
    }

    @GetMapping
    public InfoDTO getInfos() {
        accessWindow.requireOpen();
        return repository.findById(CONFIG_ID)
                .map(this::toDto)
                .orElseGet(InfoDTO::new);
    }

    @PostMapping
    public void atualizarInfos(@RequestBody InfoDTO novosDados) {
        InfoDTO dto = normalizar(novosDados);
        repository.save(toEntity(dto));
        sseService.notifyChange();
    }

    private InfoDTO normalizar(InfoDTO entrada) {
        InfoDTO dto = (entrada == null) ? new InfoDTO() : entrada;

        dto.cursos = safeList(dto.cursos);
        dto.processos = safeList(dto.processos);
        dto.cronograma = safeList(dto.cronograma);
        dto.processoVagas = safeList(dto.processoVagas);
        dto.mutiraoTituloTela1 = nvl(dto.mutiraoTituloTela1);
        dto.mutiraoTituloTela2 = nvl(dto.mutiraoTituloTela2);
        dto.mutiraoTemaCor = normalizarCor(dto.mutiraoTemaCor);
        dto.mutiraoRodapeCor = normalizarCor(dto.mutiraoRodapeCor);

        if (dto.processos.isEmpty() && (hasText(dto.empresaNome, dto.empresaData) || !dto.processoVagas.isEmpty())) {
            dto.processos.add(new ProcessoItem(dto.empresaNome, dto.empresaData, dto.processoVagas));
        }

        if (!dto.processos.isEmpty()) {
            ProcessoItem principal = dto.processos.get(0);
            dto.empresaNome = nvl(principal.empresa);
            dto.empresaData = nvl(principal.data);
            dto.processoVagas = safeList(principal.texto);
        } else {
            dto.empresaNome = "";
            dto.empresaData = "";
            dto.processoVagas = new ArrayList<>();
        }

        return dto;
    }

    private InfoDTO toDto(InfoConfig entity) {
        InfoDTO dto = new InfoDTO();
        dto.cursos = deserializeCursos(entity.getCursosJson());
        dto.processos = deserializeProcessos(entity.getProcessosJson());
        dto.cronograma = deserializeCronograma(entity.getCronogramaJson());
        dto.processoVagas = deserializeTextoLista(entity.getProcessoVagasJson());
        dto.empresaNome = nvl(entity.getEmpresaNome());
        dto.empresaData = nvl(entity.getEmpresaData());
        dto.mutiraoTituloTela1 = nvl(entity.getMutiraoTituloTela1());
        dto.mutiraoTituloTela2 = nvl(entity.getMutiraoTituloTela2());
        dto.mutiraoTemaCor = normalizarCor(entity.getMutiraoTemaCor());
        dto.mutiraoRodapeCor = normalizarCor(entity.getMutiraoRodapeCor());

        if ((dto.empresaNome.isBlank() && dto.empresaData.isBlank() && dto.processoVagas.isEmpty()) && !dto.processos.isEmpty()) {
            ProcessoItem principal = dto.processos.get(0);
            dto.empresaNome = nvl(principal.empresa);
            dto.empresaData = nvl(principal.data);
            dto.processoVagas = safeList(principal.texto);
        }

        if (dto.cursos.isEmpty() && dto.processos.isEmpty() && dto.cronograma.isEmpty()
                && dto.empresaNome.isBlank() && dto.empresaData.isBlank() && dto.processoVagas.isEmpty()) {
            return new InfoDTO();
        }

        return dto;
    }

    private InfoConfig toEntity(InfoDTO dto) {
        InfoConfig entity = new InfoConfig();
        entity.setId(CONFIG_ID);
        entity.setCursosJson(serializeCursos(dto.cursos));
        entity.setProcessosJson(serializeProcessos(dto.processos));
        entity.setCronogramaJson(serializeCronograma(dto.cronograma));
        entity.setEmpresaNome(dto.empresaNome);
        entity.setEmpresaData(dto.empresaData);
        entity.setProcessoVagasJson(serializeTextoLista(dto.processoVagas));
        entity.setMutiraoTituloTela1(dto.mutiraoTituloTela1);
        entity.setMutiraoTituloTela2(dto.mutiraoTituloTela2);
        entity.setMutiraoTemaCor(dto.mutiraoTemaCor);
        entity.setMutiraoRodapeCor(dto.mutiraoRodapeCor);
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

    private String serializeCursos(List<Curso> cursos) {
        List<String> out = new ArrayList<>();
        for (Curso c : safeList(cursos)) {
            out.add(enc(c == null ? "" : c.nome) + "|" + enc(c == null ? "" : c.detalhe));
        }
        return String.join(";", out);
    }

    private List<Curso> deserializeCursos(String raw) {
        List<Curso> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(";", -1)) {
            if (item.isBlank()) {
                continue;
            }
            String[] parts = item.split("\\|", -1);
            String nome = parts.length > 0 ? dec(parts[0]) : "";
            String detalhe = parts.length > 1 ? dec(parts[1]) : "";
            if (!nome.isBlank() || !detalhe.isBlank()) {
                out.add(new Curso(nome, detalhe));
            }
        }
        return out;
    }

    private String serializeProcessos(List<ProcessoItem> processos) {
        List<String> out = new ArrayList<>();
        for (ProcessoItem p : safeList(processos)) {
            String empresa = enc(p == null ? "" : p.empresa);
            String data = enc(p == null ? "" : p.data);
            String texto = serializeTextoLista(p == null ? new ArrayList<>() : p.texto);
            out.add(empresa + "|" + data + "|" + texto);
        }
        return String.join(";", out);
    }

    private List<ProcessoItem> deserializeProcessos(String raw) {
        List<ProcessoItem> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(";", -1)) {
            if (item.isBlank()) {
                continue;
            }
            String[] parts = item.split("\\|", -1);
            String empresa = parts.length > 0 ? dec(parts[0]) : "";
            String data = parts.length > 1 ? dec(parts[1]) : "";
            List<String> texto = parts.length > 2 ? deserializeTextoLista(parts[2]) : new ArrayList<>();
            if (!empresa.isBlank() || !data.isBlank() || !texto.isEmpty()) {
                out.add(new ProcessoItem(empresa, data, texto));
            }
        }
        return out;
    }

    private String serializeCronograma(List<CronogramaItem> cronograma) {
        List<String> out = new ArrayList<>();
        for (CronogramaItem c : safeList(cronograma)) {
            out.add(enc(c == null ? "" : c.data) + "|" + enc(c == null ? "" : c.evento));
        }
        return String.join(";", out);
    }

    private List<CronogramaItem> deserializeCronograma(String raw) {
        List<CronogramaItem> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(";", -1)) {
            if (item.isBlank()) {
                continue;
            }
            String[] parts = item.split("\\|", -1);
            String data = parts.length > 0 ? dec(parts[0]) : "";
            String evento = parts.length > 1 ? dec(parts[1]) : "";
            if (!data.isBlank() || !evento.isBlank()) {
                out.add(new CronogramaItem(data, evento));
            }
        }
        return out;
    }

    private String serializeTextoLista(List<String> texto) {
        List<String> out = new ArrayList<>();
        for (String linha : safeList(texto)) {
            if (linha == null) {
                continue;
            }
            out.add(enc(linha));
        }
        return String.join(",", out);
    }

    private List<String> deserializeTextoLista(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String item : raw.split(",", -1)) {
            if (item.isBlank()) {
                continue;
            }
            out.add(dec(item));
        }
        return out;
    }

    public static class InfoDTO {
        public List<Curso> cursos = new ArrayList<>();
        public List<ProcessoItem> processos = new ArrayList<>();
        public String empresaNome = "";
        public String empresaData = "";
        public List<String> processoVagas = new ArrayList<>();
        public List<CronogramaItem> cronograma = new ArrayList<>();
        public String mutiraoTituloTela1 = "";
        public String mutiraoTituloTela2 = "";
        public String mutiraoTemaCor = "#1f4f8a";
        public String mutiraoRodapeCor = "#6889ec";
    }

    public static class Curso {
        public String nome;
        public String detalhe;

        public Curso() {
        }

        public Curso(String nome, String detalhe) {
            this.nome = nome;
            this.detalhe = detalhe;
        }
    }

    public static class ProcessoItem {
        public String empresa;
        public String data;
        public List<String> texto;

        public ProcessoItem() {
        }

        public ProcessoItem(String empresa, String data, List<String> texto) {
            this.empresa = empresa;
            this.data = data;
            this.texto = texto;
        }
    }

    public static class CronogramaItem {
        public String data;
        public String evento;

        public CronogramaItem() {
        }

        public CronogramaItem(String data, String evento) {
            this.data = data;
            this.evento = evento;
        }
    }
}
