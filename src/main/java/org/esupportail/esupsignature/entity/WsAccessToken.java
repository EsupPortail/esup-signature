package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
public class WsAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    private String appName;

    @Column(unique=true)
    private String token;

    private Boolean publicAccess = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "ws_access_token_workflows",
            joinColumns = @JoinColumn(name = "ws_access_token_id"),
            inverseJoinColumns = @JoinColumn(name = "workflows_id")
    )
    private Set<Workflow> workflows = new HashSet<>();

    private Boolean createSignrequest = true;

    private Boolean readSignrequest = true;

    private Boolean updateSignrequest = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getPublicAccess() {
        if(publicAccess == null) {
            return false;
        }
        return publicAccess;
    }

    public void setPublicAccess(Boolean publicAccess) {
        this.publicAccess = publicAccess;
    }

    public Set<Workflow> getWorkflows() {
        return workflows;
    }

    public void setWorkflows(Set<Workflow> workflows) {
        this.workflows = workflows;
    }

    public Boolean getCreateSignrequest() {
        return Objects.requireNonNullElse(createSignrequest, true);
    }

    public void setCreateSignrequest(Boolean createSignrequest) {
        this.createSignrequest = createSignrequest;
    }

    public Boolean getReadSignrequest() {
        return Objects.requireNonNullElse(readSignrequest, true);
    }

    public void setReadSignrequest(Boolean readSignrequest) {
        this.readSignrequest = readSignrequest;
    }

    public Boolean getUpdateSignrequest() {
        return Objects.requireNonNullElse(updateSignrequest, true);
    }

    public void setUpdateSignrequest(Boolean deleteSignrequest) {
        this.updateSignrequest = deleteSignrequest;
    }
}
