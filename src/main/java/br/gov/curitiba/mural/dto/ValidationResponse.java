package br.gov.curitiba.mural.dto;

/**
 * Resposta de checagem da validade do token de acesso do cidadão.
 *
 * @param valid Indica se o token é válido e pode ser utilizado
 * @param expired Indica se o token já expirou o tempo de uso
 * @param wrongNetwork Indica se o token foi gerado em outra rede/IP não autorizado
 * @param wrongType Indica se o tipo do token não corresponde ao esperado (ex: QR vs SESSION)
 * @param expiresAtEpochSeconds Momento em que o token irá expirar
 */
public record ValidationResponse(
        boolean valid,
        boolean expired,
        boolean wrongNetwork,
        boolean wrongType,
        long expiresAtEpochSeconds
) {}
