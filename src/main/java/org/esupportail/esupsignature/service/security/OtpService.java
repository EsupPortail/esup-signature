package org.esupportail.esupsignature.service.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.web.controller.IndexController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private static final Integer EXPIRE_MINS = 15;

    private static LoadingCache<String, Otp> otpCache;

    public OtpService() {
        otpCache = CacheBuilder.newBuilder().expireAfterWrite(EXPIRE_MINS, TimeUnit.MINUTES).build(new CacheLoader<>() {
            public Otp load(String urlId) {
                return null;
            }
        });
    }

    public void generateOtpForSignRequest(SignRequest signRequest, String phoneNumber) {
        Otp otp = new Otp();
        otp.setCreateDate(new Data());
        String password = randomOtpPassword(6);
        otp.setPassword(password);
        otp.setPhoneNumber(phoneNumber);
        otp.setSignRequestId(signRequest.getId());
        String urlId = UUID.randomUUID().toString();
        otpCache.put(urlId, otp);
        logger.info("new url for otp : " + urlId + " : " + password);
    }

    public Otp getOtp(String urlId){
        try{
            return otpCache.get(urlId);
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
            if (otp.getPassword().equals(password)) {
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
        SecureRandom randomGenerator = null;
        try {
            randomGenerator = SecureRandom.getInstance("SHA1PRNG");
            HashSet<Integer> set = new HashSet<>();
            while(set.size() < size) {
                set.add(randomGenerator.nextInt(9));
            }
            StringBuilder builder = new StringBuilder();
            for (Integer i : set) {
                builder.append(i);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

}
