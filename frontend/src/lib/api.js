// Cliente da API REST. Em desenvolvimento o Vite faz proxy de /api para o Spring Boot (localhost:8080);
// em produção, VITE_API_URL aponta para o Azure App Service.
export const API_URL = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')

const CHAVE_SESSAO = 'rota-solidaria:sessao'

export function lerSessao() {
  try {
    const s = JSON.parse(localStorage.getItem(CHAVE_SESSAO))
    if (s && new Date(s.expiraEm) > new Date()) return s
  } catch { /* sessão inválida */ }
  return null
}

export function salvarSessao(s) {
  if (s) localStorage.setItem(CHAVE_SESSAO, JSON.stringify(s))
  else localStorage.removeItem(CHAVE_SESSAO)
}

export class ErroApi extends Error {
  constructor(status, corpo) {
    super(corpo?.detail || mensagemPadrao(status))
    this.status = status
    this.campos = corpo?.campos || {}
  }
}

function mensagemPadrao(status) {
  if (status === 0) return 'Sem conexão com o servidor. Verifique a internet e tente de novo.'
  if (status === 401) return 'Sua sessão expirou. Entre de novo.'
  if (status === 403) return 'Seu perfil não tem acesso a esta ação.'
  if (status >= 500) return 'O servidor teve um problema. Tente de novo em instantes.'
  return 'Não foi possível concluir a ação.'
}

export async function api(caminho, { metodo = 'GET', corpo, formulario } = {}) {
  const sessao = lerSessao()
  const headers = {}
  if (sessao?.token) headers.Authorization = `Bearer ${sessao.token}`
  let body
  if (formulario) body = formulario
  else if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(corpo)
  }
  let resp
  try {
    resp = await fetch(API_URL + caminho, { method: metodo, headers, body })
  } catch {
    throw new ErroApi(0)
  }
  if (resp.status === 401 && sessao) {
    salvarSessao(null)
    window.dispatchEvent(new Event('rota:sessao-expirada'))
  }
  const texto = await resp.text()
  const dados = texto ? JSON.parse(texto) : null
  if (!resp.ok) throw new ErroApi(resp.status, dados)
  return dados
}

export const urlFoto = (caminho) => API_URL + caminho
