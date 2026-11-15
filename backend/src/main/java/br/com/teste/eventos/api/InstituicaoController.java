package br.com.teste.eventos.api;
import br.com.teste.eventos.service.InstituicaoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/instituicoes")
public class InstituicaoController {
	private final InstituicaoService service;
	public InstituicaoController(InstituicaoService service) {
		this.service=service;
	}
	@PostMapping public ResponseEntity<InstituicaoResponse> criar(@Valid @RequestBody InstituicaoRequest r) {
		var novo=service.criar(r);
		return ResponseEntity.created(URI.create("/api/instituicoes/"+novo.id())).body(novo);
	}
	@GetMapping public List<InstituicaoResponse> listar() {
		return service.listar();
	}
	@GetMapping("/{id}") public InstituicaoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}
	@PutMapping("/{id}") public InstituicaoResponse atualizar(@PathVariable Long id,@Valid @RequestBody InstituicaoRequest r) {
		return service.atualizar(id,r);
	}
	@DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {
		service.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
