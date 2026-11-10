import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import Logo, { RotaDecorativa } from '../components/Logo'
import { Erro } from '../components/Comum'
import { ROTA_INICIAL, useAuth } from '../lib/auth'

// Usuários de demonstração (senha demo123), criados pelo seed do Flyway.
const DEMO = [
  { email: 'padaria@demo.com', nome: 'Padaria Bom Dia', papel: 'Comerciante' },
  { email: 'quitanda@demo.com', nome: 'Quitanda da Vila', papel: 'Comerciante' },
  { email: 'maosdadas@demo.com', nome: 'Associação Mãos Dadas', papel: 'Receptor' },
  { email: 'esperanca@demo.com', nome: 'Comunidade Esperança', papel: 'Receptor' },
  { email: 'vilanova@demo.com', nome: 'Casa de Apoio Vila Nova', papel: 'Receptor (fora do raio)' },
  { email: 'ana@demo.com', nome: 'Ana', papel: 'Voluntária · bicicleta' },
]

export default function Login() {
  const { usuario, entrar } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)

  if (usuario) return <Navigate to={ROTA_INICIAL[usuario.perfil]} replace />

  async function fazerLogin(e, s) {
    setErro(null)
    setEnviando(true)
    try {
      const r = await entrar(e, s)
      navigate(ROTA_INICIAL[r.usuario.perfil], { replace: true })
    } catch (err) {
      setErro(err)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="abertura escuro">
      <RotaDecorativa />
      <main className="abertura-conteudo">
        <div className="topo-capa">
          <span className="pill">CitizenTech</span>
          <span>GLOBAL SOLUTION 2026.2 · FIAP</span>
        </div>
        <h1 className="sr-only">Rota Solidária</h1>
        <Logo variante="escuro" tamanho="medio" />
        <p className="slogan">Da sobra ao prato, no mesmo bairro, em menos de uma hora.</p>

        <section aria-labelledby="titulo-demo" className="pilha" style={{ marginBottom: '1.75rem' }}>
          <h2 id="titulo-demo" className="eyebrow" style={{ fontFamily: 'var(--fonte-corpo)', fontSize: 12.5, letterSpacing: '.12em' }}>
            Entrar como
          </h2>
          <div className="demo-botoes">
            {DEMO.map((d) => (
              <button key={d.email} type="button" className="botao botao-contorno" disabled={enviando}
                onClick={() => fazerLogin(d.email, 'demo123')}>
                <span>{d.nome}<br /><small>{d.papel}</small></span>
                <span aria-hidden="true">→</span>
              </button>
            ))}
          </div>
        </section>

        <form className="cartao pilha" onSubmit={(ev) => { ev.preventDefault(); fazerLogin(email, senha) }}>
          <h2 style={{ fontSize: '1.1rem' }}>Entrar com e-mail</h2>
          <div className="campo">
            <label htmlFor="email">E-mail</label>
            <input id="email" type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
          <div className="campo">
            <label htmlFor="senha">Senha</label>
            <input id="senha" type="password" autoComplete="current-password" required value={senha} onChange={(e) => setSenha(e.target.value)} />
          </div>
          <Erro erro={erro} />
          <button className="botao botao-primario botao-bloco" disabled={enviando}>{enviando ? 'Entrando…' : 'Entrar'}</button>
        </form>

        <p style={{ marginTop: '1.25rem' }}>
          <Link to="/como-funciona" style={{ color: 'var(--ambar)' }}>Como funciona, a Lei 14.016/2020 e seus dados</Link>
        </p>
      </main>
    </div>
  )
}
