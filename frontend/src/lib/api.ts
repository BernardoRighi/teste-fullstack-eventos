export type TipoInstituicao = "CONFEDERACAO" | "SINGULAR" | "CENTRAL" | "COOPERATIVA";
export type Instituicao = { id: number; nome: string; tipo: TipoInstituicao };
export type InstituicaoInput = Omit<Instituicao, "id">;
export type Evento = { id: number; instituicaoId: number; nome: string; dataInicial: string; dataFinal: string; ativo: boolean };
export type EventoInput = Omit<Evento, "id" | "ativo">;
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`/backend${path}`, { ...options, cache: "no-store", headers: { "Content-Type": "application/json", ...options.headers } });
  if (!response.ok) {
    let detail = `Erro HTTP ${response.status}`;
    try { const body: { detail?: string } = await response.json(); detail = body.detail || detail; } catch { /* resposta sem JSON */ }
    throw new Error(detail);
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}
