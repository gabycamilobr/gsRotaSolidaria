// Formatação em português do Brasil, sempre no fuso de São Paulo.
const FUSO = 'America/Sao_Paulo'

const fmtHora = new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, hour: '2-digit', minute: '2-digit' })
const fmtDataHora = new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, dateStyle: 'short', timeStyle: 'short' })
const fmtData = new Intl.DateTimeFormat('pt-BR', { timeZone: FUSO, dateStyle: 'long' })
const fmtNumero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
const fmtUmaCasa = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })

export const hora = (iso) => fmtHora.format(new Date(iso))
export const dataHora = (iso) => fmtDataHora.format(new Date(iso))
export const dataLonga = (isoOuData) => fmtData.format(typeof isoOuData === 'string' && isoOuData.length === 10
  ? new Date(isoOuData + 'T12:00:00-03:00') : new Date(isoOuData))
export const numero = (n) => fmtNumero.format(Number(n))
export const umaCasa = (n) => fmtUmaCasa.format(Number(n))

/** "21h" ou "21h30", como se fala. */
export function horaFalada(iso) {
  const [h, m] = hora(iso).split(':')
  return m === '00' ? `${Number(h)}h` : `${Number(h)}h${m}`
}

export function distancia(m) {
  if (m == null) return ''
  if (m < 1000) return `${m} m`
  return `${umaCasa(Math.round(m / 100) / 10)} km`
}

const UNIDADES = { UNIDADES: ['unidade', 'unidades'], KG: ['kg', 'kg'], BANDEJAS: ['bandeja', 'bandejas'] }

/** "20 pães franceses", "3 kg de frutas", "2 bandejas de iogurte". */
export function quantidade(lote) {
  const q = Number(lote.quantidade)
  const n = numero(q)
  if (lote.unidade === 'KG') return `${n} kg de ${lote.descricao}`
  if (lote.unidade === 'BANDEJAS') return `${n} ${q === 1 ? 'bandeja' : 'bandejas'} de ${lote.descricao}`
  return `${n} ${lote.descricao}`
}

export const unidadeRotulo = (u, q = 2) => UNIDADES[u]?.[Number(q) === 1 ? 0 : 1] ?? u

export const STATUS_LOTE = {
  DISPONIVEL: 'Disponível', RESERVADO: 'Reservado', RETIRADO: 'Retirado', EXPIRADO: 'Expirado', CANCELADO: 'Cancelado',
}
export const STATUS_RESERVA = { ATIVA: 'Ativa', EXPIRADA: 'Expirada', CONCLUIDA: 'Retirada', CANCELADA: 'Cancelada' }
