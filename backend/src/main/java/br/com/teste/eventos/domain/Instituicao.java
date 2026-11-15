package br.com.teste.eventos.domain;
import jakarta.persistence.*;
@Entity @Table(name="instituicao")
public class Instituicao {
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
	@Column(nullable=false,length=150) private String nome;
	@Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private TipoInstituicao tipo;
	protected Instituicao() {}
	public Instituicao(String nome, TipoInstituicao tipo) {
		atualizar(nome,tipo);
	}
	public void atualizar(String nome, TipoInstituicao tipo) {
		this.nome=nome;
		this.tipo=tipo;
	}
	public Long getId() {
		return id;
	} public String getNome() {
		return nome;
	} public TipoInstituicao getTipo() {
		return tipo;
	}
}
