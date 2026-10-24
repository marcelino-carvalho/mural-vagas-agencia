/**
 * Service Worker para funcionamento offline e cache inteligente dos assets
 * do Mural de Vagas da Agência do Trabalhador de Curitiba.
 */
const CACHE_VERSION = "mural-v10";
const STATIC_CACHE = `static-${CACHE_VERSION}`;
const API_CACHE = `api-${CACHE_VERSION}`;

/**
 * Lista de recursos estáticos pré-cacheados na instalação
 */
const STATIC_ASSETS = [
  "/",
  "/mural.html",
  "/mural-mutiroes.html",
  "/tv.html",
  "/tv-mutiroes.html",
  "/admin.html",
  "/css/admin.css",
  "/css/tv.css",
  "/css/tv-mutiroes.css",
  "/css/mural.css",
  "/css/mural-mutiroes.css",
  "/js/admin.js",
  "/js/tv.js",
  "/js/tv-mutiroes.js",
  "/js/mural.js",
  "/js/mural-mutiroes.js",
  "/logo-agencia-2026.png",
  "/manifest.json",
  "/sw.js"
];

// Instalação: baixa e armazena os assets estáticos no cache
self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => cache.addAll(STATIC_ASSETS))
  );
  self.skipWaiting();
});

// Ativação: remove caches de versões antigas e assume controle dos clientes
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== STATIC_CACHE && key !== API_CACHE)
          .map((key) => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

// Interceptação de requisições de rede
self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) {
    return;
  }

  // Requisições para a API REST seguem estratégia Network-First (com fallback de cache)
  if (url.pathname.startsWith("/api/")) {
    event.respondWith(networkFirst(request));
    return;
  }

  // Recursos estáticos (HTML, CSS, JS, Imagens) seguem estratégia Network-First para sempre pegar atualizações se online
  event.respondWith(networkFirstStatic(request));
});

/**
 * Estratégia Network-First para arquivos estáticos:
 * Se houver internet, baixa a versão mais nova e atualiza o cache;
 * se estiver sem internet, usa o cache salvo.
 */
async function networkFirstStatic(request) {
  const cache = await caches.open(STATIC_CACHE);
  try {
    const response = await fetch(request);
    cache.put(request, response.clone());
    return response;
  } catch (_) {
    const cached = await cache.match(request);
    if (cached) return cached;
    throw _;
  }
}

/**
 * Estratégia Network-First para API REST:
 * Tenta obter a versão mais recente na rede; se offline, entrega a última resposta em cache.
 */
async function networkFirst(request) {
  const cache = await caches.open(API_CACHE);
  try {
    const response = await fetch(request);
    cache.put(request, response.clone());
    return response;
  } catch (_) {
    const cached = await cache.match(request);
    if (cached) return cached;
    throw _;
  }
}
