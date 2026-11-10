// Categorias de alimento segundo a Lei nº 14.016/2020 (espelho do enum Categoria do back-end,
// que é quem valida de fato). Pesos por unidade/bandeja servem só para pré-preencher o peso estimado.
export const CATEGORIAS = [
  { id: 'PANIFICADOS', rotulo: 'Panificados', exemplo: 'pães, bolos, salgados assados', aceita: true,
    descricao: 'pães franceses', kgUnidade: 0.05, kgBandeja: 0.5, emoji: '🍞' },
  { id: 'HORTIFRUTI', rotulo: 'Hortifrúti', exemplo: 'frutas, legumes, verduras', aceita: true,
    descricao: 'frutas', kgUnidade: 0.2, kgBandeja: 1, emoji: '🍎' },
  { id: 'INDUSTRIALIZADOS', rotulo: 'Industrializados', exemplo: 'na validade, mesmo com embalagem danificada', aceita: true,
    descricao: 'itens de mercearia', kgUnidade: 0.5, kgBandeja: 2, emoji: '🥫' },
  { id: 'REFRIGERADOS', rotulo: 'Refrigerados', exemplo: 'laticínios e frios', aceita: true, refrigerado: true,
    descricao: 'laticínios', kgUnidade: 0.3, kgBandeja: 1, emoji: '🧀' },
  { id: 'REFEICOES_PRONTAS', rotulo: 'Refeições prontas', aceita: false,
    motivo: 'Não aceitas: exigem controle de temperatura e manipulação que a retirada rápida não garante.' },
  { id: 'VENCIDOS', rotulo: 'Itens vencidos', aceita: false,
    motivo: 'Não aceitos: a lei protege apenas alimentos dentro do prazo de validade.' },
]

export const categoria = (id) => CATEGORIAS.find((c) => c.id === id)

/** Peso estimado em kg (ex.: 20 pães franceses ≈ 1 kg). */
export function pesoEstimado(catId, quantidade, unidade) {
  const c = categoria(catId)
  const q = Number(quantidade)
  if (!c || !c.aceita || !q || q <= 0) return ''
  const kg = unidade === 'KG' ? q : unidade === 'BANDEJAS' ? q * c.kgBandeja : q * c.kgUnidade
  return String(Math.round(kg * 10) / 10 || 0.1)
}
