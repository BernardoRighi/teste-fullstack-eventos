package br.com.teste.eventos.repository;
import br.com.teste.eventos.domain.Evento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface EventoRepository extends JpaRepository<Evento,Long> {
	@Query("select e from Evento e join fetch e.instituicao where e.instituicao.id = :id order by e.id desc")
	List<Evento> porInstituicao(@Param("id") Long id);
	@Query("select e from Evento e join fetch e.instituicao where e.dataInicial <= :hoje and e.dataFinal >= :hoje order by e.id desc")
	List<Evento> vigentes(@Param("hoje") LocalDate hoje);
	@Modifying(clearAutomatically=true,flushAutomatically=true)
	@Query("update Evento e set e.ativo = case when e.dataInicial <= :hoje and e.dataFinal >= :hoje then true else false end where (e.ativo = true and (e.dataInicial > :hoje or e.dataFinal < :hoje)) or (e.ativo = false and e.dataInicial <= :hoje and e.dataFinal >= :hoje)")
	int sincronizar(@Param("hoje") LocalDate hoje);
	boolean existsByInstituicaoId(Long id);
}
