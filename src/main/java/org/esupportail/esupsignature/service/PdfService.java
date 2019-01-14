package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
@Service
public class PdfService {

	@Resource
	FileService fileService;
	
	public File addWhitePageOnTop(File pdfFile, int position) throws DocumentException, IOException {

	    File targetFile =  File.createTempFile("outFile", ".tmp");
	    PdfReader reader = new PdfReader(new FileInputStream(pdfFile));
	    PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(targetFile));
	    if(position < 0) {
	    	stamper.insertPage(reader.getNumberOfPages() + 1, reader.getPageSizeWithRotation(1));
	    } else {
	    	stamper.insertPage(position, reader.getPageSizeWithRotation(1));
	    }
	    stamper.close();
	    reader.close();
	    
	    return targetFile;

	}

	public File addImage(File pdfFile, File signImage, int page, int x, int y) throws DocumentException, IOException {
		
		File targetFile =  File.createTempFile("outFile", ".tmp");
	    PdfReader reader = new PdfReader(new FileInputStream(pdfFile));
	    PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(targetFile));
        Image image = Image.getInstance(IOUtils.toByteArray(new FileInputStream(signImage)));
        image.setAbsolutePosition(x, y);
        stamper.getOverContent(page).addImage(image);
	    stamper.close();
	    reader.close();
	    return targetFile;
	}
	
}
