package org.esupportail.esupsignature.domain;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Entity
public class SignRequestParams {
	
	public enum SignType {
		visa, pdfImageStamp, certSign, nexuSign;
	}

	public enum NewPageType {
		none, onBegin;
	}
	
	@Enumerated(EnumType.STRING)
	private SignType signType;
    
	@Enumerated(EnumType.STRING)
	private NewPageType newPageType;

	private int signPageNumber;
	
	private int xPos;

	private int yPos;
	
    public SignType getSignType() {
        return this.signType;
    }

    public String getSignTypeLabel() {
        return this.signType.toString();
    }
    
    public void setSignType(SignType signType) {
        this.signType = signType;
    }
    
    public NewPageType getNewPageType() {
        return this.newPageType;
    }
    
    public void setNewPageType(NewPageType newPageType) {
        this.newPageType = newPageType;
    }

	@PersistenceContext
    transient EntityManager entityManager;

	public static final List<String> fieldNames4OrderClauseFilter = java.util.Arrays.asList("signType", "newPageType", "signPageNumber", "xPos", "yPos");

	public static final EntityManager entityManager() {
        EntityManager em = new SignRequestParams().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

	public static long countSignRequestParamses() {
        return entityManager().createQuery("SELECT COUNT(o) FROM SignRequestParams o", Long.class).getSingleResult();
    }

	public static List<SignRequestParams> findAllSignRequestParamses() {
        return entityManager().createQuery("SELECT o FROM SignRequestParams o", SignRequestParams.class).getResultList();
    }

	public static List<SignRequestParams> findAllSignRequestParamses(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignRequestParams o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignRequestParams.class).getResultList();
    }

	public static SignRequestParams findSignRequestParams(Long id) {
        if (id == null) return null;
        return entityManager().find(SignRequestParams.class, id);
    }

	public static List<SignRequestParams> findSignRequestParamsEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM SignRequestParams o", SignRequestParams.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	public static List<SignRequestParams> findSignRequestParamsEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignRequestParams o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignRequestParams.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	@Transactional
    public void persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }

	@Transactional
    public void remove() {
        if (this.entityManager == null) this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            SignRequestParams attached = SignRequestParams.findSignRequestParams(this.id);
            this.entityManager.remove(attached);
        }
    }

	@Transactional
    public void flush() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.flush();
    }

	@Transactional
    public void clear() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.clear();
    }

	@Transactional
    public SignRequestParams merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        SignRequestParams merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

	public int getSignPageNumber() {
        return this.signPageNumber;
    }

	public void setSignPageNumber(int signPageNumber) {
        this.signPageNumber = signPageNumber;
    }

	public int getXPos() {
        return this.xPos;
    }

	public void setXPos(int xPos) {
        this.xPos = xPos;
    }

	public int getYPos() {
        return this.yPos;
    }

	public void setYPos(int yPos) {
        this.yPos = yPos;
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }
}
