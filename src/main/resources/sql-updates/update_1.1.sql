update user_account set user_type = 'ldap';
update user_account set user_type = 'system' where eppn = 'creator';
update user_account set user_type = 'system' where eppn = 'system';