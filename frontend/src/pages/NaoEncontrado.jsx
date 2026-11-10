import { Link } from 'react-router-dom'

export default function NaoEncontrado() {
  return (
    <div className="pilha centro" style={{ padding: '3rem 16px' }}>
      <p className="eyebrow">Erro 404</p>
      <h1>Essa rota não existe</h1>
      <Link to="/" className="botao botao-primario">Voltar ao início</Link>
    </div>
  )
}
