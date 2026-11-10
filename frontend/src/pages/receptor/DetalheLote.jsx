import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Icone from '../../components/Icone'
import { AvisoRefrigerado, Carregando, Erro, Status, Voltar } from '../../components/Comum'
import { api, urlFoto } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { distancia, hora, numero, quantidade } from '../../lib/formatos'
import { useDados } from '../../lib/useDados'
import ReservaPainel from './ReservaPainel'

export default function DetalheLote() {
  const { id } = useParams()
  const { usuario } = useAuth()
  const { dados: lote, erro, carregando, recarregar } = useDados(`/api/lotes/${id}`)
  const [reserva, setReserva] = useState(null)
  const [erroReserva, setErroReserva] = useState(null)
  const [reservando, setReservando] = useState(false)

  async function reservar() {
    setErroReserva(null)
    setReservando(true)
    try {
      setReserva(await api(`/api/lotes/${id}/reservas`, { metodo: 'POST' }))
    } catch (e) {
      setErroReserva(e)
      recarregar()
    } finally {
      setReservando(false)
    }
  }

  if (carregando) return <Carregando />
  if (erro) return <div className="pilha"><Voltar para="/receptor">Lotes próximos</Voltar><Erro erro={erro} /></div>

  // reserva recém-criada nesta tela, ou reserva ativa já existente deste receptor
  const minhaReserva = reserva ?? (lote.reserva && lote.reserva.receptorId === usuario.receptorId ? lote.reserva : null)

  return (
    <div className="pilha-grande">
      <Voltar para="/receptor">Lotes próximos</Voltar>
      <img className="foto-grande" src={urlFoto(lote.fotoUrl)} alt={`Foto do lote: ${quantidade(lote)}`} width="608" height="230" />
      <header>
        <div className="linha" style={{ marginBottom: '.4rem' }}>
          <span className="pill-suave">{lote.categoriaRotulo}</span>
          <Status valor={reserva ? 'RESERVADO' : lote.status} />
        </div>
        <h1>{quantidade(lote)}</h1>
        <p style={{ margin: 0 }}><strong>{lote.comercio.nome}</strong><br /><span className="texto-2">{lote.comercio.endereco}</span></p>
      </header>
      <dl className="grade-2">
        <div className="cartao dado"><dt>Distância</dt><dd>{distancia(lote.distanciaM)}</dd></div>
        <div className="cartao dado"><dt>Retirar até</dt><dd>{hora(lote.retirarAte)}</dd></div>
        <div className="cartao dado"><dt>Peso estimado</dt><dd>≈ {numero(lote.pesoKg)} kg</dd></div>
        <div className="cartao dado"><dt>Validade</dt><dd className="pequeno">{lote.confirmacaoValidade ? 'Confirmada pelo comércio' : '—'}</dd></div>
      </dl>
      {lote.refrigerado && <AvisoRefrigerado />}

      {reserva ? <ReservaPainel reserva={reserva} /> : minhaReserva ? (
        <div className="aviso aviso-ok">
          <Icone nome="check" />
          <span>Vocês já reservaram este lote. Veja o QR code em <Link to="/receptor/reservas">Minhas reservas</Link>.</span>
        </div>
      ) : lote.status === 'DISPONIVEL' ? (
        <div className="pilha">
          <Erro erro={erroReserva} />
          <button className="botao botao-primario botao-bloco botao-gigante" onClick={reservar} disabled={reservando}>
            {reservando ? 'Reservando…' : 'Reservar este lote'}
          </button>
          <p className="pequeno texto-2 centro" style={{ margin: 0 }}>Ao reservar, começa a contagem do prazo para a retirada.</p>
        </div>
      ) : (
        <div className="pilha">
          <Erro erro={erroReserva} />
          <p className="aviso aviso-alerta">Este lote não está mais disponível.</p>
        </div>
      )}
    </div>
  )
}
