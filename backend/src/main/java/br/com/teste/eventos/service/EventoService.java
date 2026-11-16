package br.com.teste.eventos.service;

import br.com.teste.eventos.api.EventoRequest;
import br.com.teste.eventos.api.EventoResponse;
import br.com.teste.eventos.domain.Evento;
import br.com.teste.eventos.domain.Instituicao;
import br.com.teste.eventos.domain.RegraVigencia;
import br.com.teste.eventos.repository.EventoRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {

    private final EventoRepository eventos;
    private final InstituicaoService instituicoes;
    private final RegraVigencia regra;
    private final Clock clock;

    public EventoService(
            EventoRepository eventos,
            InstituicaoService instituicoes,
            RegraVigencia regra,
            Clock clock
    ) {
        this.eventos = eventos;
        this.instituicoes = instituicoes;
        this.regra = regra;
        this.clock = clock;
    }

    @Transactional
    public EventoResponse criar(EventoRequest request) {
        validar(request);

        LocalDate hoje = LocalDate.now(clock);
        Instituicao instituicao =
            instituicoes.obter(request.instituicaoId());

        boolean ativo = regra.estaAtivo(
            request.dataInicial(),
            request.dataFinal(),
            hoje
        );

        Evento evento = new Evento(
            instituicao,
            request.nome().trim(),
            request.dataInicial(),
            request.dataFinal(),
            ativo
        );

        return resposta(eventos.save(evento), hoje);
    }

    @Transactional
    public EventoResponse atualizar(Long id, EventoRequest request) {
        validar(request);

        LocalDate hoje = LocalDate.now(clock);
        Evento evento = obter(id);

        Instituicao instituicao =
            instituicoes.obter(request.instituicaoId());

        boolean ativo = regra.estaAtivo(
            request.dataInicial(),
            request.dataFinal(),
            hoje
        );

        evento.atualizar(
            instituicao,
            request.nome().trim(),
            request.dataInicial(),
            request.dataFinal(),
            ativo
        );

        return resposta(evento, hoje);
    }

    @Transactional(readOnly = true)
    public EventoResponse buscar(Long id) {
        LocalDate hoje = LocalDate.now(clock);
        return resposta(obter(id), hoje);
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar(
            Long instituicaoId,
            boolean somenteAtivos
    ) {
        if (somenteAtivos && instituicaoId != null) {
            throw new IllegalArgumentException(
                "Use apenas um filtro por vez."
            );
        }

        if (!somenteAtivos && instituicaoId == null) {
            throw new IllegalArgumentException(
                "Informe instituicaoId ou somenteAtivos=true."
            );
        }

        LocalDate hoje = LocalDate.now(clock);
        List<Evento> encontrados;

        if (somenteAtivos) {
            encontrados = eventos.vigentes(hoje);
        } else {
            instituicoes.obter(instituicaoId);
            encontrados = eventos.porInstituicao(instituicaoId);
        }

        return encontrados.stream()
            .map(evento -> resposta(evento, hoje))
            .toList();
    }

    @Transactional
    public void excluir(Long id) {
        eventos.delete(obter(id));
    }

    private Evento obter(Long id) {
        return eventos.findById(id)
            .orElseThrow(() ->
                new NaoEncontradoException("Evento não encontrado.")
            );
    }

    private EventoResponse resposta(Evento evento, LocalDate hoje) {
        boolean ativo = regra.estaAtivo(
            evento.getDataInicial(),
            evento.getDataFinal(),
            hoje
        );

        return new EventoResponse(
            evento.getId(),
            evento.getInstituicao().getId(),
            evento.getNome(),
            evento.getDataInicial(),
            evento.getDataFinal(),
            ativo
        );
    }

    private void validar(EventoRequest request) {
        if (request.instituicaoId() == null ||
                request.nome() == null ||
                request.nome().isBlank() ||
                request.dataInicial() == null ||
                request.dataFinal() == null) {
            throw new IllegalArgumentException(
                "Instituição, nome e datas são obrigatórios."
            );
        }

        if (request.dataFinal().isBefore(request.dataInicial())) {
            throw new IllegalArgumentException(
                "Data final não pode ser anterior à inicial."
            );
        }
    }
}