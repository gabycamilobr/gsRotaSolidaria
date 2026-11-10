import { Link } from 'react-router-dom'
import { Cabecalho, Carregando, Erro } from '../../components/Comum'
import { dataHora, numero } from '../../lib/formatos'
import { useDados } from '../../lib/useDados'

export default function Recibos() {
  const { dados, erro, carregando, recarregar } = useDados('/api/recibos')
  return (
    <div className="pilha-grande">
      <Cabecalho eyebrow="Recibos" titulo="Doações realizadas">
        <p className="texto-2">Um recibo digital é gerado a cada retirada confirmada.</p>
      </Cabecalho>
      <Erro erro={erro} tentarDeNovo={recarregar} />
      {carregando ? <Carregando /> : dados?.length === 0 ? <p className="cartao vazio">Nenhum recibo ainda.</p> : (
        <ul className="pilha" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {dados?.map((r) => (
            <li key={r.id}>
              <Link to={`/comerciante/recibos/${r.id}`} className="cartao linha-entre" style={{ textDecoration: 'none', color: 'inherit' }}>
                <span>
                  <strong style={{ display: 'block' }}>{numero(r.quantidade)} × {r.item}</strong>
                  <span className="pequeno texto-2">{r.receptorNome} · {dataHora(r.emitidoEm)}</span>
                </span>
                <span className="pill-suave">{r.numero}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
