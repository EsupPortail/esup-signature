package org.esupportail.esupsignature.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.File;
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
	
	public File addFile(MultipartFile multipartFile) throws IOException, SQLException {
		return addFile(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getSize(), multipartFile.getContentType());
    }

	public File addFile(InputStream inputStream, String name, long size, String contentType) throws IOException, SQLException {
		//TODO : à tester sans persist
        File file = new File();
        file.setFileName(name);
        file.getBigFile().setBinaryFileStream(inputStream, size);
        file.getBigFile().persist();
        file.setSize(size);
        file.setContentType(contentType);
        file.persist();
        return file;
    }
	
	public File signPdf(File file, String certif, List<String> certifChain, File imageFile) throws IOException, SQLException, DocumentException {
		
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
		signaturePdfForm.setBase64SignatureValue("TEST DSI");
		signaturePdfForm.setEncryptionAlgorithm(params.getEncryptionAlgorithm());

	    MultipartFile multipartFile = new MockMultipartFile(file.getFileName(), file.getFileName(), file.getContentType(), pdfService.addWhitePageOnTop(file.getBigFile().getBinaryFile().getBinaryStream()));
		signaturePdfForm.setDocumentToSign(multipartFile);		
        
		DSSDocument dssDocument = signingService.visibleSignDocument(signaturePdfForm, 1, 200, 200, toJavaIoFile(imageFile));

        InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
        
        return addFile(signedPdfDocument.openStream(), signedPdfDocument.getName(), signedPdfDocument.getBytes().length, signedPdfDocument.getMimeType().getMimeTypeString());
	}
	
	public String getBase64Image(File file) throws IOException, SQLException {
		String out = "";
		BufferedImage imBuff = ImageIO.read(file.getBigFile().getBinaryFile().getBinaryStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imBuff, file.getFileName().replaceAll(".*\\.", ""), baos);
        baos.flush();
        out = DatatypeConverter.printBase64Binary(baos.toByteArray());
        baos.close();
        return out;
        
	}
	
	public java.io.File toJavaIoFile(File file) throws SQLException, IOException {
		InputStream inputStream = file.getBigFile().getBinaryFile().getBinaryStream();
	    java.io.File targetFile = java.io.File.createTempFile("outFile", ".tmp");
	    OutputStream outputStream = new FileOutputStream(targetFile);
	    IOUtils.copy(inputStream, outputStream);
	    outputStream.close();
		return targetFile;
	}
	
}
