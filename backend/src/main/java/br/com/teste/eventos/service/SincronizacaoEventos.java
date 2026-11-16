package br.com.teste.eventos.service;

import br.com.teste.eventos.repository.EventoRepository;

import java.time.Clock;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SincronizacaoEventos {

    private static final Logger log =
        LoggerFactory.getLogger(SincronizacaoEventos.class);

    private final EventoRepository eventos;
    private final Clock clock;

    public SincronizacaoEventos(
            EventoRepository eventos,
            Clock clock
    ) {
        this.eventos = eventos;
        this.clock = clock;
    }

    @Scheduled(
        cron = "${app.sincronizacao.cron:0 */5 * * * *}",
        zone = "${app.zone}"
    )
    @Transactional
    public void executar() {
        LocalDate hoje = LocalDate.now(clock);

        int alterados = eventos.sincronizar(hoje);

        log.info(
            "Sincronização processada: dataReferencia={}, registrosAlterados={}",
            hoje,
            alterados
        );
    }
}
