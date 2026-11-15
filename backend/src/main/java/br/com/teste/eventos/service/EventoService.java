package br.com.teste.eventos.service;
import br.com.teste.eventos.api.*;
import br.com.teste.eventos.domain.*;
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
	public EventoService(EventoRepository eventos,InstituicaoService instituicoes,RegraVigencia regra,Clock clock) {
		this.eventos=eventos;
		this.instituicoes=instituicoes;
		this.regra=regra;
		this.clock=clock;
	}
	@Transactional public EventoResponse criar(EventoRequest r) {
		validar(r);
		Instituicao i=instituicoes.obter(r.instituicaoId());
		return resposta(eventos.save(new Evento(i,r.nome().trim(),r.dataInicial(),r.dataFinal(),regra.estaAtivo(r.dataInicial(),r.dataFinal(),hoje()))));
	}
	@Transactional public EventoResponse atualizar(Long id,EventoRequest r) {
		validar(r);
		Evento e=obter(id);
		Instituicao i=instituicoes.obter(r.instituicaoId());
		e.atualizar(i,r.nome().trim(),r.dataInicial(),r.dataFinal(),regra.estaAtivo(r.dataInicial(),r.dataFinal(),hoje()));
		return resposta(e);
	}
	@Transactional(readOnly=true) public EventoResponse buscar(Long id) {
		return resposta(obter(id));
	}
	@Transactional(readOnly=true) public List<EventoResponse> listar(Long instituicaoId,boolean somenteAtivos) {
		if(somenteAtivos && instituicaoId!=null)throw new IllegalArgumentException("Use apenas um filtro por vez.");
		if(somenteAtivos)return eventos.vigentes(hoje()).stream().map(this::resposta).toList();
		if(instituicaoId==null)throw new IllegalArgumentException("Informe instituicaoId ou somenteAtivos=true.");
		instituicoes.obter(instituicaoId);
		return eventos.porInstituicao(instituicaoId).stream().map(this::resposta).toList();
	}
	@Transactional public void excluir(Long id) {
		eventos.delete(obter(id));
	}
	private Evento obter(Long id) {
		return eventos.findById(id).orElseThrow(()->new NaoEncontradoException("Evento não encontrado."));
	}
	private EventoResponse resposta(Evento e) {
		return new EventoResponse(e.getId(),e.getInstituicao().getId(),e.getNome(),e.getDataInicial(),e.getDataFinal(),regra.estaAtivo(e.getDataInicial(),e.getDataFinal(),hoje()));
	}
	private LocalDate hoje() {
		return LocalDate.now(clock);
	}
	private void validar(EventoRequest r) {
		if(r.dataInicial()!=null && r.dataFinal()!=null && r.dataFinal().isBefore(r.dataInicial()))throw new IllegalArgumentException("Data final não pode ser anterior à inicial.");
	}
}
