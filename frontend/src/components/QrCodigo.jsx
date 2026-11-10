import { useEffect, useState } from 'react'
import QRCode from 'qrcode'

/** QR code gerado no próprio navegador (sem serviço externo). */
export default function QrCodigo({ conteudo, codigo }) {
  const [svg, setSvg] = useState('')
  useEffect(() => {
    QRCode.toString(conteudo, { type: 'svg', margin: 1, errorCorrectionLevel: 'M', color: { dark: '#1E2A21', light: '#FFFFFF' } })
      .then(setSvg)
  }, [conteudo])
  return (
    <div className="qr" role="img" aria-label={`QR code da retirada. Código ${codigo.split('').join(' ')}`}
      dangerouslySetInnerHTML={{ __html: svg }} />
  )
}
