"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";

export function useListaRemota<T>(rota: string | null) {
  const [dados, setDados] = useState<T[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");

  const controlador = useRef<AbortController | null>(null);

  const atualizar = useCallback(async () => {
    controlador.current?.abort();

    if (!rota) {
      setDados([]);
      setErro("");
      setCarregando(false);
      return;
    }

    const atual = new AbortController();
    controlador.current = atual;

    setCarregando(true);
    setErro("");

    try {
      const resposta = await api<T[]>(rota, {
        signal: atual.signal,
      });

      if (!atual.signal.aborted) {
        setDados(resposta);
      }
    } catch (exception) {
      if (!atual.signal.aborted) {
        setErro(
          exception instanceof Error
            ? exception.message
            : "Não foi possível carregar os dados.",
        );
      }
    } finally {
      if (!atual.signal.aborted) {
        setCarregando(false);
      }
    }
  }, [rota]);

  useEffect(() => {
    setDados([]);
    void atualizar();

    return () => {
      controlador.current?.abort();
    };
  }, [atualizar]);

  return {
    dados,
    carregando,
    erro,
    atualizar,
  };
}
