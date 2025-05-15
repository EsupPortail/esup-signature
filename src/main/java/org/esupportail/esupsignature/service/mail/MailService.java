package org.esupportail.esupsignature.service.mail;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.transaction.Transactional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.mail.MailConfig;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureMailException;
import org.esupportail.esupsignature.service.ReportService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final GlobalProperties globalProperties;
    private final MailConfig mailConfig;
    private final JavaMailSenderImpl mailSender;
    private final TemplateEngine templateEngine;
    private final UserService userService;
    private final MessageSource messageSource;
    private final UserShareService userShareService;
    private final ReportService reportService;

    //    @Autowired(required = false)
//    public void setMailSender(JavaMailSenderImpl mailSender) {
//        this.mailSender = mailSender;
//    }

//    @Resource
//    private CertificatService certificatService;

    public MailService(GlobalProperties globalProperties, @Autowired(required = false) MailConfig mailConfig, @Autowired(required = false) JavaMailSenderImpl mailSender, TemplateEngine templateEngine, UserService userService, MessageSource messageSource, UserShareService userShareService, ReportService reportService) {
        this.globalProperties = globalProperties;
        this.mailConfig = mailConfig;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.userService = userService;
        this.messageSource = messageSource;
        this.userShareService = userShareService;
        this.reportService = reportService;
    }

    public void sendEmailAlerts(SignBook signBook, String userEppn, Data data, boolean forceSend) throws EsupSignatureMailException {
        for (Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
            User recipientUser = recipient.getUser();
            if (!UserType.external.equals(recipientUser.getUserType())
                    && (!recipientUser.getEppn().equals(userEppn) || forceSend)
                    && (recipientUser.getEmailAlertFrequency() == null
                    || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately)
                    || userService.checkEmailAlert(recipientUser))) {
                sendSignRequestAlert(Collections.singletonList(recipientUser.getEmail()), signBook);
            }
            sendSignRequestAlertsShare(signBook, recipientUser, data);
        }
    }

    public void sendSignRequestAlertsShare(SignBook signBook, User recipientUser, Data data) throws EsupSignatureMailException {
        Date date = new Date();
        Workflow workflow = signBook.getLiveWorkflow().getWorkflow();
        recipientUser.setLastSendAlertDate(date);
        Map<String, UserShare> toShareEmails = new HashMap<>();
        for (UserShare userShare : userShareService.getUserSharesByUser(recipientUser.getEppn())) {
            if (userShare.getShareTypes().contains(ShareType.sign) &&
                ((data != null && data.getForm() != null && userShare.getForm() != null && data.getForm().getId().equals(userShare.getForm().getId()))
                || (workflow != null && userShare.getWorkflow() != null &&  workflow.getId().equals(userShare.getWorkflow().getId()))
                || (userShare.getAllSignRequests()
                        && BooleanUtils.isTrue(userShare.getForceTransmitEmails()
                        && (userShare.getBeginDate() == null || userShare.getBeginDate().before(new Date()))
                        && (userShare.getEndDate() == null || userShare.getEndDate().after(new Date()))
                )))) {
                for (User toUser : userShare.getToUsers()) {
                    toShareEmails.put(toUser.getEmail(), userShare);
                }
            }
        }
        if(!toShareEmails.isEmpty()) {
            sendSignRequestAlertShare(new ArrayList<>(toShareEmails.keySet()), recipientUser, toShareEmails.values().stream().toList().get(0), signBook);
        }
    }

    private void sendSignRequestAlertShare(List<String> recipientsEmails, User recipientUser, UserShare userShare, SignBook signBook) {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);

        PersonLdap personLdap = userService.findPersonLdapByUser(signBook.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        ctx.setVariable("recipientUser", recipientUser);
        ctx.setVariable("userShare", userShare);
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        setTemplate(ctx, signBook);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-alert-share.html", ctx);
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setSubject(recipientUser.getFirstname() + " " + recipientUser.getName() + " a une nouvelle demande de signature");
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email alert for " + recipientsEmails.get(0));
            sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
            signBook.setLastNotifDate(new Date());
        } catch (Exception e) {
            logger.error("unable to send ALERT email share", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail delegation", e);
        }
    }

    public Set<String> sendCompletedMail(SignBook signBook, String userEppn) throws EsupSignatureMailException {
        User user = userService.getByEppn(userEppn);
        if (!checkMailSender()) {
            return null;
        }
        final Context ctx = new Context(Locale.FRENCH);
        setTemplate(ctx, signBook);
        Set<String> toEmails = new HashSet<>();
        if(!signBook.getCreateBy().getEppn().equals("system")) toEmails.add(signBook.getCreateBy().getEmail());
        toEmails.remove(user.getEmail());
        if(!toEmails.isEmpty()) {
            try {
                MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
                String htmlContent = templateEngine.process("mail/email-completed.html", ctx);
                mimeMessage.setText(htmlContent, true);
                mimeMessage.setSubject("Votre demande de signature est terminée");
                mimeMessage.setTo(toEmails.toArray(String[]::new));
                logger.info("send email completed to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
                sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
                return toEmails;
            } catch (MailSendException | MessagingException e) {
                logger.error("unable to send COMPLETE email", e);
                throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public void sendPostit(SignBook signBook, Comment comment, String userEppn, Boolean sendToAll) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        setTemplate(ctx, signBook);
        ctx.setVariable("comment", comment);
        Set<String> toEmails = new HashSet<>();
        if(!signBook.getCreateBy().getEppn().equals("system")) toEmails.add(signBook.getCreateBy().getEmail());
        if(BooleanUtils.isTrue(sendToAll)) {
            String systemAddress = "system@" + globalProperties.getDomain();
            toEmails.addAll(signBook.getTeam().stream().map(User::getEmail).toList().stream().filter(email -> !email.equals(systemAddress) && !email.equals("system")).toList());
        }
        User user = userService.getByEppn(userEppn);
        toEmails.removeIf(e -> e.equals(user.getEmail()));
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-postit.html", ctx);
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setSubject("Un postit a été déposé sur votre demande");
            mimeMessage.setTo(toEmails.toArray(String[]::new));
            logger.info("send postit to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
            sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
        } catch (MailSendException | MessagingException | IOException e) {
            logger.error("unable to send COMPLETE email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    @Transactional
    public void sendCompletedCCMail(SignBook signBook, String userEppn, Set<String> toMails) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        User user = signBook.getCreateBy();
        final Context ctx = new Context(Locale.FRENCH);
        setTemplate(ctx, signBook);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-completed-cc.html", ctx);
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setSubject("Une demande de signature que vous suivez est terminée");
            if (!signBook.getTeam().isEmpty()) {
                mimeMessage.setTo(signBook.getTeam().stream().filter(userTo -> !userTo.getUserType().equals(UserType.external) && (toMails == null || !toMails.contains(userTo.getEmail())) && !userTo.getUserType().equals(UserType.system) && !userTo.getEppn().equals(userEppn)).map(User::getEmail).toArray(String[]::new));
                logger.info("send email completes cc for " + user.getEppn());
                sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
            } else {
                logger.debug("no viewers to send mail");
            }
        } catch (MailSendException | MessagingException e) {
            logger.error("unable to send email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendRefusedMail(SignBook signBook, String comment, String userEppn) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("comment", comment);
        setTemplate(ctx, signBook);
        User user = userService.getByEppn(userEppn);
        ctx.setVariable("user", user);
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
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setSubject("Votre demande de signature a été refusée");
            mimeMessage.setTo(toEmails.toArray(String[]::new));
            String[] viewersArray = new String[signBook.getViewers().size()];
            for (int i = 0 ;  i < signBook.getViewers().size() ; i++) {
                viewersArray[i] = new ArrayList<>(signBook.getViewers()).get(i).getEmail();
            }
            mimeMessage.setCc(viewersArray);
            logger.info("send email refused to : " + StringUtils.join(toEmails.toArray(String[]::new), ";"));
            sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
        } catch (MessagingException e) {
            logger.error("unable to send REFUSE email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendSignRequestAlert(List<String> recipientsEmails, SignBook signBook) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        setTemplate(ctx, signBook);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-alert.html", ctx);
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setSubject("Vous avez une nouvelle demande de signature");
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email alert for " + recipientsEmails.get(0));
            sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
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
        setTemplate(ctx, signBook);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-replay-alert.html", ctx);
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setSubject("Relance pour la signature d'un document");
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            logger.info("send email replay alert for " + recipientsEmails.get(0));
            sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
            signBook.setLastNotifDate(new Date());
        } catch (MessagingException e) {
            logger.error("unable to send ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        setTemplate(ctx, signBook);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-cc.html", ctx);
            mimeMessage.setText(htmlContent, true);
            User creator = signBook.getCreateBy();
            mimeMessage.setSubject("Vous êtes en copie d'une demande de signature crée par " + creator.getFirstname() + " " + creator.getName());
            mimeMessage.setTo(recipientsCCEmails.toArray(String[]::new));
            logger.info("send email cc for " + String.join(";", recipientsCCEmails));
            sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
        } catch (MessagingException e) {
            logger.error("unable to send CC ALERT email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendSignRequestSummaryAlert(List<String> recipientsEmails, List<SignRequest> signRequests) throws EsupSignatureMailException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        setTemplate(ctx, signRequests.get(0).getParentSignBook());
        ctx.setVariable("rootUrl", globalProperties.getRootUrl());
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-alert-summary.html", ctx);
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setSubject("Liste des demandes à signer");
            mimeMessage.setTo(recipientsEmails.toArray(String[]::new));
            mimeMessage.setText(htmlContent, true);
            logger.info("send email summary for " + recipientsEmails.get(0));
            sendMail(mimeMessage, null);
        } catch (MessagingException e) {
            logger.error("unable to send SUMMARY email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendOtp(Otp otp, SignBook signBook, boolean signature) throws EsupSignatureMailException {
        final Context ctx = new Context(Locale.FRENCH);
        setTemplate(ctx, signBook);
        ctx.setVariable("url", globalProperties.getRootUrl() + "/otp-access/first/" + otp.getUrlId());
        ctx.setVariable("urlControl", globalProperties.getRootUrl() + "/public/control/" + signBook.getSignRequests().get(0).getToken());
        ctx.setVariable("otpValidity", new Date(otp.getCreateDate().getTime() + TimeUnit.MINUTES.toMillis(globalProperties.getOtpValidity())));
        ctx.setVariable("otp", otp);
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            String htmlContent = templateEngine.process("mail/email-otp-download.html", ctx);
            mimeMessage.setSubject("Un document émanant de " + messageSource.getMessage("application.footer", null, Locale.FRENCH) + " est disponible au téléchargement");
            if(signature) {
                htmlContent = templateEngine.process("mail/email-otp.html", ctx);
                mimeMessage.setSubject("Vous avez un document à signer émanant de " + messageSource.getMessage("application.footer", null, Locale.FRENCH));
                logger.info("send signature email otp for " + otp.getUser().getEmail());
            } else {
                logger.info("send download email otp for " + otp.getUser().getEmail());
            }
            mimeMessage.setText(htmlContent, true);
            mimeMessage.setTo(otp.getUser().getEmail());
            sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());
        } catch (MessagingException | IOException e) {
            logger.error("unable to send OTP email", e);
            throw new EsupSignatureMailException("Problème lors de l'envoi du mail", e);
        }
    }

    public void sendFile(String title, SignBook signBook, String targetUri, boolean sendDocument, boolean sendReport) throws MessagingException, IOException {
        if (!checkMailSender()) {
            return;
        }
        final Context ctx = new Context(Locale.FRENCH);
        setTemplate(ctx, signBook);
        MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
        String htmlContent = templateEngine.process("mail/email-file.html", ctx);
        mimeMessage.setText(htmlContent, true);
        mimeMessage.setSubject("Un document signé vous est transmit : " + title);
        mimeMessage.setTo(targetUri.replace("mailto:", "").split(","));
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(sendDocument) {
                Document toSendDocument = signRequest.getLastSignedDocument();
                mimeMessage.addAttachment(toSendDocument.getFileName(), new ByteArrayResource(IOUtils.toByteArray(toSendDocument.getInputStream())));
            }
            if(sendReport) {
                mimeMessage.addAttachment(signBook.getSubject() + "-report.zip", new ByteArrayResource(reportService.getReportBytes(signRequest)));
            }
        }
        sendMail(mimeMessage, signBook.getLiveWorkflow().getWorkflow());

    }

    public void sendTest(List<String> recipientsEmails) throws MessagingException, IOException {
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
        sendMail(mimeMessage, null);
    }

    private void sendMail(MimeMessageHelper mimeMessageHelper, Workflow workflow) throws MessagingException, IOException {
        mimeMessageHelper.addInline("logo", resizeImage(new ClassPathResource("/static/images/logo.png", MailService.class).getInputStream(), 30));
        mimeMessageHelper.addInline("logo-univ", resizeImage(new ClassPathResource("/static/images/logo-univ.png", MailService.class).getInputStream(), 30));
        mimeMessageHelper.addInline("logo-file", new ClassPathResource("/static/images/fa-file.png", MailService.class));
        if(workflow != null && BooleanUtils.isTrue(workflow.getDisableEmailAlerts())) {
            logger.debug("email alerts are disabled for this workflow " + workflow.getName());
            return;
        }
        try {
            String systemAddress = "system@" + globalProperties.getDomain();
            InternetAddress[] tosArray = (InternetAddress[]) mimeMessageHelper.getMimeMessage().getRecipients(Message.RecipientType.TO);
            if (tosArray != null) {
                List<InternetAddress> tos = Arrays.stream(tosArray).filter(addr -> !"system".equalsIgnoreCase(addr.getAddress()) && !systemAddress.equalsIgnoreCase(addr.getAddress())).toList();
                logger.info("send email to : " + String.join(",", tos.stream().map(Address::toString).toList()));
                mimeMessageHelper.getMimeMessage().setFrom(mailConfig.getMailFrom());
                InternetAddress replyToAddress = new InternetAddress(mailConfig.getMailFrom());
                if (workflow != null && org.springframework.util.StringUtils.hasText(workflow.getMailFrom())) {
                    replyToAddress = new InternetAddress(workflow.getMailFrom());
                }
                mimeMessageHelper.getMimeMessage().setReplyTo(new Address[]{replyToAddress});

                if (org.springframework.util.StringUtils.hasText(globalProperties.getTestEmail())) {
                    tos = new ArrayList<>();
                    tos.add(new InternetAddress(globalProperties.getTestEmail()));
                }
                if (!tos.isEmpty()) {
                    mimeMessageHelper.setTo(tos.toArray(new InternetAddress[0]));
                    mailSender.send(mimeMessageHelper.getMimeMessage());
                }
            }
        } catch(MessagingException e){
            if (!(e instanceof SMTPAddressFailedException)) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setTemplate(Context ctx, SignBook signBook) {
        ctx.setVariable("user", signBook.getCreateBy());
        ctx.setVariable("url", globalProperties.getRootUrl() + "/user/signbooks/"+ signBook.getId());
        ctx.setVariable("urlControl", globalProperties.getRootUrl() + "/public/control/" + signBook.getSignRequests().get(0).getToken());
        ctx.setVariable("signBook", signBook);
        ctx.setVariable("signRequests", signBook.getSignRequests());
        PersonLdap personLdap = userService.findPersonLdapByUser(signBook.getCreateBy());
        if(personLdap != null) {
            OrganizationalUnitLdap organizationalUnitLdap = userService.findOrganizationalUnitLdapByPersonLdap(personLdap);
            ctx.setVariable("organizationalUnitLdap", organizationalUnitLdap);
        }
        try (Reader reader = new InputStreamReader(new ClassPathResource("/static/css/bootstrap.min.css", MailService.class).getInputStream(), UTF_8)) {
            ctx.setVariable("css", FileCopyUtils.copyToString(reader));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public ByteArrayDataSource resizeImage(InputStream inputStream, int targetHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double ratio = (double) targetHeight / originalHeight;
        int targetWidth = (int) (originalWidth * ratio);
        Image scaledImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "PNG", byteArrayOutputStream);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return new ByteArrayDataSource(byteArrayInputStream.readAllBytes(), "image/png");
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

    public void sendAdminError(String message, String trace) {
        try {
            MimeMessageHelper mimeMessage = new MimeMessageHelper(getMailSender().createMimeMessage(), true, "UTF-8");
            mimeMessage.setTo(globalProperties.getApplicationEmail());
            mimeMessage.setSubject("esup-signature : " + message);
            mimeMessage.setText(trace, false);
            sendMail(mimeMessage, null);
        } catch (MessagingException e) {
            logger.error("unable to send ADMIN ERROR email", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
