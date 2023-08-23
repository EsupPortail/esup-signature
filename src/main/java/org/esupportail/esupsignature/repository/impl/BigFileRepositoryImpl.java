package org.esupportail.esupsignature.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.repository.custom.BigFileRepositoryCustom;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.sql.Blob;

@Repository
public class BigFileRepositoryImpl implements BigFileRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public void addBinaryFileStream(BigFile bigFile, InputStream inputStream, long length) {
		LobHelper lobHelper = entityManager.unwrap(Session.class).getLobHelper();
		Blob blob = lobHelper.createBlob(inputStream, length);
		bigFile.setBinaryFile(blob);
		entityManager.persist(bigFile);
	}

}
