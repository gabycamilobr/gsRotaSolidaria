import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { api } from './api'
import { useAuth } from './auth'
import Icone from '../components/Icone'

// Avisos dentro do app: polling leve a cada 15 s (só com a aba visível). É o caminho garantido
// mesmo sem Firebase; quando o push está ativo, ele é um canal a mais.
const INTERVALO_MS = 15_000
const Ctx = createContext(null)

export function NotificacoesProvider({ children }) {
  const { sessao } = useAuth()
  const [itens, setItens] = useState([])
  const [naoLidas, setNaoLidas] = useState(0)
  const [toasts, setToasts] = useState([])
  const ultimoId = useRef(0)
  const primeiraCarga = useRef(true)

  const buscar = useCallback(async () => {
    if (!sessao) return
    try {
      const r = await api(`/api/notificacoes?apos=${ultimoId.current}`)
      if (r.itens.length) {
        ultimoId.current = Math.max(ultimoId.current, ...r.itens.map((n) => n.id))
        setItens((atual) => [...r.itens, ...atual].slice(0, 30))
        if (!primeiraCarga.current) {
          const novos = r.itens.slice(0, 3).map((n) => ({ ...n, chave: n.id }))
          setToasts((t) => [...novos, ...t].slice(0, 3))
          novos.forEach((n) => setTimeout(() => setToasts((t) => t.filter((x) => x.chave !== n.chave)), 8000))
          // avisa as telas abertas para recarregarem seus dados
          window.dispatchEvent(new CustomEvent('rota:atualizar', { detail: r.itens }))
        }
      }
      setNaoLidas(r.naoLidas)
      primeiraCarga.current = false
    } catch { /* rede instável: tenta no próximo ciclo */ }
  }, [sessao])

  useEffect(() => {
    ultimoId.current = 0
    primeiraCarga.current = true
    setItens([])
    setNaoLidas(0)
    if (!sessao) return undefined
    buscar()
    const id = setInterval(() => { if (document.visibilityState === 'visible') buscar() }, INTERVALO_MS)
    const aoVoltar = () => { if (document.visibilityState === 'visible') buscar() }
    document.addEventListener('visibilitychange', aoVoltar)
    return () => { clearInterval(id); document.removeEventListener('visibilitychange', aoVoltar) }
  }, [sessao, buscar])

  const marcarLidas = useCallback(async () => {
    setNaoLidas(0)
    try { await api('/api/notificacoes/lidas', { metodo: 'POST' }) } catch { /* ignora */ }
    setItens((l) => l.map((n) => ({ ...n, lida: true })))
  }, [])

  const fecharToast = (chave) => setToasts((t) => t.filter((x) => x.chave !== chave))

  return (
    <Ctx.Provider value={{ itens, naoLidas, marcarLidas, buscar }}>
      {children}
      <div className="toast-area" aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div className="toast" key={t.chave} role="status">
            <Icone nome="sino" />
            <div style={{ flex: 1 }}>
              <strong>{t.titulo}</strong>
              <p>{t.mensagem}</p>
            </div>
            <button className="botao-icone" onClick={() => fecharToast(t.chave)} aria-label="Fechar aviso">
              <Icone nome="x" tamanho={18} />
            </button>
          </div>
        ))}
      </div>
    </Ctx.Provider>
  )
}

export const useNotificacoes = () => useContext(Ctx)

/** Recarrega a tela quando chega um aviso novo (ex.: lote reservado, lote disponível). */
export function useAoReceberAviso(callback) {
  const ref = useRef(callback)
  ref.current = callback
  useEffect(() => {
    const f = () => ref.current()
    window.addEventListener('rota:atualizar', f)
    return () => window.removeEventListener('rota:atualizar', f)
  }, [])
}
