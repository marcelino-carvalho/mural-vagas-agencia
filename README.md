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
