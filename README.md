# Desafio Fullstack — Instituições e Eventos

Aplicação para cadastro, consulta, edição e exclusão de instituições e eventos, com backend Java/Spring Boot, frontend Next.js e banco PostgreSQL.

Cada evento pertence a uma instituição. Seu status de atividade é calculado a partir de um período de vigência com datas inicial e final inclusivas. A aplicação não implementa autenticação.

## Tecnologias

- Java 21 e Spring Boot 3.4.5.
- Spring Web, Spring Data JPA, Bean Validation e Actuator.
- PostgreSQL 16 e Flyway para migrações do banco.
- Next.js 15.3.3, React 19.1.0 e TypeScript.
- JUnit 5, MockMvc e Testcontainers com PostgreSQL para testes do backend.
- ESLint no frontend.
- Docker Compose para executar o PostgreSQL localmente.

As dependências do backend estão declaradas em `backend/pom.xml`. O frontend possui `package.json` e `package-lock.json` versionado; use `npm ci` para instalar as dependências conforme o lockfile.

## Pré-requisitos

- JDK 21.
- Maven 3.9 ou superior instalado localmente.
- Node.js 20.9 ou superior e npm.
- Docker em execução e Docker Compose v2.
- Portas locais 3000, 8080 e 5433 disponíveis.

Docker também é necessário para os testes de integração com Testcontainers.

## Executar localmente

### 1. Iniciar o banco

Na raiz do repositório:

```bash
docker compose up -d
docker compose ps
```

O Compose inicia somente o PostgreSQL, não o backend nem o frontend. O banco é publicado na porta `5433` do host, correspondente à porta `5432` do container, e usa um volume persistente chamado `postgres_data`.

Aguarde o banco ficar saudável antes de iniciar o backend. As credenciais de desenvolvimento estão no Compose e na configuração do backend; não devem ser usadas em produção.

### 2. Iniciar o backend

Em um terminal, partindo da raiz do repositório:

```bash
cd backend
mvn spring-boot:run
```

O Flyway aplica as migrações ao iniciar. O Hibernate está configurado com `ddl-auto: validate`, para validar o schema em vez de criá-lo automaticamente.

- API: http://localhost:8080/api
- Health check: http://localhost:8080/actuator/health

### 3. Iniciar o frontend

Em outro terminal, partindo da raiz do repositório:

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

No PowerShell, substitua o comando de cópia por:

```powershell
Copy-Item .env.example .env.local
```

Abra http://localhost:3000.

O frontend encaminha `/backend/:path*` para `/api/:path*` no backend por meio de um rewrite do Next.js. A variável `JAVA_API_URL` define o endereço do Java, com padrão `http://localhost:8080`. Informe a URL base sem o sufixo `/api`. Reinicie o Next.js após alterar a configuração.

### Encerrar

Encerre os processos do frontend e backend com `Ctrl+C`. Na raiz, execute:

```bash
docker compose down
```

Esse comando preserva o volume do banco. `docker compose down -v` também remove os volumes e apaga os dados locais; use apenas se desejar reiniciar o banco do zero.

## Configuração

| Variável | Finalidade |
| --- | --- |
| `DB_URL` | URL JDBC do PostgreSQL. Padrão local: `jdbc:postgresql://localhost:5433/teste`. |
| `DB_USER` | Usuário do banco utilizado pelo backend. |
| `DB_PASSWORD` | Senha do banco utilizado pelo backend. |
| `BUSINESS_ZONE` | Fuso usado para determinar a data de negócio. Padrão: `America/Sao_Paulo`. |
| `JAVA_API_URL` | URL base do backend usada pelo rewrite do frontend. |

A tarefa de sincronização usa a propriedade Spring `app.sincronizacao.cron`, com expressão padrão `0 */5 * * * *` (a cada cinco minutos). Nos testes de integração, essa propriedade recebe `-` para desativar o agendamento automático.

