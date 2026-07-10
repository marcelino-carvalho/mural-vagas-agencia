package com.example.demo;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mural")
public class MuralController {

    private static final String MODULO_VAGAS = Vaga.MODULO_VAGAS;
    private static final String MODULO_MUTIROES = Vaga.MODULO_MUTIROES;

    private final VagaRepository vagaRepository;
    private final InfoController infoController;
    private final MuralTokenService tokenService;
    private final PanelAccessWindow accessWindow;

    public MuralController(VagaRepository vagaRepository, InfoController infoController, MuralTokenService tokenService, PanelAccessWindow accessWindow) {
        this.vagaRepository = vagaRepository;
        this.infoController = infoController;
        this.tokenService = tokenService;
        this.accessWindow = accessWindow;
    }

    @GetMapping("/token")
    public TokenResponse issueToken(@RequestParam(name = "m", required = false) String modulo, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        MuralTokenService.IssuedToken issued = tokenService.issueQrToken(ip);
        String normalizedModule = normalizeModulo(modulo);
        String muralUrl = buildMuralUrl(issued.token(), normalizedModule);
        String qrUrl = "/api/mural/qr?t=" + urlEncode(issued.token()) + "&m=" + urlEncode(normalizedModule);
        return new TokenResponse(issued.token(), issued.expiresAtEpochSeconds(), normalizedModule, muralUrl, qrUrl);
    }

    @GetMapping("/session")
    public SessionResponse issueSession(@RequestParam("t") String qrToken, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        MuralTokenService.ValidationResult qrValidation = tokenService.validate(qrToken, ip, "QR");
        if (!qrValidation.valid()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR invalido, expirado ou fora da rede.");
        }
        MuralTokenService.IssuedToken session = tokenService.issueSessionToken(ip);
        return new SessionResponse(session.token(), session.expiresAtEpochSeconds());
    }

    @GetMapping("/validate")
    public ValidationResponse validate(@RequestParam("t") String token, HttpServletRequest request) {
        MuralTokenService.ValidationResult result = tokenService.validate(token, resolveClientIp(request), "SESSION");
        return new ValidationResponse(result.valid(), result.expired(), result.wrongNetwork(), result.wrongType(), result.expiresAtEpochSeconds());
    }

    @GetMapping("/data")
    public MuralDataResponse data(@RequestParam("t") String token,
                                  @RequestParam(name = "m", required = false) String modulo,
                                  HttpServletRequest request) {
        accessWindow.requireOpen();
        MuralTokenService.ValidationResult result = tokenService.validate(token, resolveClientIp(request), "SESSION");
        if (!result.valid()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso do mural invalido para esta rede.");
        }

        String normalizedModule = normalizeModulo(modulo);
        List<Vaga> vagas = VagaController.ordenarPorModulo(
                vagaRepository.findByModuloOrderByValidadeAsc(normalizedModule),
                normalizedModule
        );
        InfoController.InfoDTO infos = infoController.getInfos();
        return new MuralDataResponse(vagas, infos, normalizedModule, result.expiresAtEpochSeconds());
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@RequestParam("t") String token,
                                     @RequestParam(name = "m", required = false) String modulo) {
        String muralUrl = buildMuralUrl(token, normalizeModulo(modulo));
        byte[] png = gerarQrPng(muralUrl, 280);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    private byte[] gerarQrPng(String text, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao gerar QR code");
        }
    }

    private String buildMuralUrl(String token, String modulo) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String path = MODULO_MUTIROES.equals(modulo) ? "/mural-mutiroes.html" : "/mural.html";
        return base + path + "?t=" + urlEncode(token);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeModulo(String modulo) {
        if (modulo == null || modulo.isBlank()) {
            return MODULO_VAGAS;
        }
        return MODULO_MUTIROES.equalsIgnoreCase(modulo) ? MODULO_MUTIROES : MODULO_VAGAS;
    }

    public record TokenResponse(String token, long expiresAtEpochSeconds, String modulo, String muralUrl, String qrUrl) {}
    public record SessionResponse(String token, long expiresAtEpochSeconds) {}
    public record ValidationResponse(boolean valid, boolean expired, boolean wrongNetwork, boolean wrongType, long expiresAtEpochSeconds) {}
    public record MuralDataResponse(List<Vaga> vagas, InfoController.InfoDTO infos, String modulo, long expiresAtEpochSeconds) {}
}
