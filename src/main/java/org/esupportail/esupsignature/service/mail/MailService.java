package org.esupportail.esupsignature.service.mail;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.mail.MailConfig;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.esupportail.esupsignature.entity.Otp;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final GlobalProperties globalProperties;

    private final MailConfig mailConfig;

    public MailService(GlobalProperties globalProperties, @Autowired(required = false) MailConfig mailConfig, @Autowired(required = false) JavaMailSenderImpl mailSender, TemplateEngine templateEngine) {
        this.globalProperties = globalProperties;
        this.mailConfig = mailConfig;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    private final JavaMailSenderImpl mailSender;

//    @Autowired(required = false)
//    public void setMailSender(JavaMailSenderImpl mailSender) {
//        this.mailSender = mailSender;
//    }

    private final TemplateEngine templateEngine;

    @Resource
    private UserService userService;

//    @Resource
//    private CertificatService certificatService;

    @Resource
    private FileService fileService;

    @Resource
    private MessageSource messageSource;

    @Resource
    private UserShareService userShareService;

    public void sendEmailAlerts(SignBook signBook, String userEppn, Data data, boolean forceSend) throws EsupSignatureMailException {
        for (Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
            User recipientUser = recipient.getUser();
            if (!UserType.external.equals(recipientUser.getUserType())
                    && (!recipientUser.getEppn().equals(userEppn) || forceSend)
                    && (recipientUser.getEmailAlertFrequency() == null
                    || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately)
                    || userService.checkEmailAlert(recipientUser))) {
                sendSignRequestEmailAlert(signBook, recipientUser, data);
            }
        }
    }

    public void sendSignRequestEmailAlert(SignBook signBook, User recipientUser, Data data) throws EsupSignatureMailException {
        Date date = new Date();
        Set<String> toEmails = new HashSet<>();
        toEmails.add(recipientUser.getEmail());
        Workflow workflow = signBook.getLiveWorkflow().getWorkflow();
        recipientUser.setLastSendAlertDate(date);
        if(data != null && data.getForm() != null) {
            for (UserShare userShare : userShareService.getUserSharesByUser(recipientUser.getEppn())) {
                if (userShare.getShareTypes().contains(ShareType.sign)) {
                    if ((data.getForm().equals(userShare.getForm())) || (workflow != null && workflow.equals(userShare.getWorkflow()))) {
                        for (User toUser : userShare.getToUsers()) {
                            toEmails.add(toUser.getEmail());
                        }
                    }
                }
            }
        }
        sendSignRequestAlert(new ArrayList<>(toEmails), signBook);
    }

    public void sendCompletedMail(SignBook signBook, String userEppn) throws EsupSignatureMailException {
        User user = userService.getByEppn(userEppn);
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        Set<String> toEmails = new HashSet<>();
        if(!signBook.getCreateBy().getEppn().equals("system")) toEmails.add(signBook.getCreateBy().getEmail());
        toEmails.remove(user.getEmail());
        if(!toEmails.isEmpty()) {
            try {
                MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
                String htmlContent = templateEngine.process("mail/email-completed.html", ctx);
                addInLineImages(mimeMessage, htmlContent);
                mimeMessage.setSubject("Votre demande de signature est terminée");
                mimeMessage.setTo(toEmails.toArray(String[]::new));
                logger.info("send email completed to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
                sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
            } catch (MailSendException | MessagingException e) {
                logger.error("unable to send COMPLETE email", e);
                throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
            }
        }
    }

    public void sendPostit(SignBook signBook, Comment comment) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("comment", comment);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        Set<String> toEmails = new HashSet<>();
        if(!signBook.getCreateBy().getEppn().equals("system")) toEmails.add(signBook.getCreateBy().getEmail());
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-postit.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Un postit a été déposé sur votre demande");
            mimeMessage.setTo(toEmails.toArray(String[]::new));
            logger.info("send postit to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
            sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
        } catch (MailSendException | MessagingException e) {
            logger.error("unable to send COMPLETE email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    @Transactional
    public void sendCompletedCCMail(SignBook signBook) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        User user = signBook.getCreateBy();
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-completed-cc.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Une demande de signature que vous suivez est terminée");
            List<User> viewersArray = getViewers(signBook);
            if (!viewersArray.isEmpty()) {
                String[] to = new String[viewersArray.size()];
                int i = 0;
                for (User userTo : viewersArray) {
                    to[i] = userTo.getEmail();
                    i++;
                }
                mimeMessage.setTo(to);
                logger.info("send email completes cc for " + user.getEppn());
                sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
            } else {
                logger.debug("no viewers to send mail");
            }
        } catch (MailSendException | MessagingException e) {
            logger.error("unable to send email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    @NotNull
    private List<User> getViewers(SignBook signBook) {
        List<User> viewersArray = new ArrayList<>(signBook.getViewers());
        if(signBook.getLiveWorkflow().getWorkflow() != null && signBook.getLiveWorkflow().getWorkflow().getSendAlertToAllRecipients() != null && signBook.getLiveWorkflow().getWorkflow().getSendAlertToAllRecipients()) {
            List<LiveWorkflowStep> liveWorkflowSteps = signBook.getLiveWorkflow().getLiveWorkflowSteps();
            for (LiveWorkflowStep liveWorkflowStep : liveWorkflowSteps) {
                List<Recipient> recipients = liveWorkflowStep.getRecipients();
                for (Recipient recipient : recipients) {
                    User user1 = userService.getById(recipient.getUser().getId());
                    if (!viewersArray.contains(user1)) {
                        viewersArray.add(user1);
                    }
                }
            }
        }
        if(!signBook.getLiveWorkflow().getLiveWorkflowSteps().isEmpty()) {
            viewersArray.removeAll(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() - 1).getRecipients().stream().filter(Recipient::getSigned).map(Recipient::getUser).toList());
            return viewersArray;
        } else {
            return new ArrayList<>();
        }
    }

    public void sendRefusedMail(SignBook signBook, String comment, String userEppn) throws EsupSignatureMailException {
        User user = userService.getByEppn(userEppn);
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        ctx.setVariable("comment", comment);
        ctx.setVariable("user", user);
        setTemplate(ctx);
        Set<String> toEmails = new HashSet<>();
        if(!signBook.getCreateBy().getEppn().equals("system")) toEmails.add(signBook.getCreateBy().getEmail());
        for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            for(User toUser : liveWorkflowStep.getUsers()) {
                if(!user.getEppn().equals(toUser.getEppn())) {
                    toEmails.add(toUser.getEmail());
                }
            }
            if(liveWorkflowStep.equals(signBook.getLiveWorkflow().getCurrentStep())) break;
        }
        if(toEmails.isEmpty()) return;
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-refused.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Votre demande de signature a été refusée");
            mimeMessage.setTo(toEmails.toArray(String[]::new));
            String[] viewersArray = new String[signBook.getViewers().size()];
            for (int i = 0 ;  i < signBook.getViewers().size() ; i++) {
                viewersArray[i] = new ArrayList<>(signBook.getViewers()).get(i).getEmail();
            }
            mimeMessage.setCc(viewersArray);
            logger.info("send email refused to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
            sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
        } catch (MessagingException e) {
            logger.error("unable to send REFUSE email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    public void sendSignRequestAlert(List<String> recipientsEmails, SignBook signBook) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signBook.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-alert.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Vous avez une nouvelle demande de signature");
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email alert for " + recipientsEmails.get(0));
//            sendMail(signMessage(mimeMessage.getMimeMessage()));
            sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
            signBook.setLastNotifDate(new Date());
        } catch (Exception e) {
            logger.error("unable to send ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    public void sendSignRequestReplayAlert(List<String> recipientsEmails, SignBook signBook) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signBook.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-replay-alert.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Relance pour la signature d'un document");
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email replay alert for " + recipientsEmails.get(0));
            sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
            signBook.setLastNotifDate(new Date());
        } catch (MessagingException e) {
            logger.error("unable to send ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendCCAlert(SignBook signBook, List<String> recipientsCCEmails) throws EsupSignatureMailException {
        if(recipientsCCEmails == null) {
            recipientsCCEmails = signBook.getViewers().stream().map(User::getEmail).toList();
        }
        if (!checkMailSender() || recipientsCCEmails.isEmpty()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signBook.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-cc.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            User creator = signBook.getCreateBy();
            mimeMessage.setSubject("Vous êtes en copie d'une demande de signature crée par " + creator.getFirstname() + " " + creator.getName());
            mimeMessage.setTo(recipientsCCEmails.toArray(String[]::new));
            logger.info("send email cc for " + String.join(";", recipientsCCEmails));
            sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
        } catch (MessagingException e) {
            logger.error("unable to send CC ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendSignRequestSummaryAlert(List<String> recipientsEmails, List<SignRequest> signRequests) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("signRequests", signRequests);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-alert-summary.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Liste des demandes à signer");
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            mimeMessage.setText(htmlContent, true);
            logger.info("send email alert for " + recipientsEmails.get(0));
            sendMail(mimeMessage.getMimeMessage(), null);
        } catch (MessagingException e) {
            logger.error("unable to send SUMMARY email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }

    }

    public void sendOtp(Otp otp, String urlId, SignBook signBook) throws EsupSignatureMailException {
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("url", globalProperties.getRootUrl() + "/otp-access/first/" + urlId);
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("userService", userService);
        setTemplate(ctx);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-otp.html", ctx);
            addInLineImages(mimeMessage, htmlContent);
            mimeMessage.setSubject("Vous avez un document à signer émanant de " + messageSource.getMessage("application.footer", null, Locale.FRENCH));
            mimeMessage.setTo(otp.getUser().getEmail());
            logger.info("send email alert for " + otp.getUser().getEmail());
            sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());
        } catch (MessagingException e) {
            logger.error("unable to send OTP email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    public void sendFile(String title, SignBook signBook, String targetUri) throws MessagingException, IOException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("signRequests", signBook.getSignRequests());
        User user = signBook.getCreateBy();
        ctx.setVariable("user", user);
        setTemplate(ctx);
        MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
        String htmlContent = templateEngine.process("mail/email-file.html", ctx);
        addInLineImages(mimeMessage, htmlContent);
        mimeMessage.setSubject("Nouveau document signé à télécharger : " + title);
        mimeMessage.setTo(targetUri.replace("mailto:", "").split(","));
        sendMail(mimeMessage.getMimeMessage(), signBook.getLiveWorkflow().getWorkflow());

    }

    private void addInLineImages(MimeMessageHelper mimeMessage, String htmlContent) throws MessagingException {
        mimeMessage.setText(htmlContent, true);
        mimeMessage.addInline("logo", new ClassPathResource("/static/images/logo.png", MailService.class));
        mimeMessage.addInline("logo-univ", new ClassPathResource("/static/images/logo-univ.png", MailService.class));
    }

    public void sendTest(List<String> recipientsEmails) throws MessagingException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
        mimeMessage.setSubject("esup-signature test mail");
        mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
        String htmlContent = templateEngine.process("mail/email-test.html", ctx);
        mimeMessage.setText(htmlContent, true);
        logger.info("send test email for " + recipientsEmails.get(0));
        sendMail(mimeMessage.getMimeMessage(), null);
    }

    private void sendMail(MimeMessage mimeMessage, Workflow workflow) {
        if(workflow != null && BooleanUtils.isTrue(workflow.getDisableEmailAlerts())) {
            logger.debug("email alerts are disabled for this workflow " + workflow.getName());
            return;
        }
        try {
            mimeMessage.setFrom(mailConfig.getMailFrom());
            if(workflow != null && org.springframework.util.StringUtils.hasText(workflow.getMailFrom())) {
                mimeMessage.setFrom(workflow.getMailFrom());
            }
            String[] toHeader =  mimeMessage.getHeader("To");
            List<String> tos = new ArrayList<>();
            if(org.springframework.util.StringUtils.hasText(globalProperties.getTestEmail())) {
                tos.add(globalProperties.getTestEmail());
            } else {
                for(String to : toHeader) {
                    if (!to.equals("system") && !to.equals("system@" + globalProperties.getDomain())) {
                        tos.add(to);
                    }
                }
            }
            if(!tos.isEmpty()) {
                mimeMessage.setHeader("To", String.join(",", tos));
                mailSender.send(mimeMessage);
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTemplate(Context ctx) {
        try {
            ctx.setVariable("logo", fileService.getBase64Image(new ClassPathResource("/static/images/logo.png", MailService.class).getInputStream(), "logo.png"));
            ctx.setVariable("logoUrn", fileService.getBase64Image(new ClassPathResource("/static/images/logo-univ.png", MailService.class).getInputStream(), "logo-univ.png"));
            try (Reader reader = new InputStreamReader(new ClassPathResource("/static/css/bootstrap.min.css", MailService.class).getInputStream(), UTF_8)) {
                ctx.setVariable("css", FileCopyUtils.copyToString(reader));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

        } catch (IOException e) {
            logger.error("unable to set template", e);
        }
    }

    private boolean checkMailSender() {
        if (mailSender == null) {
            logger.warn("message not sended : mail host not configured");
            return false;
        }
        return true;
    }

    public MailConfig getMailConfig() {
        return mailConfig;
    }

    public JavaMailSenderImpl getMailSender() {
        return mailSender;
    }

//    public MimeMessage signMessage(MimeMessage message) {
//        try {
//            if(globalProperties.getSignEmailWithSealCertificat()) {
//                AbstractKeyStoreTokenConnection tokenConnection = certificatService.getSealToken();
//                DSSPrivateKeyEntry dssPrivateKeyEntry = tokenConnection.getKeys().get(0);
//                X509Certificate x509Certificate = dssPrivateKeyEntry.getCertificate().getCertificate();
//                PrivateKey privateKey = certificatService.getSealPrivateKey();
//                SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
//                capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
//                capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
//                capabilities.addCapability(SMIMECapability.dES_CBC);
//                capabilities.addCapability(SMIMECapability.aES256_CBC);
//                ASN1EncodableVector attributes = new ASN1EncodableVector();
//                attributes.add(new SMIMECapabilitiesAttribute(capabilities));
//                IssuerAndSerialNumber issAndSer = new IssuerAndSerialNumber(new X500Name(x509Certificate.getIssuerX500Principal().getName()), x509Certificate.getSerialNumber());
//                attributes.add(new SMIMEEncryptionKeyPreferenceAttribute(issAndSer));
//                SMIMESignedGenerator signer = new SMIMESignedGenerator();
//                signer.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
//                        .setSignedAttributeGenerator(new AttributeTable(attributes))
//                        .build("SHA1withRSA", privateKey, x509Certificate));
//                List<X509Certificate> certList = new ArrayList<>();
//                certList.add(x509Certificate);
//                JcaCertStore jcaCertStore = new JcaCertStore(certList);
//                signer.addCertificates(jcaCertStore);
//                MimeMultipart mm = signer.generate(message);
//                message.setContent(mm, mm.getContentType());
//                message.saveChanges();
//            }
//            return message;
//        } catch (Exception e) {
//            logger.debug(e.getMessage(), e);
//        }
//        return message;
//    }

}
