# Task Plan - Sistema de Gestão de Férias

## Stack
- **Frontend**: Next.js 16, shadcn/ui, Tailwind CSS, react-hook-form
- **Backend**: Spring Boot 4 (Java 17, Gradle)
- **Base de dados**: PostgreSQL
- **Auth**: JWT (jjwt 0.12.6)
- **Infra**: Docker Compose

---

## Backend (Spring Boot)

- [x] Inicializar projecto Spring Boot (Maven/Gradle, estrutura Controllers/Services/Models)
- [x] Configurar ligação PostgreSQL (application.properties / datasource)
- [x] Configurar Spring Security + JWT (geração e validação de tokens)
- [x] Model/Entity: User (id, name, email, password, role, managerId)
- [x] Model/Entity: VacationRequest (id, userId, startDate, endDate, status)
- [x] Seed: dados iniciais (admin, managers, collaborators)
- [x] Endpoint: Auth — login e refresh token
- [x] Endpoint: CRUD colaboradores (apenas admin)
- [x] Endpoint: CRUD pedidos de férias
- [x] Validação: férias sobrepostas por utilizador na criação (PENDING/APPROVED)
- [x] Validação: sobreposição global entre colaboradores na aprovação (conforme enunciado)
- [x] Lógica de roles: admin / manager / collaborator
- [x] Aprovação/rejeição de férias (manager e admin)
- [x] Manager vê as suas próprias férias e as dos seus colaboradores
- [x] Manager não pode aprovar as suas próprias férias
- [x] Tratamento de erros e status codes consistentes
- [x] Documentação da API (Swagger / OpenAPI)

## Frontend (Next.js)

- [x] Inicializar projecto Next.js
- [x] Configurar cliente HTTP (axios / fetch) com interceptor JWT
- [x] Página: login
- [x] Gestão de sessão (armazenar e renovar token JWT)
- [x] Vista: listagem de colaboradores
- [x] Vista: detalhe / edição de colaborador
- [x] Vista: listagem de pedidos de férias
- [x] Vista: criação de pedido de férias
- [x] Vista: edição de pedido de férias (só o dono, só PENDING)
- [x] Controlo de permissões por role na UI
- [x] Toasts de sucesso/erro (sonner)
- [x] Loading states nas listagens
- [x] Paginação e filtros (colaboradores: nome/email/role; férias: colaborador/estado)
- [x] Polling automático nas listagens (15s)
- [x] Redirect automático com sessão expirada (verificação do JWT no cliente)
- [x] Nome do utilizador visível na navbar
- [ ] Calendário de férias (bónus)

## Infraestrutura (Docker Compose)

- [x] Dockerfile — Spring Boot
- [x] Dockerfile — Next.js
- [x] docker-compose.yml (backend + frontend + PostgreSQL)
- [x] Variáveis de ambiente (.env)
- [x] README com instruções de setup e execução local

## Testes

- [x] `tests.md` — testes sugeridos para backend (auth, users, vacation-requests)
- [x] `tests.md` — secção "Testes do PDF" com 33 cenários mapeados ao enunciado LBC
- [x] `tests-frontend.md` — testes sugeridos para frontend (UI, fluxos, controlo de acesso)
