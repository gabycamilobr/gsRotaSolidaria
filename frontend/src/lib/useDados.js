import { useCallback, useEffect, useState } from 'react'
import { api } from './api'

/** Busca dados da API com estados de carregamento e erro. */
export function useDados(caminho) {
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState(null)
  const [carregando, setCarregando] = useState(true)

  const recarregar = useCallback(async () => {
    try {
      const r = await api(caminho)
      setDados(r)
      setErro(null)
    } catch (e) {
      setErro(e)
    } finally {
      setCarregando(false)
    }
  }, [caminho])

  useEffect(() => { setCarregando(true); recarregar() }, [recarregar])
  return { dados, erro, carregando, recarregar, setDados }
}
