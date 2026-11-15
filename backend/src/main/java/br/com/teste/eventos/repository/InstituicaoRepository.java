package br.com.teste.eventos.repository;
import br.com.teste.eventos.domain.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InstituicaoRepository extends JpaRepository<Instituicao,Long> {}
