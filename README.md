# BakeFlow

Plataforma de gestão de estoque e produção para padarias e pequenos fabricantes de alimentos.

O BakeFlow é uma base open source para operações confiáveis. A versão atual oferece catálogo e saldo de estoque, rastreabilidade de movimentações, receitas, ordens de produção e consulta pública opcional de produtos por código de barras.

## Tecnologias

- Angular 22, TypeScript, SCSS, PrimeNG e PrimeIcons
- Spring Boot 4, Java 21, Maven e Flyway
- PostgreSQL e Redis
- Docker Compose e pgAdmin

## Arquitetura

O backend é um monólito modular. Inventory mantém itens, lotes, locais, saldos e movimentos. Production mantém receitas, ordens, consumos e resultados, utilizando operações de estoque por uma fronteira explícita. Controllers acionam serviços de aplicação e persistência e APIs externas permanecem como detalhes de infraestrutura.

```text
Angular → REST API → Spring Boot → Application → Domain → Infrastructure
                                                        ├── PostgreSQL
                                                        └── Open Food Facts
```

O Open Food Facts fica isolado por `ProductInformationGateway`. A resposta é convertida para um contrato pequeno, pertencente ao BakeFlow; timeouts ou indisponibilidade externa não impedem o cadastro manual. O uso de Redis para cache foi adiado intencionalmente.

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
| Saúde do backend | http://localhost:8090/actuator/health |
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

Os endpoints versionados ficam em `/api/v1/items`, `/api/v1/batches`, `/api/v1/locations`, `/api/v1/recipes` e `/api/v1/production-orders`. Operações explícitas iniciam, finalizam ou cancelam ordens. A consulta por código de barras fica em `/api/v1/product-information/barcode/{barcode}`.

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

## Roadmap

- [x] Gestão de itens
- [x] Controle de lotes
- [x] Locais de estoque
- [x] Controle de saldo
- [x] Movimentações
- [x] Integração com Open Food Facts
- [x] Internacionalização pt-BR e en
- [x] Receitas
- [x] Ordens de produção
- [x] FEFO
- [x] Rastreabilidade de produção
- [ ] Autenticação e RBAC
- [ ] Cache com Redis
- [ ] Integrações externas adicionais
- [ ] Observabilidade
- [ ] CI/CD

## Licença

Licenciado sob a [Licença MIT](LICENSE).
