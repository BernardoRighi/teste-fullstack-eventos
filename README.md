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
- Docker e Docker Compose para executar PostgreSQL, backend e frontend.

As dependências do backend estão declaradas em `backend/pom.xml`. O frontend possui `package.json` e `package-lock.json` versionados; use `npm ci` para instalar as dependências conforme o lockfile.

## Executar com Docker

Esta é a forma recomendada para executar a aplicação completa. Não é necessário instalar Java, Maven ou Node.js na máquina: a compilação ocorre nas imagens Docker.

### Pré-requisitos

- Git.
- Docker em execução, com Docker Compose v2.
- Portas locais 3000, 8080 e 5433 disponíveis.

### 1. Clonar o repositório

```bash
git clone https://github.com/BernardoRighi/teste-fullstack-eventos.git
cd teste-fullstack-eventos
```

### 2. Compilar e iniciar os serviços

Na raiz do repositório:

```bash
docker compose up --build -d
```

O Compose inicia três serviços:

| Serviço    | Finalidade          | Acesso local              |
| ---------- | ------------------- | ------------------------- |
| `postgres` | Banco PostgreSQL 16 | `localhost:5433`          |
| `backend`  | API Spring Boot     | http://localhost:8080/api |
| `frontend` | Aplicação Next.js   | http://localhost:3000     |

Na primeira execução, o Docker baixa as imagens e as dependências e compila o backend e o frontend. Isso pode levar alguns minutos.

O backend aguarda o health check do PostgreSQL antes de iniciar. O frontend depende da inicialização do container do backend, mas essa condição não garante que a API já esteja pronta. Aguarde a conclusão da inicialização do Spring Boot antes de utilizar a interface.

O Flyway aplica as migrações ao iniciar o backend. O Hibernate utiliza `ddl-auto: validate` para validar o schema, em vez de criá-lo automaticamente.

### 3. Verificar a execução

```bash
docker compose ps
docker compose logs -f
```

Para acompanhar somente um serviço:

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

Abra http://localhost:3000.

O health check da API está disponível em http://localhost:8080/actuator/health.

Encerrar o acompanhamento dos logs com `Ctrl+C` não encerra os containers iniciados com `-d`.

### Comunicação entre os serviços

Dentro da rede do Compose, os serviços são acessados pelo nome:

- O backend acessa o banco em `jdbc:postgresql://postgres:5432/teste`.
- O servidor Next.js acessa o backend em `http://backend:8080`.
- O navegador acessa o frontend em `http://localhost:3000`.

Dentro de um container, `localhost` aponta para o próprio container, não para outro serviço nem para a máquina host.

O frontend encaminha `/backend/:path*` para `/api/:path*` por meio de um rewrite do Next.js. O navegador não acessa diretamente o nome `backend`; o encaminhamento é realizado pelo servidor Next.js.

### Configuração do build

Os Dockerfiles utilizam múltiplas etapas de build:

- Backend: Maven e JDK 21 para compilar; JRE 21 para executar o JAR.
- Frontend: Node.js 22 para instalar dependências e compilar; saída `standalone` do Next.js para execução.

As imagens de execução utilizam usuários sem privilégios de root.

A variável `JAVA_API_URL` é passada como argumento de build do frontend pelo Compose, com valor `http://backend:8080`. Como o rewrite é definido no build de produção do Next.js, alterar apenas uma variável no container em execução não atualiza esse destino. Após alterar o argumento, reconstrua a imagem:

```bash
docker compose up --build -d frontend
```

Não é necessário criar `frontend/.env.local` para executar a configuração Docker fornecida. Os arquivos `.dockerignore` excluem arquivos de ambiente locais do contexto de build.

O build da imagem do backend utiliza `-DskipTests`. Os testes devem ser executados separadamente, conforme a seção de testes.

### Encerrar e preservar os dados

```bash
docker compose down
```

Esse comando encerra os serviços e preserva o volume do banco.

Para encerrar e também apagar os dados locais:

```bash
docker compose down -v
```

**Atenção:** a opção `-v` remove o volume do PostgreSQL. Use apenas quando desejar reiniciar o banco do zero.

### Credenciais locais

A configuração Docker sugerida utiliza:

| Parâmetro            | Valor           |
| -------------------- | --------------- |
| Banco                | `teste`         |
| Usuário              | `eventos`       |
| Senha                | `eventos_local` |
| Porta no host        | `5433`          |
| Porta na rede Docker | `5432`          |

Essas credenciais são exclusivamente para desenvolvimento local e não devem ser usadas em produção.

Se já existir um volume criado com credenciais anteriores, alterar `POSTGRES_USER` e `POSTGRES_PASSWORD` no Compose não altera automaticamente os usuários desse banco. Mantenha as credenciais compatíveis com o volume existente ou, se os dados forem descartáveis, recrie-o com `docker compose down -v` antes de iniciar novamente.

