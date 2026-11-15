package br.com.teste.eventos.service;
import br.com.teste.eventos.api.*;
import br.com.teste.eventos.domain.Instituicao;
import br.com.teste.eventos.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class InstituicaoService {
	private final InstituicaoRepository instituicoes;
	private final EventoRepository eventos;
	public InstituicaoService(InstituicaoRepository instituicoes,EventoRepository eventos) {
		this.instituicoes=instituicoes;
		this.eventos=eventos;
	}
	@Transactional public InstituicaoResponse criar(InstituicaoRequest r) {
		return InstituicaoResponse.de(instituicoes.save(new Instituicao(r.nome().trim(),r.tipo())));
	}
	@Transactional(readOnly=true) public List<InstituicaoResponse> listar() {
		return instituicoes.findAll().stream().map(InstituicaoResponse::de).toList();
	}
	@Transactional(readOnly=true) public InstituicaoResponse buscar(Long id) {
		return InstituicaoResponse.de(obter(id));
	}
	@Transactional public InstituicaoResponse atualizar(Long id,InstituicaoRequest r) {
		Instituicao i=obter(id);
		i.atualizar(r.nome().trim(),r.tipo());
		return InstituicaoResponse.de(i);
	}
	@Transactional public void excluir(Long id) {
		Instituicao i=obter(id);
		if(eventos.existsByInstituicaoId(id))throw new ConflitoException("Instituição possui eventos; exclua-os antes.");
		instituicoes.delete(i);
	}
	public Instituicao obter(Long id) {
		return instituicoes.findById(id).orElseThrow(()->new NaoEncontradoException("Instituição não encontrada."));
	}
}
