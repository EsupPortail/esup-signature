package org.esupportail.esupsignature.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.springframework.stereotype.Service;
@Service
public class PdfService {

	@Resource
	FileService fileService;

	public int signPageNumber = 0;

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

	public File addImage(File pdfFile, File signImage, int page, int x, int y) throws IOException {
		
		File targetFile =  File.createTempFile(pdfFile.getName(), ".pdf");

		PDDocument pdDocument = PDDocument.load(pdfFile);
		PDPage pdPage = pdDocument.getPage(page - 1);

		PDImageXObject pdImage = PDImageXObject.createFromFileByContent(signImage, pdDocument);

		PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true);

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
