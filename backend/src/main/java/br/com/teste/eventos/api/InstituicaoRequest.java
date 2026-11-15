package br.com.teste.eventos.api;
import br.com.teste.eventos.domain.TipoInstituicao;
import jakarta.validation.constraints.*;
public record InstituicaoRequest(@NotBlank @Size(max=150) String nome,@NotNull TipoInstituicao tipo) {}
