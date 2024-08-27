CREATE TABLE IF NOT EXISTS public.players
(
    name character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT player_pkey PRIMARY KEY (name)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.players
    OWNER to postgres;

CREATE TABLE IF NOT EXISTS public.game_duration
(
    game bigint NOT NULL,
    duration character varying COLLATE pg_catalog."default" NOT NULL,
    file character varying COLLATE pg_catalog."default" NOT NULL,
    creation_date timestamp without time zone NOT NULL,
    past_games character varying,
    duration_tot character varying,
    duration_other character varying,
    CONSTRAINT game_duration_pkey PRIMARY KEY (file, game, creation_date)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.game_duration
    OWNER to postgres;

INSERT INTO players VALUES('Player1') ON CONFLICT DO NOTHING;
INSERT INTO players VALUES('Player2') ON CONFLICT DO NOTHING;
INSERT INTO players VALUES('Player3') ON CONFLICT DO NOTHING;
INSERT INTO players VALUES('Player4') ON CONFLICT DO NOTHING;