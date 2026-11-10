-- Dados FICTÍCIOS de demonstração, na Vila Mariana (São Paulo).
-- Coordenadas aproximadas em ruas reais do bairro. Distâncias a partir da Padaria Bom Dia:
--   Associação Mãos Dadas   ~ 400 m  (aceita todas as categorias)
--   Comunidade Esperança    ~ 1,2 km (aceita Panificados e Hortifrúti)
--   Casa de Apoio Vila Nova ~ 2,8 km (FORA do raio de 2 km)
-- Senha de todos os usuários: demo123 (hash BCrypt abaixo).

INSERT INTO comercio (id, nome, endereco, latitude, longitude) VALUES
 (1, 'Padaria Bom Dia',   'Rua Domingos de Morais, 1500 – Vila Mariana', -23.58910, -46.63470),
 (2, 'Quitanda da Vila',  'Rua Joaquim Távora, 520 – Vila Mariana',      -23.59050, -46.63750);

INSERT INTO receptor (id, nome, endereco, latitude, longitude, categorias_aceitas) VALUES
 (1, 'Associação Mãos Dadas',   'Rua França Pinto, 310 – Vila Mariana',    -23.59270, -46.63520,
     ARRAY['PANIFICADOS','HORTIFRUTI','INDUSTRIALIZADOS','REFRIGERADOS']::VARCHAR(30)[]),
 (2, 'Comunidade Esperança',    'Rua Sena Madureira, 980 – Vila Mariana',  -23.59300, -46.64550,
     ARRAY['PANIFICADOS','HORTIFRUTI']::VARCHAR(30)[]),
 (3, 'Casa de Apoio Vila Nova', 'Rua Afonso Braz, 410 – Vila Nova Conceição', -23.59050, -46.66200,
     ARRAY['PANIFICADOS','HORTIFRUTI','INDUSTRIALIZADOS','REFRIGERADOS']::VARCHAR(30)[]);

INSERT INTO voluntario (id, nome, receptor_id, modal, latitude, longitude) VALUES
 (1, 'Ana', 1, 'BICICLETA', -23.58700, -46.63350);

INSERT INTO usuario (email, senha_hash, perfil, comercio_id, receptor_id, voluntario_id, consentimento_localizacao_em) VALUES
 ('padaria@demo.com',   '$2a$10$62QxE2FjZ0qkyqt92SdWwubZ9kbxfpJlrOrWC4dF/1ZX3SLfblm56', 'COMERCIANTE', 1, NULL, NULL, now()),
 ('quitanda@demo.com',  '$2a$10$62QxE2FjZ0qkyqt92SdWwubZ9kbxfpJlrOrWC4dF/1ZX3SLfblm56', 'COMERCIANTE', 2, NULL, NULL, now()),
 ('maosdadas@demo.com', '$2a$10$62QxE2FjZ0qkyqt92SdWwubZ9kbxfpJlrOrWC4dF/1ZX3SLfblm56', 'RECEPTOR',    NULL, 1, NULL, now()),
 ('esperanca@demo.com', '$2a$10$62QxE2FjZ0qkyqt92SdWwubZ9kbxfpJlrOrWC4dF/1ZX3SLfblm56', 'RECEPTOR',    NULL, 2, NULL, now()),
 ('vilanova@demo.com',  '$2a$10$62QxE2FjZ0qkyqt92SdWwubZ9kbxfpJlrOrWC4dF/1ZX3SLfblm56', 'RECEPTOR',    NULL, 3, NULL, now()),
 ('ana@demo.com',       '$2a$10$62QxE2FjZ0qkyqt92SdWwubZ9kbxfpJlrOrWC4dF/1ZX3SLfblm56', 'VOLUNTARIO',  NULL, NULL, 1, now());

-- ajusta as sequências depois dos ids fixos
SELECT setval('comercio_id_seq',   (SELECT MAX(id) FROM comercio));
SELECT setval('receptor_id_seq',   (SELECT MAX(id) FROM receptor));
SELECT setval('voluntario_id_seq', (SELECT MAX(id) FROM voluntario));
