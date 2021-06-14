-- supprimer les contrainte unique sur les sign_request_params

alter table live_workflow_step_sign_request_params drop constraint if exists uk_dr5qobtrwkn9s8776lq0ykw4a;
