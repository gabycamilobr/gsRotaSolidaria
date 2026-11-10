import { useState } from 'react'
import Icone from '../../components/Icone'
import { Cabecalho, Carregando, Erro } from '../../components/Comum'
import QrCodigo from '../../components/QrCodigo'
import { distancia, hora, numero, unidadeRotulo } from '../../lib/formatos'
import { useAoReceberAviso } from '../../lib/notificacoes'
import { useDados } from '../../lib/useDados'

function Item({ item }) {
  const [qrAberto, setQrAberto] = useState(false)
  const qtd = item.unidade === 'UNIDADES' ? `${numero(item.quantidade)} ${item.descricao}`
    : `${numero(item.quantidade)} ${unidadeRotulo(item.unidade, item.quantidade)} de ${item.descricao}`
  return (
    <div className="cartao pilha" style={{ padding: '.75rem' }}>
      <div className="linha-entre">
        <span>
          <strong style={{ display: 'block' }}>{qtd}</strong>
          <span className="pequeno texto-2">≈ {numero(item.pesoKg)} kg · reserva até {hora(item.reservaExpiraEm)}</span>
        </span>
        <button className="botao botao-contorno" onClick={() => setQrAberto((a) => !a)} aria-expanded={qrAberto}
          style={{ minHeight: 44, padding: '0 .8rem' }}>
          <Icone nome="qr" tamanho={18} /> {qrAberto ? 'Ocultar' : 'QR'}
        </button>
      </div>
      {item.refrigerado && <p className="aviso aviso-frio" style={{ margin: 0 }}><Icone nome="floco" /> Leve bolsa térmica.</p>}
      {qrAberto && (
        <div className="centro pilha">
          <QrCodigo conteudo={item.qrConteudo} codigo={item.codigo} />
          <p className="codigo-retirada" style={{ margin: 0 }}>{item.codigo}</p>
        </div>
      )}
    </div>
  )
}

export default function Rota() {
  const [posicao, setPosicao] = useState(null)
  const [erroGeo, setErroGeo] = useState(null)
  const caminho = posicao ? `/api/rota?lat=${posicao.lat}&lng=${posicao.lng}` : '/api/rota'
  const { dados: r, erro, carregando, recarregar } = useDados(caminho)
  useAoReceberAviso(recarregar)

  function usarLocalizacao() {
    setErroGeo(null)
    if (!navigator.geolocation) return setErroGeo(new Error('Este aparelho não informa a localização.'))
    navigator.geolocation.getCurrentPosition(
      (p) => setPosicao({ lat: p.coords.latitude.toFixed(6), lng: p.coords.longitude.toFixed(6) }),
      () => setErroGeo(new Error('Não foi possível obter sua localização. Usando o ponto de partida cadastrado.')),
      { enableHighAccuracy: true, timeout: 10_000 })
  }

  if (carregando) return <Carregando texto="Calculando a rota…" />
  if (erro) return <Erro erro={erro} tentarDeNovo={recarregar} />

  return (
    <div className="pilha-grande">
      <Cabecalho eyebrow="Minha rota" titulo={`Olá, ${r.voluntarioNome}`}>
        <p className="texto-2">Coletas dos lotes reservados pela sua organização, na ordem mais curta a partir de você.</p>
      </Cabecalho>

      {r.paradas.length === 0 ? (
        <div className="cartao vazio">
          <p><strong>Nenhuma coleta agora.</strong></p>
          <p style={{ margin: 0 }}>Quando a sua organização reservar um lote, ele aparece aqui.</p>
        </div>
      ) : (
        <>
          <section className="cartao-destaque pilha" aria-label="Resumo da rota">
            <div className="grade-2">
              <div><p className="eyebrow">Distância</p><p className="numero-grande" style={{ margin: 0, fontSize: '1.9rem' }}>{distancia(r.distanciaTotalM)}</p></div>
              <div><p className="eyebrow">{r.modalRotulo}</p><p className="numero-grande" style={{ margin: 0, fontSize: '1.9rem' }}>≈ {r.tempoEstimadoMin} min</p></div>
            </div>
            <p className="pequeno" style={{ margin: 0 }}>Distâncias em linha reta; tempo estimado a {r.velocidadeKmH} km/h.</p>
            <a className="botao botao-primario botao-bloco" href={r.googleMapsUrl} target="_blank" rel="noopener noreferrer">
              <Icone nome="mapa" /> Abrir no Google Maps
            </a>
          </section>

          <ol className="paradas" aria-label="Paradas em ordem">
            <li>
              <span className="marcador" aria-hidden="true"><Icone nome="pino" tamanho={20} /></span>
              <p className="eyebrow" style={{ marginTop: 10 }}>Partida</p>
              <p style={{ fontWeight: 800 }}>{r.origem.nome}</p>
            </li>
            {r.paradas.map((p) => (
              <li key={p.ordem}>
                {p.tipo === 'ENTREGA'
                  ? <span className="marcador" aria-hidden="true" style={{ border: 0, background: 'transparent' }}><LogoAlvo /></span>
                  : <span className="marcador" aria-hidden="true">{p.ordem}</span>}
                <p className="trecho">+ {distancia(p.distanciaTrechoM)}</p>
                <p className="eyebrow">{p.tipo === 'ENTREGA' ? 'Entrega · ponto de distribuição' : `Parada ${p.ordem} · coleta`}</p>
                <h2 style={{ fontSize: '1.15rem', marginBottom: '.15rem' }}>{p.nome}</h2>
                <p className="texto-2 pequeno">{p.endereco}</p>
                <div className="pilha">{p.itens.map((i) => <Item key={i.reservaId} item={i} />)}</div>
              </li>
            ))}
          </ol>
        </>
      )}

      <div className="pilha">
        <button className="botao botao-contorno botao-bloco" onClick={usarLocalizacao}>
          <Icone nome="pino" /> {posicao ? 'Atualizar com minha localização' : 'Usar minha localização atual'}
        </button>
        <p className="pequeno texto-2 centro" style={{ margin: 0 }}>A localização é usada só para calcular a rota e não é guardada.</p>
        <Erro erro={erroGeo} />
      </div>
    </div>
  )
}

function LogoAlvo() {
  return (
    <svg width="45" height="45" viewBox="0 0 45 45" aria-hidden="true">
      <circle cx="22.5" cy="22.5" r="19" fill="var(--creme)" stroke="var(--ambar)" strokeWidth="5" />
      <circle cx="22.5" cy="22.5" r="9" fill="none" stroke="var(--ambar)" strokeWidth="4" />
      <circle cx="22.5" cy="22.5" r="3" fill="var(--ambar)" />
    </svg>
  )
}
