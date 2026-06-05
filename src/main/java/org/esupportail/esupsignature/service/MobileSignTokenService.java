package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MobileSignTokenService {

    private static final Logger logger = LoggerFactory.getLogger(MobileSignTokenService.class);
    
    private static final long TOKEN_VALIDITY_MINUTES = 5;
    
    private final Map<String, MobileSignTokenData> tokenCache = new ConcurrentHashMap<>();
    
    private final UserService userService;
    private final DocumentService documentService;
    private final FileService fileService;

    public MobileSignTokenService(UserService userService,
                                  DocumentService documentService,
                                  FileService fileService) {
        this.userService = userService;
        this.documentService = documentService;
        this.fileService = fileService;
    }

    public String createToken(String userEppn) {
        tokenCache.entrySet().removeIf(entry -> entry.getValue().getUserEppn().equals(userEppn));

        String token = UUID.randomUUID().toString();
        Date expirationDate = new Date(System.currentTimeMillis() + TOKEN_VALIDITY_MINUTES * 60 * 1000);
        
        MobileSignTokenData tokenData = new MobileSignTokenData(token, userEppn, expirationDate);
        tokenCache.put(token, tokenData);
        
        logger.info("Created mobile sign token for user: {}", userEppn);
        
        return token;
    }

    public boolean validateToken(String token) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        return tokenData != null && tokenData.isValid();
    }

    public boolean tokenExists(String token) {
        return tokenCache.containsKey(token);
    }

    public boolean isTokenUsed(String token) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        return tokenData != null && tokenData.isUsed();
    }

    public boolean isTokenExpired(String token) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        return tokenData != null && tokenData.isExpired();
    }

    public boolean hasPendingSignaturePreview(String token) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        return tokenData != null && !tokenData.isUsed() && !tokenData.isExpired() && tokenData.hasPendingSignaturePreview();
    }

    public Long getPendingSignaturePreviewTimestamp(String token) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        return (tokenData != null) ? tokenData.getPendingSignaturePreviewTimestamp() : null;
    }

    public String getPendingSignaturePreview(String token) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        if (tokenData == null || tokenData.isUsed() || tokenData.isExpired()) {
            return null;
        }
        return tokenData.getPendingSignaturePreview();
    }

    public Date getTokenExpirationDate(String token) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        if (tokenData == null) {
            return null;
        }
        return new Date(tokenData.getExpirationDate().getTime());
    }

    public boolean saveSignaturePreview(String token, String signImageBase64) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        if (tokenData == null || !tokenData.isValid() || !StringUtils.hasText(signImageBase64)) {
            return false;
        }
        tokenData.setPendingSignaturePreview(signImageBase64);
        tokenData.setPendingSignaturePreviewTimestamp(System.currentTimeMillis());
        return true;
    }

    public void consumePendingSignaturePreview(String token, String userEppn, String signImageBase64) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        if (tokenData == null || !tokenData.isValid()) {
            return;
        }
        if (!StringUtils.hasText(userEppn) || !userEppn.equals(tokenData.getUserEppn())) {
            return;
        }
        if (!StringUtils.hasText(signImageBase64) || !signImageBase64.equals(tokenData.getPendingSignaturePreview())) {
            return;
        }

        tokenData.setUsed(true);
    }

    @Transactional
    public boolean saveSignatureAndMarkTokenUsed(String token, String signImageBase64) {
        MobileSignTokenData tokenData = tokenCache.get(token);
        if (tokenData == null || !tokenData.isValid() || !StringUtils.hasText(signImageBase64)) {
            return false;
        }
        try {
            String userEppn = tokenData.getUserEppn();
            User user = userService.getByEppn(userEppn);
            if (user == null) {
                return false;
            }
            user.getSignImages().add(documentService.createDocument(
                    fileService.base64Transparence(signImageBase64),
                    user,
                    user.getEppn() + "_sign.png",
                    "image/png"));
            if (user.getSignImages().size() == 1) {
                user.setDefaultSignImageNumber(999998);
            }
            tokenData.setPendingSignaturePreview(null);
            tokenData.setPendingSignaturePreviewTimestamp(null);
            tokenData.setUsed(true);
            logger.info("Saved mobile signature for user: {}", userEppn);
            return true;
        } catch (Exception e) {
            logger.error("Error saving mobile signature: ", e);
        }
        return false;
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredTokens() {
        try {
            tokenCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            logger.debug("Cleaned up expired mobile sign tokens. Cache size: {}", tokenCache.size());
        } catch (Exception e) {
            logger.error("Error cleaning up mobile sign tokens: ", e);
        }
    }

    private static class MobileSignTokenData {
        private final String token;
        private final String userEppn;
        private final Date expirationDate;
        private boolean used = false;
        private String pendingSignaturePreview;
        private Long pendingSignaturePreviewTimestamp;

        public MobileSignTokenData(String token, String userEppn, Date expirationDate) {
            this.token = token;
            this.userEppn = userEppn;
            this.expirationDate = expirationDate;
        }

        public String getToken() {
            return token;
        }

        public String getUserEppn() {
            return userEppn;
        }

        public Date getExpirationDate() {
            return expirationDate;
        }

        public boolean isUsed() {
            return used;
        }

        public void setUsed(boolean used) {
            this.used = used;
        }

        public String getPendingSignaturePreview() {
            return pendingSignaturePreview;
        }

        public void setPendingSignaturePreview(String pendingSignaturePreview) {
            this.pendingSignaturePreview = pendingSignaturePreview;
        }

        public Long getPendingSignaturePreviewTimestamp() {
            return pendingSignaturePreviewTimestamp;
        }

        public void setPendingSignaturePreviewTimestamp(Long pendingSignaturePreviewTimestamp) {
            this.pendingSignaturePreviewTimestamp = pendingSignaturePreviewTimestamp;
        }

        public boolean hasPendingSignaturePreview() {
            return StringUtils.hasText(pendingSignaturePreview);
        }

        public boolean isExpired() {
            return new Date().after(expirationDate);
        }

        public boolean isValid() {
            return !used && !isExpired();
        }
    }
}
