package br.gov.curitiba.mural.dto;

/**
 * Resposta contendo o token temporário de QR Code emitido pela TV para os cidadãos.
 *
 * @param token Token HMAC assinado
 * @param expiresAtEpochSeconds Timestamp unix de expiração do token
 * @param modulo Módulo associado (VAGAS ou MUTIROES)
 * @param muralUrl URL completa de destino que o QR Code aponta
 * @param qrUrl Endpoint relativo da imagem PNG do QR Code
 */
public record TokenResponse(
        String token,
        long expiresAtEpochSeconds,
        String modulo,
        String muralUrl,
        String qrUrl
) {}
