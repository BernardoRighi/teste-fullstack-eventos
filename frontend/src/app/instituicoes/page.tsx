"use client";
import { FormEvent, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api, Instituicao, InstituicaoInput, TipoInstituicao } from "@/lib/api";
const vazio: InstituicaoInput = { nome: "", tipo: "COOPERATIVA" };
const tipos: TipoInstituicao[] = [
  "CONFEDERACAO",
  "SINGULAR",
  "CENTRAL",
  "COOPERATIVA",
];
export default function InstituicoesPage() {
  const [lista, setLista] = useState<Instituicao[]>([]),
    [form, setForm] = useState<InstituicaoInput>(vazio),
    [id, setId] = useState<number | null>(null);
  const [erro, setErro] = useState(""),
    [ocupado, setOcupado] = useState(false);
  const carregar = useCallback(async () => {
    setLista(await api<Instituicao[]>("/instituicoes"));
  }, []);
  useEffect(() => {
    void carregar().catch((e) => setErro((e as Error).message));
  }, [carregar]);
  async function salvar(e: FormEvent) {
    e.preventDefault();
    setErro("");
    setOcupado(true);
    try {
      await api(id === null ? "/instituicoes" : `/instituicoes/${id}`, {
        method: id === null ? "POST" : "PUT",
        body: JSON.stringify({ ...form, nome: form.nome.trim() }),
      });
      await carregar();
      setId(null);
      setForm(vazio);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setOcupado(false);
    }
  }
  async function excluir(i: Instituicao) {
    if (!window.confirm(`Excluir ${i.nome}?`)) return;
    setErro("");
    try {
      await api<void>(`/instituicoes/${i.id}`, { method: "DELETE" });
      await carregar();
    } catch (e) {
      setErro((e as Error).message);
    }
  }
  return (
    <main className="container">
      <nav>
        <Link href="/">Início</Link>
        <Link href="/eventos">Eventos</Link>
      </nav>
      <h1>Instituições</h1>
      <form onSubmit={salvar}>
        <h2>{id === null ? "Cadastrar" : "Editar"} instituição</h2>
        <label htmlFor="nome">Nome</label>
        <input
          id="nome"
          required
          maxLength={150}
          value={form.nome}
          onChange={(e) => setForm({ ...form, nome: e.target.value })}
        />
        <label htmlFor="tipo">Tipo</label>
        <select
          id="tipo"
          value={form.tipo}
          onChange={(e) =>
            setForm({ ...form, tipo: e.target.value as TipoInstituicao })
          }
        >
          {tipos.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
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
      <h2>Instituições cadastradas</h2>
      <ul>
        {lista.map((i) => (
          <li key={i.id}>
            <span>
              {i.nome} — {i.tipo}
            </span>
            <div className="actions">
              <button
                type="button"
                onClick={() => {
                  setId(i.id);
                  setForm({ nome: i.nome, tipo: i.tipo });
                }}
              >
                Editar
              </button>
              <button type="button" onClick={() => void excluir(i)}>
                Excluir
              </button>
            </div>
          </li>
        ))}
      </ul>
    </main>
  );
}
