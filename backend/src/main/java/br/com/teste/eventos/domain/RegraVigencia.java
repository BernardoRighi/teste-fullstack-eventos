package br.com.teste.eventos.domain;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
@Component
public class RegraVigencia {
	public boolean estaAtivo(LocalDate inicio,LocalDate fim,LocalDate hoje) {
		return !hoje.isBefore(inicio)&&!hoje.isAfter(fim);
	}
}
