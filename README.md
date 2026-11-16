# Desafio Fullstack — Java + Next.js

## Escopo

CRUD completo de instituições e eventos; cada evento pertence a uma instituição. Datas de vigência são inclusivas. Não há autenticação, conforme enunciado. Projeto didático: antes de publicar, configurar senhas reais, políticas de CORS/rede, observabilidade e controle de concorrência.

## Pré-requisitos

Java 21, Maven 3.9+, Node.js 20.9+, npm e Docker com Compose. As versões das bibliotecas estão fixadas no pom.xml/package.json; em ambiente corporativo, revisar atualizações de segurança antes do deploy.

## Iniciar

Na raiz:

```bash
docker compose up -lockfile
cd backend && mvn spring-boot:run
```

Em outro terminal, na raiz:

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Abra http://localhost:3000; Java responde em http://localhost:8080/api e health em http://localhost:8080/actuator/health. Em Windows, copie `.env.example` manualmente para `.env.local`.

## Testes e build

```bash
cd backend && mvn test && mvn package
cd frontend && npm run build
```

## API

| Método         | Rota                   | Descrição                |
| -------------- | ---------------------- | ------------------------ |
| GET/POST       | /api/instituicoes      | Listar/criar             |
| GET/PUT/DELETE | /api/instituicoes/{id} | Consultar/editar/excluir |
| GET/POST       | /api/eventos           | Listar filtrado/criar    |
| GET/PUT/DELETE | /api/eventos/{id}      | Consultar/editar/excluir |

`GET /api/eventos?instituicaoId=1` ou `GET /api/eventos?somenteAtivos=true` (filtros mutuamente exclusivos). Excluir instituição com eventos retorna 409; primeiro exclua seus eventos.

Exemplos:

```bash
curl -X POST http://localhost:8080/api/instituicoes -H 'Content-Type: application/json' -d '{"nome":"Unidade A","tipo":"COOPERATIVA"}'
curl -X POST http://localhost:8080/api/eventos -H 'Content-Type: application/json' -d '{"instituicaoId":1,"nome":"Assembleia","dataInicial":"2026-11-10","dataFinal":"2026-11-12"}'
```

Tipos: `CONFEDERACAO`, `SINGULAR`, `CENTRAL`, `COOPERATIVA`. Datas ISO `YYYY-MM-DD`. `ativo` não é aceito na entrada.

## Por que esta arquitetura?

- **Monólito modular, não microserviços:** único contexto pequeno, uma transação local e uma relação 1:N. Separar serviços criaria consistência distribuída, latência e custo operacional sem benefício comprovado. Camadas `api`, `service`, `domain`, `repository` separam mudanças e responsabilidades (SRP). Interfaces de repositório e injeção por construtor facilitam substituição/testes (DIP); não criar abstrações fictícias apenas para alegar SOLID.
- **PostgreSQL + JPA:** chaves estrangeiras e check constraint protegem integridade além da validação da API. Flyway versiona schema; `ddl-auto: validate` evita criação acidental em produção. `LAZY` reduz consultas desnecessárias; consultas de listagem usam `join fetch` para evitar N+1.
- **DTOs/records:** contrato HTTP não expõe diretamente entidades JPA. `@Valid`, validação entre datas no serviço e restrição SQL fazem defesa em camadas; exceções viram 400/404/409. `nome.trim()` normaliza texto simples; Bean Validation impede string vazia antes do serviço.
- **LocalDate + Clock:** enunciado usa DATE, sem horário. Intervalo fechado `[inicio, fim]`; fuso `BUSINESS_ZONE` é configurável e default America/Sao_Paulo. Clock injetado permite testes determinísticos. Não converter data civil em timestamp UTC no navegador.
- **Status ativo:** fonte de verdade é o intervalo de datas. Respostas da API calculam o status no momento da leitura. Coluna `ativo` existe para respeitar diagrama e como projeção; tarefa idempotente a cada 5 min atualiza apenas linhas divergentes. Se a tarefa atrasar, a coluna fica defasada, mas listagem e resposta continuam corretas. Para exigência de exatidão na própria coluna, alinhar SLA e estratégia operacional antes de prometer sincronia instantânea.
- **Sem mensageria:** não há eventos externos a notificar nem volume que justifique broker; fila exigiria deduplicação e reagendamento ao editar datas. Com múltiplas réplicas, agendamento pode rodar redundantemente; usar lock distribuído ou executor único em produção.
- **Next.js App Router + TypeScript:** rotas por arquivos e tipagem explícita. Formulários Client Components porque precisam de estado e eventos. Rewrite `/backend/*` encaminha para API Java e evita CORS no navegador; não é um BFF complexo. Frontend valida para UX, mas backend é autoridade; API centraliza tratamento de erros. Listagem atualiza ao recuperar foco e em intervalos (sessão aberta durante virada do dia).
- **Operação:** Actuator health e variáveis para banco/fuso. Compose disponibiliza só PostgreSQL local; credenciais de exemplo são exclusivamente para desenvolvimento. Sem autenticação porque explicitamente dispensada pelo teste, não porque seja apropriado para produção.

## Estrutura

```
backend/src/main/java/br/com/teste/eventos/
  api/           # controllers, DTOs, erros HTTP
  domain/        # entidades e regra de vigência
  repository/    # persistência e consultas
  service/       # casos de uso, transações, agendamento
  config/        # relógio de negócio
backend/src/main/resources/db/migration/ # schema versionado
frontend/src/app/                      # telas de instituições/eventos
frontend/src/lib/api.ts                 # contratos TS e HTTP
```

## Evolução sugerida

Acrescentar testes de integração com PostgreSQL/Testcontainers (FK, SQL de sincronização, validação de JSON e concorrência), testes de interface, paginação, métricas da tarefa, lock em ambiente escalado, documentação OpenAPI e lockfile de dependências. A tarefa e as operações de edição podem concorrer no banco: o estado calculado da API continua correto; para integrações que leem a coluna, adicionar estratégia formal de consistência/concorrência.
