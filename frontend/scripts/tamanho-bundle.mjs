// Mede o bundle inicial (o que o index.html carrega: script, modulepreload e CSS) em gzip
// e falha se passar de 300 KB. Uso: npm run build && npm run tamanho
import { readFileSync, readdirSync } from 'node:fs'
import { gzipSync } from 'node:zlib'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const LIMITE_KB = 300
const dist = join(dirname(fileURLToPath(import.meta.url)), '..', 'dist')
const html = readFileSync(join(dist, 'index.html'), 'utf8')
const arquivos = [...html.matchAll(/(?:src|href)="(\/assets\/[^"]+\.(?:js|css))"/g)].map((m) => m[1])
const gz = (f) => gzipSync(readFileSync(join(dist, f))).length / 1024

let total = gz('index.html')
console.log(`index.html  ${gz('index.html').toFixed(1)} KB`)
for (const a of arquivos) {
  const t = gz(a)
  total += t
  console.log(`${a}  ${t.toFixed(1)} KB`)
}
console.log(`\nBundle inicial (gzip): ${total.toFixed(1)} KB (limite ${LIMITE_KB} KB)`)

const sw = readFileSync(join(dist, 'sw.js'), 'utf8')
const precache = readdirSync(join(dist, 'assets')).filter((f) => sw.includes(`assets/${f}`))
const precacheKb = precache.reduce((s, f) => s + gz(`assets/${f}`), 0)
console.log(`Precache do service worker: ${precache.length} arquivos em /assets, ${precacheKb.toFixed(1)} KB gzip`)
if (total > LIMITE_KB) {
  console.error('Bundle inicial acima do limite!')
  process.exit(1)
}
