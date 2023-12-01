package org.esupportail.esupsignature.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@EnableConfigurationProperties({GlobalProperties.class, SignProperties.class})
public class SignWithService {

    @Resource
    private UserService userService;

    @Resource
    private DataService dataService;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private CertificatService certificatService;

    @Resource
    private final GlobalProperties globalProperties;

    private static LoadingCache<String, Boolean> sealCertOKCache;

    public SignWithService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
        sealCertOKCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<>() {
            @Override
            public @NotNull Boolean load(@NotNull String s) {
                return false;
            }
        });
    }

    @Transactional
    public List<SignWith> getAuthorizedSignWiths(String userEppn, SignRequest signRequest) {
        User user = userService.getByEppn(userEppn);
        List<SignWith> signWiths = new ArrayList<>(List.of(SignWith.values()));
        if(globalProperties.getDisableCertStorage() || user.getKeystore() == null) {
            signWiths.remove(SignWith.userCert);
        }
        if(!checkSealCertificat(userEppn, false)) {
            signWiths.remove(SignWith.sealCert);
        }
        if(certificatService.getCertificatByUser(user.getEppn()).isEmpty()) {
            signWiths.remove(SignWith.groupCert);
        }
        if(globalProperties.getOpenXPKIServerUrl() == null) {
            signWiths.remove(SignWith.openPkiCert);
        }
        if(signRequest.getCurrentSignType() != null) {
            signWiths.removeIf(signWith -> signWith.getValue() < signRequest.getCurrentSignType().getValue());
        }
        if(dataService.getBySignBook(signRequest.getParentSignBook()) != null && signRequestService.isMoreWorkflowStepAndNotAutoSign(signRequest.getParentSignBook())) {
            signWiths.removeIf(signWith -> signWith.getValue() > 2);
        }
        List<SignWith> toRemoveSignWiths = new ArrayList<>();
        for (SignWith signWith : signWiths) {
            List<SignType> signTypes = globalProperties.getAuthorizedSignTypes().stream().filter(s -> s.getValue() >= signWith.getValue()).toList();
            if(signTypes.isEmpty()) {
                toRemoveSignWiths.add(signWith);
            }
        }
        signWiths.removeAll(toRemoveSignWiths);
        signWiths.remove(SignWith.autoCert);
        return signWiths;
    }

    public boolean checkSealCertificat(String userEppn, boolean force) {
        if(Boolean.TRUE.equals(sealCertOKCache.getIfPresent("sealOK"))) return true;
        User user = userService.getByEppn(userEppn);
        if(user.getRoles().contains("ROLE_SEAL") &&
                StringUtils.hasText(globalProperties.getSealCertificatPin()) &&
                (
                    (StringUtils.hasText(globalProperties.getSealCertificatType()) && globalProperties.getSealCertificatType().equals("PKCS11") && StringUtils.hasText(globalProperties.getSealCertificatDriver()))
                    ||
                    (StringUtils.hasText(globalProperties.getSealCertificatType()) && globalProperties.getSealCertificatType().equals("OPENSC"))
                    ||
                    (StringUtils.hasText(globalProperties.getSealCertificatType()) && globalProperties.getSealCertificatType().equals("PKCS12") && StringUtils.hasText(globalProperties.getSealCertificatFile()))
                )
        ) {
            if(!force) return true;
            if(!certificatService.getSealCertificats().isEmpty()) {
                sealCertOKCache.put("sealOK", true);
                return true;
            } else {
                return false;
            }
        } else {
            return force;
        }
    }
}
