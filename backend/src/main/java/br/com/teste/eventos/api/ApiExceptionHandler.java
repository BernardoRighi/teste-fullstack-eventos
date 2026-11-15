package br.com.teste.eventos.api;
import br.com.teste.eventos.service.*;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class ApiExceptionHandler {
	private ResponseEntity<Erro> erro(HttpStatus status,String detalhe) {
		return ResponseEntity.status(status).body(new Erro(status.value(),detalhe,Instant.now()));
	}
	@ExceptionHandler(NaoEncontradoException.class) ResponseEntity<Erro> notFound(NaoEncontradoException ex) {
		return erro(HttpStatus.NOT_FOUND,ex.getMessage());
	}
	@ExceptionHandler(ConflitoException.class) ResponseEntity<Erro> conflito(ConflitoException ex) {
		return erro(HttpStatus.CONFLICT,ex.getMessage());
	}
	@ExceptionHandler(IllegalArgumentException.class) ResponseEntity<Erro> invalido(IllegalArgumentException ex) {
		return erro(HttpStatus.BAD_REQUEST,ex.getMessage());
	}
	@ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Erro> validacao(MethodArgumentNotValidException ex) {
		var primeiro=ex.getBindingResult().getFieldErrors().stream().findFirst();
		return erro(HttpStatus.BAD_REQUEST,primeiro.map(f->f.getField()+": "+f.getDefaultMessage()).orElse("Dados inválidos."));
	}
	@ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<Erro> json(HttpMessageNotReadableException ex) {
		return erro(HttpStatus.BAD_REQUEST,"JSON inválido; confira os campos, tipo e datas no formato AAAA-MM-DD.");
	}
	public record Erro(int status,String detail,Instant timestamp) {}
}
