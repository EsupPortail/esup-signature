package org.esupportail.esupsignature.repository.impl;

import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.repository.BigFileRepositoryCustom;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.InputStream;

@Repository
public class BigFileRepositoryImpl implements BigFileRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;
	
	@Override
	public void addBinaryFileStream(BigFile bigFile, InputStream inputStream, long length) {
        Session session = (Session) this.entityManager.getDelegate(); 
        LobHelper helper = session.getLobHelper(); 
        bigFile.setBinaryFile(helper.createBlob(inputStream, length)); 
        entityManager.persist(bigFile);
	}

	
}
