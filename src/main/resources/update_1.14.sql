-- supprimer les contrainte unique sur les sign_request_params

alter table form drop constraint if exists uk_dmd9eaby8k6ff9nl1gqy92rud;
alter table live_workflow_step_sign_request_params drop constraint if exists uk_dr5qobtrwkn9s8776lq0ykw4a;
update sign_request_params set sign_document_number = 0;
alter table data_datas alter column datas TYPE text;
