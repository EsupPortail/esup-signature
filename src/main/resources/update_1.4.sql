CREATE OR REPLACE FUNCTION updateliveworkflow() returns void AS
$BODY$
DECLARE
    lw live_workflow_workflow_steps%rowtype;
BEGIN
    for lw in select * from live_workflow_workflow_steps
        LOOP
            insert into live_workflow_live_workflow_steps values(lw.live_workflow_id, lw.workflow_steps_id, lw.workflow_steps_order);
        END LOOP ;
END
$BODY$
    LANGUAGE plpgsql;

DO $$ BEGIN
    PERFORM updateliveworkflow();
END $$;