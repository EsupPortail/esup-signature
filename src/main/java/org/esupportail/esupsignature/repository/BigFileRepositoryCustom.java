package org.esupportail.esupsignature.repository;

import java.io.InputStream;

import org.esupportail.esupsignature.entity.BigFile;

public interface BigFileRepositoryCustom {

	void addBinaryFileStream(BigFile bigFile, InputStream inputStream, long length);

}
