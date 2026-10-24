package br.gov.curitiba.mural.dto;

/**
 * Resposta com o token de sessão do usuário gerado após a leitura do QR Code.
 *
 * @param token Token de sessão HMAC assinado
 * @param expiresAtEpochSeconds Timestamp unix de expiração da sessão
 */
public record SessionResponse(
        String token,
        long expiresAtEpochSeconds
) {}
