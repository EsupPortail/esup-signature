// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupnfccarteculture.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;
import org.esupportail.esupnfccarteculture.domain.Gestionnaire;

privileged aspect Gestionnaire_Roo_Jpa_Entity {
    
    declare @type: Gestionnaire: @Entity;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long Gestionnaire.id;
    
    @Version
    @Column(name = "version")
    private Integer Gestionnaire.version;
    
    public Long Gestionnaire.getId() {
        return this.id;
    }
    
    public void Gestionnaire.setId(Long id) {
        this.id = id;
    }
    
    public Integer Gestionnaire.getVersion() {
        return this.version;
    }
    
    public void Gestionnaire.setVersion(Integer version) {
        this.version = version;
    }
    
}
