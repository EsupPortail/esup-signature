package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.repository.BigFileRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.InputStream;

@Service
public class BigFileService {

	@Resource
	private BigFileRepository bigFileRepository;

	public void setBinaryFileStream(BigFile bigFile, InputStream inputStream, long length) {
		bigFileRepository.addBinaryFileStream(bigFile, inputStream, length);
	}

	public void delete(long id) {
		bigFileRepository.deleteById(id);
	}

}
