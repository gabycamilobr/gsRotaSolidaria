import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import Icone from '../../components/Icone'
import { AvisoRefrigerado, Cabecalho, Erro } from '../../components/Comum'
import { api } from '../../lib/api'
import { CATEGORIAS, categoria, pesoEstimado } from '../../lib/categorias'
import { distancia, quantidade as textoQuantidade } from '../../lib/formatos'
import { comprimirFoto } from '../../lib/imagem'

const doisDigitos = (n) => String(n).padStart(2, '0')
const paraHHMM = (d) => `${doisDigitos(d.getHours())}:${doisDigitos(d.getMinutes())}`

/** Horário padrão: 21h, se estiver entre 30 min e o limite da categoria; senão, daqui a 1 h (refrigerados: 1h30). */
function horarioPadrao(refrigerado) {
  const agora = new Date()
  const limiteMs = (refrigerado ? 2 : 24) * 3600_000
  const vinteUma = new Date(agora)
  vinteUma.setHours(21, 0, 0, 0)
  const ate21 = vinteUma - agora
  let alvo = ate21 >= 30 * 60_000 && ate21 <= limiteMs ? vinteUma : new Date(agora.getTime() + (refrigerado ? 90 : 60) * 60_000)
  alvo.setSeconds(0, 0)
  alvo.setMinutes(alvo.getMinutes() - (alvo.getMinutes() % 5))
  if (alvo.getDate() !== agora.getDate()) {
    alvo = new Date(agora)
    alvo.setHours(23, 55, 0, 0)
  }
  return paraHHMM(alvo)
}

/** Converte "HH:MM" (horário de hoje, no relógio do aparelho) em Date. */
function horarioParaData(hhmm) {
  const [h, m] = hhmm.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m, 0, 0)
  return d
}

