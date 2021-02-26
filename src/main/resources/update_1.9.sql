CREATE OR REPLACE FUNCTION updateFormWorkflow() returns void AS
$BODY$
DECLARE
    f field%rowtype;
    wf workflow%rowtype;
    frm form%rowtype;
    wfs workflow_workflow_steps%rowtype;
    sn varchar;
BEGIN
    alter table workflow drop constraint if exists uk_3je18ux0wru0pxv6un40yhbn4;
    for frm in select * from form
        LOOP
            update form set workflow_id = (select id from workflow where workflow.name = frm.workflow_type) where form.id = frm.id;
        end loop;
    alter table form drop column workflow_type;
end;
$BODY$
    LANGUAGE plpgsql;

DO $$ BEGIN
    PERFORM updateFormWorkflow();
END $$;