import { useState } from 'react'
import { Contagem } from '../../components/Comum'
import QrCodigo from '../../components/QrCodigo'
import { IconeWhatsApp } from '../../components/Icone'
import { hora } from '../../lib/formatos'
import { linkWhatsApp, mensagemWhatsApp } from '../../lib/whatsapp'

/** Reserva ativa: contagem regressiva, QR code + código de 4 dígitos e repasse pelo WhatsApp. */
export default function ReservaPainel({ reserva, aoExpirar }) {
  const [expirou, setExpirou] = useState(new Date(reserva.expiraEm) <= new Date())
  const mensagem = mensagemWhatsApp(reserva)
  return (
    <div className="pilha">
      <section className="cartao-destaque pilha centro" aria-label="Reserva">
        <p className="eyebrow">{expirou ? 'Reserva expirada' : 'Reservado para vocês · expira em'}</p>
        {expirou ? <p>O prazo terminou e o lote foi liberado para a próxima organização.</p> : (
          <>
            <Contagem ate={reserva.expiraEm} aoTerminar={() => { setExpirou(true); aoExpirar?.() }} />
            <p style={{ margin: 0 }}>Busque até {hora(reserva.expiraEm)} e mostre o QR code no comércio.</p>
          </>
        )}
      </section>
      {!expirou && (
        <>
          <section className="cartao pilha centro" aria-label="Código de retirada">
            <p className="eyebrow">Código de retirada</p>
            <QrCodigo conteudo={reserva.qrConteudo} codigo={reserva.codigo} />
            <p className="codigo-retirada" style={{ margin: 0 }}>{reserva.codigo}</p>
            <p className="pequeno texto-2" style={{ margin: 0 }}>O comerciante lê o QR code ou digita o código.</p>
          </section>
          <section className="cartao pilha" aria-labelledby={`zap-${reserva.id}`}>
            <h2 id={`zap-${reserva.id}`} style={{ fontSize: '1.1rem' }}>Avise as famílias</h2>
            <p className="pequeno texto-2" style={{ margin: 0 }}>Mensagem pronta para o grupo de WhatsApp que vocês já usam. As famílias respondem a vocês, não à plataforma.</p>
            <blockquote className="aviso aviso-ok" style={{ margin: 0 }}>{mensagem}</blockquote>
            <a className="botao botao-whatsapp botao-bloco" href={linkWhatsApp(mensagem)} target="_blank" rel="noopener noreferrer">
              <IconeWhatsApp /> Compartilhar no WhatsApp
            </a>
          </section>
        </>
      )}
    </div>
  )
}
