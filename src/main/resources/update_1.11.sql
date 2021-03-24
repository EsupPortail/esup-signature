-- connexion à la base esupsignature

\c esupsignature

-- supprimer la contrainte unique sur viewers_id de la table sign_book_viewers

\d sign_book_viewers

-- postgres=# \c esupsignature_test
-- You are now connected to database "esupsignature_test" as user "postgres".
-- esupsignature_test=# \d sign_book_
-- Did not find any relation named "sign_book_".
-- esupsignature_test=# \d sign_book_viewers
--  Table "public.sign_book_viewers"
--     Column    |  Type  | Modifiers
-- --------------+--------+-----------
--  sign_book_id | bigint | not null
--  viewers_id   | bigint | not null
-- Indexes:
--     "uk_gt3duxi8s9hmjyrakbjrwlhpk" UNIQUE CONSTRAINT, btree (viewers_id)
-- Foreign-key constraints:
--     "fkbba225ae2q2t0ncl2cihttsh6" FOREIGN KEY (sign_book_id) REFERENCES sign_book(id)
--     "fkecvisojhprwbqypbx5ec8q2jf" FOREIGN KEY (viewers_id) REFERENCES user_account(id)

--on supprime la contrainte ainsi :

alter table sign_book_viewers drop constraint if exists uk_gt3duxi8s9hmjyrakbjrwlhpk;

drop table if exists sign_request_viewers;
