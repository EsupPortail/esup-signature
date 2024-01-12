package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.entity.enums.SignType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

//@Configuration
@ConfigurationProperties(prefix="global")
public class GlobalProperties {

    /**
     * Chemin d’accès à l’application
     */
    private String rootUrl;
    /**
     * Nom de domaine ex : univ-ville.fr
     */
    private String domain;
    /**
     * Activer ou non l’archivage et le nettoyage automatique. false par défaut
     */
    private Boolean enableScheduledCleanup = false;
    /**
     * Chemin d’écoute d’Esup-DSS-Client
     */
    private String nexuUrl = "http://localhost:9795";
    /**
     * Masquer la tuile Créer une demande personnalisée
     */
    private Boolean hideWizard;
    /**
     * Masquer la tuile Créer une demande personnalisée
     */
    private Boolean hideWizardWorkflow = true;
    /**
     * Masquer la tuile Auto-signature
     */
    private Boolean hideAutoSign;
    /**
     * Masquer la tuile Demander une signature
     */
    private Boolean hideSendSignRequest;
    /**
     * Liste des roles faisant exception à la valeur de hideWizard
     */
    private List<String> hideWizardExceptRoles = new ArrayList<>();
    /**
     * Liste des roles faisant exception à la valeur de hideAutoSign
     */
    private List<String> hideAutoSignExceptRoles = new ArrayList<>();
    /**
     * Liste des roles faisant exception à la valeur de hideSendSignRequest
     */
    private List<String> hideSendSignExceptRoles = new ArrayList<>();
    /**
     * Les documents des demandes terminées seront archivées vers ce dossier
     */
    private String archiveUri;
    /**
     * Délai en nombre de jours avant que les documents des demandes archivées ne soient effacés de la base (-1 non actif)
     */
    private Integer delayBeforeCleaning = -1;
    /**
     * Délai de conservation dans la corbeille en jours (-1 non actif)
     */
    private Integer trashKeepDelay = -1;
    /**
     * Activer la fonction Switch User pour les administrateurs
     */
    private Boolean enableSu = false;
    /**
     * Activer le message d’accueil pour les nouveaux utilisateurs
     */
    private Boolean enableSplash = false;
    /**
     * Géré automatiquement, ne pas modifier !
     */
    private String version = "";
    /**
     * Adresse email du contact technique de l’application
     */
    private String applicationEmail = "esup.signature@univ-ville.fr";
    /**
     * Nombre d'heure minimum entre deux relances manuelles
     */
    private Integer hoursBeforeRefreshNotif = 24;
    /**
     * Activer le scrolling infini sur le tableau de bord (sinon pagination)
     */
    private Boolean infiniteScrolling = true;
    /**
     * Redirection après signature. true : retour à l'accueil, false : on reste sur la demande
     */
    private Boolean returnToHomeAfterSign = true;

    /**
     *Le modèle est construit à l'aide d'attributs entre crochets.
     *default : [title]
     *Les attributs disponibles sont :
     *<ul>
     *  <li>[title] : titre du document original</li>
     *  <li>[id] : identifiant du parapheur</li>
     *  <li>[worflowName] : nom du circuit</li>
     *  <li>[user.name] : nom prénom de l'utilisateur courant</li>
     *  <li>[user.eppn] : eppn de l'utilisateur courant</li>
     *  <li>[user.initials] : initiales de l'utilisateur courant</li>
     *  <li>[UUID] : un identifiant unique</li>
     *  <li>[order] : le numéro d'ordre de création pour un même circuit</li>
     *  <li>[timestamp] : timestamp sous forme de long</li>
     *  <li>[date-fr] : date dd/MM/yyyy hh:mm</li>
     *  <li>[date-en] : date yyyy-MM-dd hh:mm</li>
     *</ul>
     */
    private String namingTemplate = "[title]";
    /**
     * Suffix ajouté aux documents signés
     */
    private String signedSuffix = "_signed";
    /**
     * Choisir le fonctionnement des délégations :
     *  <ul>
     *      <li>0 : système de délégation désactivé</li>
     *      <li>1 : le délégué ne peut signer qu'avec sa propre signature</li>
     *      <li>2 : le délégué ne peut signer qu'avec la signature du mandant</li>
     *      <li>3 : le mandant peut choisir la signature du délégué</li>
     *  </ul>
     */
    private Integer shareMode = 0;
    /**
     * Activer/Désactiver la possibilité de stocker des certificats utilisateurs
     */
    private Boolean disableCertStorage = false;
    /**
     * Activer/Désactiver la detection de robot à la connexion
     */
    private Boolean enableCaptcha = false;
    /**
     * Taille maximum des uploads de fichiers en bytes
     */
    private Integer maxUploadSize = 52428800;
    /**
     * Nombre de jours avant alerte de suppression pour les demandes en attente (-1 non actif)
     */
    private Integer nbDaysBeforeWarning = -1;
    /**
     * Nombre de jours après alerte pour suppression des demandes en attente (-1 non actif)
     */
    private Integer nbDaysBeforeDeleting = -1;

