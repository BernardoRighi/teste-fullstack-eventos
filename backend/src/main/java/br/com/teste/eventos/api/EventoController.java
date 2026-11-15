package br.com.teste.eventos.api;
import br.com.teste.eventos.service.EventoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/eventos")
public class EventoController {
	private final EventoService service;
	public EventoController(EventoService service) {
		this.service=service;
	}
	@PostMapping public ResponseEntity<EventoResponse> criar(@Valid @RequestBody EventoRequest r) {
		var novo=service.criar(r);
		return ResponseEntity.created(URI.create("/api/eventos/"+novo.id())).body(novo);
	}
	@GetMapping public List<EventoResponse> listar(@RequestParam(required=false) Long instituicaoId,@RequestParam(defaultValue="false") boolean somenteAtivos) {
		return service.listar(instituicaoId,somenteAtivos);
	}
	@GetMapping("/{id}") public EventoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}
	@PutMapping("/{id}") public EventoResponse atualizar(@PathVariable Long id,@Valid @RequestBody EventoRequest r) {
		return service.atualizar(id,r);
	}
	@DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) {
		service.excluir(id);
		return ResponseEntity.noContent().build();
	}
}
