package br.com.teste.eventos.api;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record EventoRequest(@NotNull Long instituicaoId,@NotBlank @Size(max=150) String nome,@NotNull LocalDate dataInicial,@NotNull LocalDate dataFinal) {}
