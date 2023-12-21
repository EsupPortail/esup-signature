CREATE OR REPLACE FUNCTION clean() returns void AS
$BODY$
DECLARE
    w workflow%rowtype;
BEGIN
    alter table data drop column description;
    alter table recipient drop column parent_type,
                          drop column parent_id;
    alter table sign_book drop column current_workflow_step_number,
                          drop column description,
                          drop column create_by_eppn,
                          drop column documents_target_uri,
                          drop column target_type,
                          drop column workflow_id,
                          drop column workflow_name,
                          drop column exported_documenturi;
    alter table sign_request drop column create_by_eppn,
                             drop column all_sign_to_complete,
                             drop column current_step_number,
                             drop column sign_type;
    alter table workflow_step drop column parent_id,
                              drop column parent_type;
    for w in select * from workflow
        Loop
            if (w.create_by is null) then
                update workflow set create_by_id = (select id from user_account where eppn = 'system') where id = w.id;
            else
                update workflow set create_by_id = (select id from user_account where eppn = w.create_by) where id = w.id;
            end if;
        end loop;
        alter table workflow drop column create_by,
                             drop column external;
END
$BODY$
    LANGUAGE plpgsql;

DO $$ BEGIN
    PERFORM clean();
END $$;
