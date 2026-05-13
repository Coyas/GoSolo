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
docker compose up --build
```

| Serviço     | URL                                   |
| ----------- | ------------------------------------- |
| Frontend    | http://localhost:3000                 |
| Backend API | http://localhost:8080/api/v1          |
| Swagger UI  | http://localhost:8080/swagger-ui.html |
| PostgreSQL  | localhost:5432                        |

Para parar:

```bash
docker compose down
```

Para parar e apagar os volumes da base de dados:

```bash
docker compose down -v
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
| `NEXT_PUBLIC_API_URL` | URL base da API | `http://localhost:8080/api/v1` |

---

## Documentação da API

Após iniciar o backend, acede ao Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

---

## Roles

| Role           | Permissões                                     |
| -------------- | ---------------------------------------------- |
| `ADMIN`        | Gestão total de utilizadores e férias          |
| `MANAGER`      | Aprova/rejeita férias dos seus colaboradores   |
| `COLLABORATOR` | Cria e gere os seus próprios pedidos de férias |

---

## Dados iniciais (seed)

Na primeira execução, a aplicação cria automaticamente os seguintes utilizadores:

| Nome         | Email             | Role         | Password     |
| ------------ | ----------------- | ------------ | ------------ |
| Admin        | admin@gosolo.pt   | ADMIN        | `admin123`   |
| Manager      | manager@gosolo.pt | MANAGER      | `manager123` |
| Collaborator | collab@gosolo.pt  | COLLABORATOR | `collab123`  |
