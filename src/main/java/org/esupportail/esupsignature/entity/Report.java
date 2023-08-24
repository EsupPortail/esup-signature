package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;
import org.esupportail.esupsignature.entity.enums.ReportStatus;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence-generator")
    @GenericGenerator(
            name = "sequence-generator",
            type = org.hibernate.id.enhanced.SequenceStyleGenerator.class,
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "hibernate_sequence"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
            }
    )
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