    /**
     *  Url du serveur openXPKI
     */
    private String openXPKIServerUrl;

    /**
     *  Lancer automatiquement les mises à jour au démarrage
     */
    private Boolean autoUpgrade = true;

    /**
     *  Upload des PDF seuls
     */
    private Boolean pdfOnly = false;

    /**
     * Exporter les pièces jointes (si actif, l'export sera un dossier contenant le document signé ainsi que les PJ)
     */
    public Boolean exportAttachements = true;

    /**
     *  Type de certificat cachet (PKCS11, PKCS12, OPENSC)
     */
    private TokenType sealCertificatType;

    public enum TokenType {
        PKCS11, PKCS12, OPENSC
    }

    /**
     *  Emplacement du certificat cachet (actif pour PKCS12)
     */
    private String sealCertificatFile;

    /**
     *  Pilote du certificat cachet
     */
    private String sealCertificatDriver;

    /**
     *  Pin du certificat cachet
     */
    private String sealCertificatPin = "";

    /**
     *  Pin du certificat cachet
     */
    private Boolean signEmailWithSealCertificat = false;

    /**
     *  Appliquer le cachet sur toutes les demandes terminées
     */
    private Boolean sealAllDocs = false;

    /**
     *  Whitelist des domaines authorisés à obtenir le ROLE_USER pour les connexions Shibboleth
     */
    private List<String> shibUsersDomainWhiteList;

    /**
     *  Adresse du web service de données externes
     */
    private String restExtValueUrl;

    /**
     *  Envoyer un email au créateur de la demande lors de l’ajout d’un postit
     */
    private Boolean sendPostitByEmail = false;

    /**
     *  Envoyer un email aux observateurs à la création d’une demande
     */
    private Boolean sendCreationMailToViewers = false;

    /**
     *  Imposer la double authentification par SMS pour les externes
     */
    private Boolean smsRequired = true;

    /**
     * The org.bouncycastle.rsa.max_mr_tests property check has been added to allow capping of MR tests done on RSA moduli.
     */
    private Integer bouncycastelMaxMrTests = 0;

    /**
     *  Séparateur CSV
     */
    private String csvSeparator = null;

    /**
     *  Quote CSV
     */
    private Character csvQuote = null;

    /**
     *  Remplacer les espaces dans les noms de fichiers par le caractère suivant
     */
    private String fileNameSpacesReplaceBy = "_";

    /**
     * Durée de validité des liens de OTP en minutes
     */
    private Integer otpValidity = 10;

    /**
     * Liste des types de signature autorisés
     */
    private List<SignType> authorizedSignTypes = List.of(SignType.values());

    /**
     *  Resolution de l’image de signature
     */
    private Integer signatureImageDpi = 600;

    /**
     *  Facteur d’échelle entre pdfBox et pdfJs
     */
    private Float fixFactor = .75f;

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Boolean getEnableScheduledCleanup() {
        return enableScheduledCleanup;
    }

    public void setEnableScheduledCleanup(Boolean enableScheduledCleanup) {
        this.enableScheduledCleanup = enableScheduledCleanup;
    }

    public String getNexuUrl() {
        return nexuUrl;
    }

    public void setNexuUrl(String nexuUrl) {
        this.nexuUrl = nexuUrl;
    }

    public Boolean getHideWizard() {
        return hideWizard;
    }

    public void setHideWizard(Boolean hideWizard) {
        this.hideWizard = hideWizard;
    }

    public Boolean getHideWizardWorkflow() {
        return hideWizardWorkflow;
    }

    public void setHideWizardWorkflow(Boolean hideWizardWorkflow) {
        this.hideWizardWorkflow = hideWizardWorkflow;
    }

    public Boolean getHideAutoSign() {
        return hideAutoSign;
    }

    public void setHideAutoSign(Boolean hideAutoSign) {
        this.hideAutoSign = hideAutoSign;
    }

    public Boolean getHideSendSignRequest() {
        return hideSendSignRequest;
    }

    public void setHideSendSignRequest(Boolean hideSendSignRequest) {
        this.hideSendSignRequest = hideSendSignRequest;
    }

    public List<String> getHideWizardExceptRoles() {
        return hideWizardExceptRoles;
    }

