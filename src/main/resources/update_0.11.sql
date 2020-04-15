update sign_request set create_by_eppn = create_by;
update sign_request as sr set create_by_id = (select id from user_account as u where u.eppn = sr.create_by);

update sign_book set create_by_eppn = create_by;
update sign_book as sb set create_by_id = (select id from user_account as u where u.eppn = sb.create_by);

insert into user_account_sign_images (user_id, sign_images_id) (select id, sign_image_id from user_account where sign_image_id is not null);