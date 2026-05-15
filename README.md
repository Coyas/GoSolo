# GoSolo — Sistema de Gestão de Férias

Sistema interno da TaskFlow Ltda. para gestão de colaboradores e pedidos de férias.

---

## Stack

| Camada        | Tecnologia                              |
| ------------- | --------------------------------------- |
| Frontend      | Next.js 16, shadcn/ui, react-hook-form  |
| Backend       | Spring Boot 4 (Java 17, Gradle)         |
| Base de dados | PostgreSQL 16                           |
| Autenticação  | JWT (jjwt 0.12.6)                       |
| Infra         | Docker Compose                          |

---

## Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e [Docker Compose](https://docs.docker.com/compose/)

---

## Execução

```bash
git clone https://github.com/Coyas/GoSolo.git
cd GoSolo
cp .env.example .env
./start.sh
```

O `start.sh` corre os testes de backend e frontend antes de subir os serviços. Se algum teste falhar, o compose não sobe e qualquer serviço já em execução é parado com `docker compose down`.

Para subir directamente sem testes:

```bash
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

| Variável              | Descrição       | Exemplo                   |
| --------------------- | --------------- | ------------------------- |
| `NEXT_PUBLIC_API_URL` | URL base da API | `http://localhost:8085`   |

---

## pgAdmin

Acede em `http://localhost:5050` com as credenciais do `.env` (`PGADMIN_EMAIL` / `PGADMIN_PASSWORD`).

Para ligar ao servidor PostgreSQL:

1. Clica com o botão direito em **Servers** → **Register** → **Server**
2. No separador **General**, define um nome (ex: `gosolo`)
3. No separador **Connection**, preenche:

| Campo    | Valor                          |
| -------- | ------------------------------ |
| Host     | `db`                           |
| Port     | `5432`                         |
| Database | `gosolo`                       |
| Username | valor de `DB_USER` no .env     |
| Password | valor de `DB_PASSWORD` no .env |

4. Clica **Save**

---

## Documentação da API

Após iniciar o backend, acede ao Swagger UI:

```
http://localhost:8085/swagger-ui.html
```

A documentação inclui todos os endpoints com os seus schemas de request/response, códigos de estado e requisitos de autorização. Para testar endpoints autenticados, clica em **Authorize** e introduz o token JWT obtido no login (`Bearer <token>`).

### Endpoints principais

| Método   | Endpoint                              | Descrição                        | Roles                        |
| -------- | ------------------------------------- | -------------------------------- | ---------------------------- |
| `POST`   | `/api/v1/auth/login`                  | Login — devolve token, role, userId, name | Público             |
| `GET`    | `/api/v1/users`                       | Listar colaboradores             | ADMIN                        |
| `POST`   | `/api/v1/users`                       | Criar colaborador                | ADMIN                        |
| `PUT`    | `/api/v1/users/{id}`                  | Actualizar colaborador           | ADMIN                        |
| `DELETE` | `/api/v1/users/{id}`                  | Remover colaborador              | ADMIN                        |
| `GET`    | `/api/v1/vacation-requests`           | Listar pedidos (filtrado por role) | Autenticado                |
| `POST`   | `/api/v1/vacation-requests`           | Criar pedido de férias           | Autenticado                  |
| `PUT`    | `/api/v1/vacation-requests/{id}`      | Editar pedido (só dono, PENDING) | Dono do pedido               |
| `DELETE` | `/api/v1/vacation-requests/{id}`      | Cancelar pedido (PENDING)        | Dono ou ADMIN                |
| `PATCH`  | `/api/v1/vacation-requests/{id}/approve` | Aprovar pedido               | ADMIN, MANAGER               |
| `PATCH`  | `/api/v1/vacation-requests/{id}/reject`  | Rejeitar pedido              | ADMIN, MANAGER               |

---

## Funcionalidades do Frontend

| Área                  | Detalhe                                                                 |
| --------------------- | ----------------------------------------------------------------------- |
| Autenticação          | Login com JWT; sessão em localStorage; redirect automático se expirada; nome do utilizador na navbar |
| Listagem de férias    | Filtro por colaborador e por estado; paginação (8 por página)           |
| Listagem de colaboradores | Filtro por nome/email e por role; paginação (8 por página)          |
| Pedidos de férias     | Criar, editar (só dono / PENDING), cancelar, aprovar, rejeitar          |
| Colaboradores         | Criar, editar, remover (só ADMIN)                                       |
| Controlo de acesso    | Menus e botões condicionais por role; redirect se sem permissão         |
| Feedback              | Toasts de sucesso e erro (sonner); loading state nas listagens          |

---

## Arquitectura do Backend

O backend segue a arquitectura em camadas:

```
Controller → Service → Repository → Base de dados
```

| Camada         | Responsabilidade                                      |
| -------------- | ----------------------------------------------------- |
| **Controller** | Recebe pedidos HTTP, valida input, devolve resposta   |
| **Service**    | Contém a lógica de negócio e as regras de autorização |
| **Repository** | Acesso à base de dados via JPA                        |
| **Model**      | Representação das entidades da base de dados          |

---

## Regras de Negócio

