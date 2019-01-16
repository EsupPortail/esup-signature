package org.esupportail.esupsignature.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.dss.web.model.DataToSignParams;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.service.SigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.DocumentException;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.SignatureForm;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignaturePackaging;

@Service
public class FileService {

	@Autowired
	private SigningService signingService;
	
	@Autowired
	private PdfService pdfService;

	public Document addFile(Document file) {
		file.persist();
		return file;
    }
	
	public Document addFile(String base64File, String name, String type) throws FileNotFoundException, IOException, SQLException {
		return addFile(fromBase64Image(base64File, name), type);
    }

	
	public Document addFile(File file, String type) throws FileNotFoundException, IOException, SQLException {
		return addFile(new FileInputStream(file), file.getName(), file.length(), type);
    }
	
	public Document addFile(MultipartFile multipartFile) throws IOException, SQLException {
		return addFile(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getSize(), multipartFile.getContentType());
    }

	public Document addFile(InputStream inputStream, String name, long size, String contentType) throws IOException, SQLException {
		//TODO : à tester sans persist
        Document file = new Document();
        file.setFileName(name);
        file.getBigFile().setBinaryFileStream(inputStream, size);
        file.getBigFile().persist();
        file.setSize(size);
        file.setContentType(contentType);
        file.persist();
        return file;
    }
	
	public Document certSignPdf(File file, String certif, List<String> certifChain, Document imageFile, int page, int x, int y) throws IOException, SQLException, DocumentException {
		
		DataToSignParams params = new DataToSignParams();
        List<String> certificateChain = new ArrayList<String>();
        certificateChain.add(certif);
        if(certifChain != null && certifChain.size() > 0){
        	for(String chain : certifChain) {
        		certificateChain.add(chain);
        	}
        }
        params.setCertificateChain(certificateChain);
        params.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
        params.setSigningCertificate(certif);
        
        SignatureDocumentForm signaturePdfForm = new SignatureDocumentForm();
		signaturePdfForm.setSignatureForm(SignatureForm.PAdES);
		signaturePdfForm.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
		signaturePdfForm.setDigestAlgorithm(DigestAlgorithm.SHA256);
		signaturePdfForm.setSignaturePackaging(SignaturePackaging.ENVELOPED);
		signaturePdfForm.setBase64Certificate(params.getSigningCertificate());
		signaturePdfForm.setBase64CertificateChain(params.getCertificateChain());
		signaturePdfForm.setBase64SignatureValue("");
		signaturePdfForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());

		MultipartFile multipartFile = new MockMultipartFile(file.getName(), file.getName(), "application/pdf", new FileInputStream(file));

		signaturePdfForm.setDocumentToSign(multipartFile);		
        
		DSSDocument dssDocument = signingService.visibleSignDocument(signaturePdfForm, page, x, y, imageFile.getBigFile().toJavaIoFile());

        InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
        
        return addFile(signedPdfDocument.openStream(), signedPdfDocument.getName(), signedPdfDocument.getBytes().length, signedPdfDocument.getMimeType().getMimeTypeString());
	}
/*
	public Content simpleSignPdf(Content file, Content imageFile, int x, int y, boolean onNewPage, int positionOfNewPage) throws IOException, SQLException, DocumentException {
		File signedFile;
		if(onNewPage) {
			signedFile = pdfService.addImage(pdfService.addWhitePage(file.getBigFile().toJavaIoFile(), positionOfNewPage), imageFile.getBigFile().toJavaIoFile(), 0, 200, 200);
		} else {
			signedFile = pdfService.addImage(file.getBigFile().toJavaIoFile(), imageFile.getBigFile().toJavaIoFile(), 0, 200, 200);			
		}
		return addFile(new FileInputStream(signedFile), "signed_" + file.getFileName(), signedFile.length(), file.getContentType());
	}
*/	
	public String getBase64Image(Document file) throws IOException, SQLException {
		String out = "";
		BufferedImage imBuff = ImageIO.read(file.getBigFile().getBinaryFile().getBinaryStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imBuff, file.getFileName().replaceAll(".*\\.", ""), baos);
        baos.flush();
        out = DatatypeConverter.printBase64Binary(baos.toByteArray());
        baos.close();
        return out;
        
	}
	
	public File fromBase64Image(String base64Image, String name) throws IOException, SQLException {
		File fileImage = File.createTempFile(name, ".png");
		ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(base64Image.substring(base64Image.lastIndexOf(',') + 1).trim()));
        BufferedImage image = ImageIO.read(bis);
        ImageIO.write(image, "png", fileImage);
        return fileImage;
	}
	
	
	
}
