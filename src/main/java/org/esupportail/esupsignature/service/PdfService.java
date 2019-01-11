package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

@Service
public class PdfService {

	@Resource
	FileService fileService;
	
	public InputStream addWhitePageOnTop(InputStream intputStream) throws DocumentException, IOException {
		
	    File targetFile =  File.createTempFile("outFile", ".tmp");
	    
	    PdfReader reader = new PdfReader(intputStream);
	    PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(targetFile));
	    stamper.insertPage(0, reader.getPageSizeWithRotation(1));
	    stamper.close();
	    reader.close();
	    
	    return new FileInputStream(targetFile);

	}

}
