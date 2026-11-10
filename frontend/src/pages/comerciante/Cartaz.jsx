import { LogoIcone } from '../../components/Logo'
import Icone from '../../components/Icone'
import { Carregando, Erro, Voltar } from '../../components/Comum'
import { dataLonga, distancia, horaFalada, numero, unidadeRotulo } from '../../lib/formatos'
import { useDados } from '../../lib/useDados'

function textoItem(i) {
  const q = numero(i.quantidade)
  if (i.unidade === 'UNIDADES') return `${q} ${i.descricao}`
  return `${q} ${unidadeRotulo(i.unidade, i.quantidade)} de ${i.descricao}`
}

/** Cartaz do dia (A4) para quem não tem telefone: itens de hoje, horário e ponto de distribuição. */
export default function Cartaz() {
  const { dados: c, erro, carregando } = useDados('/api/cartaz')
  if (carregando) return <Carregando />
  if (erro) return <Erro erro={erro} />
  return (
    <div className="pilha-grande">
      <div className="linha-entre nao-imprimir">
        <Voltar para="/comerciante">Início</Voltar>
        <button className="botao botao-escuro" onClick={() => window.print()} disabled={!c.itens.length}>
          <Icone nome="impressora" /> Imprimir cartaz
        </button>
      </div>
      {!c.itens.length && <p className="aviso aviso-alerta nao-imprimir">Publique um lote hoje para gerar o cartaz.</p>}
      <article className="cartaz" aria-label="Cartaz do dia">
        <header className="cartaz-topo">
          <div className="linha" style={{ gap: 12 }}>
            <LogoIcone variante="escuro" tamanho={64} />
            <span style={{ fontFamily: 'var(--fonte-titulo)', fontWeight: 800, fontSize: '1.7rem', lineHeight: 1 }}>
              Rota <span style={{ color: 'var(--ambar)' }}>Solidária</span>
            </span>
          </div>
          <h1>Hoje tem doação de alimentos</h1>
          <p style={{ color: 'var(--texto-claro-2)', margin: 0, fontWeight: 700 }}>
            {c.comercioNome} · {dataLonga(c.data)}
          </p>
        </header>
        <div className="cartaz-corpo">
          <p className="eyebrow">Disponível hoje</p>
          <ul className="cartaz-itens">
            {c.itens.map((i, n) => <li key={n}>{textoItem(i)}</li>)}
          </ul>
          {c.pontoDistribuicao && (
            <div className="cartaz-ponto">
              <p style={{ margin: 0, fontWeight: 800 }}>Retire no ponto de distribuição{c.horarioAte ? ` até as ${horaFalada(c.horarioAte)}` : ''}:</p>
              <strong>{c.pontoDistribuicao.nome}</strong>
              <span style={{ fontWeight: 700 }}>{c.pontoDistribuicao.endereco} · a {distancia(c.pontoDistribuicao.distanciaM)} daqui</span>
            </div>
          )}
          <p style={{ marginTop: '1rem', fontWeight: 700 }}>Gratuito e sem cadastro. Procure a organização no endereço acima.</p>
          <p className="pequeno texto-2" style={{ margin: 0 }}>Alimentos doados dentro da validade, nos termos da Lei nº 14.016/2020.</p>
        </div>
      </article>
    </div>
  )
}
