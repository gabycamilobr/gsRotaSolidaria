import { useState } from 'react'
import { Link } from 'react-router-dom'
import Icone from '../../components/Icone'
import { AvisoRefrigerado, Carregando, Contagem, Erro, Status } from '../../components/Comum'
import { api, urlFoto } from '../../lib/api'
import { useAuth } from '../../lib/auth'
import { distancia, hora, numero, quantidade } from '../../lib/formatos'
import { useAoReceberAviso } from '../../lib/notificacoes'
import { useDados } from '../../lib/useDados'

function LoteDoDia({ lote, aoCancelar }) {
  const [cancelando, setCancelando] = useState(false)
  const [erro, setErro] = useState(null)
  async function cancelar() {
    if (!window.confirm('Cancelar este lote? As organizações deixarão de vê-lo.')) return
    setCancelando(true)
    try { await api(`/api/lotes/${lote.id}/cancelar`, { metodo: 'POST' }); aoCancelar() } catch (e) { setErro(e) } finally { setCancelando(false) }
  }
  return (
    <article className="cartao pilha">
      <div className="lote">
        <img className="lote-foto" src={urlFoto(lote.fotoUrl)} alt={`Foto do lote: ${quantidade(lote)}`} width="96" height="96" loading="lazy" />
        <div style={{ minWidth: 0 }}>
          <Status valor={lote.status} />
          <h3 style={{ marginTop: '.35rem' }}>{quantidade(lote)}</h3>
          <div className="meta">
            <span>{lote.categoriaRotulo}</span>
            <span>≈ {numero(lote.pesoKg)} kg</span>
            <span><Icone nome="relogio" tamanho={16} /> até {hora(lote.retirarAte)}</span>
          </div>
        </div>
      </div>
      {lote.refrigerado && lote.status === 'DISPONIVEL' && <AvisoRefrigerado />}
      {lote.reserva && (
        <div className="aviso aviso-alerta">
          <Icone nome="check" />
          <div>
            <strong>{lote.reserva.receptorNome}</strong> reservou. A reserva expira em{' '}
            <Contagem ate={lote.reserva.expiraEm} />
            <p style={{ margin: 0 }} className="pequeno">Na retirada, leia o QR code ou digite o código em <Link to="/comerciante/retirada">Retirada</Link>.</p>
          </div>
        </div>
      )}
      {lote.status === 'DISPONIVEL' && (
        <p className="pequeno texto-2" style={{ margin: 0 }}>
          {lote.candidatos?.length
            ? <>Avisamos: {lote.candidatos.map((c) => `${c.nome} (${distancia(c.distanciaM)})`).join(', ')}.</>
            : 'Nenhuma organização que aceita esta categoria num raio de 2 km.'}
        </p>
      )}
      <Erro erro={erro} />
      {(lote.status === 'DISPONIVEL' || lote.status === 'RESERVADO') && (
        <button className="botao botao-texto" onClick={cancelar} disabled={cancelando} style={{ alignSelf: 'flex-start', paddingLeft: 0 }}>
          Cancelar lote
        </button>
      )}
    </article>
  )
}

export default function Inicio() {
  const { usuario } = useAuth()
  const { dados: lotes, erro, carregando, recarregar } = useDados('/api/lotes/meus')
  useAoReceberAviso(recarregar)

  return (
    <div className="pilha-grande">
      <header>
        <p className="eyebrow">{usuario.nome}</p>
        <h1>Sobrou alimento hoje?</h1>
        <p className="texto-2">Publique em 30 segundos. Avisamos as organizações a até 2 km.</p>
      </header>

      <Link to="/comerciante/publicar" className="botao botao-primario botao-gigante botao-bloco">
        <Icone nome="mais" tamanho={30} /> Publicar sobra
      </Link>

      <div className="grade-2">
        <Link to="/comerciante/retirada" className="botao botao-escuro"><Icone nome="qr" /> Confirmar retirada</Link>
        <Link to="/comerciante/cartaz" className="botao botao-contorno"><Icone nome="cartaz" /> Cartaz do dia</Link>
      </div>

      <section aria-labelledby="lotes-hoje" className="pilha">
        <h2 id="lotes-hoje">Lotes de hoje</h2>
        <Erro erro={erro} tentarDeNovo={recarregar} />
        {carregando ? <Carregando /> : lotes?.length === 0 ? (
          <p className="vazio cartao">Nenhum lote publicado hoje.</p>
        ) : lotes?.map((l) => <LoteDoDia key={l.id} lote={l} aoCancelar={recarregar} />)}
      </section>
    </div>
  )
}