## Executar localmente para desenvolvimento

Nesta modalidade, somente o PostgreSQL roda no Docker. Backend e frontend são executados diretamente na máquina.

### Pré-requisitos

- JDK 21.
- Maven 3.9 ou superior.
- Node.js 20.9 ou superior e npm.
- Docker em execução e Docker Compose v2.
- Portas locais 3000, 8080 e 5433 disponíveis.

Docker também é necessário para os testes de integração com Testcontainers.

Se a aplicação completa estiver rodando pelo Compose, encerre-a antes de iniciar os processos locais para evitar conflitos de portas:

```bash
docker compose down
```

### 1. Iniciar somente o banco

Na raiz do repositório:

```bash
docker compose up -d postgres
docker compose ps
```

Aguarde o PostgreSQL ficar saudável. O banco é publicado na porta `5433` do host, correspondente à porta `5432` do container, e utiliza o volume persistente `postgres_data`.

### 2. Iniciar o backend

Configure as variáveis com as credenciais do Compose. Em Bash, partindo da raiz:

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5433/teste
export DB_USER=eventos
export DB_PASSWORD=eventos_local
export BUSINESS_ZONE=America/Sao_Paulo
mvn spring-boot:run
```

No PowerShell:

```powershell
cd backend
$env:DB_URL = "jdbc:postgresql://localhost:5433/teste"
$env:DB_USER = "eventos"
$env:DB_PASSWORD = "eventos_local"
$env:BUSINESS_ZONE = "America/Sao_Paulo"
mvn spring-boot:run
```

- API: http://localhost:8080/api
- Health check: http://localhost:8080/actuator/health

O Flyway aplica as migrações ao iniciar.

### 3. Iniciar o frontend

Em outro terminal, partindo da raiz:

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

Para desenvolvimento local, utilize em `.env.local`:

```dotenv
JAVA_API_URL=http://localhost:8080
```

Informe a URL base sem o sufixo `/api`. Reinicie o Next.js após alterar a configuração.

Abra http://localhost:3000.

### Encerrar

Encerre os processos do frontend e backend com `Ctrl+C`. Na raiz, execute:

```bash
docker compose down
```

O volume do banco é preservado. Use `docker compose down -v` somente se também desejar apagar os dados locais.

## Configuração

| Variável        | Finalidade                                                                                                                |
| --------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `DB_URL`        | URL JDBC do PostgreSQL. Local: `jdbc:postgresql://localhost:5433/teste`. Docker: `jdbc:postgresql://postgres:5432/teste`. |
| `DB_USER`       | Usuário do banco utilizado pelo backend.                                                                                  |
| `DB_PASSWORD`   | Senha do banco utilizado pelo backend.                                                                                    |
| `BUSINESS_ZONE` | Fuso usado para determinar a data de negócio. Padrão: `America/Sao_Paulo`.                                                |
| `JAVA_API_URL`  | URL base do backend usada pelo rewrite do frontend, sem `/api`. No Docker, é informada no build.                          |

As variáveis do PostgreSQL no Compose (`POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD`) devem ser compatíveis com a configuração de conexão do backend.

A tarefa de sincronização utiliza a propriedade Spring `app.sincronizacao.cron`, com expressão padrão `0 */5 * * * *` (a cada cinco minutos). Nos testes de integração, essa propriedade recebe `-` para desativar o agendamento automático.

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

| Método | Rota                     | Resultado em caso de sucesso                 |
| ------ | ------------------------ | -------------------------------------------- |
| GET    | `/api/instituicoes`      | Lista de instituições — 200                  |
| POST   | `/api/instituicoes`      | Instituição criada — 201 e header `Location` |
| GET    | `/api/instituicoes/{id}` | Instituição encontrada — 200                 |
| PUT    | `/api/instituicoes/{id}` | Instituição atualizada — 200                 |
| DELETE | `/api/instituicoes/{id}` | Exclusão — 204                               |
| GET    | `/api/eventos`           | Lista de eventos conforme filtro — 200       |
| POST   | `/api/eventos`           | Evento criado — 201 e header `Location`      |
| GET    | `/api/eventos/{id}`      | Evento encontrado — 200                      |
| PUT    | `/api/eventos/{id}`      | Evento atualizado — 200                      |
| DELETE | `/api/eventos/{id}`      | Exclusão — 204                               |

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

Os comandos acima utilizam sintaxe de shell Bash. No Windows, adapte as aspas ao terminal utilizado ou use um cliente HTTP.

### Erros

O tratamento centralizado retorna um objeto com `status`, `detail` e `timestamp`.

- **400:** validação de entrada, período inválido, parâmetros inválidos, JSON ilegível ou filtros incorretos.
- **404:** recurso não encontrado.
- **409:** tentativa de excluir instituição com eventos ou violação de integridade do banco.

