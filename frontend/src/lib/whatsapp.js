import { categoria } from './categorias'
import { horaFalada, quantidade } from './formatos'

// Repasse às famílias sem custo e sem API paga: link wa.me com a mensagem pronta.
// A família responde à ONG, nunca à plataforma.
export function mensagemWhatsApp(reserva) {
  const lote = reserva.lote
  const emoji = categoria(lote.categoria)?.emoji ?? '🍽️'
  return `${emoji} Rota Solidária: hoje temos ${quantidade(lote)} disponíveis. ` +
    `Retirada na ${reserva.receptorNome} (${reserva.receptorEndereco}) até as ${horaFalada(lote.retirarAte)}. ` +
    'Responda aqui para garantir o seu.'
}

export const linkWhatsApp = (texto) => `https://wa.me/?text=${encodeURIComponent(texto)}`