## Regras de negócio

### Instituições

- Nome obrigatório, não vazio, com até 150 caracteres.
- Tipo obrigatório: `CONFEDERACAO`, `SINGULAR`, `CENTRAL` ou `COOPERATIVA`.
- Os nomes são normalizados com `trim()` ao criar e editar.
- Uma instituição com eventos vinculados não pode ser excluída. A API retorna `409 Conflict`; exclua primeiro os eventos.

### Eventos

- Instituição existente, nome e datas são obrigatórios.
- Nome não vazio, com até 150 caracteres.
- Datas no formato `YYYY-MM-DD`.
- A data final não pode ser anterior à inicial.
- Um evento pode começar e terminar no mesmo dia.
- O status `ativo` é calculado pelo sistema; não é um campo editável do contrato de entrada.

Um evento está ativo quando:

```text
dataInicial <= data de negócio atual <= dataFinal
```

O backend utiliza `LocalDate` e um `Clock` injetado. As datas representam dias civis, não instantes UTC.

### Status calculado e coluna persistida

As respostas de eventos calculam `ativo` com base nas datas e no relógio de negócio. A consulta de eventos ativos também filtra pelas datas, não pela coluna `ativo`.

A coluna `ativo` é preenchida na criação e atualização do evento. A tarefa `SincronizacaoEventos` atualiza periodicamente apenas os registros cujo valor persistido diverge da vigência atual e registra em log a data de referência e a quantidade de alterações.

Entre execuções da tarefa, a coluna pode estar defasada em relação à passagem dos dias. Portanto, não há garantia de atualização instantânea da coluna na virada do dia. A resposta calculada da API não depende dessa sincronização periódica.

## API HTTP

| Método | Rota | Resultado em caso de sucesso |
| --- | --- | --- |
| GET | `/api/instituicoes` | Lista de instituições — 200 |
| POST | `/api/instituicoes` | Instituição criada — 201 e header `Location` |
| GET | `/api/instituicoes/{id}` | Instituição encontrada — 200 |
| PUT | `/api/instituicoes/{id}` | Instituição atualizada — 200 |
| DELETE | `/api/instituicoes/{id}` | Exclusão — 204 |
| GET | `/api/eventos` | Lista de eventos conforme filtro — 200 |
| POST | `/api/eventos` | Evento criado — 201 e header `Location` |
| GET | `/api/eventos/{id}` | Evento encontrado — 200 |
| PUT | `/api/eventos/{id}` | Evento atualizado — 200 |
| DELETE | `/api/eventos/{id}` | Exclusão — 204 |

### Filtros de eventos

A listagem exige uma das modalidades abaixo:

```text
GET /api/eventos?instituicaoId=1
GET /api/eventos?somenteAtivos=true
```

- `instituicaoId`: lista os eventos da instituição informada, ativos ou não.
- `somenteAtivos=true`: lista os eventos vigentes de todas as instituições.
- Sem instituição e sem `somenteAtivos=true`, a API retorna 400.
- Combinar `instituicaoId` com `somenteAtivos=true` também retorna 400.
- Informar uma instituição inexistente retorna 404.
- As consultas filtradas de eventos ordenam por ID decrescente.

### Exemplos

Criar uma instituição:

```bash
curl -i -X POST http://localhost:8080/api/instituicoes \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Unidade A","tipo":"COOPERATIVA"}'
```

Criar um evento, substituindo `instituicaoId` pelo ID retornado:

```bash
curl -i -X POST http://localhost:8080/api/eventos \
  -H 'Content-Type: application/json' \
  -d '{"instituicaoId":1,"nome":"Assembleia","dataInicial":"2026-11-16","dataFinal":"2026-11-20"}'
```

O status retornado depende da data de negócio no momento da requisição, não das datas em que este exemplo foi escrito.

Os comandos acima usam sintaxe de shell Bash. No Windows, adapte as aspas ao terminal utilizado.

