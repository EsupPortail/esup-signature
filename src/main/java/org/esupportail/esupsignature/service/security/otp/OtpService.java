package org.esupportail.esupsignature.service.security.otp;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bouncycastle.util.encoders.Hex;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private static final Integer EXPIRE_MINS = 10;

    private static LoadingCache<String, Otp> otpCache;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private MailService mailService;

    public OtpService() {
        otpCache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRE_MINS, TimeUnit.MINUTES).build(new CacheLoader<>() {
            public Otp load(String urlId) {
                return null;
            }
        });
    }

    public void generateOtpForSignRequest(Long id, User extUser) throws MessagingException {
        SignRequest signRequest = signRequestService.getById(id);
        Otp otp = new Otp();
        otp.setCreateDate(new Data());
        otp.setPhoneNumber(extUser.getEppn());
        otp.setEmail(extUser.getEmail());
        otp.setSignRequestId(signRequest.getId());
        String urlId = UUID.randomUUID().toString();
        mailService.sendOtp(otp, urlId);
//        signRequestService.addRecipients(signRequest, extUser);
        removeOtpFromCache(extUser.getEppn());
        removeOtpFromCache(extUser.getEmail());
        otpCache.put(urlId, otp);
        logger.info("new url for otp : " + urlId);
    }

    public void removeOtpFromCache(String searchString) {
        for (Map.Entry<String, Otp> otpEntry : otpCache.asMap().entrySet()) {
            if(otpEntry.getValue().getEmail().equals(searchString) || otpEntry.getValue().getPhoneNumber().equals(searchString)) {
                clearOTP(otpEntry.getKey());
            }
        }
    }

    public String generateOtpPassword(String urlId) {
        Otp otp = getOtp(urlId);
        String password = randomOtpPassword(6);
        otp.setPassword(hashPassword(password));
        logger.info("new password for otp " + urlId + " : " + password);
        return password;
    }

    public Otp getOtp(String urlId){
        try{
            return otpCache.getUnchecked(urlId);
        }catch (Exception e){
            logger.error("error on get otp : " + e.getMessage());
            return null;
        }
    }

    public void clearOTP(String urlId){
        otpCache.invalidate(urlId);
        logger.info("disable url for otp : " + urlId);
    }

    public Boolean checkOtp(String urlId, String password) {
        Otp otp = getOtp(urlId);
        if(otp != null) {
            if (otp.getPassword().equals(hashPassword(password))) {
                return true;
            } else {
                otp.setTries(otp.getTries() + 1);
                if(otp.getTries() > 2) {
                    clearOTP(urlId);
                    return null;
                }
                return false;
            }
        }
        return null;
    }

    private String randomOtpPassword(int size) {
        try {
            SecureRandom randomGenerator = SecureRandom.getInstance("SHA1PRNG");
            HashSet<Integer> set = new HashSet<>();
            while(set.size() < size - 3) {
                set.add(randomGenerator.nextInt(9));
            }
            StringBuilder builder = new StringBuilder();
            for (Integer i : set) {
                builder.append(i);
            }
            return builder.toString() + Strings.padStart((1000 - randomGenerator.nextInt(999)) + "", 3, '0');
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private String hashPassword(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hash));
    }

}
