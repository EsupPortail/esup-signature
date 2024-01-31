-- ajouter une signature favorite pour tout le monde

update user_account set default_sign_image_number = 0 where default_sign_image_number is null;