### Erros

O tratamento centralizado retorna um objeto com `status`, `detail` e `timestamp`.

- **400:** validação de entrada, período inválido, parâmetros inválidos, JSON ilegível ou filtros incorretos.
- **404:** recurso não encontrado.
- **409:** tentativa de excluir instituição com eventos ou violação de integridade do banco.

## Testes, lint e build

Execute cada bloco a partir da raiz do repositório, em terminais separados ou retornando à raiz entre eles.

### Backend

```bash
cd backend
mvn test
```

Testes existentes:

- `RegraVigenciaTest`: seis cenários de vigência, incluindo início e fim inclusivos e evento de um dia.
- `EventosApiIntegrationTest`: onze testes com Spring Boot, MockMvc e PostgreSQL 16 via Testcontainers. Incluem criação e persistência, período inválido, instituição inexistente, impedimento de exclusão de instituição com eventos, atualização de datas e instituição, evento encerrado, sincronização idempotente, nome vazio, identificador não numérico e filtros de listagem.

O Testcontainers cria seu próprio banco de testes; não depende do banco iniciado pelo Compose. Docker deve estar disponível, e a primeira execução pode precisar baixar a imagem do PostgreSQL. O relógio dos testes de integração é fixo para tornar os resultados determinísticos.

Para testar e gerar o artefato do backend em uma única execução:

```bash
cd backend
mvn clean package
```

O artefato é gerado em `backend/target/`. O comando `package` já executa os testes; não é necessário rodar `mvn test` imediatamente antes dele.

### Frontend

```bash
cd frontend
npm ci
npm run lint
npm run build
```

Para iniciar o frontend após o build:

```bash
npm run start
```

Há configuração de ESLint no repositório, mas não há script de testes automatizados de interface no `package.json`. Lint e build não substituem testes funcionais.

## Organização do código

```text
backend/
  pom.xml
  src/main/java/br/com/teste/eventos/
    api/          # Controllers, DTOs HTTP e tratamento de erros
    config/       # Configuração do relógio de negócio
    domain/       # Entidades JPA, enum e regra de vigência
    repository/   # Repositórios Spring Data e consultas
    service/      # Operações transacionais e sincronização
  src/main/resources/
    application.yml
    db/migration/V1__schema.sql
  src/test/java/br/com/teste/eventos/
    api/EventosApiIntegrationTest.java
    domain/RegraVigenciaTest.java
frontend/
  package.json
  package-lock.json
  eslint.config.mjs
  next.config.ts
  src/app/        # Páginas, layout e estilos
  src/hooks/      # Hook de carregamento remoto
  src/lib/api.ts  # Contratos TypeScript e acesso HTTP
docker-compose.yml
```

### Arquitetura implementada

O backend é uma aplicação única organizada por pacotes de responsabilidade. Os controllers recebem as requisições, os serviços executam operações transacionais e os repositórios acessam o banco. O contrato HTTP usa records em vez de expor diretamente as entidades JPA.

Essa organização não representa isolamento completo entre camadas: `EventoService` importa `EventoRequest` e `EventoResponse` de `api`, e `InstituicaoService` também utiliza os DTOs desse pacote. Os serviços, portanto, estão acoplados ao contrato da API. O projeto não deve ser descrito como arquitetura limpa, hexagonal ou camadas totalmente independentes.

O Flyway versiona o schema. Chave estrangeira, campos obrigatórios e constraints protegem a integridade do banco, incluindo o período do evento e os tipos de instituição. As listagens filtradas de eventos usam `join fetch` da instituição.

No frontend, o Next.js App Router organiza as páginas por arquivos. Os formulários e o carregamento remoto usam componentes e hooks de cliente; `src/lib/api.ts` centraliza o acesso HTTP.

O Compose fornecido é voltado ao desenvolvimento local. A implementação e a documentação não constituem garantia de prontidão para produção.
