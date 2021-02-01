CREATE OR REPLACE FUNCTION updateDataUserProperty() returns void AS
$BODY$
DECLARE
    d data%rowtype;
BEGIN
    for d in select * from data where create_by_id is null
        LOOP
            update data set create_by_id = (select id from user_account where eppn = data.create_by), owner_id = (select id from user_account where eppn = data.owner) where id = d.id;
        END LOOP ;
    ALTER TABLE data DROP COLUMN create_by;
    ALTER TABLE data DROP COLUMN owner;
    UPDATE sign_request_params set blue = 0, green = 0, red = 0;
    UPDATE workflow_step set repeatable = false;
    UPDATE live_workflow_step set repeatable = false;
END
$BODY$
    LANGUAGE plpgsql;

DO $$ BEGIN
    PERFORM updateDataUserProperty();
END $$;