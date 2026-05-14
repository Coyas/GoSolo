# Task Plan - Sistema de Gestão de Férias

## Stack
- **Frontend**: Next.js
- **Backend**: Spring Boot (Java)
- **Base de dados**: PostgreSQL
- **Auth**: JWT
- **Infra**: Docker Compose

---

## Backend (Spring Boot)

- [x] Inicializar projecto Spring Boot (Maven/Gradle, estrutura Controllers/Services/Models)
- [x] Configurar ligação PostgreSQL (application.properties / datasource)
- [ ] Configurar Spring Security + JWT (geração e validação de tokens)
- [ ] Model/Entity: User (id, name, email, password, role, managerId)
- [ ] Model/Entity: VacationRequest (id, userId, startDate, endDate, status)
- [x] Seed: dados iniciais (admin, managers, collaborators)
- [ ] Endpoint: Auth — login e refresh token
- [ ] Endpoint: CRUD colaboradores (apenas admin)
- [ ] Endpoint: CRUD pedidos de férias
- [ ] Validação: férias sobrepostas entre colaboradores
- [ ] Lógica de roles: admin / manager / collaborator
- [ ] Aprovação/rejeição de férias (manager e admin)
- [ ] Tratamento de erros e status codes consistentes
- [x] Documentação da API (Swagger / OpenAPI)

## Frontend (Next.js)

- [x] Inicializar projecto Next.js
- [ ] Configurar cliente HTTP (axios / fetch) com interceptor JWT
- [ ] Página: login
- [ ] Gestão de sessão (armazenar e renovar token JWT)
- [ ] Vista: listagem de colaboradores
- [ ] Vista: detalhe / edição de colaborador
- [ ] Vista: listagem de pedidos de férias
- [ ] Vista: criação / edição de pedido de férias
- [ ] Controlo de permissões por role na UI
- [ ] Paginação e filtros (bónus)
- [ ] Calendário de férias (bónus)

## Infraestrutura (Docker Compose)

- [x] Dockerfile — Spring Boot
- [x] Dockerfile — Next.js
- [x] docker-compose.yml (backend + frontend + PostgreSQL)
- [x] Variáveis de ambiente (.env)
- [x] README com instruções de setup e execução local
