-- supprimer la contrainte unique form_fields

\d form_fields

--Table "public.form_fields"
--Column    |  Type   | Modifiers
----------------+---------+-----------
-- form_id      | bigint  | not null
-- fields_id    | bigint  | not null
-- fields_order | integer | not null
--Indexes:
--    "form_fields_pkey" PRIMARY KEY, btree (form_id, fields_order)
--    "uk_qyes453jb4ho6jt8bwjcdninr" UNIQUE CONSTRAINT, btree (fields_id)
--Foreign-key constraints:
--    "fk1wng9ijav5mar32b6bi2vwfk7" FOREIGN KEY (form_id) REFERENCES form(id)
--    "fk785uuku5axshwitce6byh7433" FOREIGN KEY (fields_id) REFERENCES field(id)


alter table form_fields drop constraint uk_qyes453jb4ho6jt8bwjcdninr;