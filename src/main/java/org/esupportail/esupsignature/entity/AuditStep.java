package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.Map;

@Entity
public class AuditStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    private String login;

    private String name;

    private String firstname;

    private String email;

    private Integer page;

    private Integer posX;

    private Integer posY;

    @Column(columnDefinition = "TEXT")
    private String signId;

    @Column(columnDefinition = "TEXT")
    private String timeStampId;

    @Column(columnDefinition = "TEXT")
    private String signCertificat;

    @Column(columnDefinition = "TEXT")
    private String timeStampCertificat;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date timeStampDate;

    private Boolean allScrolled;

    @OrderColumn
    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private Map<String, String> authenticationDetails;

    @ManyToOne(cascade = CascadeType.DETACH)
    private SignRequestParams signRequestParams;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPosX() {
        return posX;
    }

    public void setPosX(Integer posX) {
        this.posX = posX;
    }

    public Integer getPosY() {
        return posY;
    }

    public void setPosY(Integer posY) {
        this.posY = posY;
    }

    public String getSignId() {
        return signId;
    }

    public void setSignId(String signId) {
        this.signId = signId;
    }

    public String getTimeStampId() {
        return timeStampId;
    }

    public void setTimeStampId(String timeStampId) {
        this.timeStampId = timeStampId;
    }

    public String getSignCertificat() {
        return signCertificat;
    }

    public void setSignCertificat(String signCertificat) {
        this.signCertificat = signCertificat;
    }

    public String getTimeStampCertificat() {
        return timeStampCertificat;
    }

    public void setTimeStampCertificat(String timeStampCertificat) {
        this.timeStampCertificat = timeStampCertificat;
    }

    public Date getTimeStampDate() {
        return timeStampDate;
    }

    public void setTimeStampDate(Date timeStampDate) {
        this.timeStampDate = timeStampDate;
    }

    public Boolean getAllScrolled() {
        return allScrolled;
    }

    public void setAllScrolled(Boolean allScrolled) {
        this.allScrolled = allScrolled;
    }

    public Map<String, String> getAuthenticationDetails() {
        return authenticationDetails;
    }

    public void setAuthenticationDetails(Map<String, String> authenticationDetails) {
        this.authenticationDetails = authenticationDetails;
    }

    public String getPettyPrintAuthenticationDetails() {
        try {
            ObjectWriter prettyPrinter = new ObjectMapper().writerWithDefaultPrettyPrinter();
            return prettyPrinter.writeValueAsString(authenticationDetails);
        } catch (Exception e) {
            return "{}";
        }
    }

    public SignRequestParams getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }
}
