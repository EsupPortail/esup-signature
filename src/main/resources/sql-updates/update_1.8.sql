CREATE OR REPLACE FUNCTION updateFieldStepNumbers() returns void AS
$BODY$
DECLARE
    f field%rowtype;
    wf workflow%rowtype;
    frm form%rowtype;
    wfs workflow_workflow_steps%rowtype;
    sn varchar;
BEGIN
    for frm in select * from form
        LOOP
            for f in select * from field where field.id in (select fields_id from form_fields where form_id = frm.id)
                LOOP
                    for wf in select * from workflow where name = frm.workflow_type
                        LOOP
                            for sn in select step_numbers from field where id = f.id
                                LOOP
                                    if sn = '0' then
                                        update field set step_zero = true where id = f.id;
                                    else
                                        for wfs in select * from workflow_workflow_steps where workflow_steps_order = (CAST(sn as bigint) - 1) and workflow_id = wf.id
                                            LOOP
                                                insert into field_workflow_steps(field_id, workflow_steps_id) values (f.id, wfs.workflow_steps_id);
                                            END LOOP;
                                    end if;
                                END LOOP;
                        END LOOP;
                END LOOP;
        END LOOP;
    return;
END
$BODY$
    LANGUAGE plpgsql;

DO $$ BEGIN
    PERFORM updateFieldStepNumbers();
END $$;