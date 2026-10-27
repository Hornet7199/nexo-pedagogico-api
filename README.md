# pedagogico-api

Microserviço pedagógico do Nexo: turmas, matérias e conteúdos, notas, frequência, avaliações,
inscrições, observações, gestão de evasão e relatórios. Porta padrão: **8082**.

Não tem login nem conta de usuário: quem autentica é o **usuario-api**, e este serviço só
valida o token que ele emite.

## Rodando localmente

```bash
cp .env.example .env          # preencha NEXO_JWT_SECRET e DB_PASSWORD
docker compose up -d postgres-pedagogico
./mvnw spring-boot:run
```

- `NEXO_JWT_SECRET` precisa ser **o mesmo do usuario-api**: é o que faz o token emitido por
  ele valer aqui.
- O banco é **próprio** (`pedagogico_db`, porta 5437 no host), separado do usuario-api. As
  tabelas `alunos` e `professores` existem nos dois, com formatos diferentes.
- Com `NEXO_SEED_ENABLED=true` (só dev, nunca com o perfil `prod`) nascem turmas, notas,
  frequência e avaliações de exemplo. **Ligue também o seed do usuario-api**: os ids de aluno
  (1 a 12) e de professor (1 a 5) dos dois seeds precisam bater.

## Aluno e professor: cópia local

O pedagógico guarda uma cópia enxuta de aluno e professor (id, nome, turma, foto, e as
métricas que são daqui), para ligar nota, frequência e inscrição a uma pessoa sem uma chamada
de rede por linha. A fonte de verdade é o usuario-api, e o **id é o mesmo nos dois**.

O usuario-api mantém a cópia em dia chamando `/api/sync/*` quando alguém é cadastrado ou muda
de nome, turma ou foto. Essas rotas não são usadas pelo front.

| Rota | Quem |
|---|---|
| `PUT /api/sync/alunos/{id}`, `PUT /api/sync/professores/{id}` | DIRETOR, ADMIN |
| `PATCH /api/sync/usuarios/{usuarioId}/perfil` | o próprio dono da conta, DIRETOR, ADMIN |

## Rotas

| Rota | Quem |
|---|---|
| `/api/turmas` (lista, frequência, conteúdos, notas) | PROFESSOR (só as dele), DIRETOR |
| `/api/materias`, `/api/materias/{id}/conteudos` | autenticado (o aluno vê só a etapa dele) |
| `/api/alunos/{id}/notas`, `/api/alunos/{id}/observacoes` | PROFESSOR (turma dele), DIRETOR |
| `/api/aluno/notas`, `/api/aluno/materias`, `/api/aluno/conteudos/{id}/concluido` | ALUNO |
| `/api/professor/dashboard`, `/api/professor/materias` | PROFESSOR |
| `/api/avaliacoes`, `/api/questoes` | PROFESSOR, DIRETOR |
| `/api/inscricoes` | DIRETOR, ADMIN |
| `/api/evasao/risco`, `/api/relatorios/desempenho[/export]`, `/api/monitoramento/professores`, `/api/auditoria/eventos` | DIRETOR |
| `/api/admin/dashboard`, `/api/admin/catalogo/**` | ADMIN, DIRETOR |

## O que NÃO é daqui

- **Login, contas, perfil, foto e cadastro de aluno/professor** → usuario-api.
- **Gamificação** (XP, ranking, desafios, dashboard do aluno) → ainda no monólito.
- **Comunicação** (chat, avisos, dúvidas, mensagens) → ainda no monólito.
- **Limite conhecido:** o logout é do usuario-api, e este serviço não guarda tokens revogados.
  Um token já "deslogado" continua aceito aqui até expirar (15 minutos).

## Testes

`./mvnw verify` — usam H2 em memória (perfil `teste`), sem Postgres nem outros serviços. Os
tokens são emitidos pelo próprio teste (`TokenDeTeste`), com os mesmos ids de usuário do seed.
