package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.SignRequestStatus;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
public class Data {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    private Form form;

	private String formName;

	private Integer formVersion;

    private String name;

	private String description;

	private String owner;

	private String createBy;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updateDate;
    
    @ElementCollection(fetch = FetchType.EAGER)
	private Map<String, String> datas = new HashMap<>();

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
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
