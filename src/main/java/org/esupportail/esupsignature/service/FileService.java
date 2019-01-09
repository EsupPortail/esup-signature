package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.sql.SQLException;

import org.esupportail.esupsignature.domain.File;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

	public File addFile(MultipartFile multipartFile) throws IOException, SQLException {
        File file = new File();
        file.setFileName(multipartFile.getOriginalFilename());
        file.setBinaryFileStream(multipartFile.getInputStream(), multipartFile.getSize());
        file.setSize(multipartFile.getSize());
        file.setContentType(multipartFile.getContentType());
        file.persist();
        return file;
    }
	
}
