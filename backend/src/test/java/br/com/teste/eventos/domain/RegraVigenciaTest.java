package br.com.teste.eventos.domain;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class RegraVigenciaTest {
	private final RegraVigencia regra=new RegraVigencia();
	@Test void extremosInclusivos() {
		var inicio=LocalDate.of(2026,11,10);
		var fim=LocalDate.of(2026,11,12);
		assertFalse(regra.estaAtivo(inicio,fim,inicio.minusDays(1)));
		assertTrue(regra.estaAtivo(inicio,fim,inicio));
		assertTrue(regra.estaAtivo(inicio,fim,fim));
		assertFalse(regra.estaAtivo(inicio,fim,fim.plusDays(1)));
	}
}
