package org.esupportail.esupsignature.service;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
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
import org.apache.pdfbox.util.Matrix;
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.SignBook.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.service.SigningService;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.SignatureForm;
import eu.europa.esig.dss.SignatureLevel;
import eu.europa.esig.dss.SignaturePackaging;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;

@Service
public class PdfService {

	private static final Logger log = LoggerFactory.getLogger(PdfService.class);

	@Resource
	private FileService fileService;
	
	@Resource
	private UserKeystoreService userKeystoreService;
	
	@Autowired
	private SigningService signingService;
	
	private int pdfToImageDpi = 72;
	private int signWidth = 100;
	private int signHeight = 75;
	private int xCenter = 297;
	private int yCenter = 421;
	
	public File signPdf(File toSignFile, File signImage, SignType signType, int pageNumber, int xPos, int yPos, NewPageType newPageType, User user, String password) throws IOException, EsupSignatureException {
		//toSignFile = toPdfA(toSignFile);
    	File signedFile = null;
    	if(newPageType.equals(NewPageType.onBegin)) {
        	log.info("add page on begin");
        	toSignFile = addBlankPage(toSignFile, 0);
        	pageNumber = 1;
        	xPos = xCenter;
        	yPos = yCenter;
        } else 
        if(newPageType.equals(NewPageType.onEnd)) {
        	log.info("add page on end");
        	toSignFile = addBlankPage(toSignFile, -1);
        	pageNumber = getTotalNumberOfPages(toSignFile) + 1;
        	xPos = xCenter;
        	yPos = yCenter;
        }
    	
        if(signType.equals(SignType.imageStamp)) {
        	log.info("imageStamp signature " + xPos + " : " + yPos);
        	signedFile = addImage(toSignFile, signImage, pageNumber, xPos, yPos);
        } else 
        if(signType.equals(SignType.certPAdES)) {
        	log.info("cades signature");
          	signedFile = pAdESSign(toSignFile, signImage, pageNumber, xPos, yPos, user, password);
        }
        return signedFile;

	}
	
	public File pAdESSign(File toSignFile, File signImage, int pageNumber, int xPos, int yPos, User user, String password) throws IOException, EsupSignatureException {
		
        SignatureDocumentForm signaturePdfForm = new SignatureDocumentForm();
		signaturePdfForm.setSignatureForm(SignatureForm.PAdES);
		signaturePdfForm.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
		signaturePdfForm.setDigestAlgorithm(DigestAlgorithm.SHA256);
		signaturePdfForm.setSignaturePackaging(SignaturePackaging.ENVELOPED);
		signaturePdfForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
		signaturePdfForm.setSigningDate(new Date());
		signaturePdfForm.setDocumentToSign(fileService.toMultipartFile(toSignFile, "application/pdf"));
        
		File keyStoreFile = user.getKeystore().getBigFile().toJavaIoFile();
		
		SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(keyStoreFile, password);
		CertificateToken certificateToken = userKeystoreService.getCertificateToken(keyStoreFile, password);
		CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(keyStoreFile, password);

        signaturePdfForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
		List<String> base64CertificateChain = new ArrayList<>();
		for(CertificateToken token : certificateTokenChain) {
			base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
		}
		signaturePdfForm.setBase64CertificateChain(base64CertificateChain);
		
		DSSDocument dssDocument = signingService.visibleSignDocument(signaturePdfForm, signatureTokenConnection, certificateToken, certificateTokenChain, pageNumber, xPos, yPos, signImage, signWidth, signHeight);

        InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
        
        return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName(), "pdf");
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

	public File toPdfA(File pdfFile) {
        try {
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
	        try {
		        metadata.importXMPMetadata(xmp.asByteArray());
				pdDocument.save(targetFile);
	        } catch (Exception e) {
				log.error("PDF/A convert error", e);
			}
			pdDocument.close();
	        return targetFile;
        } catch (IOException e) {
			log.error("file read error", e);
		}
        return pdfFile;
	}	
	
	public File addImage(File pdfFile, File signImage, int page, int x, int y) throws IOException {
	
		BufferedImage bufferedImage = ImageIO.read(signImage);

        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -bufferedImage.getHeight(null));
        AffineTransformOp op = new AffineTransformOp(tx,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        bufferedImage = op.filter(bufferedImage, null);
		
		File flipedSignImage = File.createTempFile("preview", ".png");
		ImageIO.write(bufferedImage, "png", flipedSignImage);
		
		File targetFile =  File.createTempFile("output", ".pdf");

		PDDocument pdDocument = PDDocument.load(pdfFile);
       
        PDPage pdPage = pdDocument.getPage(page - 1);

		PDImageXObject pdImage = PDImageXObject.createFromFileByContent(flipedSignImage, pdDocument);
				
		PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, false, true);
		float height=pdPage.getMediaBox().getHeight();
		contentStream.transform(new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, height)));
        
		Matrix matrix = pdPage.getMatrix();
		matrix.rotate(180);
		matrix.translate(x, y);

		//contentStream.drawXObject(pdImage, x, y, signWidth, signHeight);
		contentStream.drawImage(pdImage, x, y, signWidth, signHeight);
		contentStream.close();
		
		pdDocument.save(targetFile);
		pdDocument.close();
	    return targetFile;
	}
	
	public List<String> pagesAsBase64Images(File pdfFile) throws IOException   {
		List<String> imagePages = new ArrayList<String>();
		PDDocument pdDocument = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
        for(int i = 0; i < (pdDocument.getNumberOfPages()); i++) {
	        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, pdfToImageDpi, ImageType.RGB);
	        imagePages.add(fileService.getBase64Image(bufferedImage, pdfFile.getName()));
        }
        pdDocument.close();
        return imagePages;
	}
	
	public int getTotalNumberOfPages(File pdfFile) throws IOException {
		PDDocument pdDocument = PDDocument.load(pdfFile);
		int numberOfPages =pdDocument.getNumberOfPages();
		pdDocument.close();
		return numberOfPages;
	}
	
	public String pageAsBase64Image(File pdfFile, int page) throws Exception {
		PDDocument pdDocument = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, pdfToImageDpi, ImageType.RGB);
        String imagePage = fileService.getBase64Image(bufferedImage, pdfFile.getName());
        pdDocument.close();
        return imagePage;
	}
	
	public BufferedImage pageAsBufferedImage(File pdfFile, int page) throws Exception {
		PDDocument pdDocument = PDDocument.load(pdfFile);
        PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, pdfToImageDpi, ImageType.RGB);
        pdDocument.close();
        return bufferedImage;
	}

	public InputStream pageAsInputStream(File pdfFile, int page) throws Exception {
		return fileService.bufferedImageToInputStream(pageAsBufferedImage(pdfFile, page), "png");
        
	}
	
}
