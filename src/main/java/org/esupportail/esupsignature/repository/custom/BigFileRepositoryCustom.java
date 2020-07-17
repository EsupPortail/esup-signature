package org.esupportail.esupsignature.repository.custom;

import org.esupportail.esupsignature.entity.BigFile;

import java.io.InputStream;

public interface BigFileRepositoryCustom {

	void addBinaryFileStream(BigFile bigFile, InputStream inputStream, long length);

}
