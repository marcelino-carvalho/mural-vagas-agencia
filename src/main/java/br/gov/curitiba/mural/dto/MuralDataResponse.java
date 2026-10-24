package br.gov.curitiba.mural.dto;

import br.gov.curitiba.mural.domain.Vaga;

import java.util.List;

/**
 * Agrupa os dados completos de vagas e informes institucionais
 * entregues ao celular do usuário quando acessa o mural via QR Code.
 *
 * @param vagas Lista de vagas ativas ordenadas
 * @param infos Configurações e comunicados institucionais
 * @param modulo Módulo consultado (VAGAS ou MUTIROES)
 * @param expiresAtEpochSeconds Expiração da sessão do usuário
 */
public record MuralDataResponse(
        List<Vaga> vagas,
        InfoDTO infos,
        String modulo,
        long expiresAtEpochSeconds
) {}
