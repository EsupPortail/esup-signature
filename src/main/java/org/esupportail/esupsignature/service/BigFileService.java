package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.repository.BigFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class BigFileService {

	@Autowired
	private BigFileRepository bigFileRepository;

	public List<BigFile> getAllBigFiles() {
		List<BigFile> list = new ArrayList<BigFile>();
		bigFileRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public BigFile getBigFileById(long id) {
		return bigFileRepository.findById(id).get();
	}

	public boolean addBigFile(BigFile bigFile) {
		BigFile test = bigFileRepository.findById(bigFile.getId()).get(); 	
	       if (test != null) {
	    	   return false;
	       } else {
	    	   bigFileRepository.save(bigFile);
	    	   return true;
	       }
	}

	public void setBinaryFileStream(BigFile bigFile, InputStream inputStream, long length) {
		bigFileRepository.addBinaryFileStream(bigFile, inputStream, length);
	}
	
	public void updateBigFile(BigFile bigFile) {
		bigFileRepository.save(bigFile);
		
	}

	public void deleteBigFile(long id) {
		bigFileRepository.deleteById(id);
	}
	
	
	
}
