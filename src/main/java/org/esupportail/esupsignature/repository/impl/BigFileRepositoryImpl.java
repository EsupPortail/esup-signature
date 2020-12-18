package org.esupportail.esupsignature.repository.impl;

import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.repository.custom.BigFileRepositoryCustom;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.InputStream;
import java.sql.Blob;

@Repository
public class BigFileRepositoryImpl implements BigFileRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;
	
	@Override
	public void addBinaryFileStream(BigFile bigFile, InputStream inputStream, long length) {
		Blob blob = Hibernate.getLobCreator(entityManager.unwrap(Session.class)).createBlob(inputStream, length);
        bigFile.setBinaryFile(blob);
        entityManager.persist(bigFile);
	}

	
}
