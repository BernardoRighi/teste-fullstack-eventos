package br.com.teste.eventos.api;
import br.com.teste.eventos.domain.Instituicao;
import br.com.teste.eventos.domain.TipoInstituicao;
public record InstituicaoResponse(Long id,String nome,TipoInstituicao tipo) {
	public static InstituicaoResponse de(Instituicao i) {
		return new InstituicaoResponse(i.getId(),i.getNome(),i.getTipo());
	}
}
