# Testes Sugeridos

## Auth

- `POST /api/v1/auth/login` com credenciais válidas → 200 com token e role
- `POST /api/v1/auth/login` com password errada → 401
- `POST /api/v1/auth/login` com email inexistente → 401
- `POST /api/v1/auth/login` sem body → 400

---

## Users `/api/v1/users`

### Autorização
- Qualquer endpoint com token de `MANAGER` → 403
- Qualquer endpoint com token de `COLLABORATOR` → 403
- Qualquer endpoint sem token → 401

### GET /api/v1/users
- Com token de `ADMIN` → 200 com lista de utilizadores (sem campo `password`)

### GET /api/v1/users/{id}
- ID existente com token de `ADMIN` → 200 com utilizador
- ID inexistente → 404

### POST /api/v1/users
- Body válido com token de `ADMIN` → 201 sem campo `password`
- Email duplicado → 409
- Sem campo `password` → 409
- Body sem `name` ou `email` → 400
- `managerId` inválido (não existe) → 404

### PUT /api/v1/users/{id}
- Body válido → 200 com dados actualizados
- Sem `password` no body → 200 (password mantida)
- Com `password` no body → 200 (password alterada)
- ID inexistente → 404

### DELETE /api/v1/users/{id}
- ID existente → 204
- ID inexistente → 404

---

## Vacation Requests `/api/v1/vacation-requests`

### GET /api/v1/vacation-requests
- Token de `ADMIN` → 200 com todos os pedidos
- Token de `MANAGER` → 200 apenas com pedidos dos seus colaboradores
- Token de `COLLABORATOR` → 200 apenas com os seus próprios pedidos
- Sem token → 401

### GET /api/v1/vacation-requests/{id}
- `ADMIN` acede a qualquer pedido → 200
- `MANAGER` acede a pedido de colaborador seu → 200
- `MANAGER` acede a pedido de colaborador de outro manager → 403
- `COLLABORATOR` acede ao seu próprio pedido → 200
- `COLLABORATOR` acede ao pedido de outro → 403
- ID inexistente → 404

### POST /api/v1/vacation-requests
- `COLLABORATOR` cria pedido → 201 com userId forçado ao próprio (ignora userId do body)
- `ADMIN` cria pedido para qualquer userId → 201
- Datas sobrepostas com pedido aprovado → 409
- `endDate` antes de `startDate` → aceite (validação de negócio a adicionar)
- Body sem `startDate` ou `endDate` → 400

### PUT /api/v1/vacation-requests/{id}
- `COLLABORATOR` edita o seu pedido `PENDING` → 200
- `COLLABORATOR` tenta editar pedido de outro → 403
- Editar pedido `APPROVED` ou `REJECTED` → 409
- Novas datas com sobreposição (excluindo o próprio) → 409

### DELETE /api/v1/vacation-requests/{id} (cancel)
- `COLLABORATOR` cancela o seu pedido `PENDING` → 204
- `COLLABORATOR` cancela pedido de outro → 403
- Cancelar pedido `APPROVED` → 409
- `ADMIN` cancela qualquer pedido não aprovado → 204

### PATCH /api/v1/vacation-requests/{id}/approve
- `MANAGER` aprova pedido de colaborador seu `PENDING` → 200 com status APPROVED
- `MANAGER` tenta aprovar pedido de colaborador de outro manager → 403
- `ADMIN` aprova qualquer pedido `PENDING` → 200
- `COLLABORATOR` tenta aprovar → 403
- Pedido já `APPROVED` ou `REJECTED` → 409

### PATCH /api/v1/vacation-requests/{id}/reject
- `MANAGER` rejeita pedido de colaborador seu `PENDING` → 200 com status REJECTED
- `MANAGER` tenta rejeitar pedido de colaborador de outro manager → 403
- `ADMIN` rejeita qualquer pedido `PENDING` → 200
- `COLLABORATOR` tenta rejeitar → 403
- Pedido já `APPROVED` ou `REJECTED` → 409

