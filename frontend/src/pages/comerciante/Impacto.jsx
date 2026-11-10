import { useState } from 'react'
import { Cabecalho, Carregando, Erro } from '../../components/Comum'
import { numero, umaCasa } from '../../lib/formatos'
import { useAoReceberAviso } from '../../lib/notificacoes'
import { useDados } from '../../lib/useDados'

function Painel({ caminho }) {
  const { dados: d, erro, carregando, recarregar } = useDados(caminho)
  useAoReceberAviso(recarregar)
  if (carregando) return <Carregando />
  if (erro) return <Erro erro={erro} tentarDeNovo={recarregar} />
  return (
    <div className="pilha">
      <section className="cartao-destaque pilha" aria-label="Impacto">
        <div>
          <p className="eyebrow">Quilos salvos</p>
          <p className="numero-grande" style={{ margin: 0 }}>{umaCasa(d.kgSalvos)} kg</p>
        </div>
        <div className="grade-2">
          <div>
            <p className="eyebrow">Refeições</p>
            <p className="numero-grande" style={{ margin: 0, fontSize: '1.9rem' }}>≈ {numero(d.refeicoes)}</p>
          </div>
          <div>
            <p className="eyebrow">CO₂e evitado</p>
            <p className="numero-grande" style={{ margin: 0, fontSize: '1.9rem' }}>≈ {umaCasa(d.co2eEvitadoKg)} kg</p>
          </div>
        </div>
      </section>
      <dl className="grade-2">
        <div className="cartao dado"><dt>Lotes publicados</dt><dd>{d.lotesPublicados}</dd></div>
        <div className="cartao dado"><dt>Retirados</dt><dd>{d.percentualRetirado}% ({d.lotesRetirados})</dd></div>
        <div className="cartao dado" style={{ gridColumn: '1 / -1' }}>
          <dt>Tempo médio da publicação à retirada</dt>
          <dd>{d.tempoMedioRetiradaMin == null ? '—' : `${d.tempoMedioRetiradaMin} min`}</dd>
        </div>
      </dl>
      <footer className="rodape-fontes">
        <p><strong>Como calculamos</strong></p>
        <p>Refeições = kg ÷ 0,54 — {d.fontes[0]}</p>
        <p>CO₂e evitado = kg × 0,627 — {d.fontes[1]}</p>
      </footer>
    </div>
  )
}

export default function Impacto() {
  const [escopo, setEscopo] = useState('meu')
  return (
    <div className="pilha-grande">
      <Cabecalho eyebrow="Painel de impacto" titulo="O que a sobra virou" />
      <div className="segmentado" role="group" aria-label="Escolha o painel">
        <button aria-pressed={escopo === 'meu'} onClick={() => setEscopo('meu')}>Meu comércio</button>
        <button aria-pressed={escopo === 'geral'} onClick={() => setEscopo('geral')}>Geral</button>
      </div>
      <Painel key={escopo} caminho={escopo === 'meu' ? '/api/impacto/meu' : '/api/impacto/geral'} />
    </div>
  )
}
