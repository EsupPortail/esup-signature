CREATE OR REPLACE FUNCTION migrate() returns void AS
$BODY$
DECLARE
    sr sign_request%rowtype;
    sbr sign_book%rowtype;
    ws workflow_step%rowtype;
    sbws sign_book_workflow_steps%rowtype;
    rec recipient%rowtype;
    w workflow%rowtype;
    r bigint;
    lw bigint;
    lws bigint;
    sb bigint;
BEGIN
    alter table sign_book drop constraint fkdbg7t5ofub4l25xsrv7sevrtf;
    FOR sbr IN SELECT * FROM sign_book
        LOOP
            lw = nextval('hibernate_sequence');
            insert into live_workflow values (lw, null, sbr.create_date, null, null, null, false, null, null, null, false, null, null, null, null, null, 1, null);
            FOR ws IN select * from workflow_step inner join sign_book_workflow_steps sbws2 on workflow_step.id = sbws2.workflow_steps_id and sbws2.sign_book_id = sbr.id
                LOOP
                    For sbws in select * from sign_book_workflow_steps where sign_book_workflow_steps.workflow_steps_id = ws.id
                        LOOP
                            lws = nextval('hibernate_sequence');
                            insert into live_workflow_step values (lws, ws.all_sign_to_complete, ws.changeable, ws.description, ws.max_recipients, ws.name, ws.parent_id, ws.parent_type, ws.sign_type, ws.version);
                            for rec in select * from recipient inner join workflow_step_recipients on recipient.id = workflow_step_recipients.recipients_id and workflow_step_id = ws.id
                                Loop
                                    insert into live_workflow_step_recipients values (lws, rec.id);
                                end loop;
                            if sbr.current_workflow_step_number - 1 = sbws.workflow_steps_order then
                                update live_workflow set current_step_id = lws where id = lw;
                            else
                                if sbr.current_workflow_step_number - 2 = sbws.workflow_steps_order then
                                    update live_workflow set current_step_id = lws where id = lw;
                                end if;
                            end if;

                            insert into live_workflow_workflow_steps values (lw, lws, sbws.workflow_steps_order);
                        end loop;
                end loop;
            update sign_book set live_workflow_id = lw where id=sbr.id;
        END LOOP;

    FOR sr IN SELECT * FROM sign_request
              WHERE parent_sign_book_id is null
        LOOP
            r = nextval('hibernate_sequence');
            IF sr.status = 'pending' THEN
                insert into recipient values (r, null, null, false, 1, sr.create_by_id);
            ELSE
                insert into recipient values (r, null, null, true, 1, sr.create_by_id);
            END IF;
            lws = nextval('hibernate_sequence');
            insert into live_workflow_step values (lws, false, false, null, 99, null, null, null, sr.sign_type, 1);
            insert into live_workflow_step_recipients values (lws, r);
            lw = nextval('hibernate_sequence');
            insert into live_workflow values (lw, null, sr.create_date, null, null, null, false, null, null, null, false, null, null, null, null, null, 1, lws);
            insert into live_workflow_workflow_steps values (lw, lws, 0);
            sb = nextval('hibernate_sequence');
            insert into sign_book values (sb, null, sr.create_date, null, null, null, null, false, concat('Signature_simple_', sr.title, '_', sr.id), sr.status, null, null, null, 1, null, sr.create_by_id, lw, sr.title, null, lw);
            insert into sign_book_sign_requests values (sb, sr.id, 0);
            update sign_request set parent_sign_book_id = sb where id = sr.id;
        END LOOP;

    FOR w IN SELECT * FROM workflow
        LOOP
            for ws in select * from workflow_step inner join workflow_workflow_steps on workflow_step.id = workflow_workflow_steps.workflow_steps_id and workflow_id = w.id
                Loop
                    for rec in select * from recipient inner join workflow_step_recipients wsr on recipient.id = wsr.recipients_id and wsr.workflow_step_id = ws.id
                        Loop
                            insert into workflow_step_users values (ws.id, rec.user_id);
                        end loop;
                end loop;
        END LOOP;
    RETURN;
END
$BODY$
    LANGUAGE plpgsql;

DO $$ BEGIN
    PERFORM migrate();
END $$;