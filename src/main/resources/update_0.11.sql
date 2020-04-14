update sign_request set create_by_eppn = create_by;
update sign_request as sr set create_by_id = (select id from user_account as u where u.eppn = sr.create_by);