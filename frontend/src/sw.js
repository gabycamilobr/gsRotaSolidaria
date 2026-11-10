/* Service worker do Rota Solidária (estratégia injectManifest do vite-plugin-pwa). */
import { clientsClaim } from 'workbox-core'
import { cleanupOutdatedCaches, createHandlerBoundToURL, precacheAndRoute } from 'workbox-precaching'
import { NavigationRoute, registerRoute } from 'workbox-routing'
import { CacheFirst, StaleWhileRevalidate } from 'workbox-strategies'
import { ExpirationPlugin } from 'workbox-expiration'

self.skipWaiting()
clientsClaim()

// App shell: HTML, CSS, JS inicial e ícones ficam em cache na instalação.
precacheAndRoute(self.__WB_MANIFEST)
cleanupOutdatedCaches()
registerRoute(new NavigationRoute(createHandlerBoundToURL('index.html'), { denylist: [/^\/api\//] }))

// Telas carregadas sob demanda (leitor de QR, Firebase…): cache na primeira visita.
registerRoute(
  ({ url, request }) => url.origin === self.location.origin && url.pathname.startsWith('/assets/') && request.destination === 'script',
  new CacheFirst({ cacheName: 'telas', plugins: [new ExpirationPlugin({ maxEntries: 40 })] }),
)

// Fontes do Google
registerRoute(({ url }) => url.origin === 'https://fonts.googleapis.com',
  new StaleWhileRevalidate({ cacheName: 'google-fonts-css' }))
registerRoute(({ url }) => url.origin === 'https://fonts.gstatic.com',
  new CacheFirst({ cacheName: 'google-fonts', plugins: [new ExpirationPlugin({ maxEntries: 20, maxAgeSeconds: 365 * 24 * 3600 })] }))

// Fotos dos lotes (já comprimidas): cache para não baixar de novo em rede lenta.
registerRoute(({ url }) => /\/api\/lotes\/\d+\/foto$/.test(url.pathname),
  new CacheFirst({ cacheName: 'fotos-lotes', plugins: [new ExpirationPlugin({ maxEntries: 60, maxAgeSeconds: 7 * 24 * 3600 })] }))

// Push do Firebase Cloud Messaging (quando configurado).
self.addEventListener('push', (event) => {
  let dados = {}
  try { dados = event.data?.json() ?? {} } catch { dados = { notification: { body: event.data?.text() } } }
  const n = dados.notification || {}
  const d = dados.data || {}
  const titulo = n.title || d.titulo || 'Rota Solidária'
  event.waitUntil(self.registration.showNotification(titulo, {
    body: n.body || d.mensagem || '',
    icon: '/icons/icon-192.png',
    badge: '/icons/badge-72.png',
    tag: d.loteId ? `lote-${d.loteId}` : undefined,
    data: { url: '/' },
  }))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil((async () => {
    const janelas = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
    if (janelas.length) return janelas[0].focus()
    return self.clients.openWindow(event.notification.data?.url || '/')
  })())
})
