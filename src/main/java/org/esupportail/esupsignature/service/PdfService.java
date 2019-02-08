package org.esupportail.esupsignature.service;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.pdfa.XMPSchemaPDFAId;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.esupportail.esupsignature.domain.SignBook.NewPageType;
import org.esupportail.esupsignature.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

@Service
public class PdfService {

	private static final Logger log = LoggerFactory.getLogger(PdfService.class);

	@Resource
	private FileService fileService;
	
	//TODO properties ?
	private int pdfToImageDpi = 72;
	private int xCenter = 297;
	private int yCenter = 421;
	
	public File formatPdf(File toSignFile, Map<String, String> params) {
    	if(NewPageType.valueOf(params.get("newPageType")).equals(NewPageType.onBegin)) {
        	log.info("add page on begin");
        	toSignFile = addBlankPage(toSignFile, 0);
        	params.put("signPageNumber", "1");
        	params.put("xPos", String.valueOf(xCenter));
        	params.put("yPos", String.valueOf(yCenter));
        } else 
        if(NewPageType.valueOf(params.get("newPageType")).equals(NewPageType.onEnd)) {
        	log.info("add page on end");
        	toSignFile = addBlankPage(toSignFile, -1);
        	params.put("signPageNumber", String.valueOf(getTotalNumberOfPages(toSignFile)));
        	params.put("xPos", String.valueOf(xCenter));
        	params.put("yPos", String.valueOf(yCenter));
        }
		toSignFile = toPdfA(toSignFile);
    	return toSignFile;
	}
		
	public File stampImage(File toSignFile, Map<String, String> params, User user) {
    	File signImage = user.getSignImage().getJavaIoFile();
		toSignFile = formatPdf(toSignFile, params);
		try {
			BufferedImage bufferedImage = ImageIO.read(signImage);
	        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
	        tx.translate(0, -bufferedImage.getHeight(null));
	        AffineTransformOp op = new AffineTransformOp(tx,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
	        bufferedImage = op.filter(bufferedImage, null);
			
			File flipedSignImage = File.createTempFile("preview", ".png");
			ImageIO.write(bufferedImage, "png", flipedSignImage);
			
			File targetFile =  new File(Files.createTempDir(), toSignFile.getName());

			PDDocument pdDocument = PDDocument.load(toSignFile);
	       
	        PDPage pdPage = pdDocument.getPage(Integer.valueOf(params.get("signPageNumber")) - 1);

			PDImageXObject pdImage = PDImageXObject.createFromFileByContent(flipedSignImage, pdDocument);
					
			PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, false, true);
			float height=pdPage.getMediaBox().getHeight();
			contentStream.transform(new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, height)));
	        
			Matrix matrix = pdPage.getMatrix();
			matrix.rotate(180);
			matrix.translate(Integer.valueOf(params.get("xPos")), Integer.valueOf(params.get("yPos")));

			//contentStream.drawXObject(pdImage, x, y, signWidth, signHeight);
			contentStream.drawImage(pdImage, Integer.valueOf(params.get("xPos")), Integer.valueOf(params.get("yPos")), SignRequestService.signWidth, SignRequestService.signHeight);
			contentStream.close();
			
			pdDocument.save(targetFile);
			pdDocument.close();
		    return targetFile;
		} catch (IOException e) {
			log.error("error to add image", e);
		}
		return null;
	}
	
	public File addBlankPage(File pdfFile, int position) {
		try {
			File targetFile = File.createTempFile(pdfFile.getName(), ".pdf");
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
		} catch (IOException e) {
			log.error("error to add blank page", e);
		}
		return null;
	}

	public File flatten(File pdfFile) {
        try {
			File targetFile =  File.createTempFile(pdfFile.getName(), ".pdf");
			PDDocument pdDocument = PDDocument.load(pdfFile);
			PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
			pdAcroForm.setNeedAppearances(false);
			pdAcroForm.flatten();
	        try {
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
	
	public File toPdfA(File pdfFile) {
        try {
			File targetFile =  new File(Files.createTempDir(), pdfFile.getName());
			PDDocument pdDocument = PDDocument.load(pdfFile);
			PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
			if(pdAcroForm != null) {
				pdAcroForm.setNeedAppearances(false);
				PDResources pdResources = new PDResources();
				pdAcroForm.setDefaultResources(pdResources);

			    List<PDField> fields = new ArrayList<>(pdAcroForm.getFields());
			    processFields(fields, pdResources);
			    pdAcroForm.flatten();
			}
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
	
	private void processFields(List<PDField> fields, PDResources resources) {
	    fields.stream().forEach(f -> {
	        f.setReadOnly(true);
	        COSDictionary cosObject = f.getCOSObject();
	        String value = cosObject.getString(COSName.DV) == null ?
	                       cosObject.getString(COSName.V) : cosObject.getString(COSName.DV);
	        try {
	        	if(value == null) {
	        		value = "";
	        		if(f.getFieldType().equals("Btn")) {
	        			value = "Off";
	        		}
	        	}
	            f.setValue(value);
	        } catch (IOException e) {
	            if (e.getMessage().matches("Could not find font: /.*")) {
	                String fontName = e.getMessage().replaceAll("^[^/]*/", "");
	                System.out.println("Adding fallback font for: " + fontName);
	                resources.put(COSName.getPDFName(fontName), PDType1Font.HELVETICA);
	                try {
	                    f.setValue(value);
	                } catch (IOException e1) {
	                    e1.printStackTrace();
	                }
	            } else {
	                e.printStackTrace();
	            }
	        }
	        if (f instanceof PDNonTerminalField) {
	            processFields(((PDNonTerminalField) f).getChildren(), resources);
	        }
	    });
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
	
	public static Integer getTotalNumberOfPages(File pdfFile) {
		try {
			PDDocument pdDocument = PDDocument.load(pdfFile);
			int numberOfPages =pdDocument.getNumberOfPages();
			return numberOfPages;
		} catch (IOException e) {
			log.error("error to get number of pages", e);
		}
		return null;
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