- Um colaborador não pode ter dois pedidos seus sobrepostos (PENDING ou APPROVED) — verificado na criação
- Dois colaboradores diferentes não podem ter férias aprovadas no mesmo período — verificado na aprovação
- Só é possível editar ou cancelar pedidos com status `PENDING`
- Apenas o próprio utilizador pode editar o seu pedido (nem admins podem editar pedidos alheios)
- Pedidos `APPROVED` não podem ser cancelados
- Apenas o manager responsável ou um admin pode aprovar/rejeitar pedidos
- Um manager não pode aprovar as suas próprias férias
- Um manager vê as suas próprias férias e as dos seus colaboradores directos

---

## Roles

| Role           | Permissões                                                                         |
| -------------- | ---------------------------------------------------------------------------------- |
| `ADMIN`        | Gestão total de utilizadores e férias                                              |
| `MANAGER`      | Gere os seus próprios pedidos de férias; aprova/rejeita férias dos colaboradores   |
| `COLLABORATOR` | Cria e gere os seus próprios pedidos de férias                                     |

---

## Modelo de Base de Dados

O sistema tem duas tabelas relacionadas entre si.

### `users`

Armazena todos os utilizadores do sistema independentemente do role.

| Coluna       | Tipo              | Descrição                                       |
| ------------ | ----------------- | ----------------------------------------------- |
| `id`         | BIGSERIAL PK      | Identificador único                             |
| `name`       | VARCHAR(100)      | Nome do utilizador                              |
| `email`      | VARCHAR(150)      | Email único, usado para login                   |
| `password`   | VARCHAR(255)      | Password encriptada com BCrypt                  |
| `role`       | VARCHAR(20)       | `ADMIN`, `MANAGER` ou `COLLABORATOR`            |
| `manager_id` | BIGINT FK → users | Manager responsável (null para ADMIN e MANAGER) |
| `created_at` | TIMESTAMP         | Data de criação do registo                      |

### `vacation_requests`

Armazena todos os pedidos de férias criados pelos colaboradores.

| Coluna        | Tipo              | Descrição                                                      |
| ------------- | ----------------- | -------------------------------------------------------------- |
| `id`          | BIGSERIAL PK      | Identificador único                                            |
| `user_id`     | BIGINT FK → users | Colaborador que fez o pedido                                   |
| `start_date`  | DATE              | Data de início das férias (inclusiva)                          |
| `end_date`    | DATE              | Data de fim das férias (inclusiva)                             |
| `status`      | VARCHAR(20)       | `PENDING`, `APPROVED` ou `REJECTED`                            |
| `reviewed_by` | BIGINT FK → users | Manager ou admin que aprovou/rejeitou (null enquanto pendente) |
| `reviewed_at` | TIMESTAMP         | Data da decisão (null enquanto pendente)                       |
| `created_at`  | TIMESTAMP         | Data de criação do pedido                                      |

> **Regra de negócio:** cada utilizador não pode ter dois pedidos de férias sobrepostos (PENDING ou APPROVED). A validação é feita na camada de serviço tanto na criação como na aprovação.

---

## Migrations

As migrations são geridas pelo Flyway e correm automaticamente ao iniciar o backend.

| Versão | Ficheiro                                 | Descrição                  |
| ------ | ---------------------------------------- | -------------------------- |
| V1     | `V1__create_users_table.sql`             | Tabela `users`             |
| V2     | `V2__create_vacation_requests_table.sql` | Tabela `vacation_requests` |
| V3     | `V3__seed_users.sql`                     | Dados iniciais (seed)      |

---

## Testes

### Backend

```bash
cd gosolo
./gradlew test
```

41 testes no total, divididos em três classes:

| Classe | Tipo | Testes |
|---|---|---|
| `VacationRequestServiceTest` | Unitário (Mockito) | 14 — create, approve, reject, cancel, update |
| `UserServiceTest` | Unitário (Mockito) | 9 — create, findById, delete |
| `VacationRequestRepositoryTest` | Repositório (@DataJpaTest + H2) | 16 — queries JPQL de sobreposição |
| `GosoloApplicationTests` | Contexto (@SpringBootTest + H2) | 1 — context loads |

Os testes de repositório correm com H2 em memória (Flyway desligado, schema gerado pelo Hibernate). Não requerem PostgreSQL.

### Frontend

```bash
cd wgosolo
npm test
```

15 testes no total:

| Ficheiro | Testes |
|---|---|
| `auth.test.ts` | 9 — getSession (válida, expirada, malformed), saveSession, clearSession, getToken, getUserId |
| `api.test.ts` | 6 — Authorization header, sem sessão, 200, erro backend, 204 → undefined, POST body |

---

## Dados iniciais (seed)

Na primeira execução, o Flyway insere automaticamente os seguintes utilizadores:

| Nome         | Email             | Role         | Password     |
| ------------ | ----------------- | ------------ | ------------ |
| Admin        | admin@gosolo.pt   | ADMIN        | `admin123`   |
| Manager      | manager@gosolo.pt | MANAGER      | `manager123` |
| Collaborator | collab@gosolo.pt  | COLLABORATOR | `collab123`  |
