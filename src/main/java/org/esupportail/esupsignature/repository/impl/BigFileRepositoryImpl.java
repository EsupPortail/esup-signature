package org.esupportail.esupsignature.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.repository.custom.BigFileRepositoryCustom;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Repository;

import java.io.InputStream;

@Repository
public class BigFileRepositoryImpl implements BigFileRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public void addBinaryFileStream(BigFile bigFile, InputStream inputStream, long length) {
		bigFile.setBinaryFile(Hibernate.getLobHelper().createBlob(inputStream, length));
		entityManager.persist(bigFile);
	}

}
