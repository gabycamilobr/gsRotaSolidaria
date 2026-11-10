-- Limpa os lotes, reservas, recibos e avisos para regravar o vídeo do zero.
-- Mantém comércios, receptores, voluntária e usuários do seed.
-- Uso: psql -h localhost -U rota -d rotasolidaria -f docs/reset-demo.sql
TRUNCATE notificacao, recibo, reserva, lote_candidato, lote_foto, lote RESTART IDENTITY CASCADE;
