package org.esupportail.esupsignature.entity;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Long, Boolean> signBooks = new HashMap<>();

    private Boolean allSignToComplete = false;

    @OneToOne
    private SignRequestParams signRequestParams;

    transient Map<String, Boolean> signBooksLabels;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Map<Long, Boolean> getSignBooks() {
        return signBooks;
    }

    public void setSignBooks(Map<Long, Boolean> signBooks) {
        this.signBooks = signBooks;
    }

    public Boolean isAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

    public SignRequestParams getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

    public Boolean isCompleted() {
        int nbSign = 0;
        for (Map.Entry<Long, Boolean> signBookEntry : signBooks.entrySet()) {
            if(!allSignToComplete && signBookEntry.getValue()) {
                return true;
            }
            if(signBookEntry.getValue()) {
                nbSign++;
            }
        }
        if(nbSign == signBooks.size()) {
            return true;
        } else {
            return false;
        }
    }

    public Map<String, Boolean> getSignBooksLabels() {
        return signBooksLabels;
    }

    public void setSignBooksLabels(Map<String, Boolean> signBooksLabels) {
        this.signBooksLabels = signBooksLabels;
    }

}
