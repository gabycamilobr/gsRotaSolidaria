import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Icone from './Icone'
import { STATUS_LOTE, STATUS_RESERVA } from '../lib/formatos'

export function Status({ valor, reserva = false }) {
  const rotulo = (reserva ? STATUS_RESERVA : STATUS_LOTE)[valor] ?? valor
  return <span className={`status status-${valor}`}>{rotulo}</span>
}

export function Carregando({ texto = 'Carregando…' }) {
  return <p className="carregando" role="status">{texto}</p>
}

export function Erro({ erro, tentarDeNovo }) {
  if (!erro) return null
  return (
    <div className="aviso aviso-erro" role="alert">
      <Icone nome="alerta" />
      <div>
        <p style={{ margin: 0 }}>{erro.message}</p>
        {tentarDeNovo && <button className="botao botao-texto" onClick={tentarDeNovo}>Tentar de novo</button>}
      </div>
    </div>
  )
}

export function AvisoRefrigerado() {
  return (
    <div className="aviso aviso-frio">
      <Icone nome="floco" />
      <span><strong>Refrigerado:</strong> transporte em bolsa térmica ou caixa com gelo. Janela de retirada de no máximo 2 horas.</span>
    </div>
  )
}

export function Voltar({ para, children = 'Voltar' }) {
  return <Link to={para} className="voltar"><Icone nome="voltar" tamanho={18} /> {children}</Link>
}

/** Cabeçalho de página: eyebrow âmbar em caixa alta + título pesado, como nos slides. */
export function Cabecalho({ eyebrow, titulo, children }) {
  return (
    <header className="cabecalho-pagina">
      {eyebrow && <p className="eyebrow">{eyebrow}</p>}
      <h1>{titulo}</h1>
      {children}
    </header>
  )
}

/** Contagem regressiva até "ate" (ISO). Mostra mm:ss. */
export function Contagem({ ate, aoTerminar }) {
  const calcular = () => Math.max(0, Math.floor((new Date(ate) - Date.now()) / 1000))
  const [seg, setSeg] = useState(calcular)
  useEffect(() => {
    setSeg(calcular())
    const id = setInterval(() => {
      const s = calcular()
      setSeg(s)
      if (s === 0) { clearInterval(id); aoTerminar?.() }
    }, 1000)
    return () => clearInterval(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ate])
  const mm = String(Math.floor(seg / 60)).padStart(2, '0')
  const ss = String(seg % 60).padStart(2, '0')
  return (
    <span className="contagem" role="timer" aria-label={`Faltam ${Math.floor(seg / 60)} minutos e ${seg % 60} segundos`}>
      {mm}:{ss}
    </span>
  )
}
