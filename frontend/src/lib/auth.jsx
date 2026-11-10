import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, lerSessao, salvarSessao } from './api'

const AuthContext = createContext(null)

export const ROTA_INICIAL = { COMERCIANTE: '/comerciante', RECEPTOR: '/receptor', VOLUNTARIO: '/voluntario' }

export function AuthProvider({ children }) {
  const [sessao, setSessao] = useState(lerSessao)

  useEffect(() => {
    const expirou = () => setSessao(null)
    window.addEventListener('rota:sessao-expirada', expirou)
    return () => window.removeEventListener('rota:sessao-expirada', expirou)
  }, [])

  const entrar = useCallback(async (email, senha) => {
    const r = await api('/api/auth/login', { metodo: 'POST', corpo: { email, senha } })
    salvarSessao(r)
    setSessao(r)
    return r
  }, [])

  const sair = useCallback(() => {
    salvarSessao(null)
    setSessao(null)
  }, [])

  return (
    <AuthContext.Provider value={{ sessao, usuario: sessao?.usuario, entrar, sair }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
