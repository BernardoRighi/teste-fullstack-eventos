package br.com.teste.eventos.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class RegraVigenciaTest {

    private final RegraVigencia regra = new RegraVigencia();

    private final LocalDate inicio = LocalDate.of(2026, 11, 10);
    private final LocalDate fim = LocalDate.of(2026, 11, 12);

    @Test
    void deveFicarInativoAntesDoInicio() {
        assertFalse(
            regra.estaAtivo(inicio, fim, inicio.minusDays(1))
        );
    }

    @Test
    void deveFicarAtivoNaDataInicial() {
        assertTrue(regra.estaAtivo(inicio, fim, inicio));
    }

    @Test
    void deveFicarAtivoDentroDoPeriodo() {
        assertTrue(
            regra.estaAtivo(inicio, fim, inicio.plusDays(1))
        );
    }

    @Test
    void deveFicarAtivoNaDataFinal() {
        assertTrue(regra.estaAtivo(inicio, fim, fim));
    }

    @Test
    void deveFicarInativoDepoisDoFim() {
        assertFalse(
            regra.estaAtivo(inicio, fim, fim.plusDays(1))
        );
    }

    @Test
    void deveAceitarEventoComDuracaoDeUmDia() {
        assertTrue(regra.estaAtivo(inicio, inicio, inicio));
        assertFalse(
            regra.estaAtivo(inicio, inicio, inicio.plusDays(1))
        );
    }
}