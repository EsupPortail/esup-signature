-- supprimer les contrainte unique sur les sign_request_params

-- alter table sign_book_viewers drop constraint if exists uk_gt3duxi8s9hmjyrakbjrwlhpk;

alter table live_workflow_step_sign_request_params drop constraint if exists ???