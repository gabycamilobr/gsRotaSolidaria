import { useParams } from 'react-router-dom'
import Logo from '../../components/Logo'
import Icone from '../../components/Icone'
import { Carregando, Erro, Voltar } from '../../components/Comum'
import { dataHora, numero, unidadeRotulo } from '../../lib/formatos'
import { useDados } from '../../lib/useDados'

export default function Recibo() {
  const { id } = useParams()
  const { dados: r, erro, carregando } = useDados(`/api/recibos/${id}`)
  if (carregando) return <Carregando />
  if (erro) return <Erro erro={erro} />
  return (
    <div className="pilha-grande">
      <div className="linha-entre nao-imprimir">
        <Voltar para="/comerciante/recibos">Recibos</Voltar>
        <button className="botao botao-escuro" onClick={() => window.print()}><Icone nome="impressora" /> Imprimir / PDF</button>
      </div>
      <article className="folha pilha" aria-labelledby="titulo-recibo">
        <div className="linha-entre" style={{ alignItems: 'flex-start' }}>
          <Logo variante="claro" tamanho="pequeno" />
          <span className="pill-suave">{r.numero}</span>
        </div>
        <div>
          <p className="eyebrow">Recibo digital de doação</p>
          <h1 id="titulo-recibo">Doação de alimentos</h1>
        </div>
        <table>
          <tbody>
            <tr><th scope="row">Data e hora da retirada</th><td>{dataHora(r.emitidoEm)}</td></tr>
            <tr><th scope="row">Doador (comércio)</th><td>{r.comercioNome}<br /><span className="texto-2">{r.comercioEndereco}</span></td></tr>
            <tr><th scope="row">Item</th><td>{r.item} ({r.categoria})</td></tr>
            <tr><th scope="row">Quantidade</th><td>{numero(r.quantidade)} {unidadeRotulo(r.unidade, r.quantidade)}</td></tr>
            <tr><th scope="row">Peso estimado</th><td>{numero(r.pesoKg)} kg</td></tr>
            <tr><th scope="row">Receptor</th><td>{r.receptorNome}<br /><span className="texto-2">{r.receptorEndereco}</span></td></tr>
            <tr><th scope="row">Declaração do doador</th><td>“{r.confirmacaoValidade}”<br /><span className="texto-2">Registrada em {dataHora(r.confirmadoEm)}, na publicação do lote.</span></td></tr>
          </tbody>
        </table>
        <p className="texto-legal">{r.textoLegal}</p>
        <p className="pequeno texto-2" style={{ margin: 0 }}>Recibo gerado automaticamente pelo Rota Solidária na confirmação da retirada.</p>
      </article>
    </div>
  )
}
