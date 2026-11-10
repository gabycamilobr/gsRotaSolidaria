import { Cabecalho, Carregando, Erro, Status } from '../../components/Comum'
import { dataHora, quantidade } from '../../lib/formatos'
import { useAoReceberAviso } from '../../lib/notificacoes'
import { useDados } from '../../lib/useDados'
import ReservaPainel from './ReservaPainel'

export default function MinhasReservas() {
  const { dados, erro, carregando, recarregar } = useDados('/api/reservas/minhas')
  useAoReceberAviso(recarregar)
  const ativas = dados?.filter((r) => r.status === 'ATIVA') ?? []
  const historico = dados?.filter((r) => r.status !== 'ATIVA') ?? []
  return (
    <div className="pilha-grande">
      <Cabecalho eyebrow="Minhas reservas" titulo="Para buscar agora" />
      <Erro erro={erro} tentarDeNovo={recarregar} />
      {carregando ? <Carregando /> : (
        <>
          {ativas.length === 0 && <p className="cartao vazio">Nenhuma reserva ativa.</p>}
          {ativas.map((r) => (
            <article key={r.id} className="pilha" aria-labelledby={`res-${r.id}`}>
              <div>
                <h2 id={`res-${r.id}`} style={{ marginBottom: '.2rem' }}>{quantidade(r.lote)}</h2>
                <p className="texto-2" style={{ margin: 0 }}>{r.lote.comercio.nome} · {r.lote.comercio.endereco}</p>
              </div>
              <ReservaPainel reserva={r} aoExpirar={() => setTimeout(recarregar, 6000)} />
            </article>
          ))}
          {historico.length > 0 && (
            <section className="pilha" aria-labelledby="historico">
              <h2 id="historico">Histórico</h2>
              <ul className="pilha" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {historico.map((r) => (
                  <li key={r.id} className="cartao linha-entre">
                    <span>
                      <strong style={{ display: 'block' }}>{quantidade(r.lote)}</strong>
                      <span className="pequeno texto-2">{r.lote.comercio.nome} · {dataHora(r.criadaEm)}</span>
                    </span>
                    <Status valor={r.status} reserva />
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
      )}
    </div>
  )
}