## Testes, lint e build

Execute cada bloco a partir da raiz do repositório, em terminais separados ou retornando à raiz entre eles.

### Backend

Com JDK 21, Maven e Docker disponíveis:

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

**Importante:** o Dockerfile utiliza `mvn clean package -DskipTests`. Portanto, construir ou iniciar a aplicação pelo Compose não executa nem valida a suíte de testes.

### Frontend

Com Node.js e npm disponíveis:

```bash
cd frontend
npm ci
npm run lint
npm run build
```

Para iniciar o frontend após o build local:

```bash
npm run start
```

Para o build local, mantenha `JAVA_API_URL` apontando para `http://localhost:8080`, caso o backend também seja acessado pela máquina host.

Há configuração de ESLint no repositório, mas não há script de testes automatizados de interface no `package.json`. Lint e build não substituem testes funcionais.

### Verificação funcional sugerida

Após iniciar a aplicação:

1. Acesse http://localhost:3000.
2. Crie uma instituição.
3. Crie um evento vinculado à instituição.
4. Consulte e edite os registros.
5. Verifique a listagem de eventos ativos conforme as datas informadas.
6. Tente excluir uma instituição com eventos e confira o impedimento.
7. Exclua o evento e, em seguida, a instituição.

## Organização do código

```text
backend/
  Dockerfile
  .dockerignore
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
  Dockerfile
  .dockerignore
  .env.example
  package.json
  package-lock.json
  eslint.config.mjs
  next.config.ts
  src/app/        # Páginas, layout e estilos
  src/hooks/      # Hook de carregamento remoto
  src/lib/api.ts  # Contratos TypeScript e acesso HTTP
docker-compose.yml
README.md
```

### Arquitetura implementada

O backend é uma aplicação única organizada por pacotes de responsabilidade. Os controllers recebem as requisições, os serviços executam operações transacionais e os repositórios acessam o banco. O contrato HTTP utiliza records em vez de expor diretamente as entidades JPA.

Essa organização não representa isolamento completo entre camadas: `EventoService` importa `EventoRequest` e `EventoResponse` de `api`, e `InstituicaoService` também utiliza os DTOs desse pacote. Os serviços, portanto, estão acoplados ao contrato da API. O projeto não deve ser descrito como arquitetura limpa, hexagonal ou camadas totalmente independentes.

O Flyway versiona o schema. Chave estrangeira, campos obrigatórios e constraints protegem a integridade do banco, incluindo o período do evento e os tipos de instituição. As listagens filtradas de eventos utilizam `join fetch` da instituição.

No frontend, o Next.js App Router organiza as páginas por arquivos. Os formulários e o carregamento remoto utilizam componentes e hooks de cliente; `src/lib/api.ts` centraliza o acesso HTTP.

O Compose integra os três serviços para facilitar a execução local. O frontend utiliza saída `standalone` e um rewrite para encaminhar as chamadas à API.

## Problemas comuns

### Porta já está em uso

Encerre o processo que está utilizando a porta ou altere o mapeamento no `docker-compose.yml`.

Ao alterar apenas a porta publicada no host, preserve as portas internas usadas na comunicação entre containers. Por exemplo, publicar o backend em `8081:8080` não exige alterar `JAVA_API_URL=http://backend:8080`.

### Frontend abre, mas a API ainda não responde

Aguarde a inicialização do backend e consulte:

```bash
docker compose logs -f backend
```

Verifique também http://localhost:8080/actuator/health.

### Falha de autenticação no PostgreSQL

Confira se as credenciais do backend correspondem às do banco. Se o volume já existia, ele pode manter usuários e senhas da configuração anterior.

Não remova o volume se precisar preservar os dados. Para um banco local descartável, `docker compose down -v` permite reinicializá-lo na próxima execução.

### Alterações não aparecem na aplicação Docker

A configuração fornecida não utiliza hot reload nem monta o código-fonte nos containers. Reconstrua as imagens após alterar o código:

```bash
docker compose up --build -d
```

Para hot reload, utilize a execução local de desenvolvimento.

### Testcontainers não consegue iniciar

Verifique se o Docker está em execução e acessível pelo usuário que executa o Maven. O banco do Compose não substitui o container criado pelos testes.

## Limitações e escopo

- A aplicação não implementa autenticação.
- Não há testes automatizados de interface no frontend.
- O build Docker do backend não executa os testes.
- A coluna persistida `ativo` é sincronizada periodicamente, não instantaneamente na virada do dia.
- A configuração fornecida é voltada ao desenvolvimento e à avaliação local.

As credenciais locais, a publicação das portas e a ausência de autenticação não constituem uma configuração pronta para produção. A implementação e esta documentação não representam garantia de prontidão para produção.
