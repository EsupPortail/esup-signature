CREATE OR REPLACE FUNCTION populateTeamField() returns void AS
$BODY$
DECLARE
    sb sign_book%rowtype;
    sbv sign_book_viewers%rowtype;
    sr sign_request%rowtype;
    rhs sign_request_recipient_has_signed%rowtype;
    lw live_workflow%rowtype;
    lws live_workflow_step%rowtype;
    r recipient%rowtype;
    test1 bigint;
    test2 bigint;
    test3 bigint;
    test4 bigint;
BEGIN
    for sb in select * from sign_book
        LOOP
            test1 = (select count(*) from sign_book_team where sign_book_id = sb.id and team_id = sb.create_by_id);
            if test1 = 0 then
                insert into sign_book_team(sign_book_id, team_id) values (sb.id, sb.create_by_id);
            end if;
            for sbv in select * from sign_book_viewers where sign_book_id = sb.id
                LOOP
                    test2 = (select count(*) from sign_book_team where sign_book_id = sb.id and team_id = sbv.viewers_id);
                    if test2 = 0 then
                        insert into sign_book_team(sign_book_id, team_id) values (sb.id, sbv.viewers_id);
                    end if;
                END LOOP;
            for sr in select * from sign_request where parent_sign_book_id = sb.id
                LOOP
                    for rhs in select * from sign_request_recipient_has_signed where sign_request_id = sr.id
                        LOOP
                            for r in select * from recipient where id = rhs.recipient_has_signed_key
                                LOOP
                                    test3 = (select count(*) from sign_book_team where sign_book_id = sb.id and team_id = r.user_id);
                                    if test3 = 0 then
                                        insert into sign_book_team(sign_book_id, team_id) values (sb.id, r.user_id);
                                    end if;
                            END LOOP;
                        END LOOP;
                END LOOP;
            for lw in select * from live_workflow where id = sb.live_workflow_id
                LOOP
                    for lws in select * from live_workflow_step where id in (select live_workflow_steps_id from live_workflow_live_workflow_steps where live_workflow_steps_id = lw.id)
                        LOOP
                            for r in select * from recipient where id in (select recipients_id from live_workflow_step_recipients where live_workflow_step_id = lws.id)
                            LOOP
                                test4 = (select count(*) from sign_book_team where sign_book_id = sb.id and team_id = r.user_id);
                                if test4 = 0 then
                                    insert into sign_book_team(sign_book_id, team_id) values (sb.id, r.user_id);
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
    PERFORM populateTeamField();
END $$;