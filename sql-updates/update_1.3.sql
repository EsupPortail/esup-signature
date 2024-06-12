CREATE OR REPLACE FUNCTION migrate() returns void AS
$BODY$
DECLARE
    sr sign_request%rowtype;
    sbr sign_book%rowtype;
    ws workflow_step%rowtype;
    sbws sign_book_workflow_steps%rowtype;
    rec recipient%rowtype;
    w workflow%rowtype;
    lo public.log%rowtype;
    r bigint;
    lw bigint;
    lws bigint;
    sb bigint;
    a bigint;
    sd date;
BEGIN
    alter table sign_book drop constraint if exists fkdbg7t5ofub4l25xsrv7sevrtf;

    -- MIGRATE ORPHAN SIGNBOOK TO LIVEWORKFLOW AND LIVEWORKFLOWSTEP
    FOR sbr IN SELECT * FROM sign_book
        LOOP
            lw = nextval('hibernate_sequence');
            insert into live_workflow(id, create_date, documents_target_uri, target_type, update_by, update_date, version, current_step_id, workflow_id)
                values (lw, sbr.create_date, sbr.documents_target_uri, sbr.target_type, null, null, 1, null, sbr.workflow_id);
            FOR ws IN select * from workflow_step inner join sign_book_workflow_steps sbws2 on workflow_step.id = sbws2.workflow_steps_id and sbws2.sign_book_id = sbr.id
                LOOP
                    For sbws in select * from sign_book_workflow_steps where sign_book_workflow_steps.workflow_steps_id = ws.id
                        LOOP
                            lws = nextval('hibernate_sequence');
                            insert into live_workflow_step(id, all_sign_to_complete, sign_type, version)
                            values (lws, ws.all_sign_to_complete, ws.sign_type, ws.version);
                            for rec in select * from recipient inner join workflow_step_recipients on recipient.id = workflow_step_recipients.recipients_id and workflow_step_id = ws.id
                                Loop
                                    insert into live_workflow_step_recipients(live_workflow_step_id, recipients_id) values (lws, rec.id);
                                end loop;
                            if sbr.current_workflow_step_number - 1 = sbws.workflow_steps_order then
                                update live_workflow set current_step_id = lws where id = lw;
                            else
                                if sbr.current_workflow_step_number - 2 = sbws.workflow_steps_order then
                                    update live_workflow set current_step_id = lws where id = lw;
                                end if;
                            end if;

                            insert into live_workflow_workflow_steps(live_workflow_id, workflow_steps_id, workflow_steps_order) values (lw, lws, sbws.workflow_steps_order);
                        end loop;
                end loop;
            update sign_book set live_workflow_id = lw where id=sbr.id;
        END LOOP;

    -- MIGRATE ORPHAN SIGNREQUEST TO PARENT SIGNBOOK
    FOR sr IN SELECT * FROM sign_request
              WHERE parent_sign_book_id is null
        LOOP
            r = nextval('hibernate_sequence');
            IF sr.status = 'pending' THEN
                insert into recipient(id, parent_id, parent_type, signed, version, user_id) values (r, null, null, false, 1, sr.create_by_id);
            ELSE
                insert into recipient(id, parent_id, parent_type, signed, version, user_id) values (r, null, null, true, 1, sr.create_by_id);
            END IF;
            lws = nextval('hibernate_sequence');
            insert into live_workflow_step(id, all_sign_to_complete, sign_type, version) values (lws, false, sr.sign_type, 1);
            insert into live_workflow_step_recipients(live_workflow_step_id, recipients_id) values (lws, r);
            lw = nextval('hibernate_sequence');
            insert into live_workflow(id, create_date, documents_target_uri, target_type, update_by, update_date, version, current_step_id, workflow_id)
                values (lw, sr.create_date, null, null, null, null, 1, lws, null);
            insert into live_workflow_workflow_steps(live_workflow_id, workflow_steps_id, workflow_steps_order) values (lw, lws, 0);
            sb = nextval('hibernate_sequence');
            insert into sign_book(id, create_date, current_workflow_step_number, description, documents_target_uri, exported_documenturi, external, name, status, target_type, update_by, update_date, version, create_by_eppn, create_by_id, workflow_id, title, workflow_name, live_workflow_id)
                values (sb, sr.create_date, null, null, null, null, false, concat('Signature_simple_', sr.title, '_', sr.id), sr.status, null, null, null, 1, null, sr.create_by_id, lw, sr.title, null, lw);
            insert into sign_book_sign_requests(sign_book_id, sign_requests_id, sign_requests_order) values (sb, sr.id, 0);
            update sign_request set parent_sign_book_id = sb where id = sr.id;
        END LOOP;

    -- MIGRATE WORKFLOW RECIPIENT TO WORKFLOW USER
    FOR w IN SELECT * FROM workflow
        LOOP
            for ws in select * from workflow_step inner join workflow_workflow_steps on workflow_step.id = workflow_workflow_steps.workflow_steps_id and workflow_id = w.id
                Loop
                    for rec in select * from recipient inner join workflow_step_recipients wsr on recipient.id = wsr.recipients_id and wsr.workflow_step_id = ws.id
                        Loop
                            insert into workflow_step_users(workflow_step_id, users_id) values (ws.id, rec.user_id);
                        end loop;
                end loop;
        END LOOP;

    -- MIGRATE RECIPIENT TO SIGNREQUEST_RECIPIENTHASSIGNED
    FOR sr IN SELECT * FROM sign_request
        LOOP
            for rec in select * from recipient
                                         inner join live_workflow_step_recipients lwsr on recipient.id = lwsr.recipients_id
                                         inner join live_workflow_step l on lwsr.live_workflow_step_id = l.id
                                         inner join live_workflow_workflow_steps lwws on l.id = lwws.workflow_steps_id
                                         inner join live_workflow lw2 on l.id = lw2.current_step_id
                                         inner join sign_book s on lw2.id = s.live_workflow_id
                                         inner join sign_request sr2 on s.id = sr2.parent_sign_book_id and sr2.id = sr.id
                Loop
                    a = nextval('hibernate_sequence');
                    insert into action(id, action_type, date, version) values (a, 'none', null, 1);
                    IF rec.signed = true THEN
                        for lo in select * from public.log where sign_request_id = sr.id and final_status = 'completed'
                            loop
                                update action SET action_type = 'signed', date = lo.log_date where id = a;
                            end loop;
                    END IF;
                    insert into sign_request_recipient_has_signed(sign_request_id, recipient_has_signed_id, recipient_has_signed_key) values (sr.id, a, rec.id);
                end loop;
        end loop;
    RETURN;
END
$BODY$
    LANGUAGE plpgsql;

DO $$ BEGIN
    PERFORM migrate();
END $$;
