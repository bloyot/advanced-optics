-- store ship information, all string columns are xws values for simplicity
CREATE TABLE ship (
   ship_id INTEGER PRIMARY KEY,
   faction TEXT NOT NULL,
   pilot TEXT NOT NULL,
   ship TEXT NOT NULL,
   agility INTEGER NOT NULL,
   initiative INTEGER NOT NULL,
   hull INTEGER NOT NULL,
   shields INTEGER NOT NULL,
   force INTEGER NOT NULL,
   UNIQUE(faction, pilot, ship)
);

CREATE TABLE ship_attack (
   ship_attack_id INTEGER PRIMARY KEY,
   ship_id INTEGER NOT NULL,
   value INTEGER NOT NULL,
   FOREIGN KEY(ship_id) REFERENCES ship(ship_id)
);

CREATE TABLE tournament (
   tournament_id INTEGER PRIMARY KEY,
   date INTEGER NOT NULL,
   format INTEGER NOT NULL,
   type INTEGER NOT NULL,
   county TEXT,
   -- can be null because we won't necessarily have this until we load the lists for the tournament
   num_players INTEGER
);

CREATE TABLE list (
   list_id INTEGER PRIMARY KEY,
   tournament_id INTEGER NOT NULL,
   -- faction and list can be null if some participants did not submit a list
   faction TEXT,
   list_xws TEXT,
   swiss_rank INTEGER,
   top_cut_rank INTEGER,
   FOREIGN KEY(tournament_id) REFERENCES tournament(tournament_id)
);