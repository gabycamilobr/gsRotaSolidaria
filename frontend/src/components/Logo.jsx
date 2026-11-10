/**
 * Logo do Rota Solidária: rota tracejada que sobe da esquerda para a direita, passa por duas
 * paradas (círculos vazados) e termina num alvo âmbar. Sobre fundo escuro a rota é creme;
 * sobre fundo claro, verde-noite.
 */
const CORES = {
  escuro: { rota: '#F3EFE6', fundo: '#1E2A21' },
  claro: { rota: '#1E2A21', fundo: '#FFFFFF' },
}

export function LogoIcone({ variante = 'escuro', tamanho = 48, titulo }) {
  const c = CORES[variante]
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 64 64" role={titulo ? 'img' : undefined}
      aria-hidden={titulo ? undefined : true} aria-label={titulo} focusable="false">
      <LogoGrupo c={c} />
    </svg>
  )
}

/**
 * Logo completo (ícone + wordmark) num único SVG: "Rota" em creme ou verde-noite acima de
 * "Solidária" em âmbar, na fonte dos títulos. horizontal: wordmark numa linha só.
 */
export default function Logo({ variante = 'escuro', tamanho = 'medio', horizontal = false }) {
  const c = CORES[variante]
  const escala = { pequeno: 0.42, medio: 1, grande: 1.5 }[tamanho] ?? 1
  const fonte = { fontFamily: 'Inter, Arial, sans-serif', fontWeight: 800, letterSpacing: '-0.035em' }
  if (horizontal) {
    return (
      <svg width={300 * escala} height={64 * escala} viewBox="0 0 300 64" role="img" aria-label="Rota Solidária" style={{ overflow: 'visible' }}>
        <LogoGrupo c={c} />
        <text x="74" y="44" fontSize="34" style={fonte}>
          <tspan fill={c.rota}>Rota </tspan><tspan fill="#E9A23B">Solidária</tspan>
        </text>
      </svg>
    )
  }
  return (
    <svg width={290 * escala} height={104 * escala} viewBox="0 0 290 104" role="img" aria-label="Rota Solidária" style={{ overflow: 'visible' }}>
      <g transform="translate(0 14) scale(1.3)"><LogoGrupo c={c} /></g>
      <text x="100" y="46" fontSize="54" fill={c.rota} style={fonte}>Rota</text>
      <text x="100" y="98" fontSize="54" fill="#E9A23B" style={fonte}>Solidária</text>
    </svg>
  )
}

function LogoGrupo({ c }) {
  return (
    <g aria-hidden="true">
      <path d="M9 55 C15 49 20 45 27 42 S41 30 49 17" fill="none" stroke={c.rota} strokeWidth="3.4"
        strokeLinecap="round" strokeDasharray="4.5 4.5" />
      <circle cx="9" cy="55" r="5" fill={c.fundo} stroke={c.rota} strokeWidth="3.4" />
      <circle cx="27" cy="42" r="5" fill={c.fundo} stroke={c.rota} strokeWidth="3.4" />
      <circle cx="49" cy="17" r="11.5" fill={c.fundo} stroke="#E9A23B" strokeWidth="3.6" />
      <circle cx="49" cy="17" r="5.6" fill="none" stroke="#E9A23B" strokeWidth="3" />
      <circle cx="49" cy="17" r="2" fill="#E9A23B" />
    </g>
  )
}

/** Rota decorativa da capa: linha tracejada longa terminando num alvo âmbar. */
export function RotaDecorativa({ className = 'rota-decorativa' }) {
  return (
    <svg className={className} viewBox="0 0 300 540" aria-hidden="true" focusable="false">
      <path d="M40 560 C70 470 90 440 120 400 S200 320 220 250 S250 140 262 70" fill="none"
        stroke="var(--tracejado)" strokeWidth="5" strokeLinecap="round" strokeDasharray="14 12" />
      <circle cx="120" cy="400" r="12" fill="var(--verde-noite-2)" stroke="var(--tracejado)" strokeWidth="5" />
      <circle cx="220" cy="250" r="12" fill="var(--verde-noite-2)" stroke="var(--tracejado)" strokeWidth="5" />
      <circle cx="262" cy="56" r="27" fill="var(--verde-noite)" stroke="var(--ambar)" strokeWidth="9" />
      <circle cx="262" cy="56" r="12" fill="none" stroke="var(--ambar)" strokeWidth="6" />
      <circle cx="262" cy="56" r="4" fill="var(--ambar)" />
    </svg>
  )
}
