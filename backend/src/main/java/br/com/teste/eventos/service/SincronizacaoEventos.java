package br.com.teste.eventos.service;
import br.com.teste.eventos.repository.EventoRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class SincronizacaoEventos {
	private final EventoRepository eventos;
	private final Clock clock;
	public SincronizacaoEventos(EventoRepository eventos,Clock clock) {
		this.eventos=eventos;
		this.clock=clock;
	}
	@Scheduled(cron="0 */5 * * * *",zone="${app.zone}") @Transactional
	public void executar() {
		eventos.sincronizar(LocalDate.now(clock));
	}
}
