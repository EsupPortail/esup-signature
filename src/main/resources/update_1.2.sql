update user_account set email_alert_hour = '12';
alter table user_account alter COLUMN email_alert_hour type integer USING email_alert_hour::integer;

update user_account set email = 'creator' where eppn = 'creator';
update user_account set email = 'system' where eppn = 'system';

update sign_book set title = name;

drop table user_share_workflows;
drop table user_share_forms;