-- =====================================================================
-- Rota Solidária – consulta de matching (o coração do sistema)
-- =====================================================================
-- Para rodar no psql (troque 1 pelo id do lote publicado no vídeo):
--   \set loteId 1
--   \i docs/queries.sql
--
-- Receptores num raio de 2.000 m do lote que aceitam a categoria dele,
-- em ordem de distância. ST_DWithin usa o índice GIST das colunas geography.

SELECT r.nome, ROUND(ST_Distance(r.localizacao, l.localizacao)) AS distancia_m
FROM receptor r JOIN lote l ON l.id = :loteId
WHERE ST_DWithin(r.localizacao, l.localizacao, 2000)
  AND l.categoria = ANY(r.categorias_aceitas)
ORDER BY distancia_m;

-- Resultado esperado para os 20 pães da Padaria Bom Dia (seed da Vila Mariana):
--          nome          | distancia_m
-- -----------------------+-------------
--  Associação Mãos Dadas |         402
--  Comunidade Esperança  |        1184
-- (a Casa de Apoio Vila Nova, a ~2,8 km, fica de fora)


-- ---------------------------------------------------------------------
-- Consultas de apoio para a demonstração
-- ---------------------------------------------------------------------

-- Distância de TODOS os receptores até o lote (mostra quem ficou fora do raio e por quê)
SELECT r.nome,
       ROUND(ST_Distance(r.localizacao, l.localizacao)) AS distancia_m,
       ST_DWithin(r.localizacao, l.localizacao, 2000)   AS dentro_do_raio,
       l.categoria = ANY(r.categorias_aceitas)          AS aceita_categoria
FROM receptor r JOIN lote l ON l.id = :loteId
ORDER BY distancia_m;

-- Fila gravada pelo matching (lote_candidato), na ordem em que os receptores são avisados
SELECT c.ordem, r.nome, c.distancia_m, c.notificado_em
FROM lote_candidato c JOIN receptor r ON r.id = c.receptor_id
WHERE c.lote_id = :loteId
ORDER BY c.ordem;

-- Plano de execução: confira o uso do índice GIST e o tempo (meta: abaixo de 500 ms)
EXPLAIN ANALYZE
SELECT r.nome, ROUND(ST_Distance(r.localizacao, l.localizacao)) AS distancia_m
FROM receptor r JOIN lote l ON l.id = :loteId
WHERE ST_DWithin(r.localizacao, l.localizacao, 2000)
  AND l.categoria = ANY(r.categorias_aceitas)
ORDER BY distancia_m;

-- Situação dos lotes e reservas
SELECT l.id, c.nome AS comercio, l.descricao, l.status, l.peso_kg, r.status AS reserva, r.expira_em
FROM lote l JOIN comercio c ON c.id = l.comercio_id
LEFT JOIN reserva r ON r.lote_id = l.id AND r.status = 'ATIVA'
ORDER BY l.id;
