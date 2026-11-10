import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

const API_LOCAL = process.env.API_LOCAL || 'http://localhost:8080'

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src',
      filename: 'sw.js',
      registerType: 'autoUpdate',
      injectRegister: false,
      includeAssets: ['favicon.svg', 'favicon-32.png', 'icons/apple-touch-icon.png', 'icons/badge-72.png'],
      manifest: {
        name: 'Rota Solidária',
        short_name: 'Rota Solidária',
        description: 'Da sobra ao prato, no mesmo bairro, em menos de uma hora.',
        lang: 'pt-BR',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        orientation: 'portrait',
        background_color: '#1E2A21',
        theme_color: '#1E2A21',
        categories: ['social', 'food'],
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
          { src: '/icons/maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
          { src: '/icons/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      injectManifest: {
        // app shell no precache; o leitor de QR e o Firebase (pesados, sob demanda) ficam no cache em tempo de execução
        globPatterns: ['**/*.{js,css,html,svg,png,webmanifest}'],
        globIgnores: ['**/leitorQr-*.js', '**/firebase-*.js', '**/icon-512.png', '**/maskable-512.png'],
      },
      devOptions: { enabled: false },
    }),
  ],
  server: { port: 5173, proxy: { '/api': API_LOCAL } },
  preview: { port: 4173, proxy: { '/api': API_LOCAL } },
  build: { target: 'es2020', sourcemap: false },
})
