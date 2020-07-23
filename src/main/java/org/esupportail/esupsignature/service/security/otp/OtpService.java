package org.esupportail.esupsignature.service.security.otp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bouncycastle.util.encoders.Hex;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.mail.MailService;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private static final Integer EXPIRE_MINS = 10;

    private static LoadingCache<String, Otp> otpCache;

    @Resource
    private UserService userService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private MailService mailService;

    public OtpService() {
        otpCache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRE_MINS, TimeUnit.MINUTES).removalListener(notification -> clearOTP((String) notification.getKey())).build(new CacheLoader<>() {
            public Otp load(String urlId) {
                return null;
            }
        });
    }

    public void generateOtpForSignRequest(SignRequest signRequest, String phoneNumber, String email, String name, String firstname) throws MessagingException {
        //TODO check email domain
        Otp otp = new Otp();
        otp.setCreateDate(new Data());
        otp.setPhoneNumber(phoneNumber);
        otp.setEmail(email);
        otp.setSignRequestId(signRequest.getId());
        String urlId = UUID.randomUUID().toString();
        mailService.sendOtp(otp, urlId);
        User user = userService.createUser(phoneNumber, name, firstname, email);
        userRepository.save(user);
        signRequestService.addRecipients(signRequest, user);
        otpCache.put(urlId, otp);
        logger.info("new url for otp : " + urlId);
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
            return builder.toString() + (1000 - randomGenerator.nextInt(999));
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
