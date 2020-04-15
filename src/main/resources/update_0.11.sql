update sign_request set create_by_eppn = create_by;
update sign_request as sr set create_by_id = (select id from user_account as u where u.eppn = sr.create_by);

update sign_book set create_by_eppn = create_by;
update sign_book as sb set create_by_id = (select id from user_account as u where u.eppn = sb.create_by);