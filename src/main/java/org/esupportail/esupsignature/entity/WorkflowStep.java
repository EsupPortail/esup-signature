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

    /**
     * Nom de l’étape.
     */
    private String name;

    /**
     * Description de l’étape.
     */
    private String description;

    /**
     * Nombre maximal de destinataires autorisés pour cette étape (valeur par défaut : 99).
     */
    private Integer maxRecipients = 99;

    /**
     * Liste des utilisateurs assignés à cette étape.
     */
    @Fetch(FetchMode.JOIN)
    @ManyToMany(cascade = CascadeType.DETACH)
    private Set<User> users = new HashSet<>();

    /**
     * Indique si cette étape peut être modifiée par l’utilisateur.
     */
    private Boolean changeable = false;

    /**
     * Indique si cette étape peut donner lieu à une étape intermédiaire.
     */
    private Boolean repeatable = false;

    /**
     * Type de signature utilisé pour les étapes intermédiaires.
     */
    @Enumerated(EnumType.STRING)
    private SignType repeatableSignType;

    /**
     * Indique si toutes les signatures doivent être complétées pour terminer cette étape.
     */
    private Boolean allSignToComplete = false;

    /**
     * Indique si une alerte doit être déclenchée lorsqu’une pièce jointe est demandée.
     */
    private Boolean attachmentAlert = false;

    /**
     * Indique si une pièce jointe est requise pour cette étape.
     */
    private Boolean attachmentRequire = false;

    /**
     * Indique si plusieurs signatures sont autorisées pour cette étape (par défaut : true).
     */
    private Boolean multiSign = true;

    /**
     * Indique si une seule signature avec des annotations est autorisée.
     */
    private Boolean singleSignWithAnnotation = false;

    /**
     * Indique si cette étape est automatiquement signée.
     */
    private Boolean autoSign = false;

    /**
     * Indique si cette étape sera terminée par l’apposition du cachet de l’établissement.
     */
    private Boolean sealVisa = false;

    /**
     * Certificat à utiliser pour cette étape, si autoSign.
     */
    @ManyToOne
    private Certificat certificat;

    /**
     * Type de signature utilisé pour cette étape.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    private SignType signType;

    /**
     * Liste des paramètres des signatures visuelle pour cette étape.
     */
    @ManyToMany(cascade = CascadeType.DETACH)
    private List<SignRequestParams> signRequestParams = new ArrayList<>();

    /**
     * Niveau de signature minimal requis pour cette étape (valeur par défaut : simple).
     */
    @Enumerated(EnumType.STRING)
    private SignLevel minSignLevel = SignLevel.simple;

    /**
     * Niveau de signature maximal autorisé pour cette étape (valeur par défaut : qualifié).
     */
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
