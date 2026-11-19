package org.esupportail.esupsignature.config.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String from;
    private Subject subject = new Subject();

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public static class Subject {

        private String sharedRequest = "[recipient] a une nouvelle demande de signature";
        private String completed = "Votre demande de signature est terminée";
        private String postit = "Un postit a été déposé sur votre demande";
        private String completedCc = "Une demande de signature que vous suivez est terminée";
        private String refused = "Votre demande de signature a été refusée";
        private String newRequest = "Vous avez une nouvelle demande de signature";
        private String replay = "Relance pour la signature d'un document";
        private String cc = "Vous êtes en copie d'une demande de signature créée par [creator]";
        private String otpSignature = "Vous avez un document à signer émanant de [organization]";
        private String otpDownload = "Un document émanant de [organization] est disponible au téléchargement";
        private String otpReplay = "Relance pour la signature d'un document émanant de [organization]";

        public String getSharedRequest() {
            return sharedRequest;
        }

        public void setSharedRequest(String sharedRequest) {
            this.sharedRequest = sharedRequest;
        }

        public String getCompleted() {
            return completed;
        }

        public void setCompleted(String completed) {
            this.completed = completed;
        }

        public String getPostit() {
            return postit;
        }

        public void setPostit(String postit) {
            this.postit = postit;
        }

        public String getCompletedCc() {
            return completedCc;
        }

        public void setCompletedCc(String completedCc) {
            this.completedCc = completedCc;
        }

        public String getRefused() {
            return refused;
        }

        public void setRefused(String refused) {
            this.refused = refused;
        }

        public String getNewRequest() {
            return newRequest;
        }

        public void setNewRequest(String newRequest) {
            this.newRequest = newRequest;
        }

        public String getReplay() {
            return replay;
        }

        public void setReplay(String replay) {
            this.replay = replay;
        }

        public String getCc() {
            return cc;
        }

        public void setCc(String cc) {
            this.cc = cc;
        }

        public String getOtpSignature() {
            return otpSignature;
        }

        public void setOtpSignature(String otpSignature) {
            this.otpSignature = otpSignature;
        }

        public String getOtpDownload() {
            return otpDownload;
        }

        public void setOtpDownload(String otpDownload) {
            this.otpDownload = otpDownload;
        }

        public String getOtpReplay() {
            return otpReplay;
        }

        public void setOtpReplay(String otpReplay) {
            this.otpReplay = otpReplay;
        }
    }
}