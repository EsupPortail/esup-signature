--
-- Licensed to ESUP-Portail under one or more contributor license
-- agreements. See the NOTICE file distributed with this work for
-- additional information regarding copyright ownership.
--
-- ESUP-Portail licenses this file to you under the Apache License,
-- Version 2.0 (the "License"); you may not use this file except in
-- compliance with the License. You may obtain a copy of the License at:
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

ALTER TABLE tag_log ADD COLUMN textsearchable_index_col tsvector;
UPDATE tag_log SET textsearchable_index_col = setweight(to_tsvector('simple', coalesce(salle.nom,'')), 'A')||setweight(to_tsvector('simple', coalesce(etudiant.eppn,'')), 'A')||setweight(to_tsvector('simple', coalesce(etudiant.csn,'')), 'A') ||setweight(to_tsvector('simple', coalesce(etudiant.nom,'')), 'A')||setweight(to_tsvector('simple', coalesce(etudiant.prenom,'')), 'B')  FROM etudiant, salle WHERE tag_log.etudiant=etudiant.id AND tag_log.salle=salle.id;
CREATE INDEX textsearch_idx ON tag_log USING gin(textsearchable_index_col);
CREATE FUNCTION textsearchable_tag_log_trigger() RETURNS trigger AS $$ begin new.textsearchable_index_col := setweight(to_tsvector('simple', coalesce(salle.nom,'')), 'A')||setweight(to_tsvector('simple', coalesce(etudiant.eppn,'')), 'A')||setweight(to_tsvector('simple', coalesce(etudiant.csn,'')), 'A') ||setweight(to_tsvector('simple', coalesce(etudiant.nom,'')), 'A')||setweight(to_tsvector('simple', coalesce(etudiant.prenom,'')), 'B')  FROM etudiant, salle WHERE new.etudiant=etudiant.id AND new.salle=salle.id; return new; end $$ LANGUAGE plpgsql;
CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE ON tag_log FOR EACH ROW EXECUTE PROCEDURE textsearchable_tag_log_trigger();

ALTER TABLE etudiant ADD COLUMN textsearchable_index_col tsvector;
UPDATE etudiant SET textsearchable_index_col = setweight(to_tsvector('simple', coalesce(nom,'')), 'A')||setweight(to_tsvector('simple', coalesce(eppn,'')), 'A')||setweight(to_tsvector('simple', coalesce(csn,'')), 'A') ||setweight(to_tsvector('simple', coalesce(prenom,'')), 'B');
CREATE INDEX textsearch_idx ON etudiant USING gin(textsearchable_index_col);
CREATE FUNCTION textsearchable_etudiant_trigger() RETURNS trigger AS $$ begin new.textsearchable_index_col := setweight(to_tsvector('simple', coalesce(new.nom,'')), 'A')||setweight(to_tsvector('simple', coalesce(new.eppn,'')), 'A')||setweight(to_tsvector('simple', coalesce(new.csn,'')), 'A') ||setweight(to_tsvector('simple', coalesce(new.prenom,'')), 'B'); return new; end $$ LANGUAGE plpgsql;
CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE ON etudiant FOR EACH ROW EXECUTE PROCEDURE textsearchable_etudiant_trigger();

INSERT INTO appli_version (id, esup_carte_culture_version, version) SELECT nextval('hibernate_sequence'), '0.1.x', '1' WHERE NOT EXISTS (SELECT * FROM appli_version);

