package br.com.teste.eventos.api;
import java.time.LocalDate;
public record EventoResponse(Long id,Long instituicaoId,String nome,LocalDate dataInicial,LocalDate dataFinal,boolean ativo) {}
