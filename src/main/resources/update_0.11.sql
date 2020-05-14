update sign_request set create_by_eppn = create_by;
update sign_request as sr set create_by_id = (select id from user_account as u where u.eppn = sr.create_by);

update sign_book set create_by_eppn = create_by;
update sign_book as sb set create_by_id = (select id from user_account as u where u.eppn = sb.create_by);

insert into user_account_sign_images (user_id, sign_images_id, sign_images_order) (select id, sign_image_id, 0 as order from user_account where sign_image_id is not null);

alter table sign_request_params add column sign_image_number int4;
update sign_request_params set sign_image_number = 0;
alter table sign_request_params alter column sign_image_number set not null;
alter table user_account drop column sign_image_id;
alter table user_account drop column last_sign_image_id;

alter table user_account_sign_images add column sign_images_order int4;
update user_account_sign_images set sign_images_order = 0;
alter table user_account_sign_images alter column sign_images_order set not null;