import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Icone from '../../components/Icone'
import { Cabecalho, Erro } from '../../components/Comum'
import { api } from '../../lib/api'
import { numero } from '../../lib/formatos'

/** Conteúdo do QR: ROTASOLIDARIA:{reservaId}:{codigo} */
function lerConteudoQr(texto) {
  const m = /^ROTASOLIDARIA:(\d+):(\d{4})$/.exec(texto.trim())
  return m ? { reservaId: Number(m[1]), codigo: m[2] } : null
}

function LeitorQr({ aoLer, aoFechar }) {
  const [erro, setErro] = useState(null)
  const leitor = useRef(null)
  const lido = useRef(false)
  useEffect(() => {
    let ativo = true
    ;(async () => {
      try {
        // html5-qrcode só é baixado quando a câmera é aberta (fora do bundle inicial)
        const { Html5Qrcode } = await import('../../lib/leitorQr')
        if (!ativo) return
        leitor.current = new Html5Qrcode('leitor-qr', { verbose: false })
        await leitor.current.start({ facingMode: 'environment' }, { fps: 10, qrbox: { width: 220, height: 220 } },
          (texto) => {
            const dados = lerConteudoQr(texto)
            if (dados && !lido.current) {
              lido.current = true
              aoLer(dados)
            }
            else setErro(new Error('Este QR code não é de uma reserva do Rota Solidária.'))
          }, () => {})
      } catch {
        if (ativo) setErro(new Error('Não foi possível abrir a câmera. Permita o acesso ou digite o código.'))
      }
    })()
    return () => {
      ativo = false
      const l = leitor.current
      if (l?.isScanning) l.stop().then(() => l.clear()).catch(() => {})
    }
  }, [aoLer])
  return (
    <div className="cartao pilha">
      <div id="leitor-qr" className="leitor-qr" aria-label="Visualização da câmera para ler o QR code" />
      <Erro erro={erro} />
      <button type="button" className="botao botao-contorno botao-bloco" onClick={aoFechar}>Fechar câmera</button>
    </div>
  )
}

export default function ConfirmarRetirada() {
  const [codigo, setCodigo] = useState('')
  const [camera, setCamera] = useState(false)
  const [erro, setErro] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const [recibo, setRecibo] = useState(null)

  async function confirmar(dados) {
    setErro(null)
    setEnviando(true)
    try {
      setRecibo(await api('/api/retiradas', { metodo: 'POST', corpo: dados }))
      setCamera(false)
    } catch (e) {
      setErro(e)
    } finally {
      setEnviando(false)
    }
  }
  const aoLerQr = useRef((d) => { setCodigo(d.codigo); confirmar(d) }).current

  if (recibo) {
    return (
      <div className="pilha-grande">
        <div className="cartao-destaque pilha" role="status" aria-live="polite">
          <p className="eyebrow">Retirada confirmada</p>
          <h1 style={{ color: 'var(--texto-claro)' }}>Obrigado por doar!</h1>
          <p>{numero(recibo.quantidade)} × {recibo.item} (≈ {numero(recibo.pesoKg)} kg) para <strong style={{ color: 'var(--texto-claro)' }}>{recibo.receptorNome}</strong>.</p>
          <p style={{ margin: 0 }}>Recibo {recibo.numero} gerado.</p>
        </div>
        <div className="grade-2">
          <Link to={`/comerciante/recibos/${recibo.id}`} className="botao botao-primario"><Icone nome="recibo" /> Ver recibo</Link>
          <button className="botao botao-contorno" onClick={() => { setRecibo(null); setCodigo('') }}>Nova retirada</button>
        </div>
      </div>
    )
  }

  return (
    <div className="pilha-grande">
      <Cabecalho eyebrow="Confirmar retirada" titulo="Quem veio buscar?">
        <p className="texto-2">Leia o QR code mostrado pela organização ou pelo voluntário, ou digite o código de 4 dígitos.</p>
      </Cabecalho>

      {camera ? <LeitorQr aoLer={aoLerQr} aoFechar={() => setCamera(false)} /> : (
        <button className="botao botao-escuro botao-bloco botao-gigante" style={{ boxShadow: 'none' }} onClick={() => { setErro(null); setCamera(true) }}>
          <Icone nome="camera" tamanho={28} /> Ler QR code
        </button>
      )}

      <form className="cartao pilha" onSubmit={(e) => { e.preventDefault(); confirmar({ codigo }) }}>
        <div className="campo">
          <label htmlFor="codigo">Ou digite o código</label>
          <input id="codigo" className="entrada-codigo" type="text" inputMode="numeric" autoComplete="one-time-code"
            pattern="\d{4}" maxLength={4} value={codigo} placeholder="0000"
            onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 4))} aria-describedby="ajuda-codigo" />
          <span className="ajuda" id="ajuda-codigo">O código aparece abaixo do QR code, no celular de quem vai retirar.</span>
        </div>
        <Erro erro={erro} />
        <button className="botao botao-primario botao-bloco" disabled={codigo.length !== 4 || enviando}>
          {enviando ? 'Confirmando…' : 'Confirmar retirada'}
        </button>
      </form>
    </div>
  )
}
