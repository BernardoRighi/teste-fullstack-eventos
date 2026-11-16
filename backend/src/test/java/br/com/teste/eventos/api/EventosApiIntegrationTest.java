package br.com.teste.eventos.api;

import br.com.teste.eventos.domain.Evento;
import br.com.teste.eventos.domain.Instituicao;
import br.com.teste.eventos.domain.TipoInstituicao;
import br.com.teste.eventos.repository.EventoRepository;
import br.com.teste.eventos.repository.InstituicaoRepository;
import br.com.teste.eventos.service.SincronizacaoEventos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    properties = "app.sincronizacao.cron=-",
classes = {
	br.com.teste.eventos.EventosApplication.class,
	EventosApiIntegrationTest.RelogioTesteConfig.class
}
)
@AutoConfigureMockMvc
@Testcontainers
class EventosApiIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres =
	    new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private EventoRepository eventos;

	@Autowired
	private InstituicaoRepository instituicoes;

	@Autowired
	private SincronizacaoEventos sincronizacao;

	private Long instituicaoId;

	private final LocalDate hoje = LocalDate.of(2026, 11, 15);

	@BeforeEach
	void preparar() {
		eventos.deleteAll();
		instituicoes.deleteAll();

		Instituicao instituicao = instituicoes.saveAndFlush(
		                              new Instituicao(
		                                  "Instituição Teste",
		                                  TipoInstituicao.COOPERATIVA
		                              )
		                          );

		instituicaoId = instituicao.getId();
	}

	@Test
	void deveCriarEventoEPersistirDados() throws Exception {
		JsonNode criado = criarEvento(
		                      instituicaoId,
		                      hoje,
		                      hoje.plusDays(1)
		                  );

		Long id = criado.get("id").asLong();

		assertTrue(eventos.existsById(id));
		assertEquals("Evento Teste", criado.get("nome").asText());
		assertTrue(criado.get("ativo").asBoolean());
	}

	@Test
	void deveRejeitarPeriodoInvalido() throws Exception {
		mvc.perform(
		    post("/api/eventos")
		    .contentType(MediaType.APPLICATION_JSON)
		    .content(corpo(
		                 instituicaoId,
		                 hoje,
		                 hoje.minusDays(1)
		             ))
		).andExpect(status().isBadRequest());

		assertEquals(0, eventos.count());
	}

	@Test
	void deveRetornar404ParaInstituicaoInexistente()
	throws Exception {
		mvc.perform(
		    post("/api/eventos")
		    .contentType(MediaType.APPLICATION_JSON)
		    .content(corpo(
		                 Long.MAX_VALUE,
		                 hoje,
		                 hoje
		             ))
		).andExpect(status().isNotFound());

		assertEquals(0, eventos.count());
	}

	@Test
	void deveImpedirExclusaoDeInstituicaoComEventos()
	throws Exception {
		criarEvento(instituicaoId, hoje, hoje);

		mvc.perform(
		    delete("/api/instituicoes/{id}", instituicaoId)
		).andExpect(status().isConflict());

		assertTrue(instituicoes.existsById(instituicaoId));
	}

	@Test
	void deveAtualizarDatasEInstituicao() throws Exception {
		JsonNode criado = criarEvento(instituicaoId, hoje, hoje);
		Long eventoId = criado.get("id").asLong();

		Long outraInstituicaoId = instituicoes.saveAndFlush(
		                              new Instituicao(
		                                  "Outra Instituição",
		                                  TipoInstituicao.CENTRAL
		                              )
		                          ).getId();

		mvc.perform(
		    put("/api/eventos/{id}", eventoId)
		    .contentType(MediaType.APPLICATION_JSON)
		    .content(corpo(
		                 outraInstituicaoId,
		                 hoje.plusDays(1),
		                 hoje.plusDays(2)
		             ))
		)
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.instituicaoId")
		           .value(outraInstituicaoId))
		.andExpect(jsonPath("$.dataInicial")
		           .value("2026-11-16"))
		.andExpect(jsonPath("$.ativo").value(false));

		mvc.perform(get("/api/eventos/{id}", eventoId))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.dataFinal")
		           .value("2026-11-17"))
		.andExpect(jsonPath("$.ativo").value(false));
	}

	@Test
	void deveRetornarEventoEncerradoComoInativo()
	throws Exception {
		JsonNode criado = criarEvento(
		                      instituicaoId,
		                      hoje.minusDays(2),
		                      hoje.minusDays(1)
		                  );

		mvc.perform(
		    get("/api/eventos/{id}", criado.get("id").asLong())
		)
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.ativo").value(false));
	}

	@Test
	void deveSincronizarColunaAtivoDeFormaIdempotente() {
		Instituicao instituicao =
		    instituicoes.findById(instituicaoId).orElseThrow();

		Long vigenteId = eventos.saveAndFlush(
		                     new Evento(
		                         instituicao,
		                         "Vigente",
		                         hoje,
		                         hoje,
		                         false
		                     )
		                 ).getId();

		Long encerradoId = eventos.saveAndFlush(
		                       new Evento(
		                           instituicao,
		                           "Encerrado",
		                           hoje.minusDays(2),
		                           hoje.minusDays(1),
		                           true
		                       )
		                   ).getId();

		sincronizacao.executar();

		assertTrue(
		    eventos.findById(vigenteId).orElseThrow().isAtivo()
		);
		assertFalse(
		    eventos.findById(encerradoId).orElseThrow().isAtivo()
		);

		sincronizacao.executar();

		assertTrue(
		    eventos.findById(vigenteId).orElseThrow().isAtivo()
		);
		assertFalse(
		    eventos.findById(encerradoId).orElseThrow().isAtivo()
		);
	}

	@Test
	void deveRejeitarNomeVazio() throws Exception {
		String json = mapper.writeValueAsString(
		                  Map.of(
		                      "instituicaoId", instituicaoId,
		                      "nome", "   ",
		                      "dataInicial", hoje.toString(),
		                      "dataFinal", hoje.toString()
		                  )
		              );

		mvc.perform(
		    post("/api/eventos")
		    .contentType(MediaType.APPLICATION_JSON)
		    .content(json)
		).andExpect(status().isBadRequest());

		assertEquals(0, eventos.count());
	}

	@Test
	void deveRejeitarIdentificadorNaoNumerico() throws Exception {
		mvc.perform(get("/api/eventos/abc"))
		.andExpect(status().isBadRequest());
	}

	private JsonNode criarEvento(
	    Long idInstituicao,
	    LocalDate inicio,
	    LocalDate fim
	) throws Exception {
		String resposta = mvc.perform(
		                      post("/api/eventos")
		                      .contentType(MediaType.APPLICATION_JSON)
		                      .content(corpo(idInstituicao, inicio, fim))
		                  )
		                  .andExpect(status().isCreated())
		                  .andReturn()
		                  .getResponse()
		                  .getContentAsString();

		return mapper.readTree(resposta);
	}

	private String corpo(
	    Long idInstituicao,
	    LocalDate inicio,
	    LocalDate fim
	) throws Exception {
		return mapper.writeValueAsString(
		           Map.of(
		               "instituicaoId", idInstituicao,
		               "nome", "Evento Teste",
		               "dataInicial", inicio.toString(),
		               "dataFinal", fim.toString()
		           )
		       );
	}

	@Test
	void deveExigirFiltroNaListagem() throws Exception {
		mvc.perform(get("/api/eventos"))
		.andExpect(status().isBadRequest());
	}

	@Test
	void deveListarSomenteEventosDaInstituicao() throws Exception {
		criarEvento(instituicaoId, hoje, hoje);

		Long outraInstituicaoId = instituicoes.saveAndFlush(
		                              new Instituicao(
		                                  "Outra Instituição",
		                                  TipoInstituicao.CENTRAL
		                              )
		                          ).getId();

		criarEvento(outraInstituicaoId, hoje, hoje);

		mvc.perform(
		    get("/api/eventos")
		    .param("instituicaoId", instituicaoId.toString())
		)
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.length()").value(1))
		.andExpect(jsonPath("$[0].instituicaoId")
		           .value(instituicaoId));
	}


	@TestConfiguration
	static class RelogioTesteConfig {

		@Bean
		@Primary
		Clock relogioFixo() {
			return Clock.fixed(
			           Instant.parse("2026-11-15T15:00:00Z"),
			           ZoneId.of("America/Sao_Paulo")
			       );
		}
	}
}