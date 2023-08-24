package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.entity.enums.UserType;

import jakarta.validation.constraints.NotNull;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.util.*;

@Entity
@Table(name = "user_account", indexes = {
        @Index(name="user_eppn", columnList = "eppn"),
        @Index(name="user_email", columnList = "email")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
	
	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

	@Version
    private Integer version;
	
	private String name;

	private String firstname;
	
    @Column(unique=true)
    @NotNull
    private String eppn;

    @Column(unique=true)
    @NotNull
    private String email;

    @Column(unique=true)
    private String phone;

    @ElementCollection(targetClass = String.class)
    @JsonIgnore
    private Set<String> managersRoles = new HashSet<>();

    @ElementCollection
    @JsonIgnore
    private Map<UiParams, String> uiParams = new LinkedHashMap<>();

    private String formMessages = "";

    @Enumerated(EnumType.STRING)
    private UserType userType;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<Document> signImages = new ArrayList<>();

    private Integer defaultSignImageNumber = 0;

    @Transient
    private String ip;

    @Transient
    private String keystoreFileName;

    @Transient
    private List<Long> signImagesIds;

    @Transient
    private String signImageBase64;

    @Transient
    private Long userShareId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Document keystore = new Document();

    @Enumerated(EnumType.STRING)
    private EmailAlertFrequency emailAlertFrequency = EmailAlertFrequency.immediately;

    private Integer emailAlertHour;
    
    @Enumerated(EnumType.STRING)
    private DayOfWeek emailAlertDay;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSendAlertDate = new Date(0);

    @ElementCollection(targetClass = String.class)
    @JsonIgnore
    private Set<String> roles = new HashSet<>();

    @JsonIgnore
    @ManyToOne
    private User replaceByUser;

    @Temporal(TemporalType.TIMESTAMP)
    private Date replaceBeginDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date replaceEndDate;

    @OneToOne
    private SignRequestParams favoriteSignRequestParams;

    private Boolean returnToHomeAfterSign = true;

    private Boolean forceSms = false;

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

	public String getName() {
        return this.name;
    }

	public void setName(String name) {
        this.name = name;
    }

	public String getFirstname() {
        return this.firstname;
    }

	public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

	public String getEppn() {
        return this.eppn;
    }

	public void setEppn(String eppn) {
        this.eppn = eppn;
    }

	public String getEmail() {
        return this.email;
    }

	public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<String> getManagersRoles() {
        return managersRoles;
    }

    public void setManagersRoles(Set<String> managersRoles) {
        this.managersRoles = managersRoles;
    }

    public Map<UiParams, String> getUiParams() {
        return uiParams;
    }

    public void setUiParams(Map<UiParams, String> uiParams) {
        this.uiParams = uiParams;
    }

    public String getFormMessages() {
        return formMessages;
    }

    public void setFormMessages(String formMessages) {
        this.formMessages = formMessages;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public List<Document> getSignImages() {
        return signImages;
    }

    public void setSignImages(List<Document> signImages) {
        this.signImages = signImages;
    }

    public String getIp() {
        return this.ip;
    }

	public void setIp(String ip) {
        this.ip = ip;
    }

    public String getKeystoreFileName() {
        return keystoreFileName;
    }

    public void setKeystoreFileName(String keystoreFileName) {
        this.keystoreFileName = keystoreFileName;
    }

    public List<Long> getSignImagesIds() {
        return signImagesIds;
    }

    public void setSignImagesIds(List<Long> signImagesIds) {
        this.signImagesIds = signImagesIds;
    }

    public String getSignImageBase64() {
        return this.signImageBase64;
    }

	public void setSignImageBase64(String signImageBase64) {
        this.signImageBase64 = signImageBase64;
    }

    public Long getUserShareId() {
        return userShareId;
    }

    public void setUserShareId(Long userShareId) {
        this.userShareId = userShareId;
    }

    public Document getKeystore() {
        return this.keystore;
    }

	public void setKeystore(Document keystore) {
        this.keystore = keystore;
    }

	public Integer getEmailAlertHour() {
        return this.emailAlertHour;
    }

	public void setEmailAlertHour(Integer emailAlertHour) {
        this.emailAlertHour = emailAlertHour;
    }

	public DayOfWeek getEmailAlertDay() {
        return this.emailAlertDay;
    }

	public void setEmailAlertDay(DayOfWeek emailAlertDay) {
        this.emailAlertDay = emailAlertDay;
    }

	public Date getLastSendAlertDate() {
        return this.lastSendAlertDate;
    }

	public void setLastSendAlertDate(Date lastSendAlertDate) {
        this.lastSendAlertDate = lastSendAlertDate;
    }

    public EmailAlertFrequency getEmailAlertFrequency() {
        return this.emailAlertFrequency;
    }

    public void setEmailAlertFrequency(EmailAlertFrequency emailAlertFrequency) {
        this.emailAlertFrequency = emailAlertFrequency;
    }
    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Integer getDefaultSignImageNumber() {
        if(defaultSignImageNumber != null) {
            return defaultSignImageNumber;
        } else {
            if(getSignImages().size() > 0) {
                return getSignImages().size() - 1;
            }
        }
        return null;
    }

    public void setDefaultSignImageNumber(Integer defaultSignImageNumber) {
        this.defaultSignImageNumber = defaultSignImageNumber;
    }

    public User getReplaceByUser() {
        return replaceByUser;
    }

    public void setReplaceByUser(User replaceByUser) {
        this.replaceByUser = replaceByUser;
    }

    public Date getReplaceBeginDate() {
        return replaceBeginDate;
    }

    public void setReplaceBeginDate(Date replaceBeginDate) {
        this.replaceBeginDate = replaceBeginDate;
    }

    public Date getReplaceEndDate() {
        return replaceEndDate;
    }

    public void setReplaceEndDate(Date replaceEndDate) {
        this.replaceEndDate = replaceEndDate;
    }

    public SignRequestParams getFavoriteSignRequestParams() {
        return favoriteSignRequestParams;
    }

    public void setFavoriteSignRequestParams(SignRequestParams favoriteSignRequestParams) {
        this.favoriteSignRequestParams = favoriteSignRequestParams;
    }

    @JsonIgnore
    public User getCurrentReplaceUser() {
        Date checkDate = new Date();
        if((replaceBeginDate == null
                || checkDate.after(replaceBeginDate))
                && (replaceEndDate == null
                || checkDate.before(replaceEndDate))) {
            return replaceByUser;
        }
        return null;
    }

    public Boolean getReturnToHomeAfterSign() {
        return returnToHomeAfterSign;
    }

    public void setReturnToHomeAfterSign(Boolean returnToHomeAfterSign) {
        this.returnToHomeAfterSign = returnToHomeAfterSign;
    }

    public Boolean getForceSms() {
        return forceSms;
    }

    public void setForceSms(Boolean forceSms) {
        this.forceSms = forceSms;
    }
}
