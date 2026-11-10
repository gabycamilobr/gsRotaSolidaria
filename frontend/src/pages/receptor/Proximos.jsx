import { Link } from 'react-router-dom'
import Icone from '../../components/Icone'
import { Cabecalho, Carregando, Erro } from '../../components/Comum'
import { urlFoto } from '../../lib/api'
import { distancia, hora, numero, quantidade } from '../../lib/formatos'
import { useAoReceberAviso, useAtualizarACada } from '../../lib/notificacoes'
import { useDados } from '../../lib/useDados'

export function CartaoLote({ lote, para }) {
  return (
    <Link to={para} className="cartao lote" aria-label={`${quantidade(lote)}, ${lote.comercio.nome}, a ${distancia(lote.distanciaM)}, retirar até ${hora(lote.retirarAte)}`}>
      <img className="lote-foto" src={urlFoto(lote.fotoUrl)} alt="" width="96" height="96" loading="lazy" />
      <div style={{ minWidth: 0 }}>
        <span className="pill-suave">{lote.categoriaRotulo}</span>
        <h3 style={{ marginTop: '.35rem' }}>{quantidade(lote)}</h3>
        <p className="pequeno" style={{ margin: '0 0 .25rem', fontWeight: 700 }}>{lote.comercio.nome}</p>
        <div className="meta">
          <span><Icone nome="pino" tamanho={16} /> {distancia(lote.distanciaM)}</span>
          <span><Icone nome="relogio" tamanho={16} /> até {hora(lote.retirarAte)}</span>
          <span><Icone nome="balanca" tamanho={16} /> ≈ {numero(lote.pesoKg)} kg</span>
          {lote.refrigerado && <span style={{ color: 'var(--frio-texto)', fontWeight: 800 }}><Icone nome="floco" tamanho={16} /> Refrigerado</span>}
        </div>
      </div>
    </Link>
  )
}

export default function Proximos() {
  const { dados, erro, carregando, recarregar } = useDados('/api/lotes/proximos')
  useAoReceberAviso(recarregar)
  useAtualizarACada(recarregar)
  return (
    <div className="pilha-grande">
      <Cabecalho eyebrow="Lotes próximos" titulo="Comida perto de vocês">
        <p className="texto-2">Lotes a até 2 km que vocês aceitam, do mais perto ao mais longe. Quem reservar primeiro leva.</p>
      </Cabecalho>
      <Erro erro={erro} tentarDeNovo={recarregar} />
      {carregando ? <Carregando /> : dados?.length === 0 ? (
        <div className="cartao vazio">
          <p><strong>Nenhum lote disponível agora.</strong></p>
          <p style={{ margin: 0 }}>Avisamos aqui assim que um comércio próximo publicar.</p>
        </div>
      ) : (
        <ul className="pilha" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {dados?.map((l) => <li key={l.id}><CartaoLote lote={l} para={`/receptor/lotes/${l.id}`} /></li>)}
        </ul>
      )}
    </div>
  )
}
