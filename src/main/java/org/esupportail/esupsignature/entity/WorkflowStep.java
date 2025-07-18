package org.esupportail.esupsignature.entity;

import jakarta.validation.constraints.NotNull;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;

import java.util.*;

@Entity
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    private String name;

    private String description;

    private Integer maxRecipients = 99;

    @Fetch(FetchMode.JOIN)
    @ManyToMany(cascade = CascadeType.DETACH)
    private Set<User> users = new HashSet<>();

    private Boolean changeable = false;

    private Boolean repeatable = false;

    @Enumerated(EnumType.STRING)
    private SignType repeatableSignType;

    private Boolean allSignToComplete = false;

    private Boolean attachmentAlert = false;

    private Boolean attachmentRequire = false;

    private Boolean multiSign = true;

    private Boolean singleSignWithAnnotation = false;

    private Boolean autoSign = false;

    private Boolean sealVisa = false;

    @ManyToOne
    private Certificat certificat;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SignType signType;

    @ManyToMany(cascade = CascadeType.DETACH)
    private List<SignRequestParams> signRequestParams = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SignLevel minSignLevel = SignLevel.simple;

    @Enumerated(EnumType.STRING)
    private SignLevel maxSignLevel = SignLevel.qualified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Boolean getAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

    public Boolean getAttachmentAlert() {
        return attachmentAlert;
    }

    public void setAttachmentAlert(Boolean attachmentAlert) {
        this.attachmentAlert = attachmentAlert;
    }

    public Boolean getAttachmentRequire() {
        return attachmentRequire;
    }

    public void setAttachmentRequire(Boolean attachmentRequire) {
        this.attachmentRequire = attachmentRequire;
    }

    public Boolean getMultiSign() {
        return Objects.requireNonNullElse(multiSign, true);
    }

    public void setMultiSign(Boolean multiSign) {
        this.multiSign = multiSign;
    }

    public Boolean getSingleSignWithAnnotation() {
        return Objects.requireNonNullElse(singleSignWithAnnotation, false);
    }

    public void setSingleSignWithAnnotation(Boolean singleSignWithAnnotation) {
        this.singleSignWithAnnotation = singleSignWithAnnotation;
    }

    public Boolean getAutoSign() {
        if(autoSign == null) {
            return false;
        }
        return autoSign;
    }

    public void setAutoSign(Boolean autoSign) {
        this.autoSign = autoSign;
    }

    public SignType getSignType() {
        return signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getChangeable() {
        if(changeable == null) {
            return false;
        }
        return changeable;
    }

    public void setChangeable(Boolean changeable) {
        this.changeable = changeable;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public SignType getRepeatableSignType() {
        return repeatableSignType;
    }

    public void setRepeatableSignType(SignType repeatebleSignType) {
        this.repeatableSignType = repeatebleSignType;
    }

    public Integer getMaxRecipients() {
        return maxRecipients;
    }

    public void setMaxRecipients(Integer maxRecipients) {
        this.maxRecipients = maxRecipients;
    }

    public List<SignRequestParams> getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(List<SignRequestParams> signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

    public Certificat getCertificat() {
        return certificat;
    }

    public void setCertificat(Certificat certificat) {
        this.certificat = certificat;
    }

    public SignLevel getMinSignLevel() {
        if(minSignLevel == null) return SignLevel.simple;
        return minSignLevel;
    }

    public void setMinSignLevel(SignLevel minSignLevel) {
        this.minSignLevel = minSignLevel;
    }

    public SignLevel getMaxSignLevel() {
        if(maxSignLevel == null) return SignLevel.qualified;
        return maxSignLevel;
    }

    public void setMaxSignLevel(SignLevel maxSignLevel) {
        this.maxSignLevel = maxSignLevel;
    }

    public Boolean getSealVisa() {
        if(sealVisa == null) return false;
        return sealVisa;
    }

    public void setSealVisa(Boolean sealVisa) {
        this.sealVisa = sealVisa;
    }
}
