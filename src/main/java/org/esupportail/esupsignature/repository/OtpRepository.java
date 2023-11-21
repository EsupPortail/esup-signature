package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Otp;
import org.springframework.data.repository.CrudRepository;

public interface OtpRepository extends CrudRepository<Otp, Long>  {

    Otp findByUrlId(String url);

}
