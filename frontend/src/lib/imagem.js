// Compressão da foto no navegador antes do upload: lado maior 1024 px, JPEG qualidade 0,7.
const LADO_MAIOR = 1024
const QUALIDADE = 0.7

async function carregar(arquivo) {
  if ('createImageBitmap' in window) {
    try {
      return await createImageBitmap(arquivo, { imageOrientation: 'from-image' })
    } catch { /* cai no <img> */ }
  }
  const url = URL.createObjectURL(arquivo)
  try {
    const img = new Image()
    img.src = url
    await img.decode()
    return img
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
}

export async function comprimirFoto(arquivo) {
  const img = await carregar(arquivo)
  const largura = img.width, altura = img.height
  const escala = Math.min(1, LADO_MAIOR / Math.max(largura, altura))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(largura * escala)
  canvas.height = Math.round(altura * escala)
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
  img.close?.()
  const blob = await new Promise((ok) => canvas.toBlob(ok, 'image/jpeg', QUALIDADE))
  if (!blob) throw new Error('Não foi possível processar a foto.')
  return blob
}
