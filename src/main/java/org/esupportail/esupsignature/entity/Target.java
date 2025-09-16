package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
public class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    private String targetUri;

    private Boolean targetOk = false;

    private Boolean sendDocument = true;

    private Boolean sendReport = false;

    private Boolean sendAttachment = false;

    private Boolean sendZip = false;

    private Integer nbRetry = 0;

    public Boolean getTargetOk() {
        return targetOk;
    }

    public void setTargetOk(Boolean targetOk) {
        this.targetOk = targetOk;
    }

    public String getTargetUri() {
        return targetUri;
    }

    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getNbRetry() {
        if(nbRetry == null) return 0;
        return nbRetry;
    }

    public void setNbRetry(Integer nbRetry) {
        this.nbRetry = nbRetry;
    }

    public Boolean getSendDocument() {
        if(sendDocument == null) {
            return true;
        }
        return sendDocument;
    }

    public void setSendDocument(Boolean sendDocument) {
        this.sendDocument = sendDocument;
    }

    public Boolean getSendReport() {
        if(sendReport == null) {
            return false;
        }
        return sendReport;
    }

    public void setSendReport(Boolean sendReport) {
        this.sendReport = sendReport;
    }

    public Boolean getSendAttachment() {
        if(sendAttachment == null) {
            return false;
        }
        return sendAttachment;
    }

    public void setSendAttachment(Boolean sendAttachment) {
        this.sendAttachment = sendAttachment;
    }

    public Boolean getSendZip() {
        if(sendZip == null) {
            return false;
        }
        return sendZip;
    }

    public void setSendZip(Boolean sendZip) {
        this.sendZip = sendZip;
    }

    @JsonIgnore
    public String getProtectedTargetUri() {
        if(targetUri != null) {
            Pattern p = Pattern.compile("[^@]*:\\/\\/[^:]*:([^@]*)@.*?$");
            Matcher m = p.matcher(targetUri);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, m.group(0).replaceFirst(Pattern.quote(m.group(1)), "********"));
            }
            m.appendTail(sb);
            return sb.toString().replaceAll("\\?.*", "?...");
        }
        return "";
    }
}
