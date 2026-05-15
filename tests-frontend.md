# Testes Sugeridos — Frontend

## Autenticação

- Login com credenciais válidas → redireciona para `/dashboard/vacation-requests`
- Login com credenciais inválidas → mensagem de erro visível, sem redirect
- Login sem preencher campos → validação inline (sem chamada à API)
- Aceder a `/dashboard` sem sessão → redirect para `/login`
- Aceder a `/dashboard` com token expirado → redirect para `/login` (verificação no `getSession`)
- Após logout → redirect para `/login`, localStorage limpo

---

## Navbar

- Role badge visível com a role correcta (ADMIN / MANAGER / COLLABORATOR)
- Link "Colaboradores" visível apenas para ADMIN
- Link "Férias" visível para todos os roles
- Link activo destacado conforme a rota actual
- Botão "Sair" limpa sessão e redireciona

---

## Listagem de Férias

- Tabela carrega com estado "A carregar..." enquanto o pedido está em curso
- Dados visíveis após carregamento
- Datas no formato `dd/mm/yyyy`
- Estado com a cor correcta: âmbar (Pendente), verde (Aprovado), vermelho (Rejeitado)
- Botões "Aprovar" e "Rejeitar" visíveis apenas para ADMIN e MANAGER em pedidos PENDING
- Botão "Editar" visível apenas para o dono do pedido quando PENDING
- Botão "Cancelar" visível apenas em pedidos PENDING
- Aprovar pedido → linha actualiza para "Aprovado" com nome do revisor, toast de sucesso
- Rejeitar pedido → linha actualiza para "Rejeitado", toast de sucesso
- Cancelar pedido → linha removida da tabela, toast de sucesso
- Erro na aprovação (ex: sobreposição) → toast de erro com mensagem do backend
- Polling: dados actualizados automaticamente a cada 15 segundos sem reload

### Filtros e Paginação

- Pesquisa por nome filtra resultados em tempo real
- Filtro por estado filtra correctamente (Pendente / Aprovado / Rejeitado)
- Combinação de filtros funciona em simultâneo
- Mudar filtro reseta para página 1
- Controlos de paginação surgem apenas quando há mais de 8 resultados
- Botão "Anterior" desactivado na primeira página
- Botão "Seguinte" desactivado na última página
- Contador "X pedidos" reflecte os resultados filtrados

---

## Criar Pedido de Férias

- Formulário não submete sem datas preenchidas
- `endDate` não aceita data anterior à `startDate` (validação inline)
- Submissão com sucesso → redirect para lista com toast de sucesso
- Submissão com datas sobrepostas → mensagem de erro do servidor visível no formulário

---

## Editar Pedido de Férias

- Formulário pré-preenchido com as datas do pedido existente
- Aceder a pedido não-PENDING → redirect para lista com toast de erro
- Aceder a pedido de outro utilizador → backend retorna 403, toast de erro
- Guardar com sucesso → redirect para lista com toast de sucesso
- Datas inválidas → validação inline impede submissão

---

## Listagem de Colaboradores (ADMIN)

- Aceder como MANAGER ou COLLABORATOR → redirect para `/dashboard`
- Tabela carrega com estado "A carregar..."
- Dados visíveis: nome, email, role (badge azul), manager
- Botão "Editar" navega para a página de edição
- Remover → confirmação nativa, colaborador removido da lista, toast de sucesso
- Erro ao remover → toast de erro
- Polling: dados actualizados a cada 15 segundos

### Filtros e Paginação

- Pesquisa por nome ou email filtra em tempo real
- Filtro por role filtra correctamente
- Paginação com os mesmos comportamentos da listagem de férias

---

## Criar / Editar Colaborador

- Formulário inválido (campos obrigatórios em falta) → erros inline
- Campo "Manager" lista apenas utilizadores com role MANAGER
- Password obrigatória na criação, opcional na edição
- Criar com email duplicado → mensagem de erro do servidor
- Editar sem alterar password → password mantida no backend
- Submissão com sucesso → redirect para lista

---

## Controlo de Acesso

- COLLABORATOR não vê o link "Colaboradores" na navbar
- COLLABORATOR não consegue navegar para `/dashboard/users` manualmente (redirect)
- MANAGER vê os seus próprios pedidos de férias e os dos seus colaboradores
- COLLABORATOR vê apenas os seus próprios pedidos
- ADMIN vê todos os pedidos e colaboradores
