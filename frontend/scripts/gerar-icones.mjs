// Gera os ícones do PWA (192/512, "any" e "maskable"), o apple-touch-icon, o badge e o favicon PNG
// a partir do SVG do logo. Usa o Chromium do Playwright para rasterizar:
//   npm run icones    (requer o pacote "playwright" disponível: npx playwright install chromium)
import { createRequire } from 'node:module'
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const { chromium } = require('playwright')
const raiz = join(dirname(fileURLToPath(import.meta.url)), '..', 'public')
mkdirSync(join(raiz, 'icons'), { recursive: true })

// Ícone (rota creme, paradas vazadas, alvo âmbar) – mesmo desenho de src/components/Logo.jsx
const icone = (fundo = '#1E2A21', rota = '#F3EFE6') => `
  <path d="M9 55 C15 49 20 45 27 42 S41 30 49 17" fill="none" stroke="${rota}" stroke-width="3.4" stroke-linecap="round" stroke-dasharray="4.5 4.5"/>
  <circle cx="9" cy="55" r="5" fill="${fundo}" stroke="${rota}" stroke-width="3.4"/>
  <circle cx="27" cy="42" r="5" fill="${fundo}" stroke="${rota}" stroke-width="3.4"/>
  <circle cx="49" cy="17" r="11.5" fill="${fundo}" stroke="#E9A23B" stroke-width="3.6"/>
  <circle cx="49" cy="17" r="5.6" fill="none" stroke="#E9A23B" stroke-width="3"/>
  <circle cx="49" cy="17" r="2" fill="#E9A23B"/>`

// "any": cantos arredondados, ícone ocupando ~78%
const svgAny = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1E2A21"/><stop offset="1" stop-color="#263429"/></linearGradient></defs>
  <rect width="64" height="64" rx="14" fill="url(#g)"/>
  <g transform="translate(6.5 7) scale(0.8)">${icone('#22302A')}</g></svg>`

// "maskable": fundo cheio, ícone dentro da zona segura (círculo de 80%)
const svgMaskable = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1E2A21"/><stop offset="1" stop-color="#263429"/></linearGradient></defs>
  <rect width="64" height="64" fill="url(#g)"/>
  <g transform="translate(14.4 14.6) scale(0.56)">${icone('#22302A')}</g></svg>`

// badge (barra de notificações do Android): só a silhueta, branca sobre transparente
const svgBadge = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
  <g transform="translate(3 3) scale(0.9)">${icone('transparent', '#FFFFFF').replaceAll('#E9A23B', '#FFFFFF')}</g></svg>`

writeFileSync(join(raiz, 'favicon.svg'), svgAny)
writeFileSync(join(raiz, 'icons', 'maskable.svg'), svgMaskable)

const alvos = [
  ['icons/icon-192.png', svgAny, 192], ['icons/icon-512.png', svgAny, 512],
  ['icons/maskable-192.png', svgMaskable, 192], ['icons/maskable-512.png', svgMaskable, 512],
  ['icons/apple-touch-icon.png', svgMaskable, 180], ['icons/badge-72.png', svgBadge, 72],
  ['favicon-32.png', svgAny, 32],
]

const navegador = await chromium.launch()
const pagina = await navegador.newPage()
for (const [arquivo, svg, px] of alvos) {
  await pagina.setViewportSize({ width: px, height: px })
  await pagina.setContent(`<html><body style="margin:0;background:transparent">${svg.replace('<svg ', `<svg width="${px}" height="${px}" `)}</body></html>`)
  writeFileSync(join(raiz, arquivo), await pagina.screenshot({ omitBackground: true, clip: { x: 0, y: 0, width: px, height: px } }))
  console.log('gerado', arquivo)
}
await navegador.close()
console.log(readFileSync(join(raiz, 'favicon.svg'), 'utf8').length, 'bytes no favicon.svg')
