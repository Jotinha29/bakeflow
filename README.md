# BakeFlow

Plataforma de catálogo de estoque e gestão de produção para padarias e pequenos fabricantes de alimentos.

O BakeFlow é uma base open source para operações confiáveis. A versão atual oferece catálogo, lotes, locais, consulta de saldos, entradas, saídas, transferências, perdas, ajustes por inventário, histórico completo e rastreável de estoque, receitas e ordens de produção.

## Tecnologias

- Angular 22, TypeScript, SCSS, PrimeNG e PrimeIcons
- Spring Boot 4, Java 21, Maven e Flyway
- PostgreSQL e Redis para cache, stale fallback e rate limiting
- Docker Compose e pgAdmin

## Arquitetura

O backend é um monólito modular. Inventory mantém itens, lotes, locais, saldos e movimentos. Production mantém receitas, ordens, consumos e resultados, utilizando operações de estoque por uma fronteira explícita. Controllers acionam serviços de aplicação e persistência e APIs externas permanecem como detalhes de infraestrutura.

```text
Angular → REST API → Spring Boot → Application → Domain → Infrastructure
                                                        ├── PostgreSQL
                                                        ├── Redis
                                                        ├── Open Food Facts
                                                        └── BrasilAPI
```

As integrações ficam isoladas por adapters e retornam contratos pequenos pertencentes ao BakeFlow. Nenhuma consulta externa persiste itens ou empresas automaticamente.

```text
Angular → Spring Boot → Integration Service → Cache Service → Redis
                                                     ├── HIT → resposta
                                                     └── MISS → API externa
```

O caminho externo aplica timeout, retry apenas para falhas transitórias, circuit breaker local por provedor e fallback stale identificado por `fresh: false`. O breaker é local porque o BakeFlow é um monólito de instância única; distribuir esse estado aumentaria a complexidade sem benefício atual. Um lock curto por recurso no processo evita stampede nesta topologia. Redis nunca substitui o PostgreSQL como fonte de verdade.

## Execução local

Requisitos: Docker e Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

Os valores de exemplo servem apenas para desenvolvimento. Altere-os no `.env` local quando necessário; esse arquivo não é versionado.

| Serviço | URL |
| --- | --- |
| BakeFlow | http://localhost:4300 |
| Liveness | http://localhost:8090/api/system/health/live |
| Readiness | http://localhost:8090/api/system/health/ready |
| pgAdmin | http://localhost:5060 |

No pgAdmin, conecte-se ao host `postgres`, porta `5432`, usando os dados PostgreSQL do `.env`. O perfil `dev` carrega um pequeno catálogo fictício em português; produção não carrega dados de demonstração.

### Comandos de desenvolvimento

```bash
cd frontend
npm ci
npm start
npm run lint
npm test -- --watch=false
npm run build
```

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

O servidor Angular encaminha `/api` para `localhost:8090`. No ambiente em containers, o frontend encaminha o mesmo caminho diretamente ao serviço backend.

## API

Os endpoints versionados ficam em `/api/v1/items`, `/api/v1/batches`, `/api/v1/locations`, `/api/v1/recipes` e `/api/v1/production-orders`. Operações explícitas iniciam, finalizam ou cancelam ordens.

## Integrações e resiliência

| Provedor | Endpoint interno | Cache fresh | Uso |
| --- | --- | --- | --- |
| Open Food Facts | `GET /api/v1/integrations/product/{barcode}` | 24 h | Produto por código de barras |
| BrasilAPI | `GET /api/v1/integrations/company/{cnpj}` | 12 h | Empresa por CNPJ |

Os TTLs são configuráveis. O valor permanece retido por mais sete dias para fallback stale quando o provedor falha, sempre indicado no contrato. As chamadas possuem timeouts de conexão e leitura, no máximo duas tentativas e circuit breaker. Um fixed window no Redis limita apenas endpoints externos e responde HTTP 429. As chaves seguem `bakeflow:integration:{provider}:{resource}:{id}`, `bakeflow:ratelimit:{resource}:{client}:{window}` e marcadores fresh com sufixo `:fresh`.

Cada resposta inclui `X-Request-ID`, aceitando somente identificadores de formato seguro enviados pelo cliente. `/api/v1/system/integrations` informa configuração, circuitos e disponibilidade do Redis sem consultar provedores. Eventos de estoque decorrentes da produção e transições das ordens são persistidos transacionalmente em `audit_events`, separados dos logs técnicos. A auditoria é atualmente interna: os eventos identificam o usuário autenticado quando aplicável, mas ainda não há tela ou endpoint de consulta.

## Fluxo de produção

```text
Receita → Ordem de Produção → Preview FEFO → Consumo → Produção → Lote acabado → Estoque
```

O backend calcula oficialmente a necessidade dos ingredientes. Ao iniciar uma ordem, seleciona lotes válidos por FEFO: menor validade primeiro, depois criação e ID; lotes sem validade ficam por último e vencidos são ignorados. Consumo, movimentos e transição para `IN_PROGRESS` são atômicos. Ao finalizar, lote, entrada de estoque, rastreabilidade e transição para `COMPLETED` são confirmados na mesma transação.

