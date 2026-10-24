package br.gov.curitiba.mural.repository;

import br.gov.curitiba.mural.domain.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório de acesso a dados da entidade {@link Vaga}.
 * Responsável pelas operações de consulta, criação, edição e exclusão de vagas no PostgreSQL.
 */
@Repository
public interface VagaRepository extends JpaRepository<Vaga, Long> {

    /**
     * Busca todas as vagas de um determinado módulo (ex: VAGAS ou MUTIROES),
     * ordenadas pela data de validade de forma ascendente.
     * Vagas sem validade definida aparecem ao final da listagem.
     *
     * @param modulo Nome do módulo (VAGAS ou MUTIROES)
     * @return Lista de vagas ordenadas por validade
     */
    @Query("""
            SELECT v FROM Vaga v
            WHERE UPPER(COALESCE(v.modulo, 'VAGAS')) = UPPER(:modulo)
            ORDER BY
              CASE WHEN v.validade IS NULL OR v.validade = '' THEN 1 ELSE 0 END,
              v.validade ASC
            """)
    List<Vaga> findByModuloOrderByValidadeAsc(@Param("modulo") String modulo);

    /**
     * Busca todas as vagas do módulo padrão 'VAGAS' filtradas por categoria (ex: CONVENCIONAL, SUPERIOR, PCD),
     * ordenadas pela data de validade de forma ascendente.
     *
     * @param categoria Categoria da vaga
     * @return Lista de vagas da categoria ordenadas por validade
     */
    @Query("""
            SELECT v FROM Vaga v
            WHERE UPPER(v.categoria) = UPPER(:categoria)
              AND UPPER(COALESCE(v.modulo, 'VAGAS')) = 'VAGAS'
            ORDER BY
              CASE WHEN v.validade IS NULL OR v.validade = '' THEN 1 ELSE 0 END,
              v.validade ASC
            """)
    List<Vaga> findByCategoriaOrderByValidadeAsc(@Param("categoria") String categoria);
}
