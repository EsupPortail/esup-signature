package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.ReportStatus;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date date;

    @ManyToOne
    private User user;

    @ElementCollection
    private Map<Long, ReportStatus> signRequestReportStatusMap = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Map<Long, ReportStatus> getSignRequestReportStatusMap() {
        return signRequestReportStatusMap;
    }

    public void setSignRequestReportStatusMap(Map<Long, ReportStatus> signRequestReportStatusMap) {
        this.signRequestReportStatusMap = signRequestReportStatusMap;
    }
}