## Internacionalização

O BakeFlow oferece suporte a:

- Português do Brasil (`pt-BR`) — padrão
- English (`en`)

Toda a interface é internacionalizada e pode ser alterada imediatamente pelo seletor na sidebar. A preferência é persistida localmente no navegador pela chave `bakeflow.language`. Código, contratos técnicos, endpoints, enums e banco de dados permanecem em inglês. Dados cadastrados pelo usuário e informações externas do Open Food Facts não são traduzidos automaticamente.

## Configuração

Há perfis Spring para `dev`, `test` e `prod`. Banco de dados, Redis e Open Food Facts são configurados por variáveis de ambiente. O Hibernate valida o schema e o Flyway controla sua evolução. O Actuator expõe somente `health` e `info`, sem detalhes sensíveis.

## Destaques técnicos

- monólito modular e migrations Flyway;
- estoque transacional, controle de concorrência e FEFO;
- rastreabilidade de produção e auditoria de negócio;
- internacionalização pt-BR/en;
- Open Food Facts e BrasilAPI com parsing defensivo;
- cache-aside Redis, TTL, fallback stale e proteção contra stampede;
- timeout, retry, circuit breaker e rate limiting;
- testes automatizados e Docker Compose.

## Autenticação e controle de acesso

O backend é a autoridade de acesso por Spring Security. O login gera JWT HS256 de 15 minutos mantido apenas em memória pela SPA. O refresh token é aleatório, persistido somente como SHA-256, enviado em cookie HttpOnly/SameSite Strict e rotacionado a cada uso. A reutilização de um token revoga a família na mesma transação, preservando a revogação mesmo quando a API retorna erro. Senhas usam Argon2id e aceitam passphrases de 8 a 128 caracteres. Endpoints pessoais validam ownership e operações baseadas no cookie validam a origem, complementando SameSite. Em produção, configure cookie Secure e um segredo JWT aleatório com pelo menos 32 caracteres.

| Recurso | Admin | Gestor | Operador | Consulta |
| --- | --- | --- | --- | --- |
| Consultar estoque | ✓ | ✓ | ✓ | ✓ |
| Movimentar estoque | ✓ | ✓ | ✓ | — |
| Produção | ✓ | ✓ | operar | leitura |
| Receitas | ✓ | ✓ | leitura | leitura |
| Usuários | ✓ | — | — | — |
| Auditoria | ✓ | ✓ | — | — |

O perfil `dev` garante idempotentemente que o e-mail configurado possua o papel ADMIN quando `DEMO_ADMIN_PASSWORD` foi informado. Senhas demo fora da política interrompem a inicialização com uma mensagem explícita, sem revelar o valor. O perfil de produção nunca cria credenciais padrão. Ao inativar um usuário, todos os refresh tokens são revogados; por decisão documentada, o access token curto existente permanece válido até expirar.

```text
Angular → JWT Bearer → Spring Security → Application → PostgreSQL
               Refresh → HttpOnly Cookie → rotação e revogação
```

### Testes de segurança

A suíte dedicada mantém o Spring Security habilitado e cobre fronteiras 401/403, roles representativas, JWT inválido ou expirado, cookies, logout, sessões, IDOR, alteração de senha, rate limit e persistência da revogação após reuse. No frontend, os testes cobrem o interceptor restrito à API interna, refresh single-flight, falha de refresh, guards e sanitização de filtros.

### Design System e composição do frontend

O frontend preserva os tokens visuais do BakeFlow e usa componentes pequenos em `shared/ui` para cabeçalhos, filtros, containers de tabela, ações, estados e cards. Regras visuais de estoque e integrações permanecem em componentes especializados de cada feature, compostos sobre essa base. Os layouts reorganizam ações e filtros em telas estreitas e mantêm tabelas com rolagem horizontal, sem recorrer a páginas CRUD ou tabelas universais configuradas por JSON.

### Aviso de licença PrimeUI

O PrimeNG 22 inclui transitivamente `@primeuix/styled` e `@primeui/license-manager`. Em ambientes sem uma chave PrimeUI válida, a biblioteca emite um aviso no console. O aviso não foi ocultado nem contornado: a correção legítima exige configurar uma licença compatível com os termos do fornecedor ou substituir a dependência em uma evolução futura.

## Roadmap

- [x] Gestão de itens
- [x] Controle de lotes
- [x] Locais de estoque
- [x] Saldo e movimentos transacionais usados pela produção
- [x] Movimentações manuais e consulta completa de estoque
- [x] Integração com Open Food Facts
- [x] Internacionalização pt-BR e en
- [x] Receitas
- [x] Ordens de produção
- [x] FEFO
- [x] Rastreabilidade de produção
- [x] Autenticação e RBAC
- [x] Cache com Redis
- [x] Integração com BrasilAPI
- [x] Resiliência e observabilidade básica
- [ ] CI/CD

## Licença

Licenciado sob a [Licença MIT](LICENSE).
