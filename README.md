# Sistema de Mural de Vagas - Agência do Trabalhador de Curitiba

Sistema web e painel de sinalização digital (TV e celular) utilizado na Agência do Trabalhador de Curitiba para exibição e gerenciamento em tempo real de vagas de emprego, mutirões, cursos de qualificação e processos seletivos.

---

## 🛠️ Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 4.0** (WebMVC, Data JPA, Server-Sent Events)
- **PostgreSQL** com pool de conexões **HikariCP**
- **Lombok** para redução de código boilerplate
- **OpenPDF / iText** para geração de relatórios de vagas em PDF
- **Google ZXing** para emissão de QR Codes dinâmicos
- **Vanilla HTML5, CSS3, JavaScript** modularizado e otimizado
- **Progressive Web App (PWA)** com **Service Worker** e cache offline
- **Docker** com multi-stage build (Java 21 JRE Alpine)

---

## 🏛️ Arquitetura do Sistema

### Backend (`br.gov.curitiba.mural`)

O backend foi refatorado seguindo os princípios de responsabilidade única e arquitetura em camadas:

- **`domain`**: Entidades JPA mapeadas para PostgreSQL (`Vaga`, `InfoConfig`).
- **`dto`**: Objetos tipados de entrada e saída para a API REST (`InfoDTO`, `TokenResponse`, `SessionResponse`, `ValidationResponse`, `MuralDataResponse`).
- **`repository`**: Repositórios Spring Data JPA (`VagaRepository`, `InfoConfigRepository`) com consultas JPQL otimizadas.
- **`service`**: Camada de regras de negócio desacoplada:
  - `VagaService`: CRUD, validações, ordenação de mutirões e gatilhos de sincronização.
  - `InfoService`: Gestão de cursos, comunicados institucionais e temas visuais dos painéis.
  - `PdfReportService`: Formatação e renderização de relatórios tabulares em PDF agrupados por categoria.
  - `QrCodeService`: Criação de imagens PNG de QR Code para leitura por celular.
  - `MuralTokenService`: Assinatura criptográfica HMAC-SHA256 para controle de tokens temporários.
  - `PanelAccessWindow`: Controle de horário comercial de acesso ao painel.
  - `VagaSseService`: Transmissão de eventos em tempo real para as TVs via Server-Sent Events.
- **`controller`**: Controladores REST enxutos (`VagaController`, `InfoController`, `MuralController`, `HealthController`).
- **`exception`**: Tratamento global de erros (`GlobalExceptionHandler`).

### Frontend (`src/main/resources/static/`)

Modularizado em arquivos dedicados de marcação, estilo e comportamento:

- **HTML**:
  - `tv.html`: Exibição contínua e rotativa de vagas na TV da recepção.
  - `tv-mutiroes.html`: Painel para dias de eventos e mutirões de contratação.
  - `admin.html`: Painel de controle para os servidores cadastrarem vagas e editarem informes.
  - `mural.html`: Visualização responsiva no smartphone do cidadão via leitura de QR Code.
  - `mural-mutiroes.html`: Visualização de mutirões no smartphone.
- **CSS**: Arquivos isolados em `/css/` (`admin.css`, `tv.css`, `tv-mutiroes.css`, `mural.css`, `mural-mutiroes.css`).
- **JavaScript**: Scripts desacoplados em `/js/` (`admin.js`, `tv.js`, `tv-mutiroes.js`, `mural.js`, `mural-mutiroes.js`).
- **Service Worker (`sw.js`)**: Cache-first para assets estáticos e network-first para dados da API.

---

## ⚙️ Variáveis de Ambiente

As configurações principais podem ser customizadas via variáveis de ambiente com fallbacks automáticos:

| Variável | Padrão | Descrição |
|---|---|---|
| `PORT` | `8080` | Porta onde o servidor HTTP escuta |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...` | URL de conexão JDBC do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `neondb_owner` | Usuário do banco de dados |
| `SPRING_DATASOURCE_PASSWORD` | *(credencial atual)* | Senha do banco de dados |
| `PANEL_ACCESS_START` | `07:10` | Horário de início das consultas |
| `PANEL_ACCESS_END` | `17:10` | Horário de encerramento das consultas |
| `PANEL_ACCESS_ZONE` | `America/Sao_Paulo` | Fuso horário operacional da agência |
| `MURAL_TOKEN_SESSION_VALIDITY` | `15` | Validade da sessão mobile em minutos |
| `MURAL_TOKEN_QR_VALIDITY` | `120` | Validade do QR Code nas TVs em minutos |
| `MURAL_TOKEN_BIND_IP` | `false` | Se verdadeiro, restringe o token à sub-rede IP |

---

## 🚀 Como Executar

### 1. Pré-requisitos
- JDK 21 instalado
- Git

### 2. Executando Localmente (Desenvolvimento)
```bash
./gradlew bootRun
```
Acesse o painel administrativo em: `http://localhost:8080/admin.html`  
Acesse o painel da TV em: `http://localhost:8080/tv.html`

### 3. Executando os Testes Automatizados
```bash
./gradlew test
```

### 4. Gerando o Pacote Executável (.jar)
```bash
./gradlew bootJar
```
O executável será gerado em: `build/libs/mural-vagas-agencia-1.0.0-SNAPSHOT.jar`

### 5. Executando com Docker
```bash
docker build -t mural-vagas-agencia .
docker run -p 8080:8080 mural-vagas-agencia
```
