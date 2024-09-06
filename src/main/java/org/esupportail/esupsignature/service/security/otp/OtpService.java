package org.esupportail.esupsignature.service.security.otp;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import org.bouncycastle.util.encoders.Hex;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.repository.OtpRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final SignBookRepository signBookRepository;

    private final OtpRepository otpRepository;

    private final MailService mailService;

    private final UserService userService;

    private final GlobalProperties globalProperties;

    private final SmsService smsService;

    private static LoadingCache<String, Otp> otpCache;

    public OtpService(SignBookRepository signBookRepository, OtpRepository otpRepository, MailService mailService, UserService userService, GlobalProperties globalProperties, @Autowired(required = false) SmsService smsService) {
        this.signBookRepository = signBookRepository;
        this.otpRepository = otpRepository;
        this.mailService = mailService;
        this.userService = userService;
        this.globalProperties = globalProperties;
        this.smsService = smsService;
        otpCache = CacheBuilder.newBuilder().expireAfterWrite(globalProperties.getOtpValidity(), TimeUnit.MINUTES).build(new CacheLoader<>() {
            public Otp load(@NotNull String key) {
                return null;
            }
        });
    }

    @Transactional
    public boolean generateOtpForSignRequest(Long id, Long extUserId, String phone) throws EsupSignatureMailException {
        User extUser = userService.getById(extUserId);
        if(extUser.getUserType().equals(UserType.external) && (!globalProperties.getSmsRequired() || smsService != null)) {
            SignBook signBook = signBookRepository.findById(id).get();
            Otp otp = new Otp();
            otp.setCreateDate(new Date());
            if(StringUtils.hasText(phone)) {
                otp.setPhoneNumber(phone);
            } else {
                otp.setPhoneNumber(extUser.getPhone());
            }
            otp.setUser(extUser);
            otp.setSignBook(signBook);
            otp.setForceSms(extUser.getForceSms() != null && extUser.getForceSms());
            String urlId = UUID.randomUUID().toString();
            otp.setUrlId(urlId);
            signBook.setLastOtp(urlId);
            removeOtpFromCache(extUser.getEppn());
            removeOtpFromCache(extUser.getEmail());
            otpCache.put(urlId, otp);
            if(StringUtils.hasText(phone)) {
                extUser.setPhone(PhoneNumberUtil.normalizeDiallableCharsOnly(phone));
            }
            otpRepository.save(otp);
            mailService.sendOtp(otp, urlId, signBook);
            logger.info("new url for otp : " + urlId);
            return true;
        } else {
            return false;
        }
    }

    public void removeOtpFromCache(String searchString) {
        for (Map.Entry<String, Otp> otpEntry : otpCache.asMap().entrySet()) {
            if(otpEntry.getValue().getUser().getEmail().equals(searchString) || (otpEntry.getValue().getPhoneNumber() != null && otpEntry.getValue().getPhoneNumber().equals(searchString))) {
                clearOTP(otpEntry.getKey());
            }
        }
    }

    public void deleteOtpBySignBookId(Long id) {
        for (Map.Entry<String, Otp> otpEntry : otpCache.asMap().entrySet()) {
            if(otpEntry.getValue().getSignBook().getId().equals(id)) {
                clearOTP(otpEntry.getKey());
            }
        }
    }

    public String generateOtpPassword(String urlId) {
        Otp otp = getOtpFromDatabase(urlId);
        String password = randomOtpPassword();
        otp.setPassword(hashPassword(password));
        otpCache.put(urlId, otp);
        logger.info("new password for otp " + urlId + " : " + password);
        return password;
    }

    public Otp getAndCheckOtpFromCache(String urlId){
        try{
            Otp otp = otpCache.get(urlId);
            return otp;
        }catch (Exception e){
            logger.warn("error on get otp : " + e.getMessage());
        }
        return null;
    }

    public Otp getOtpFromCache(String urlId){
        try{
            return otpCache.getUnchecked(urlId);
        }catch (Exception e){
            logger.warn("error on get otp : " + e.getMessage());
        }
        return null;
    }

    public Otp getOtpFromDatabase(String urlId){
        try{
            return otpRepository.findByUrlId(urlId);
        }catch (Exception e){
            logger.warn("error on get otp : " + e.getMessage());
        }
        return null;
    }

    public void clearOTP(String urlId){
        otpCache.invalidate(urlId);
        logger.info("disable url for otp : " + urlId);
    }

    public Boolean checkOtp(String urlId, String password) {
        Otp otp = getOtpFromCache(urlId);
        if(otp != null) {
            if (otp.getPassword().equals(hashPassword(password))) {
                return true;
            } else {
                otp.setTries(otp.getTries() + 1);
                if(otp.getTries() > 2) {
                    clearOTP(urlId);
                    return null;
                } else {
                    otp.setSmsSended(false);
                    return false;
                }
            }
        }
        return null;
    }

    private String randomOtpPassword() {
        try {
            SecureRandom randomGenerator = SecureRandom.getInstance("SHA1PRNG");
            HashSet<Integer> set = new HashSet<>();
            while(set.size() < 6 - 3) {
                set.add(randomGenerator.nextInt(9));
            }
            StringBuilder builder = new StringBuilder();
            for (Integer i : set) {
                builder.append(i);
            }
            return builder + Strings.padStart((1000 - randomGenerator.nextInt(999)) + "", 3, '0');
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

    @Transactional
    public void cleanEndedOtp(){
        List<Otp> otps = otpRepository.findBySignBookStatus(SignRequestStatus.completed);
        otps.addAll(otpRepository.findBySignBookStatus(SignRequestStatus.deleted));
        otps.addAll(otpRepository.findBySignBookStatus(SignRequestStatus.refused));
        otps.addAll(otpRepository.findBySignBookStatus(SignRequestStatus.exported));
        otps.addAll(otpRepository.findBySignBookStatus(SignRequestStatus.archived));
        for(Otp otp : otps) {
            clearOTP(otp.getUrlId());
            otpRepository.delete(otp);
        }
    }

}
