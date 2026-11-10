import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import Icone from './Icone'
import { LogoIcone } from './Logo'
import { ROTA_INICIAL, useAuth } from '../lib/auth'
import { useNotificacoes } from '../lib/notificacoes'
import { dataHora } from '../lib/formatos'
import { ativarPush, pushConfigurado } from '../lib/push'

const MENUS = {
  COMERCIANTE: [
    { para: '/comerciante', icone: 'casa', rotulo: 'Início', fim: true },
    { para: '/comerciante/retirada', icone: 'qr', rotulo: 'Retirada' },
    { para: '/comerciante/recibos', icone: 'recibo', rotulo: 'Recibos' },
    { para: '/comerciante/impacto', icone: 'grafico', rotulo: 'Impacto' },
  ],
  RECEPTOR: [
    { para: '/receptor', icone: 'lista', rotulo: 'Lotes próximos', fim: true },
    { para: '/receptor/reservas', icone: 'qr', rotulo: 'Minhas reservas' },
    { para: '/como-funciona', icone: 'info', rotulo: 'Como funciona' },
  ],
  VOLUNTARIO: [
    { para: '/voluntario', icone: 'rota', rotulo: 'Minha rota', fim: true },
    { para: '/como-funciona', icone: 'info', rotulo: 'Como funciona' },
  ],
}

function PainelAvisos({ fechar }) {
  const { itens } = useNotificacoes()
  const [push, setPush] = useState(null)
  const ref = useRef(null)
  useEffect(() => {
    ref.current?.focus()
    const esc = (e) => e.key === 'Escape' && fechar()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [fechar])

  return (
    <div className="cartao painel-avisos" role="dialog" aria-label="Avisos" tabIndex={-1} ref={ref}>
      <div className="linha-entre" style={{ marginBottom: '.5rem' }}>
        <h2 style={{ margin: 0 }}>Avisos</h2>
        <button className="botao-icone" onClick={fechar} aria-label="Fechar avisos"><Icone nome="x" /></button>
      </div>
      {itens.length === 0 ? <p className="texto-2">Nenhum aviso por enquanto.</p> : (
        <ul>
          {itens.map((n) => (
            <li key={n.id} className={n.lida ? '' : 'nova'}>
              <strong>{n.titulo}</strong>
              <p style={{ margin: 0 }}>{n.mensagem}</p>
              <small className="texto-2">{dataHora(n.criadaEm)}</small>
            </li>
          ))}
        </ul>
      )}
      <div className="rodape-fontes" style={{ marginTop: '.75rem' }}>
        {pushConfigurado() ? (
          push?.ok ? <p>Notificações no celular ativadas.</p> : (
            <>
              <button className="botao botao-escuro botao-bloco" onClick={async () => setPush(await ativarPush())}>
                <Icone nome="sino" /> Ativar notificações no celular
              </button>
              {push?.motivo && <p role="status">{push.motivo}</p>}
            </>
          )
        ) : <p>Os avisos aparecem aqui dentro do app e são atualizados a cada 15 segundos.</p>}
      </div>
    </div>
  )
}

export default function Layout() {
  const { usuario, sair } = useAuth()
  const { naoLidas, marcarLidas } = useNotificacoes()
  const [avisosAbertos, setAvisosAbertos] = useState(false)
  const navigate = useNavigate()
  const menu = MENUS[usuario.perfil]

  const alternarAvisos = () => {
    if (!avisosAbertos && naoLidas) marcarLidas()
    setAvisosAbertos((a) => !a)
  }

  return (
    <div className="app">
      <a href="#conteudo" className="pular-para-conteudo">Pular para o conteúdo</a>
      <header className="topo">
        <Link to={ROTA_INICIAL[usuario.perfil]} className="marca" aria-label="Rota Solidária – início">
          <LogoIcone variante="escuro" tamanho={36} />
          <span>
            <span style={{ fontFamily: 'var(--fonte-titulo)', fontWeight: 800, fontSize: '1.05rem', letterSpacing: '-.02em' }}>
              Rota <span style={{ color: 'var(--ambar)' }}>Solidária</span>
            </span>
            <span className="quem">{usuario.nome}</span>
          </span>
        </Link>
        <button className="botao-icone" onClick={alternarAvisos} aria-expanded={avisosAbertos}
          aria-label={naoLidas ? `Avisos: ${naoLidas} novos` : 'Avisos'}>
          <Icone nome="sino" />
          {naoLidas > 0 && <span className="contador" aria-hidden="true">{naoLidas > 9 ? '9+' : naoLidas}</span>}
        </button>
        <button className="botao-icone" onClick={() => { sair(); navigate('/entrar') }} aria-label="Sair">
          <Icone nome="sair" />
        </button>
      </header>
      {avisosAbertos && <PainelAvisos fechar={() => setAvisosAbertos(false)} />}

      <main id="conteudo" className="conteudo" tabIndex={-1}>
        <Outlet />
      </main>

      <nav className="navegacao" aria-label="Navegação principal">
        <ul>
          {menu.map((m) => (
            <li key={m.para}>
              <NavLink to={m.para} end={m.fim} className={({ isActive }) => (isActive ? 'ativo' : '')}>
                <Icone nome={m.icone} />
                {m.rotulo}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
