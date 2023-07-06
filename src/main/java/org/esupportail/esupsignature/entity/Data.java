package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
public class Data {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@JsonIgnore
    @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    private Form form;

	private String formName;

	private Integer formVersion;

    private String name;

	@ManyToOne
	private User createBy;

	@ManyToOne
	private User updateBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;

    @OrderColumn
    @ElementCollection(fetch = FetchType.EAGER)
	@Column(columnDefinition = "TEXT")
	private Map<String, String> datas = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

	@OneToOne(cascade = CascadeType.DETACH)
	private SignBook signBook = null;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFormName() {
		return formName;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}

	public Integer getFormVersion() {
		return formVersion;
	}

	public void setFormVersion(Integer formVersion) {
		this.formVersion = formVersion;
	}

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}

	public Map<String, String> getDatas() {
		return datas;
	}

	public void setDatas(Map<String, String> datas) {
		this.datas = datas;
	}

	public User getCreateBy() {
		return createBy;
	}

	public void setCreateBy(User createBy) {
		this.createBy = createBy;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public SignRequestStatus getStatus() {
		return status;
	}

	public void setStatus(SignRequestStatus status) {
		this.status = status;
	}

	public User getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(User updateBy) {
		this.updateBy = updateBy;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	public SignBook getSignBook() {
		return signBook;
	}

	public void setSignBook(SignBook signBook) {
		this.signBook = signBook;
	}
}
