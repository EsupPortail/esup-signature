-- Data
--     description
-- LiveWorkflow
--     description
--     created_by
--     external
--     source_type
--     document_source_uri
--     name + constraint
--     scan pdf metadatas
--     role
--     public usage
--     title
-- LiveWorflowStep
--     description
--     name
--     max recipients
--     changeable
--     parent type
--     parent id
-- Recipient
--     parentType
--     parentId
-- SignBook
--     Create_by
--     current_workflow_step_number
--     description
--     create_by_eppn
--     document target uri
--     target type
--     workflow id
--     workflow name
--     exported document uri
-- SignRequest
--     Create_by
--     create_by_eppn
--     all sign to complete
--     current step number
--     sign_type
-- Workflow
--     create_by migrate create_by_id
--     external
-- Worflow Step
--     parent id
--     parent type

CREATE OR REPLACE FUNCTION clean() returns void AS
$BODY$
DECLARE
    w workflow%rowtype;
BEGIN
    alter table data drop column description;
    alter table recipient drop column parent_type,
                          drop column parent_id;
    alter table sign_book drop column create_by,
                          drop column current_workflow_step_number,
                          drop column description,
                          drop column create_by_eppn,
                          drop column documents_target_uri,
                          drop column target_type,
                          drop column workflow_id,
                          drop column workflow_name,
                          drop column exported_documenturi;
    alter table sign_request drop column create_by,
                             drop column create_by_eppn,
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

alter table sign_request_sign_request_params
    add column sign_request_params_order int4;

update sign_request_sign_request_params set sign_request_params_order = 0;

alter table sign_request_sign_request_params alter column sign_request_params_order set not null ;