---

## Testes do PDF

Cenários derivados directamente dos requisitos do enunciado (LBC — Teste Técnico Full Stack).
Testam a conformidade da implementação com os critérios de avaliação.

### Colaboradores

| # | Cenário | Esperado | Estado |
|---|---------|----------|--------|
| 1 | Admin cria colaborador com manager válido | 201, colaborador associado ao manager | ✅ |
| 2 | Admin lista todos os colaboradores | 200 com lista completa | ✅ |
| 3 | Admin obtém detalhe de colaborador por ID | 200 com dados do colaborador | ✅ |
| 4 | Admin edita nome/email/role de colaborador | 200 com dados actualizados | ✅ |
| 5 | Admin remove colaborador | 204 | ✅ |
| 6 | Manager tenta criar colaborador | 403 | ✅ |
| 7 | Collaborator tenta criar colaborador | 403 | ✅ |
| 8 | Criar colaborador sem associar manager | 201 (manager é opcional no modelo) | ✅ |

### Pedidos de Férias — Regras de Negócio

| # | Cenário | Esperado | Estado |
|---|---------|----------|--------|
| 9  | Collaborator cria pedido → userId no body ignorado, usa o próprio | 201 com userId correcto | ✅ |
| 10 | Collaborator vê apenas os seus próprios pedidos | 200 só com pedidos próprios | ✅ |
| 11 | Manager vê pedidos dos seus colaboradores e os próprios | 200 filtrado | ✅ |
| 12 | Admin vê todos os pedidos | 200 com tudo | ✅ |
| 13 | Pedido tem status inicial `PENDING` | campo `status` = PENDING | ✅ |
| 14 | Manager aprova pedido de colaborador seu | 200, status = APPROVED, reviewedBy preenchido | ✅ |
| 15 | Manager rejeita pedido de colaborador seu | 200, status = REJECTED | ✅ |
| 16 | Manager tenta aprovar pedido de colaborador de outro manager | 403 | ✅ |
| 17 | Admin aprova qualquer pedido | 200, status = APPROVED | ✅ |
| 18 | Collaborator tenta aprovar pedido | 403 | ✅ |
| 19 | Datas inclusivas: pedido 01/08–05/08 cobre 5 dias | startDate e endDate guardadas correctamente | ✅ |
| 20 | Collaborator edita o seu pedido PENDING | 200 com datas actualizadas | ✅ |
| 21 | Collaborator tenta editar pedido APPROVED | 409 | ✅ |
| 22 | Collaborator cancela o seu pedido PENDING | 204 | ✅ |
| 23 | Cancelar pedido APPROVED | 409 | ✅ |

### Validação de Sobreposição

| # | Cenário | Esperado | Estado |
|---|---------|----------|--------|
| 24 | Collaborator cria pedido para período já com APPROVED seu | 409 | ✅ |
| 25 | Collaborator cria pedido para período com PENDING seu | 409 | ✅ |
| 26 | Aprovar pedido quando já existe APPROVED sobreposto do mesmo utilizador | 409 | ✅ |
| 27 | Dois colaboradores diferentes com férias aprovadas no mesmo período | 409 ao tentar aprovar o segundo | ✅ |

### Bónus

| # | Cenário | Esperado | Estado |
|---|---------|----------|--------|
| 28 | Login com credenciais válidas | 200 com token JWT, role e userId | ✅ |
| 29 | Login com credenciais inválidas | 401 | ✅ |
| 30 | Chamada autenticada com token expirado | 401 | ✅ |
| 31 | Listagem de colaboradores com filtro por role | Resultados filtrados client-side | ✅ |
| 32 | Listagem de férias com filtro por estado | Resultados filtrados client-side | ✅ |
| 33 | Paginação em ambas as listagens (8 por página) | Navegação correcta entre páginas | ✅ |
