"use client";

import { useState } from "react";
import type { FormEvent } from "react";
import Link from "next/link";

import { api } from "@/lib/api";
import type { Instituicao, InstituicaoInput, TipoInstituicao } from "@/lib/api";

import { useListaRemota } from "@/hooks/useListaRemota";

const formularioVazio: InstituicaoInput = {
  nome: "",
  tipo: "COOPERATIVA",
};

const tiposInstituicao: {
  valor: TipoInstituicao;
  descricao: string;
}[] = [
  {
    valor: "CONFEDERACAO",
    descricao: "Confederação",
  },
  {
    valor: "SINGULAR",
    descricao: "Singular",
  },
  {
    valor: "CENTRAL",
    descricao: "Central",
  },
  {
    valor: "COOPERATIVA",
    descricao: "Cooperativa",
  },
];

function descreverTipo(tipo: TipoInstituicao): string {
  return (
    tiposInstituicao.find((item) => item.valor === tipo)?.descricao ?? tipo
  );
}

function mensagemErro(exception: unknown): string {
  return exception instanceof Error
    ? exception.message
    : "Ocorreu um erro inesperado.";
}

export default function InstituicoesPage() {
  const [formulario, setFormulario] =
    useState<InstituicaoInput>(formularioVazio);

  const [instituicaoEmEdicao, setInstituicaoEmEdicao] = useState<number | null>(
    null,
  );

  const [erroOperacao, setErroOperacao] = useState("");
  const [mensagemSucesso, setMensagemSucesso] = useState("");

  const [salvando, setSalvando] = useState(false);

  const [excluindoId, setExcluindoId] = useState<number | null>(null);

  const ocupado = salvando || excluindoId !== null;

  const {
    dados: instituicoes,
    carregando: carregandoInstituicoes,
    erro: erroConsulta,
    atualizar: atualizarInstituicoes,
  } = useListaRemota<Instituicao>("/instituicoes");

  function limparFormulario() {
    setInstituicaoEmEdicao(null);
    setFormulario(formularioVazio);
  }

  function cancelarEdicao() {
    if (ocupado) return;

    limparFormulario();
    setErroOperacao("");
    setMensagemSucesso("");
  }

  function editarInstituicao(instituicao: Instituicao) {
    if (ocupado) return;

    setInstituicaoEmEdicao(instituicao.id);

    setFormulario({
      nome: instituicao.nome,
      tipo: instituicao.tipo,
    });

    setErroOperacao("");
    setMensagemSucesso("");

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  async function salvarInstituicao(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (ocupado) return;

    setErroOperacao("");
    setMensagemSucesso("");

    const nome = formulario.nome.trim();

    if (!nome) {
      setErroOperacao("Informe o nome da instituição.");
      return;
    }

    if (nome.length > 150) {
      setErroOperacao(
        "O nome da instituição deve ter no máximo 150 caracteres.",
      );
      return;
    }

    const payload: InstituicaoInput = {
      nome,
      tipo: formulario.tipo,
    };

    const editando = instituicaoEmEdicao !== null;

    setSalvando(true);

    try {
      await api<Instituicao>(
        editando ? `/instituicoes/${instituicaoEmEdicao}` : "/instituicoes",
        {
          method: editando ? "PUT" : "POST",
          body: JSON.stringify(payload),
        },
      );

      // A gravação foi concluída.
      // Limpa o formulário antes de atualizar a lista para evitar
      // novo envio caso somente a consulta seguinte falhe.
      limparFormulario();

      setMensagemSucesso(
        editando
          ? "Instituição atualizada com sucesso."
          : "Instituição cadastrada com sucesso.",
      );

      await atualizarInstituicoes();
    } catch (exception) {
      setErroOperacao(mensagemErro(exception));
    } finally {
      setSalvando(false);
    }
  }

  async function excluirInstituicao(instituicao: Instituicao) {
    if (ocupado) return;

    const confirmado = window.confirm(
      `Deseja excluir a instituição "${instituicao.nome}"?\n\n` +
        "Instituições com eventos vinculados não podem ser excluídas.",
    );

    if (!confirmado) return;

    setErroOperacao("");
    setMensagemSucesso("");
    setExcluindoId(instituicao.id);

    try {
      await api<void>(`/instituicoes/${instituicao.id}`, {
        method: "DELETE",
      });

      if (instituicaoEmEdicao === instituicao.id) {
        limparFormulario();
      }

      setMensagemSucesso("Instituição excluída com sucesso.");

      await atualizarInstituicoes();
    } catch (exception) {
      // A mensagem de HTTP 409 enviada pelo backend é exibida aqui
      // quando a instituição ainda possui eventos vinculados.
      setErroOperacao(mensagemErro(exception));
    } finally {
      setExcluindoId(null);
    }
  }

  return (
    <main className="container">
      <nav>
        <Link href="/">Início</Link>
        <Link href="/eventos">Eventos</Link>
      </nav>

      <h1>Instituições</h1>

      <form onSubmit={salvarInstituicao}>
        <h2>
          {instituicaoEmEdicao === null
            ? "Cadastrar instituição"
            : "Editar instituição"}
        </h2>

        <fieldset
          disabled={ocupado}
          style={{
            border: 0,
            padding: 0,
            margin: 0,
            minWidth: 0,
          }}
        >
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

          <label htmlFor="tipo">Tipo</label>

          <select
            id="tipo"
            required
            value={formulario.tipo}
            onChange={(event) =>
              setFormulario({
                ...formulario,
                tipo: event.target.value as TipoInstituicao,
              })
            }
          >
            {tiposInstituicao.map((tipo) => (
              <option key={tipo.valor} value={tipo.valor}>
                {tipo.descricao}
              </option>
            ))}
          </select>

          <div className="actions">
            <button type="submit">{salvando ? "Salvando..." : "Salvar"}</button>

            {instituicaoEmEdicao !== null && (
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

      <section
        aria-labelledby="titulo-consulta"
        aria-busy={carregandoInstituicoes}
      >
        <h2 id="titulo-consulta">Instituições cadastradas</h2>

        <p>
          Para excluir uma instituição, primeiro exclua seus eventos vinculados.
        </p>

        <button
          type="button"
          disabled={ocupado || carregandoInstituicoes}
          onClick={() => void atualizarInstituicoes()}
        >
          {carregandoInstituicoes ? "Atualizando..." : "Atualizar"}
        </button>

        {carregandoInstituicoes && (
          <p role="status">Carregando instituições...</p>
        )}

        {erroConsulta && (
          <p role="alert" className="error">
            Não foi possível atualizar a lista: {erroConsulta} Tente novamente
            pelo botão Atualizar.
          </p>
        )}

        {!carregandoInstituicoes &&
          !erroConsulta &&
          instituicoes.length === 0 && <p>Nenhuma instituição cadastrada.</p>}

        {!erroConsulta && (
          <ul>
            {instituicoes.map((instituicao) => (
              <li key={instituicao.id}>
                <span>
                  <strong>{instituicao.nome}</strong>
                  {" — "}
                  {descreverTipo(instituicao.tipo)}
                </span>

                <div className="actions">
                  <button
                    type="button"
                    disabled={ocupado || carregandoInstituicoes}
                    onClick={() => editarInstituicao(instituicao)}
                  >
                    Editar
                  </button>

                  <button
                    type="button"
                    disabled={ocupado || carregandoInstituicoes}
                    onClick={() => void excluirInstituicao(instituicao)}
                  >
                    {excluindoId === instituicao.id
                      ? "Excluindo..."
                      : "Excluir"}
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
