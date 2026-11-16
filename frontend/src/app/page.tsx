import Link from "next/link";
export default function Home() {
  return (
    <main className="container">
      <h1>Gestão de eventos</h1>
      <p>Cadastre instituições e acompanhe a vigência dos eventos.</p>
      <nav>
        <Link href="/instituicoes">Instituições</Link>
        <Link href="/eventos">Eventos</Link>
      </nav>
    </main>
  );
}
