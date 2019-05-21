/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.esupportail.esupsignature.domain;

import java.io.InputStream;
import java.sql.Blob;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Version;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Entity
public class BigFile {

    public static final List<String> fieldNames4OrderClauseFilter = java.util.Arrays.asList("binaryFile");
	
	@Lob
	@Type(type="org.hibernate.type.BlobType")
	@Basic(fetch = FetchType.LAZY)
	private Blob binaryFile;
	
	// Here we have to use directly Hibernate and no "just" JPA, because we want to use stream to read and write big files
	// @see http://stackoverflow.com/questions/10042766/jpa-analog-of-lobcreator-from-hibernate
    public void setBinaryFileStream(InputStream inputStream, long length) {		
        if (this.entityManager == null) this.entityManager = entityManager();
        Session session = (Session) this.entityManager.getDelegate(); 
        LobHelper helper = session.getLobHelper(); 
        this.binaryFile = helper.createBlob(inputStream, length); 
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

	public Blob getBinaryFile() {
        return this.binaryFile;
    }

	public void setBinaryFile(Blob binaryFile) {
        this.binaryFile = binaryFile;
    }

	@PersistenceContext
    transient EntityManager entityManager;

	public static final EntityManager entityManager() {
        EntityManager em = new BigFile().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

	public static long countBigFiles() {
        return entityManager().createQuery("SELECT COUNT(o) FROM BigFile o", Long.class).getSingleResult();
    }

	public static List<BigFile> findAllBigFiles() {
        return entityManager().createQuery("SELECT o FROM BigFile o", BigFile.class).getResultList();
    }

	public static List<BigFile> findAllBigFiles(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM BigFile o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, BigFile.class).getResultList();
    }

	public static BigFile findBigFile(Long id) {
        if (id == null) return null;
        return entityManager().find(BigFile.class, id);
    }

	public static List<BigFile> findBigFileEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM BigFile o", BigFile.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	public static List<BigFile> findBigFileEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM BigFile o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, BigFile.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
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
            BigFile attached = BigFile.findBigFile(this.id);
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
    public BigFile merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        BigFile merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
