drop table user_share_workflows;
drop table user_share_forms;

update user_account set email = 'creator' where eppn = 'creator';
update user_account set email = 'system' where eppn = 'system';
update sign_book set title = name;
