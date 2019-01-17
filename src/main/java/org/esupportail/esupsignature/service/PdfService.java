package org.esupportail.esupsignature.service;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
import org.springframework.stereotype.Service;
@Service
public class PdfService {

	@Resource
	FileService fileService;

	private int pdfToImageDpi = 72;
	
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
