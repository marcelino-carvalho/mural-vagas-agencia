package br.gov.curitiba.mural;

import br.gov.curitiba.mural.domain.Vaga;
import br.gov.curitiba.mural.repository.VagaRepository;
import br.gov.curitiba.mural.service.VagaService;
import br.gov.curitiba.mural.service.VagaSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes unitários para validar a lógica de negócios da {@link VagaService}.
 */
class VagaServiceTests {

    private VagaService vagaService;
    private VagaRepository repository;
    private VagaSseService sseService;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(VagaRepository.class);
        sseService = Mockito.mock(VagaSseService.class);
        vagaService = new VagaService(repository, sseService);
    }

    @Test
    void normalizaModuloVagasDefault() {
        assertEquals(Vaga.MODULO_VAGAS, vagaService.normalizarModulo(null));
        assertEquals(Vaga.MODULO_VAGAS, vagaService.normalizarModulo(""));
        assertEquals(Vaga.MODULO_VAGAS, vagaService.normalizarModulo("desconhecido"));
        assertEquals(Vaga.MODULO_MUTIROES, vagaService.normalizarModulo("mutiroes"));
        assertEquals(Vaga.MODULO_MUTIROES, vagaService.normalizarModulo("MUTIROES"));
    }

    @Test
    void ordenaVagasMutiroesPorTelaEEmpresa() {
        Vaga v1 = new Vaga();
        v1.setTela("2");
        v1.setEmpresa("Beta");

        Vaga v2 = new Vaga();
        v2.setTela("1");
        v2.setEmpresa("Zeta");

        Vaga v3 = new Vaga();
        v3.setTela("1");
        v3.setEmpresa("Alfa");

        List<Vaga> lista = new ArrayList<>(List.of(v1, v2, v3));
        List<Vaga> ordenadas = vagaService.ordenarPorModulo(lista, Vaga.MODULO_MUTIROES);

        assertEquals("Alfa", ordenadas.get(0).getEmpresa());
        assertEquals("Zeta", ordenadas.get(1).getEmpresa());
        assertEquals("Beta", ordenadas.get(2).getEmpresa());
    }
}
