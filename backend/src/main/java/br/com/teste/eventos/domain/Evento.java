package br.com.teste.eventos.domain;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="evento")
public class Evento {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
	@ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="instituicao_id",nullable=false) private Instituicao instituicao;
	@Column(nullable=false,length=150) private String nome;
	@Column(name="data_inicial",nullable=false) private LocalDate dataInicial;
	@Column(name="data_final",nullable=false) private LocalDate dataFinal;
	@Column(nullable=false) private boolean ativo;
	protected Evento() {}
	public Evento(Instituicao instituicao,String nome,LocalDate dataInicial,LocalDate dataFinal,boolean ativo) {
		atualizar(instituicao,nome,dataInicial,dataFinal,ativo);
	}
	public void atualizar(Instituicao instituicao,String nome,LocalDate dataInicial,LocalDate dataFinal,boolean ativo) {
		this.instituicao=instituicao;
		this.nome=nome;
		this.dataInicial=dataInicial;
		this.dataFinal=dataFinal;
		this.ativo=ativo;
	}
	public Long getId() {
		return id;
	} public Instituicao getInstituicao() {
		return instituicao;
	} public String getNome() {
		return nome;
	}
	public LocalDate getDataInicial() {
		return dataInicial;
	} public LocalDate getDataFinal() {
		return dataFinal;
	} public boolean isAtivo() {
		return ativo;
	}
}