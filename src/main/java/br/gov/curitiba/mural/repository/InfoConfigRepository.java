package br.gov.curitiba.mural.repository;

import br.gov.curitiba.mural.domain.InfoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório de acesso a dados da entidade {@link InfoConfig}.
 * Gerencia a persistência das configurações institucionais e painéis da TV.
 */
@Repository
public interface InfoConfigRepository extends JpaRepository<InfoConfig, Long> {
}
