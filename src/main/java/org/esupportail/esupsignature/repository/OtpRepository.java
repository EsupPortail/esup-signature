package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OtpRepository extends CrudRepository<Otp, Long>  {

    Otp findByUrlId(String url);
    List<Otp> findBySignBookStatus(SignRequestStatus status);

    List<Otp> findByUser(User user);

    List<Otp> findByUserAndSignBook(User user, SignBook signBook);
}
