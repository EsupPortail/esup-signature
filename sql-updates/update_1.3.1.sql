-- ajout de la colonne order sign_request_params

alter table sign_request_sign_request_params
    add column sign_request_params_order int4;

update sign_request_sign_request_params set sign_request_params_order = 0;

alter table sign_request_sign_request_params alter column sign_request_params_order set not null;