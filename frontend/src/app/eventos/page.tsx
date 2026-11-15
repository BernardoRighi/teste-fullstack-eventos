"use client";
import { FormEvent, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api, Evento, EventoInput, Instituicao } from "@/lib/api";
type Form = {
  instituicaoId: string;
  nome: string;
  dataInicial: string;
  dataFinal: string;
};
const vazio: Form = {
  instituicaoId: "",
  nome: "",
  dataInicial: "",
  dataFinal: "",
};
function formatarData(data: string): string {
  const [ano, mes, dia] = data.split("-");
  return `${dia}-${mes}-${ano}`;
}

export default function EventosPage() {
  const [instituicoes, setInstituicoes] = useState<Instituicao[]>([]),
    [lista, setLista] = useState<Evento[]>([]),
    [form, setForm] = useState<Form>(vazio),
    [filtro, setFiltro] = useState(""),
    [id, setId] = useState<number | null>(null),
    [erro, setErro] = useState(""),
    [ocupado, setOcupado] = useState(false);
  const carregar = useCallback(async (inst: string) => {
    if (!inst) {
      setLista([]);
      return;
    }
    setLista(
      await api<Evento[]>(`/eventos?instituicaoId=${encodeURIComponent(inst)}`),
    );
  }, []);
  useEffect(() => {
    void api<Instituicao[]>("/instituicoes")
      .then(setInstituicoes)
      .catch((e) => setErro((e as Error).message));
  }, []);
  useEffect(() => {
    void carregar(filtro).catch((e) => setErro((e as Error).message));
  }, [filtro, carregar]);
  useEffect(() => {
    const atualizar = () => {
      if (document.visibilityState === "visible")
        void carregar(filtro).catch((e) => setErro((e as Error).message));
    };
    document.addEventListener("visibilitychange", atualizar);
    const timer = window.setInterval(atualizar, 60_000);
    return () => {
      document.removeEventListener("visibilitychange", atualizar);
      window.clearInterval(timer);
    };
  }, [filtro, carregar]);
  async function salvar(e: FormEvent) {
    e.preventDefault();
    setErro("");
    if (
      !form.instituicaoId ||
      !form.nome.trim() ||
      !form.dataInicial ||
      !form.dataFinal
    ) {
      setErro("Preencha todos os campos.");
      return;
    }
    if (form.dataFinal < form.dataInicial) {
      setErro("Data final deve ser igual ou posterior à inicial.");
      return;
    }
    const payload: EventoInput = {
      instituicaoId: Number(form.instituicaoId),
      nome: form.nome.trim(),
      dataInicial: form.dataInicial,
      dataFinal: form.dataFinal,
    };
    setOcupado(true);
    try {
      await api<Evento>(id === null ? "/eventos" : `/eventos/${id}`, {
        method: id === null ? "POST" : "PUT",
        body: JSON.stringify(payload),
      });
      setFiltro(form.instituicaoId);
      await carregar(form.instituicaoId);
      setId(null);
      setForm(vazio);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setOcupado(false);
    }
  }
  async function excluir(item: Evento) {
    if (!window.confirm(`Excluir ${item.nome}?`)) return;
    setErro("");
    try {
      await api<void>(`/eventos/${item.id}`, { method: "DELETE" });
      await carregar(filtro);
      if (id === item.id) {
        setId(null);
        setForm(vazio);
      }
    } catch (e) {
      setErro((e as Error).message);
    }
  }
  return (
    <main className="container">
      <nav>
        <Link href="/">Início</Link>
        <Link href="/instituicoes">Instituições</Link>
      </nav>
      <h1>Eventos</h1>
      <form onSubmit={salvar}>
        <h2>{id === null ? "Cadastrar" : "Editar"} evento</h2>
        <label htmlFor="instituicao">Instituição</label>
        <select
          id="instituicao"
          required
          value={form.instituicaoId}
          onChange={(e) => setForm({ ...form, instituicaoId: e.target.value })}
        >
          <option value="">Selecione</option>
          {instituicoes.map((i) => (
            <option key={i.id} value={i.id}>
              {i.nome}
            </option>
          ))}
        </select>
        <label htmlFor="nome">Nome</label>
        <input
          id="nome"
          required
          maxLength={150}
          value={form.nome}
          onChange={(e) => setForm({ ...form, nome: e.target.value })}
        />
        <label htmlFor="inicio">Data inicial</label>
        <input
          id="inicio"
          type="date"
          required
          value={form.dataInicial}
          onChange={(e) => setForm({ ...form, dataInicial: e.target.value })}
        />
        <label htmlFor="fim">Data final</label>
        <input
          id="fim"
          type="date"
          required
          min={form.dataInicial || undefined}
          value={form.dataFinal}
          onChange={(e) => setForm({ ...form, dataFinal: e.target.value })}
        />
        <p>
          O status é calculado automaticamente; as duas datas são inclusivas.
        </p>
        <div className="actions">
          <button disabled={ocupado}>Salvar</button>
          {id !== null && (
            <button
              type="button"
              onClick={() => {
                setId(null);
                setForm(vazio);
              }}
            >
              Cancelar
            </button>
          )}
        </div>
      </form>
      {erro && (
        <p role="alert" className="error">
          {erro}
        </p>
      )}
      <h2>Consultar eventos</h2>
      <label htmlFor="filtro">Instituição</label>
      <select
        id="filtro"
        value={filtro}
        onChange={(e) => setFiltro(e.target.value)}
      >
        <option value="">Selecione</option>
        {instituicoes.map((i) => (
          <option key={i.id} value={i.id}>
            {i.nome}
          </option>
        ))}
      </select>
      <button
        type="button"
        onClick={() =>
          void carregar(filtro).catch((e) => setErro((e as Error).message))
        }
      >
        Atualizar
      </button>
      <ul>
        {lista.map((item) => (
          <li key={item.id}>
            <span>
              <strong>{item.nome}</strong> — {formatarData(item.dataInicial)} a{" "}
              {formatarData(item.dataFinal)} —{" "}
              <span className={item.ativo ? "active" : "inactive"}>
                {item.ativo ? "Ativo" : "Inativo"}
              </span>
            </span>
            <div className="actions">
              <button
                type="button"
                onClick={() => {
                  setId(item.id);
                  setForm({
                    instituicaoId: String(item.instituicaoId),
                    nome: item.nome,
                    dataInicial: item.dataInicial,
                    dataFinal: item.dataFinal,
                  });
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              >
                Editar
              </button>
              <button type="button" onClick={() => void excluir(item)}>
                Excluir
              </button>
            </div>
          </li>
        ))}
      </ul>
    </main>
  );
}