    public void setHideWizardExceptRoles(List<String> hideWizardExceptRoles) {
        this.hideWizardExceptRoles = hideWizardExceptRoles;
    }

    public List<String> getHideAutoSignExceptRoles() {
        return hideAutoSignExceptRoles;
    }

    public void setHideAutoSignExceptRoles(List<String> hideAutoSignExceptRoles) {
        this.hideAutoSignExceptRoles = hideAutoSignExceptRoles;
    }

    public List<String> getHideSendSignExceptRoles() {
        return hideSendSignExceptRoles;
    }

    public void setHideSendSignExceptRoles(List<String> hideSendSignExceptRoles) {
        this.hideSendSignExceptRoles = hideSendSignExceptRoles;
    }

    public String getArchiveUri() {
        return archiveUri;
    }

    public void setArchiveUri(String archiveUri) {
        this.archiveUri = archiveUri;
    }

    public Integer getDelayBeforeCleaning() {
        return delayBeforeCleaning;
    }

    public void setDelayBeforeCleaning(Integer delayBeforeCleaning) {
        this.delayBeforeCleaning = delayBeforeCleaning;
    }

    public Integer getTrashKeepDelay() {
        return trashKeepDelay;
    }

    public void setTrashKeepDelay(Integer trashKeepDelay) {
        this.trashKeepDelay = trashKeepDelay;
    }

    public Boolean getEnableSu() {
        return enableSu;
    }

    public void setEnableSu(Boolean enableSu) {
        this.enableSu = enableSu;
    }

    public Boolean getEnableSplash() {
        return enableSplash;
    }

