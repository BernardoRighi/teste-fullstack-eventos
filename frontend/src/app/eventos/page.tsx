"use client";

import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import Link from "next/link";

import { api } from "@/lib/api";
import type { Evento, EventoInput, Instituicao } from "@/lib/api";

import { useListaRemota } from "@/hooks/useListaRemota";

type Formulario = {
  instituicaoId: string;
  nome: string;
  dataInicial: string;
  dataFinal: string;
};

const formularioVazio: Formulario = {
  instituicaoId: "",
  nome: "",
  dataInicial: "",
  dataFinal: "",
};

function formatarData(data: string): string {
  const [ano, mes, dia] = data.split("-");
  return `${dia}-${mes}-${ano}`;
}

function mensagemErro(exception: unknown): string {
  return exception instanceof Error
    ? exception.message
    : "Ocorreu um erro inesperado.";
}

export default function EventosPage() {
  const [formulario, setFormulario] = useState<Formulario>(formularioVazio);

  const [instituicaoFiltro, setInstituicaoFiltro] = useState("");

  const [eventoEmEdicao, setEventoEmEdicao] = useState<number | null>(null);

  const [erroOperacao, setErroOperacao] = useState("");
  const [mensagemSucesso, setMensagemSucesso] = useState("");

  const [salvando, setSalvando] = useState(false);

  const [excluindoId, setExcluindoId] = useState<number | null>(null);

  const ocupado = salvando || excluindoId !== null;

  const {
    dados: instituicoes,
    carregando: carregandoInstituicoes,
    erro: erroInstituicoes,
    atualizar: atualizarInstituicoes,
  } = useListaRemota<Instituicao>("/instituicoes");

  const {
    dados: eventos,
    carregando: carregandoEventos,
    erro: erroEventos,
    atualizar: atualizarEventos,
  } = useListaRemota<Evento>(
    instituicaoFiltro
      ? `/eventos?instituicaoId=${encodeURIComponent(instituicaoFiltro)}`
      : null,
  );

  // Atualiza os status quando a aba fica visível e a cada minuto.
  useEffect(() => {
    function atualizarSeVisivel() {
      if (
        !ocupado &&
        instituicaoFiltro &&
        document.visibilityState === "visible"
      ) {
        void atualizarEventos();
      }
    }

    document.addEventListener("visibilitychange", atualizarSeVisivel);

    const timer = window.setInterval(atualizarSeVisivel, 60_000);

    return () => {
      window.clearInterval(timer);

      document.removeEventListener("visibilitychange", atualizarSeVisivel);
    };
  }, [atualizarEventos, instituicaoFiltro, ocupado]);

  function limparFormulario() {
    setEventoEmEdicao(null);
    setFormulario(formularioVazio);
  }

  function cancelarEdicao() {
    if (ocupado) return;

    limparFormulario();
    setErroOperacao("");
    setMensagemSucesso("");
  }

  function editarEvento(evento: Evento) {
    if (ocupado) return;

    setEventoEmEdicao(evento.id);

    setFormulario({
      instituicaoId: String(evento.instituicaoId),
      nome: evento.nome,
      dataInicial: evento.dataInicial,
      dataFinal: evento.dataFinal,
    });

    setErroOperacao("");
    setMensagemSucesso("");

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  async function salvarEvento(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (ocupado) return;

    setErroOperacao("");
    setMensagemSucesso("");

    const nome = formulario.nome.trim();

    if (
      !formulario.instituicaoId ||
      !nome ||
      !formulario.dataInicial ||
      !formulario.dataFinal
    ) {
      setErroOperacao("Preencha todos os campos.");
      return;
    }

    // Os campos type="date" fornecem datas no formato YYYY-MM-DD.
    // Nesse formato, a comparação textual preserva a ordem das datas.
    if (formulario.dataFinal < formulario.dataInicial) {
      setErroOperacao(
        "A data final deve ser igual ou posterior à data inicial.",
      );
      return;
    }

    const payload: EventoInput = {
      instituicaoId: Number(formulario.instituicaoId),
      nome,
      dataInicial: formulario.dataInicial,
      dataFinal: formulario.dataFinal,
    };

    const instituicaoSalva = formulario.instituicaoId;
    const editando = eventoEmEdicao !== null;

    setSalvando(true);

    try {
      await api<Evento>(editando ? `/eventos/${eventoEmEdicao}` : "/eventos", {
        method: editando ? "PUT" : "POST",
        body: JSON.stringify(payload),
      });

      // A gravação terminou com sucesso.
      // Limpa antes de consultar para evitar um novo envio acidental
      // caso a atualização da listagem falhe.
      limparFormulario();

      setMensagemSucesso(
        editando
          ? "Evento atualizado com sucesso."
          : "Evento cadastrado com sucesso.",
      );

      if (instituicaoFiltro === instituicaoSalva) {
        await atualizarEventos();
      } else {
        // A mudança do filtro dispara a consulta pelo hook.
        setInstituicaoFiltro(instituicaoSalva);
      }
    } catch (exception) {
      setErroOperacao(mensagemErro(exception));
    } finally {
      setSalvando(false);
    }
  }

  async function excluirEvento(evento: Evento) {
    if (ocupado) return;

    const confirmado = window.confirm(
      `Deseja excluir o evento "${evento.nome}"?`,
    );

    if (!confirmado) return;

    setErroOperacao("");
    setMensagemSucesso("");
    setExcluindoId(evento.id);

    try {
      await api<void>(`/eventos/${evento.id}`, {
        method: "DELETE",
      });

      if (eventoEmEdicao === evento.id) {
        limparFormulario();
      }

      setMensagemSucesso("Evento excluído com sucesso.");

      await atualizarEventos();
    } catch (exception) {
      setErroOperacao(mensagemErro(exception));
    } finally {
      setExcluindoId(null);
    }
  }

  const formularioBloqueado =
    ocupado ||
    carregandoInstituicoes ||
    Boolean(erroInstituicoes) ||
    instituicoes.length === 0;

  return (
    <main className="container">
      <nav>
        <Link href="/">Início</Link>
        <Link href="/instituicoes">Instituições</Link>
      </nav>

      <h1>Eventos</h1>

      {carregandoInstituicoes && (
        <p role="status">Carregando instituições...</p>
      )}

      {erroInstituicoes && (
        <div>
          <p role="alert" className="error">
            Não foi possível carregar as instituições: {erroInstituicoes}
          </p>

          <button
            type="button"
            disabled={carregandoInstituicoes || ocupado}
            onClick={() => void atualizarInstituicoes()}
          >
            Tentar novamente
          </button>
        </div>
      )}

      {!carregandoInstituicoes &&
        !erroInstituicoes &&
        instituicoes.length === 0 && (
          <p>
            Cadastre uma instituição antes de cadastrar eventos.{" "}
            <Link href="/instituicoes">Ir para instituições</Link>
          </p>
        )}

      <form onSubmit={salvarEvento}>
        <h2>
          {eventoEmEdicao === null ? "Cadastrar evento" : "Editar evento"}
        </h2>

        <fieldset
          disabled={formularioBloqueado}
          style={{
            border: 0,
            padding: 0,
            margin: 0,
            minWidth: 0,
          }}
        >
          <label htmlFor="instituicao">Instituição</label>

          <select
            id="instituicao"
            required
            value={formulario.instituicaoId}
            onChange={(event) =>
              setFormulario({
                ...formulario,
                instituicaoId: event.target.value,
              })
            }
          >
            <option value="">Selecione</option>

            {instituicoes.map((instituicao) => (
              <option key={instituicao.id} value={instituicao.id}>
                {instituicao.nome}
              </option>
            ))}
          </select>

          <label htmlFor="nome">Nome</label>

          <input
            id="nome"
            type="text"
            required
            maxLength={150}
            value={formulario.nome}
            onChange={(event) =>
              setFormulario({
                ...formulario,
                nome: event.target.value,
              })
            }
          />

          <label htmlFor="inicio">Data inicial</label>

          <input
            id="inicio"
            type="date"
            required
            value={formulario.dataInicial}
            onChange={(event) =>
              setFormulario({
                ...formulario,
                dataInicial: event.target.value,
              })
            }
          />

          <label htmlFor="fim">Data final</label>

          <input
            id="fim"
            type="date"
            required
            min={formulario.dataInicial || undefined}
            value={formulario.dataFinal}
            onChange={(event) =>
              setFormulario({
                ...formulario,
                dataFinal: event.target.value,
              })
            }
          />

          <p>
            O status é calculado automaticamente; as duas datas são inclusivas.
          </p>

          <div className="actions">
            <button type="submit">{salvando ? "Salvando..." : "Salvar"}</button>

            {eventoEmEdicao !== null && (
              <button type="button" onClick={cancelarEdicao}>
                Cancelar edição
              </button>
            )}
          </div>
        </fieldset>
      </form>

      {erroOperacao && (
        <p role="alert" className="error">
          {erroOperacao}
        </p>
      )}

      {mensagemSucesso && <p role="status">{mensagemSucesso}</p>}

      <section aria-labelledby="titulo-consulta" aria-busy={carregandoEventos}>
        <h2 id="titulo-consulta">Consultar eventos</h2>

        <label htmlFor="filtro">Instituição</label>

        <select
          id="filtro"
          value={instituicaoFiltro}
          disabled={
            ocupado || carregandoInstituicoes || Boolean(erroInstituicoes)
          }
          onChange={(event) => {
            setInstituicaoFiltro(event.target.value);
            setErroOperacao("");
            setMensagemSucesso("");
          }}
        >
          <option value="">Selecione</option>

          {instituicoes.map((instituicao) => (
            <option key={instituicao.id} value={instituicao.id}>
              {instituicao.nome}
            </option>
          ))}
        </select>

        <button
          type="button"
          disabled={ocupado || carregandoEventos || !instituicaoFiltro}
          onClick={() => void atualizarEventos()}
        >
          {carregandoEventos ? "Atualizando..." : "Atualizar"}
        </button>

        {!instituicaoFiltro && (
          <p>Selecione uma instituição para consultar seus eventos.</p>
        )}

        {carregandoEventos && <p role="status">Carregando eventos...</p>}

        {erroEventos && (
          <p role="alert" className="error">
            Não foi possível atualizar a lista: {erroEventos} Tente novamente
            pelo botão Atualizar.
          </p>
        )}

        {instituicaoFiltro &&
          !carregandoEventos &&
          !erroEventos &&
          eventos.length === 0 && (
            <p>Nenhum evento encontrado para esta instituição.</p>
          )}

        {!erroEventos && (
          <ul>
            {eventos.map((evento) => (
              <li key={evento.id}>
                <span>
                  <strong>{evento.nome}</strong>
                  {" — "}
                  {formatarData(evento.dataInicial)}
                  {" a "}
                  {formatarData(evento.dataFinal)}
                  {" — "}

                  <span className={evento.ativo ? "active" : "inactive"}>
                    {evento.ativo ? "Ativo" : "Inativo"}
                  </span>
                </span>

                <div className="actions">
                  <button
                    type="button"
                    disabled={ocupado || carregandoEventos}
                    onClick={() => editarEvento(evento)}
                  >
                    Editar
                  </button>

                  <button
                    type="button"
                    disabled={ocupado || carregandoEventos}
                    onClick={() => void excluirEvento(evento)}
                  >
                    {excluindoId === evento.id ? "Excluindo..." : "Excluir"}
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
