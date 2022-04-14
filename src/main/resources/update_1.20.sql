alter table sign_book
    alter column subject type text using subject::text;