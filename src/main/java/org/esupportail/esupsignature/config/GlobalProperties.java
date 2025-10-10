package org.esupportail.esupsignature.config;

import jakarta.annotation.PostConstruct;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     *default : [title] : Titre caluler au moment de l'import
     *Les attributs disponibles sont :
     *<ul>
     *  <li>[originalFileName] : titre du document original</li>
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
     *Le modèle est construit à l'aide d'attributs entre crochets.
     *default : [title] : Titre caluler au moment de l'import
     *Les attributs disponibles sont :
     *<ul>
     *  <li>[originalFileName] : titre du document original</li>
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
    private String namingTemplateArchive = "[title]";

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
    @Deprecated
    private SealCertificatProperties.TokenType sealCertificatType;

    /**
     *  Emplacement du certificat cachet (actif pour PKCS12)
     */
    @Deprecated
    private String sealCertificatFile;

    /**
     *  Pilote du certificat cachet
     */
    @Deprecated
    private String sealCertificatDriver;

    /**
     *  Pin du certificat cachet
     */
    @Deprecated
    private String sealCertificatPin = "";

    @PostConstruct
    public void init() {
        if (sealCertificatProperties.isEmpty()) {
            if(sealCertificatType != null && StringUtils.hasText(sealCertificatPin)) {
                sealCertificatProperties.put("default", new SealCertificatProperties("Certificat cachet", sealCertificatType, sealCertificatFile, sealCertificatDriver, sealCertificatPin));
            }
        } else if(!sealCertificatProperties.containsKey("default")){
            throw new IllegalStateException("La configuration 'seal-certificat-properties' doit contenir une entrée 'default' lorsqu'elle n'est pas vide.");
        }
    }

    /**
     * Liste des propriétés de certificats cachet utilisées pour configurer ou gérer les certificats dans l'application.
     */
    public Map<String, SealCertificatProperties> sealCertificatProperties = new HashMap<>();

    /**
     *  Appliquer le cachet sur toutes les demandes terminées
     */
    private Boolean sealAllDocs = false;

    /**
     * Autoriser les externes à signer avec le cachet
     */
    private Boolean sealForExternals = false;

    /**
     * Autoriser automatiquement le certificat cachet pour les demandes internes déjà signés
     */
    private Boolean sealAuthorizedForSignedFiles = false;

    /**
     *  Whitelist des domaines authorisés à obtenir le ROLE_USER pour les connexions Shibboleth
     */
    private List<String> shibUsersDomainWhiteList;

    /**
     * Liste permettant de forcer des domaines comme externes
     */
    private List<String> forcedExternalsDomainList;

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
     * Liste des types de signature autorisés (par défaut tous les types).
     * <p>
     * Valeurs possibles :
     * - imageStamp
     * - userCert
     * - groupCert
     * - autoCert
     * - openPkiCert
     * - sealCert
     * - nexuCert
     */
    private List<SignWith> authorizedSignTypes = List.of(SignWith.values());

    /**
     *  Resolution de l’image de signature
     */
    private Integer signatureImageDpi = 600;

    /**
     *  Facteur d’échelle entre pdfBox et pdfJs
     */
    private Float fixFactor = .75f;

    /**
     * Activer le watermark pour les utilisateurs externes
     * @deprecated utiliser externalSignatureParams.addWatermark
     */
    @Deprecated
    private Boolean watermarkForExternals;

    /**
     * Activer les annotations pour les utilisateurs externes (pour les demandes hors circuit)
     */
    private Boolean externalCanEdit = false;

    /**
     * Faire apparaitre les users externes dans la recherche full texte
     *
     */
    private Boolean searchForExternalUsers = false;

    /**
     * Delai d’alerte avant expiration des certificats serveur
     */
    private Integer nbDaysBeforeCertifWarning = 60;

    private String systemUserName = "Esup-Signature";

    private String systemUserFirstName = "Automate";

    /**
     * Configuration des signatures des externes
     */
    private SignRequestParams externalSignatureParams = new SignRequestParams();

    /**
     * Activation de l’intro d’aide
     */
    private Boolean enableHelp = true;

    /*
     * Email de test remplaçant les destinataires toutes les notifications
     */
    private String testEmail;

    /**
     * Activer la vérification des numéros de téléphone français pour l’OTP
     */
    private Boolean frenchPhoneNumberOnly = false;


    /**
     * Indique si les vérifications (visas cachés) doivent être masqués dans l'interface utilisateur.
     */
    private Boolean hideHiddenVisa = false;

    public String newVersion;

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

    public String getNamingTemplateArchive() {
        return namingTemplateArchive;
    }

    public void setNamingTemplateArchive(String namingTemplateArchive) {
        this.namingTemplateArchive = namingTemplateArchive;
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

    public SealCertificatProperties.TokenType getSealCertificatType() {
        return sealCertificatType;
    }

    public void setSealCertificatType(SealCertificatProperties.TokenType sealCertificatType) {
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

    public Map<String, SealCertificatProperties> getSealCertificatProperties() {
        return sealCertificatProperties;
    }

    public void setSealCertificatProperties(Map<String, SealCertificatProperties> sealCertificatProperties) {
        this.sealCertificatProperties = sealCertificatProperties;
    }

    public Boolean getSealAllDocs() {
        return sealAllDocs;
    }

    public void setSealAllDocs(Boolean sealAllDocs) {
        this.sealAllDocs = sealAllDocs;
    }

    public Boolean getSealForExternals() {
        return sealForExternals;
    }

    public void setSealForExternals(Boolean sealForExternals) {
        this.sealForExternals = sealForExternals;
    }

    public Boolean getSealAuthorizedForSignedFiles() {
        return sealAuthorizedForSignedFiles;
    }

    public void setSealAuthorizedForSignedFiles(Boolean sealAuthorizedForSignedFiles) {
        this.sealAuthorizedForSignedFiles = sealAuthorizedForSignedFiles;
    }

    public List<String> getShibUsersDomainWhiteList() {
        return shibUsersDomainWhiteList;
    }

    public void setShibUsersDomainWhiteList(List<String> shibUsersDomainWhiteList) {
        this.shibUsersDomainWhiteList = shibUsersDomainWhiteList;
    }

    public List<String> getForcedExternalsDomainList() {
        return forcedExternalsDomainList;
    }

    public void setForcedExternalsDomainList(List<String> forcedExternalsDomainList) {
        this.forcedExternalsDomainList = forcedExternalsDomainList;
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

    public List<SignWith> getAuthorizedSignTypes() {
        return authorizedSignTypes;
    }

    public void setAuthorizedSignTypes(List<SignWith> authorizedSignTypes) {
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

    public void setWatermarkForExternals(Boolean watermarkForExternals) {
        this.watermarkForExternals = watermarkForExternals;
    }

    public Boolean getSearchForExternalUsers() {
        return searchForExternalUsers;
    }

    public void setSearchForExternalUsers(Boolean searchForExternalUsers) {
        this.searchForExternalUsers = searchForExternalUsers;
    }

    public Integer getNbDaysBeforeCertifWarning() {
        return nbDaysBeforeCertifWarning;
    }

    public void setNbDaysBeforeCertifWarning(Integer nbDaysBeforeCertifWarning) {
        this.nbDaysBeforeCertifWarning = nbDaysBeforeCertifWarning;
    }

    public String getSystemUserName() {
        return systemUserName;
    }

    public void setSystemUserName(String systemUserName) {
        this.systemUserName = systemUserName;
    }

    public String getSystemUserFirstName() {
        return systemUserFirstName;
    }

    public void setSystemUserFirstName(String systemUserFirstName) {
        this.systemUserFirstName = systemUserFirstName;
    }

    public SignRequestParams getExternalSignatureParams() {
        if(watermarkForExternals != null) {
            externalSignatureParams.setAddWatermark(watermarkForExternals);
        }
        return externalSignatureParams;
    }

    public void setExternalSignatureParams(SignRequestParams externalSignatureParams) {
        this.externalSignatureParams = externalSignatureParams;
    }

    public Boolean getEnableHelp() {
        return enableHelp;
    }

    public void setEnableHelp(Boolean enableHelp) {
        this.enableHelp = enableHelp;
    }

    public String getTestEmail() {
        return testEmail;
    }

    public void setTestEmail(String testEmail) {
        this.testEmail = testEmail;
    }

    public Boolean getFrenchPhoneNumberOnly() {
        return frenchPhoneNumberOnly;
    }

    public void setFrenchPhoneNumberOnly(Boolean frenchPhoneNumberOnly) {
        this.frenchPhoneNumberOnly = frenchPhoneNumberOnly;
    }

    public Boolean getExternalCanEdit() {
        return externalCanEdit;
    }

    public void setExternalCanEdit(Boolean externalCanEdit) {
        this.externalCanEdit = externalCanEdit;
    }

    public Boolean getHideHiddenVisa() {
        return hideHiddenVisa;
    }

    public void setHideHiddenVisa(Boolean hideHiddenVisa) {
        this.hideHiddenVisa = hideHiddenVisa;
    }


}
