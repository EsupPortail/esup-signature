package org.esupportail.esupsignature.service.interfaces.certificat;

import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import org.esupportail.esupsignature.entity.User;

public interface CertificatGenerationService {

    Pkcs12SignatureToken generateTokenForUser(User user);

}
