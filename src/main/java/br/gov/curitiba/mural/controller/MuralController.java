package br.gov.curitiba.mural.controller;

import br.gov.curitiba.mural.domain.Vaga;
import br.gov.curitiba.mural.dto.*;
import br.gov.curitiba.mural.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controlador REST responsável pelas operações do mural digital acessível via celular e TV.
 * Gerencia tokens de QR Code, sessões temporárias e entrega de dados filtrados para a tela do cidadão.
 */
@RestController
@RequestMapping("/api/mural")
public class MuralController {

    private final VagaService vagaService;
    private final InfoService infoService;
    private final MuralTokenService tokenService;
    private final QrCodeService qrCodeService;
    private final PanelAccessWindow accessWindow;

    public MuralController(
            VagaService vagaService,
            InfoService infoService,
            MuralTokenService tokenService,
            QrCodeService qrCodeService,
            PanelAccessWindow accessWindow
    ) {
        this.vagaService = vagaService;
        this.infoService = infoService;
        this.tokenService = tokenService;
        this.qrCodeService = qrCodeService;
        this.accessWindow = accessWindow;
    }

    /**
     * Emite um token de QR Code para exibição nas telas de TV da agência.
     */
    @GetMapping("/token")
    public TokenResponse issueToken(
            @RequestParam(name = "m", required = false) String modulo,
            HttpServletRequest request
    ) {
        String ip = resolveClientIp(request);
        MuralTokenService.IssuedToken issued = tokenService.issueQrToken(ip);
        String normalizedModule = vagaService.normalizarModulo(modulo);
        String muralUrl = buildMuralUrl(issued.token(), normalizedModule);
        String qrUrl = "/api/mural/qr?t=" + urlEncode(issued.token()) + "&m=" + urlEncode(normalizedModule);

        return new TokenResponse(issued.token(), issued.expiresAtEpochSeconds(), normalizedModule, muralUrl, qrUrl);
    }

    /**
     * Valida o token lido do QR Code e emite um token de sessão de navegação para o cidadão.
     */
    @GetMapping("/session")
    public SessionResponse issueSession(@RequestParam("t") String qrToken, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        MuralTokenService.ValidationResult qrValidation = tokenService.validate(qrToken, ip, "QR");
        if (!qrValidation.valid()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR Code inválido, expirado ou acesso fora da rede autorizada.");
        }
        MuralTokenService.IssuedToken session = tokenService.issueSessionToken(ip);
        return new SessionResponse(session.token(), session.expiresAtEpochSeconds());
    }

    /**
     * Verifica se o token de sessão do cidadão permanece válido ou expirou.
     */
    @GetMapping("/validate")
    public ValidationResponse validate(@RequestParam("t") String token, HttpServletRequest request) {
        MuralTokenService.ValidationResult result = tokenService.validate(token, resolveClientIp(request), "SESSION");
        return new ValidationResponse(result.valid(), result.expired(), result.wrongNetwork(), result.wrongType(), result.expiresAtEpochSeconds());
    }

    /**
     * Entrega a lista completa de vagas e avisos institucionais para o celular do cidadão.
     */
    @GetMapping("/data")
    public MuralDataResponse data(
            @RequestParam("t") String token,
            @RequestParam(name = "m", required = false) String modulo,
            HttpServletRequest request
    ) {
        accessWindow.requireOpen();
        MuralTokenService.ValidationResult result = tokenService.validate(token, resolveClientIp(request), "SESSION");
        if (!result.valid()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso do mural inválido ou expirado para esta rede.");
        }

        String normalizedModule = vagaService.normalizarModulo(modulo);
        List<Vaga> vagas = vagaService.listarPorModulo(normalizedModule);
        InfoDTO infos = infoService.getInfos();

        return new MuralDataResponse(vagas, infos, normalizedModule, result.expiresAtEpochSeconds());
    }

    /**
     * Renderiza o QR Code em formato de imagem PNG para inclusão direta em tags img das TVs.
     */
    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(
            @RequestParam("t") String token,
            @RequestParam(name = "m", required = false) String modulo
    ) {
        String muralUrl = buildMuralUrl(token, vagaService.normalizarModulo(modulo));
        byte[] png = qrCodeService.generateQrCodePng(muralUrl, 280);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    private String buildMuralUrl(String token, String modulo) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String path = Vaga.MODULO_MUTIROES.equals(modulo) ? "/mural-mutiroes.html" : "/mural.html";
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
}
