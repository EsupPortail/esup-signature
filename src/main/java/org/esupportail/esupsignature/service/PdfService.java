package org.esupportail.esupsignature.service;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.pdfa.XMPSchemaPDFAId;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.util.Matrix;
import org.esupportail.esupsignature.dss.web.model.DataToSignParams;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.service.SigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.SignatureForm;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignaturePackaging;
@Service
public class PdfService {

	@Resource
	private DocumentService documentService;

	@Resource
	private FileService fileService;
	
	@Autowired
	private SigningService signingService;
	
	private int pdfToImageDpi = 72;

	public File certSignPdf(File file, String certif, List<String> certifChain, File imageFile, int page, int x, int y) throws IOException, SQLException {
		
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
        
		DSSDocument dssDocument = signingService.visibleSignDocument(signaturePdfForm, page, x, y, imageFile);

        InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
        
        return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName(), "pdf");
        //return addFile(signedPdfDocument.openStream(), signedPdfDocument.getName(), signedPdfDocument.getBytes().length, signedPdfDocument.getMimeType().getMimeTypeString());
	}
	
	public File addBlankPage(File pdfFile, int position) throws IOException {

	    File targetFile =  File.createTempFile(pdfFile.getName(), ".pdf");
		PDDocument pdDocument = PDDocument.load(pdfFile);
		PDDocument targetPDDocument = new PDDocument();
		PDPage blankPage = new PDPage(PDRectangle.A4);
		if(position == 0) {
			targetPDDocument.addPage(blankPage);
		} 
		
		for(PDPage page : pdDocument.getPages()) {
			targetPDDocument.addPage(page);
		}
		
		if(position == -1) {
			targetPDDocument.addPage(blankPage);
		}
		targetPDDocument.save(targetFile);
		targetPDDocument.close();
	    return targetFile;

	}

	public File toPdfA(File pdfFile) throws Exception {

		File targetFile =  File.createTempFile(pdfFile.getName(), ".pdf");

		PDDocument pdDocument = PDDocument.load(pdfFile);
		PDDocumentCatalog cat = pdDocument.getDocumentCatalog();
        PDMetadata metadata = new PDMetadata(pdDocument);
        cat.setMetadata(metadata);
        XMPMetadata xmp = new XMPMetadata();
        XMPSchemaPDFAId pdfaid = new XMPSchemaPDFAId(xmp);
        xmp.addSchema(pdfaid);
        pdfaid.setConformance("B");
        pdfaid.setPart(1);
        pdfaid.setAbout("");
        metadata.importXMPMetadata(xmp.asByteArray());
		pdDocument.save(targetFile);
		pdDocument.close();
        return targetFile;

	}	
	
	public File addImage(File pdfFile, File signImage, int page, int x, int y) throws Exception {
	
		BufferedImage bufferedImage = ImageIO.read(signImage);

        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -bufferedImage.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        bufferedImage = op.filter(bufferedImage, null);
		
		File flipedSignImage = File.createTempFile("preview", ".png");
		ImageIO.write(bufferedImage, "png", flipedSignImage);
		
		File targetFile =  File.createTempFile(pdfFile.getName(), ".pdf");

		PDDocument pdDocument = PDDocument.load(pdfFile);
       
        PDPage pdPage = pdDocument.getPage(page - 1);

		PDImageXObject pdImage = PDImageXObject.createFromFileByContent(flipedSignImage, pdDocument);
		
		PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, false);
		float height=pdPage.getMediaBox().getHeight();
        contentStream.transform(new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0,height)));
        
		Matrix matrix = pdPage.getMatrix();
		matrix.rotate(180);
		matrix.translate(x, y);
        
		contentStream.drawImage(pdImage, x, y);
		contentStream.close();
		
		pdDocument.save(targetFile);
		pdDocument.close();
	    return targetFile;
	}
	
	public List<String> pageAsImage(File pdfFile) throws Exception {
		List<String> imagePages = new ArrayList<String>();
		PDDocument pdDocument = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
        for(int i = 0; i < (pdDocument.getNumberOfPages()); i++) {
        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, pdfToImageDpi, ImageType.RGB);
        ImageIO.scanForPlugins();
        ImageIOUtil.writeImage(bufferedImage, "png", pdfToImageDpi);
        imagePages.add(fileService.getBase64Image(bufferedImage, pdfFile.getName()));
        }
        pdDocument.close();
        return imagePages;

	}
}