    public void setEnableSplash(Boolean enableSplash) {
        this.enableSplash = enableSplash;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getApplicationEmail() {
        return applicationEmail;
    }

    public void setApplicationEmail(String applicationEmail) {
        this.applicationEmail = applicationEmail;
    }

    public int getHoursBeforeRefreshNotif() {
        return hoursBeforeRefreshNotif;
    }

    public void setHoursBeforeRefreshNotif(Integer hoursBeforeRefreshNotif) {
        this.hoursBeforeRefreshNotif = hoursBeforeRefreshNotif;
    }

    public int getShareMode() {
        return shareMode;
    }

    public void setShareMode(Integer shareMode) {
        this.shareMode = shareMode;
    }

    public Boolean getInfiniteScrolling() {
        return infiniteScrolling;
    }

    public void setInfiniteScrolling(Boolean infiniteScrolling) {
        this.infiniteScrolling = infiniteScrolling;
    }

    public Boolean getReturnToHomeAfterSign() {
        return returnToHomeAfterSign;
    }

    public void setReturnToHomeAfterSign(Boolean returnToHomeAfterSign) {
        this.returnToHomeAfterSign = returnToHomeAfterSign;
    }

    public String getNamingTemplate() {
        return namingTemplate;
    }

    public void setNamingTemplate(String namingTemplate) {
        this.namingTemplate = namingTemplate;
    }

    public String getSignedSuffix() {
        return signedSuffix;
    }

    public void setSignedSuffix(String signedSuffix) {
        this.signedSuffix = signedSuffix;
    }

    public Boolean getDisableCertStorage() {
        return disableCertStorage;
    }

    public void setDisableCertStorage(Boolean disableCertStorage) {
        this.disableCertStorage = disableCertStorage;
    }

    public Boolean getEnableCaptcha() {
        return enableCaptcha;
    }

    public void setEnableCaptcha(Boolean enableCaptcha) {
        this.enableCaptcha = enableCaptcha;
    }

    public Integer getMaxUploadSize() {
        return maxUploadSize;
    }

    public void setMaxUploadSize(Integer maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public int getNbDaysBeforeWarning() {
        return nbDaysBeforeWarning;
    }

    public void setNbDaysBeforeWarning(Integer nbDaysBeforeWarning) {
        this.nbDaysBeforeWarning = nbDaysBeforeWarning;
    }

    public Integer getNbDaysBeforeDeleting() {
        return nbDaysBeforeDeleting;
    }

    public void setNbDaysBeforeDeleting(Integer nbDaysBeforeDeleting) {
        this.nbDaysBeforeDeleting = nbDaysBeforeDeleting;
    }

    public String getOpenXPKIServerUrl() {
        return openXPKIServerUrl;
    }

    public void setOpenXPKIServerUrl(String openXPKIServerUrl) {
        this.openXPKIServerUrl = openXPKIServerUrl;
    }

    public Boolean getAutoUpgrade() {
        return autoUpgrade;
    }

    public void setAutoUpgrade(Boolean autoUpgrade) {
        this.autoUpgrade = autoUpgrade;
    }

    public Boolean getPdfOnly() {
        return pdfOnly;
    }

    public void setPdfOnly(Boolean pdfOnly) {
        this.pdfOnly = pdfOnly;
    }

    public boolean getExportAttachements() {
        return exportAttachements;
    }

    public void setExportAttachements(boolean exportAttachements) {
        this.exportAttachements = exportAttachements;
    }

    public TokenType getSealCertificatType() {
        return sealCertificatType;
    }

    public void setSealCertificatType(TokenType sealCertificatType) {
        this.sealCertificatType = sealCertificatType;
    }

    public String getSealCertificatFile() {
        return sealCertificatFile;
    }

    public void setSealCertificatFile(String sealCertificatFile) {
        this.sealCertificatFile = sealCertificatFile;
    }

    public String getSealCertificatDriver() {
        return sealCertificatDriver;
    }

    public void setSealCertificatDriver(String sealCertificatDriver) {
        this.sealCertificatDriver = sealCertificatDriver;
    }

    public String getSealCertificatPin() {
        return sealCertificatPin;
    }

    public void setSealCertificatPin(String sealCertificatPin) {
        this.sealCertificatPin = sealCertificatPin;
    }

    public Boolean getSignEmailWithSealCertificat() {
        return signEmailWithSealCertificat;
    }

    public void setSignEmailWithSealCertificat(Boolean signEmailWithSealCertificat) {
        this.signEmailWithSealCertificat = signEmailWithSealCertificat;
    }

    public Boolean getSealAllDocs() {
        return sealAllDocs;
    }

    public void setSealAllDocs(Boolean sealAllDocs) {
        this.sealAllDocs = sealAllDocs;
    }

    public List<String> getShibUsersDomainWhiteList() {
        return shibUsersDomainWhiteList;
    }

    public void setShibUsersDomainWhiteList(List<String> shibUsersDomainWhiteList) {
        this.shibUsersDomainWhiteList = shibUsersDomainWhiteList;
    }

    public String getRestExtValueUrl() {
        return restExtValueUrl;
    }

    public void setRestExtValueUrl(String restExtValueUrl) {
        this.restExtValueUrl = restExtValueUrl;
    }

    public Boolean getSendPostitByEmail() {
        return sendPostitByEmail;
    }

    public void setSendPostitByEmail(Boolean sendPostitByEmail) {
        this.sendPostitByEmail = sendPostitByEmail;
    }

    public Boolean getSendCreationMailToViewers() {
        return sendCreationMailToViewers;
    }

    public void setSendCreationMailToViewers(Boolean sendCreationMailToViewers) {
        this.sendCreationMailToViewers = sendCreationMailToViewers;
    }

    public Boolean getSmsRequired() {
        return smsRequired;
    }

    public void setSmsRequired(Boolean smsRequired) {
        this.smsRequired = smsRequired;
    }

    public Integer getBouncycastelMaxMrTests() {
        return bouncycastelMaxMrTests;
    }

    public void setBouncycastelMaxMrTests(Integer bouncycastelMaxMrTests) {
        this.bouncycastelMaxMrTests = bouncycastelMaxMrTests;
    }

    public String getFileNameSpacesReplaceBy() {
        return fileNameSpacesReplaceBy;
    }

    public void setFileNameSpacesReplaceBy(String fileNameSpacesReplaceBy) {
        this.fileNameSpacesReplaceBy = fileNameSpacesReplaceBy;
    }

    public String getCsvSeparator() {
        return csvSeparator;
    }

    public void setCsvSeparator(String csvSeparator) {
        this.csvSeparator = csvSeparator;
    }

    public Character getCsvQuote() {
        return csvQuote;
    }

    public void setCsvQuote(Character csvQuote) {
        this.csvQuote = csvQuote;
    }

    public void setExportAttachements(Boolean exportAttachements) {
        this.exportAttachements = exportAttachements;
    }

    public Integer getOtpValidity() {
        return otpValidity;
    }

    public void setOtpValidity(Integer otpValidity) {
        this.otpValidity = otpValidity;
    }

    public List<SignType> getAuthorizedSignTypes() {
        return authorizedSignTypes;
    }

    public void setAuthorizedSignTypes(List<SignType> authorizedSignTypes) {
        this.authorizedSignTypes = authorizedSignTypes;
    }

    public Integer getSignatureImageDpi() {
        return signatureImageDpi;
    }

    public void setSignatureImageDpi(Integer signatureImageDpi) {
        this.signatureImageDpi = signatureImageDpi;
    }

    public Float getFixFactor() {
        return fixFactor;
    }

    public void setFixFactor(Float fixFactor) {
        this.fixFactor = fixFactor;
    }
}
