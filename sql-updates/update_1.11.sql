-- supprimer la contrainte unique sur viewers_id de la table sign_book_viewers

alter table sign_book_viewers drop constraint if exists uk_gt3duxi8s9hmjyrakbjrwlhpk;

-- supprimer la table sign_request_viewers

drop table if exists sign_request_viewers;

-- supprimer la contrainte unique sur sign_book name

alter table sign_book drop constraint if exists uk_nmkr8srruubb1j93cceyg747y;