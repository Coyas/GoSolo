# GoSolo — Sistema de Gestão de Férias

Sistema interno da TaskFlow Ltda. para gestão de colaboradores e pedidos de férias.

---

## Stack

| Camada        | Tecnologia              |
| ------------- | ----------------------- |
| Frontend      | Next.js                 |
| Backend       | Spring Boot 4 (Java 17) |
| Base de dados | PostgreSQL              |
| Autenticação  | JWT                     |
| Infra         | Docker Compose          |

---

## Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e [Docker Compose](https://docs.docker.com/compose/)

---

## Execução

```bash
git clone https://github.com/Coyas/GoSolo.git
cd GoSolo
cp .env.example .env
docker compose up --build
```

> Cria o ficheiro `.env` a partir do `.env.example` antes de correr o Docker Compose.

| Serviço     | URL                                   |
| ----------- | ------------------------------------- |
| Frontend    | http://localhost:3000                 |
| Backend API | http://localhost:8085/api/v1          |
| Swagger UI  | http://localhost:8085/swagger-ui.html |
| pgAdmin     | http://localhost:5050                 |
| PostgreSQL  | localhost:5433                        |

Para parar:

```bash
docker compose down
```

Para parar e apagar os volumes da base de dados:

```bash
docker compose down -v
```

Para ver se as migrations forom atualizadas

```bash
docker exec gosolo-db-1 psql -U postgres -d gosolo -c "\dt"
```

---

## Variáveis de ambiente

### Backend (`gosolo/src/main/resources/application.properties`)

| Variável                     | Descrição                             | Exemplo                                   |
| ---------------------------- | ------------------------------------- | ----------------------------------------- |
| `spring.datasource.url`      | URL JDBC da base de dados             | `jdbc:postgresql://localhost:5432/gosolo` |
| `spring.datasource.username` | Utilizador PostgreSQL                 | `postgres`                                |
| `spring.datasource.password` | Password PostgreSQL                   | `simadioscre`                             |
| `jwt.secret`                 | Chave secreta para assinar tokens JWT | `yomuDalsemMantcHontcha`                  |
| `jwt.expiration`             | Duração do token em ms                | `86400000`                                |

### Frontend (`wgosolo/.env.local`)

| Variável              | Descrição       | Exemplo                        |
| --------------------- | --------------- | ------------------------------ |
| `NEXT_PUBLIC_API_URL` | URL base da API | `http://localhost:8085/api/v1` |

---

## pgAdmin

Acede em `http://localhost:5050` com as credenciais do `.env` (`PGADMIN_EMAIL` / `PGADMIN_PASSWORD`).

Para ligar ao servidor PostgreSQL:

1. Clica com o botão direito em **Servers** → **Register** → **Server**
2. No separador **General**, define um nome (ex: `gosolo`)
3. No separador **Connection**, preenche:

| Campo    | Valor                      |
| -------- | -------------------------- |
| Host     | `db`                       |
| Port     | `5432`                     |
| Database | `gosolo`                   |
| Username | valor de `DB_USER` no .env |
| Password | valor de `DB_PASSWORD` no .env |

4. Clica **Save**

---

## Documentação da API

Após iniciar o backend, acede ao Swagger UI:

```
http://localhost:8085/swagger-ui.html
```

---

## Roles

| Role           | Permissões                                     |
| -------------- | ---------------------------------------------- |
| `ADMIN`        | Gestão total de utilizadores e férias          |
| `MANAGER`      | Aprova/rejeita férias dos seus colaboradores   |
| `COLLABORATOR` | Cria e gere os seus próprios pedidos de férias |

---

## Modelo de Base de Dados

O sistema tem duas tabelas relacionadas entre si.

### `users`

Armazena todos os utilizadores do sistema independentemente do role.

| Coluna | Tipo | Descrição |
| --- | --- | --- |
| `id` | BIGSERIAL PK | Identificador único |
| `name` | VARCHAR(100) | Nome do utilizador |
| `email` | VARCHAR(150) | Email único, usado para login |
| `password` | VARCHAR(255) | Password encriptada com BCrypt |
| `role` | VARCHAR(20) | `ADMIN`, `MANAGER` ou `COLLABORATOR` |
| `manager_id` | BIGINT FK → users | Manager responsável (null para ADMIN e MANAGER) |
| `created_at` | TIMESTAMP | Data de criação do registo |

### `vacation_requests`

Armazena todos os pedidos de férias criados pelos colaboradores.

| Coluna | Tipo | Descrição |
| --- | --- | --- |
| `id` | BIGSERIAL PK | Identificador único |
| `user_id` | BIGINT FK → users | Colaborador que fez o pedido |
| `start_date` | DATE | Data de início das férias (inclusiva) |
| `end_date` | DATE | Data de fim das férias (inclusiva) |
| `status` | VARCHAR(20) | `PENDING`, `APPROVED` ou `REJECTED` |
| `reviewed_by` | BIGINT FK → users | Manager ou admin que aprovou/rejeitou (null enquanto pendente) |
| `reviewed_at` | TIMESTAMP | Data da decisão (null enquanto pendente) |
| `created_at` | TIMESTAMP | Data de criação do pedido |

> **Regra de negócio:** dois colaboradores não podem ter férias sobrepostas no mesmo dia. Esta validação é feita na camada de serviço.

---

## Migrations

As migrations são geridas pelo Flyway e correm automaticamente ao iniciar o backend.

| Versão | Ficheiro                                 | Descrição                  |
| ------ | ---------------------------------------- | -------------------------- |
| V1     | `V1__create_users_table.sql`             | Tabela `users`             |
| V2     | `V2__create_vacation_requests_table.sql` | Tabela `vacation_requests` |
| V3     | `V3__seed_users.sql`                     | Dados iniciais (seed)      |

---

## Dados iniciais (seed)

Na primeira execução, o Flyway insere automaticamente os seguintes utilizadores:

| Nome         | Email             | Role         | Password     |
| ------------ | ----------------- | ------------ | ------------ |
| Admin        | admin@gosolo.pt   | ADMIN        | `admin123`   |
| Manager      | manager@gosolo.pt | MANAGER      | `manager123` |
| Collaborator | collab@gosolo.pt  | COLLABORATOR | `collab123`  |
