// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.Date;
import org.esupportail.esupsignature.domain.TagLog;

privileged aspect TagLog_Roo_JavaBean {
    
    public Date TagLog.getDate() {
        return this.date;
    }
    
    public void TagLog.setDate(Date date) {
        this.date = date;
    }
    
    public int TagLog.getTarif() {
        return this.tarif;
    }
    
    public void TagLog.setTarif(int tarif) {
        this.tarif = tarif;
    }
    
    public String TagLog.getEppnInit() {
        return this.eppnInit;
    }
    
    public void TagLog.setEppnInit(String eppnInit) {
        this.eppnInit = eppnInit;
    }
    
}
