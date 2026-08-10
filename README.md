# BakeFlow

Plataforma de gestão de estoque e produção para padarias e pequenos fabricantes de alimentos.

O BakeFlow é uma base open source para operações confiáveis. A versão atual oferece o catálogo de estoque, com itens, lotes, locais hierárquicos e consulta pública opcional de produtos por código de barras.

## Tecnologias

- Angular 22, TypeScript, SCSS, PrimeNG e PrimeIcons
- Spring Boot 4, Java 21, Maven e Flyway
- PostgreSQL e Redis
- Docker Compose e pgAdmin

## Arquitetura

O backend é um monólito modular. O módulo de estoque segue limites inspirados em DDD e SOLID: controllers acionam serviços de aplicação, objetos de domínio aplicam as regras do catálogo e persistência e APIs externas permanecem como detalhes de infraestrutura.

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

Os endpoints versionados ficam em `/api/v1/items`, `/api/v1/batches` e `/api/v1/locations`. Os recursos oferecem filtros paginados, criação, atualização e ativação/desativação sem exclusão física. A consulta por código de barras fica em `/api/v1/product-information/barcode/{barcode}`.

## Internacionalização

O BakeFlow oferece suporte a:

- Português do Brasil (`pt-BR`) — padrão
- English (`en`)

Toda a interface é internacionalizada e pode ser alterada imediatamente pelo seletor na sidebar. A preferência é persistida localmente no navegador pela chave `bakeflow.language`. Código, contratos técnicos, endpoints, enums e banco de dados permanecem em inglês. Dados cadastrados pelo usuário e informações externas do Open Food Facts não são traduzidos automaticamente.

## Configuração

Há perfis Spring para `dev`, `test` e `prod`. Banco de dados, Redis e Open Food Facts são configurados por variáveis de ambiente. O Hibernate valida o schema e o Flyway controla sua evolução. O Actuator expõe somente `health` e `info`, sem detalhes sensíveis.

## Roadmap

- [x] Catálogo de estoque
- [x] Itens
- [x] Lotes
- [x] Locais hierárquicos
- [x] Integração com Open Food Facts
- [x] Internacionalização pt-BR e en
- [ ] Saldo de estoque
- [ ] Movimentações de estoque
- [ ] FEFO
- [ ] Receitas
- [ ] Ordens de produção
- [ ] Autenticação e RBAC
- [ ] Auditoria
- [ ] Cache com Redis
- [ ] Observabilidade

## Licença

Licenciado sob a [Licença MIT](LICENSE).
