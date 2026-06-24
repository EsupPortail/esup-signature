package org.esupportail.esupsignature.dto.page.user.report;

import org.esupportail.esupsignature.entity.enums.ReportStatus;

import java.util.Date;
import java.util.List;

public class ReportListViewDto {

    private List<RowDto> reports;

    public List<RowDto> getReports() {
        return reports;
    }

    public void setReports(List<RowDto> reports) {
        this.reports = reports;
    }

    public List<RowDto> reports() {
        return reports;
    }

    public static class RowDto {
        private Long id;
        private Date date;
        private List<StatusDto> statuses;

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

        public List<StatusDto> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<StatusDto> statuses) {
            this.statuses = statuses;
        }

        public Long id() {
            return id;
        }

        public Date date() {
            return date;
        }

        public List<StatusDto> statuses() {
            return statuses;
        }
    }

    public static class StatusDto {
        private ReportStatus status;
        private List<Long> signRequestIds;

        public ReportStatus getStatus() {
            return status;
        }

        public void setStatus(ReportStatus status) {
            this.status = status;
        }

        public List<Long> getSignRequestIds() {
            return signRequestIds;
        }

        public void setSignRequestIds(List<Long> signRequestIds) {
            this.signRequestIds = signRequestIds;
        }

        public int getCount() {
            return signRequestIds != null ? signRequestIds.size() : 0;
        }

        public ReportStatus status() {
            return status;
        }

        public List<Long> signRequestIds() {
            return signRequestIds;
        }

        public int count() {
            return getCount();
        }
    }
}