export default function Publicar() {
  const [foto, setFoto] = useState(null)        // Blob comprimido
  const [preview, setPreview] = useState(null)
  const [tamanhoFoto, setTamanhoFoto] = useState(null)
  const [cat, setCat] = useState('PANIFICADOS')
  const [descricao, setDescricao] = useState(categoria('PANIFICADOS').descricao)
  const [descricaoEditada, setDescricaoEditada] = useState(false)
  const [qtd, setQtd] = useState('')
  const [unidade, setUnidade] = useState('UNIDADES')
  const [peso, setPeso] = useState('')
  const [pesoEditado, setPesoEditado] = useState(false)
  const [ate, setAte] = useState(() => horarioPadrao(false))
  const [confirmo, setConfirmo] = useState(false)
  const [erro, setErro] = useState(null)
  const [campos, setCampos] = useState({})
  const [enviando, setEnviando] = useState(false)
  const [publicado, setPublicado] = useState(null)
  const inicio = useRef(Date.now())
  const refrigerado = categoria(cat)?.refrigerado

  useEffect(() => { if (!pesoEditado) setPeso(pesoEstimado(cat, qtd, unidade)) }, [cat, qtd, unidade, pesoEditado])
  useEffect(() => () => preview && URL.revokeObjectURL(preview), [preview])

  function escolherCategoria(id) {
    setCat(id)
    if (!descricaoEditada) setDescricao(categoria(id).descricao)
    if (categoria(id).refrigerado) setAte(horarioPadrao(true))
  }

  async function escolherFoto(ev) {
    const arq = ev.target.files?.[0]
    if (!arq) return
    setErro(null)
    try {
      const blob = await comprimirFoto(arq)
      setFoto(blob)
      setTamanhoFoto(Math.round(blob.size / 1024))
      setPreview(URL.createObjectURL(blob))
    } catch {
      setErro(new Error('Não foi possível ler essa imagem. Tente outra foto.'))
    }
  }

  function validar() {
    const c = {}
    if (!foto) c.foto = 'Tire ou escolha uma foto do lote.'
    if (!(Number(qtd) > 0)) c.quantidade = 'Informe a quantidade.'
    if (!(Number(peso) > 0)) c.pesoKg = 'Informe o peso estimado.'
    if (!descricao.trim()) c.descricao = 'Descreva o item.'
    const quando = horarioParaData(ate)
    if (quando <= new Date()) c.retirarAte = 'Esse horário já passou. Escolha um horário mais tarde.'
    else if (refrigerado && quando - Date.now() > 2 * 3600_000) c.retirarAte = 'Refrigerados: retirada em no máximo 2 horas.'
    if (!confirmo) c.confirmacaoValidade = 'Confirme a validade e a conservação para publicar.'
    setCampos(c)
    return Object.keys(c).length === 0
  }

  async function publicar(ev) {
    ev.preventDefault()
    setErro(null)
    if (!validar()) return
    setEnviando(true)
    const dados = {
      categoria: cat, descricao: descricao.trim(), quantidade: Number(qtd), unidade, pesoKg: Number(peso),
      retirarAte: horarioParaData(ate).toISOString(), confirmacaoValidade: confirmo,
    }
    const form = new FormData()
    form.append('lote', new Blob([JSON.stringify(dados)], { type: 'application/json' }))
    form.append('foto', foto, 'lote.jpg')
    try {
      const r = await api('/api/lotes', { metodo: 'POST', formulario: form })
      setPublicado({ ...r, segundos: Math.round((Date.now() - inicio.current) / 1000) })
      window.scrollTo(0, 0)
    } catch (e) {
      setErro(e)
      setCampos(e.campos || {})
    } finally {
      setEnviando(false)
    }
  }

  if (publicado) {
    return (
      <div className="pilha-grande">
        <div className="cartao-destaque pilha">
          <p className="eyebrow">Lote publicado</p>
          <h1 style={{ color: 'var(--texto-claro)' }}>{textoQuantidade(publicado)}</h1>
          <p>Publicado em {publicado.segundos} {publicado.segundos === 1 ? "segundo" : "segundos"}.</p>
        </div>
        <section className="cartao pilha" aria-labelledby="avisados">
          <h2 id="avisados">Organizações avisadas</h2>
          {publicado.candidatos?.length ? (
            <ol className="pilha" style={{ paddingLeft: '1.25rem', margin: 0 }}>
              {publicado.candidatos.map((c) => (
                <li key={c.receptorId}><strong>{c.nome}</strong> · {distancia(c.distanciaM)}</li>
              ))}
            </ol>
          ) : <p>Nenhuma organização que aceita esta categoria num raio de 2 km. O lote fica visível se alguma se cadastrar.</p>}
          <p className="texto-2 pequeno" style={{ margin: 0 }}>Raio de 2 km, em ordem de distância. Quem reservar primeiro leva.</p>
        </section>
        <div className="grade-2">
          <Link to="/comerciante" className="botao botao-escuro">Ver lotes de hoje</Link>
          <button className="botao botao-contorno" onClick={() => window.location.reload()}>Publicar outro</button>
        </div>
      </div>
    )
  }

  return (
    <form className="pilha-grande" onSubmit={publicar} noValidate>
      <Cabecalho eyebrow="Publicar sobra" titulo="O que sobrou hoje?" />

      <div className="campo">
        <span className="legenda" id="rotulo-foto">Foto do lote</span>
        <label className="foto-entrada" aria-labelledby="rotulo-foto">
          {preview ? <img src={preview} alt="Prévia da foto do lote" /> : (
            <>
              <Icone nome="camera" tamanho={36} />
              <span>Tirar foto</span>
              <span className="pequeno texto-2">ou escolher da galeria</span>
            </>
          )}
          <input type="file" accept="image/*" capture="environment" onChange={escolherFoto}
            aria-describedby="ajuda-foto" aria-invalid={!!campos.foto} />
        </label>
        <span className="ajuda" id="ajuda-foto">
          {tamanhoFoto ? `Foto comprimida no celular: ${tamanhoFoto} KB.` : 'A foto é comprimida no celular antes do envio.'}
        </span>
        {campos.foto && <span className="erro-campo">{campos.foto}</span>}
      </div>

      <fieldset>
        <legend className="legenda">Categoria</legend>
        <div className="opcoes">
          {CATEGORIAS.map((c) => (
            <div key={c.id} className={`opcao ${c.aceita ? '' : 'opcao-larga'}`}>
              <input type="radio" name="categoria" id={`cat-${c.id}`} value={c.id} checked={cat === c.id}
                disabled={!c.aceita} onChange={() => escolherCategoria(c.id)}
                aria-describedby={`cat-${c.id}-info`} />
              <label htmlFor={`cat-${c.id}`}>
                {c.rotulo}
                <small id={`cat-${c.id}-info`}>{c.aceita ? c.exemplo : c.motivo}</small>
              </label>
            </div>
          ))}
        </div>
      </fieldset>
      {refrigerado && <AvisoRefrigerado />}

      <div className="campo">
        <label htmlFor="descricao">Item</label>
        <input id="descricao" type="text" maxLength={80} value={descricao}
          onChange={(e) => { setDescricao(e.target.value); setDescricaoEditada(true) }} aria-invalid={!!campos.descricao} />
        {campos.descricao && <span className="erro-campo">{campos.descricao}</span>}
      </div>

      <div className="grade-2">
        <div className="campo">
          <label htmlFor="quantidade">Quantidade</label>
          <input id="quantidade" type="number" inputMode="decimal" min="0" step="any" value={qtd}
            onChange={(e) => setQtd(e.target.value)} aria-invalid={!!campos.quantidade} placeholder="ex.: 20" />
          {campos.quantidade && <span className="erro-campo">{campos.quantidade}</span>}
        </div>
        <div className="campo">
          <label htmlFor="unidade">Unidade</label>
          <select id="unidade" value={unidade} onChange={(e) => setUnidade(e.target.value)}>
            <option value="UNIDADES">unidades</option>
            <option value="KG">kg</option>
            <option value="BANDEJAS">bandejas</option>
          </select>
        </div>
      </div>

      <div className="grade-2">
        <div className="campo">
          <label htmlFor="peso">Peso estimado (kg)</label>
          <input id="peso" type="number" inputMode="decimal" min="0" step="0.1" value={peso}
            onChange={(e) => { setPeso(e.target.value); setPesoEditado(true) }} aria-describedby="ajuda-peso" aria-invalid={!!campos.pesoKg} />
          <span className="ajuda" id="ajuda-peso">Calculado pela categoria; ajuste se precisar.</span>
          {campos.pesoKg && <span className="erro-campo">{campos.pesoKg}</span>}
        </div>
        <div className="campo">
          <label htmlFor="ate">Retirar até</label>
          <input id="ate" type="time" value={ate} onChange={(e) => setAte(e.target.value)} required aria-invalid={!!campos.retirarAte} />
          {campos.retirarAte && <span className="erro-campo">{campos.retirarAte}</span>}
        </div>
      </div>

      <div className="campo">
        <label className="checagem" htmlFor="confirmo">
          <input id="confirmo" type="checkbox" checked={confirmo} onChange={(e) => setConfirmo(e.target.checked)}
            aria-invalid={!!campos.confirmacaoValidade} required />
          <span><strong>Confirmo que o alimento está dentro da validade e foi conservado corretamente.</strong></span>
        </label>
        {campos.confirmacaoValidade && <span className="erro-campo">{campos.confirmacaoValidade}</span>}
      </div>

      <Erro erro={erro} />
      <button className="botao botao-primario botao-bloco botao-gigante" disabled={enviando} style={{ minHeight: 64 }}>
        {enviando ? 'Publicando…' : 'Publicar e avisar organizações'}
      </button>
    </form>
  )
}
