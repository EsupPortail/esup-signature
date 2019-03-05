package org.esupportail.esupsignature.service.pdf;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.ValidationResult.ValidationError;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

@Service
public class PdfService {

	private static final Logger log = LoggerFactory.getLogger(PdfService.class);

	@Resource
	private FileService fileService;
	
	@Value("${pdf.pdfToImageDpi}")
	private int pdfToImageDpi;
	@Value("${pdf.xCenter}")
	private int xCenter;
	@Value("${pdf.xCenter}")
	private int yCenter;	
	
	public File formatPdf(File toSignFile, SignRequestParams params) {
    	PdfParameters pdfParameters = getPdfParameters(toSignFile);
    	if(SignRequestParams.NewPageType.onBegin.equals(params.getNewPageType())) {
        	log.info("add page on begin");
        	toSignFile = addBlankPage(toSignFile, 0);
        	params.setSignPageNumber(1);
        	params.setXPos(xCenter);
        	params.setYPos(yCenter);
        } else 
        if(SignRequestParams.NewPageType.onEnd.equals(params.getNewPageType())) {
        	log.info("add page on end");
        	toSignFile = addBlankPage(toSignFile, -1);
        	params.setSignPageNumber(pdfParameters.getTotalNumberOfPages());
        	params.setXPos(xCenter);
        	params.setYPos(yCenter);
        }
    	if(!checkPdfA(toSignFile)) {
			toSignFile = toPdfA(toSignFile);
    	}
    	return toSignFile;
	}
		
	public File stampImage(File toSignFile, SignRequestParams params, User user) {
    	File signImage = user.getSignImage().getJavaIoFile();
    	PdfParameters pdfParameters = getPdfParameters(toSignFile);
		toSignFile = formatPdf(toSignFile, params);
		try {
			File targetFile =  new File(Files.createTempDir(), toSignFile.getName());
			PDDocument pdDocument = PDDocument.load(toSignFile);
	        PDPage pdPage = pdDocument.getPage(params.getSignPageNumber() - 1);
	        
			PDImageXObject pdImage;
					
			PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, false, true);
			float height = pdPage.getMediaBox().getHeight();
			float width = pdPage.getMediaBox().getWidth();

			if(pdfParameters.getRotation() == 0) {
				BufferedImage bufferedImage = ImageIO.read(signImage);
		        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
		        tx.translate(0, -bufferedImage.getHeight(null));
		        AffineTransformOp op = new AffineTransformOp(tx,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		        bufferedImage = op.filter(bufferedImage, null);
				
				File flipedSignImage = File.createTempFile("preview", ".png");
				ImageIO.write(bufferedImage, "png", flipedSignImage);
				pdImage = PDImageXObject.createFromFileByContent(flipedSignImage, pdDocument);
				contentStream.transform(new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, height)));
				contentStream.drawImage(pdImage, (int) params.getXPos(), (int) params.getYPos(), 100, 75);

			} else {
				AffineTransform at = new java.awt.geom.AffineTransform(0, 1, -1, 0, width, 0);
			    contentStream.transform(new Matrix(at));
			    pdImage = PDImageXObject.createFromFileByContent(signImage, pdDocument);
				contentStream.drawImage(pdImage, (int) params.getXPos(), (int) params.getYPos() - 37 , 100, 75);

			}
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
	
	public boolean checkPdfA(File pdfFile) {
		log.info("check pdfa validity");
		try {
			PreflightParser parser = new PreflightParser(pdfFile);  
			parser.parse();
			PreflightDocument document = parser.getPreflightDocument();
	        document.validate();  
	        ValidationResult result = document.getResult();  
		    document.close();
	        for(ValidationError v : result.getErrorsList()) {
	        	if(v.getErrorCode().startsWith("7")) {
	        		log.warn("pdf validation error " + v.getErrorCode() + " : " + v.getDetails());
	        	}
	        	//TODO probleme pdfa non conforme
	        	if(v.getErrorCode().equals("7.1")) {
	        		log.info("contains PDFA metedata");
	        		return true;
	        	}
	        }
		    XMPMetadata metadata = result.getXmpMetaData();
		    if (metadata == null) {
		    	log.warn("not complient to PDFA");
		        return false;
		    } else {
		    	PDFAIdentificationSchema id = metadata.getPDFIdentificationSchema();
		    	System.err.println(id.getConformance());
		    	log.info("complient to PDFA");
		    	return true;
		    }

		} catch (Exception e) {
			log.error("check error", e);
		}
		return false;
	}
	
	public File toPdfA(File pdfFile) {
        try {
			File targetFile =  new File(Files.createTempDir(), pdfFile.getName());
			PDDocument pdDocument = PDDocument.load(pdfFile);
	        PDDocumentCatalog cat = pdDocument.getDocumentCatalog();
			PDAcroForm pdAcroForm = cat.getAcroForm();
			PDDocumentInformation info = pdDocument.getDocumentInformation();
			if(pdAcroForm != null) {
				pdAcroForm.setNeedAppearances(false);
				PDResources pdResources = new PDResources();
				pdAcroForm.setDefaultResources(pdResources);

			    List<PDField> fields = new ArrayList<>(pdAcroForm.getFields());
			    processFields(fields, pdResources);
			    pdAcroForm.flatten();
			}
	        try {
		        XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();
	        
		        DublinCoreSchema dublinCoreSchema = xmpMetadata.createAndAddDublinCoreSchema();
		        dublinCoreSchema.setTitle(info.getTitle());
		        
		        XMPBasicSchema xmpBasicSchema = xmpMetadata.createAndAddXMPBasicSchema();
		        xmpBasicSchema.setCreatorTool(info.getCreator());
		        xmpBasicSchema.setCreateDate(info.getCreationDate());
		        xmpBasicSchema.setModifyDate(info.getModificationDate());

		        
		        PDFAIdentificationSchema pdfaid = new PDFAIdentificationSchema(xmpMetadata);
	        	pdfaid.setConformance("B");
		        pdfaid.setPart(1);
		        pdfaid.setAboutAsSimple(null);
		        xmpMetadata.addSchema(pdfaid);
		        
		        XmpSerializer serializer = new XmpSerializer();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                serializer.serialize(xmpMetadata, baos, false);
            
		        PDMetadata metadata = new PDMetadata(pdDocument);
                metadata.importXMPMetadata(baos.toByteArray());
		        cat.setMetadata(metadata);

		        InputStream colorProfile = PdfService.class.getResourceAsStream("/sRGB.icc");
	            PDOutputIntent intent = new PDOutputIntent(pdDocument, colorProfile);
	            intent.setInfo("sRGB IEC61966-2.1");
	            intent.setOutputCondition("sRGB IEC61966-2.1");
	            intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
	            intent.setRegistryName("http://www.color.org");
	            cat.addOutputIntent(intent);
	            		
		        pdDocument.save(targetFile);
				pdDocument.close();
		        return targetFile;		        
	        } catch (Exception e) {
				log.error("PDF/A convert error", e);
			}
        } catch (IOException e) {
			log.error("file read error", e);
		}
        return null;
	}	
	
	private void processFields(List<PDField> fields, PDResources resources) {
	    fields.stream().forEach(f -> {
	    	log.debug("process :" + f.getFullyQualifiedName() + " : " + f.getFieldType());
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
	    	    	if(f.getFieldType().equals("Sig")) {
	    	    		//TODO gerer les signatures
	    	    		return;
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
	                    log.error("process fields error", e1);
	                }
	            } else {
	            	log.error("process fields error", e);
	            }
	        }
	        if (f instanceof PDNonTerminalField) {
	            processFields(((PDNonTerminalField) f).getChildren(), resources);
	        }
	    });
	}
	
	public List<String> pagesAsBase64Images(File pdfFile) {
		List<String> imagePages = new ArrayList<String>();
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
		  PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
	        for(int i = 0; i < (pdDocument.getNumberOfPages()); i++) {
		        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, pdfToImageDpi, ImageType.RGB);
		        imagePages.add(fileService.getBase64Image(bufferedImage, pdfFile.getName()));
	        }
		} catch (IOException e) {
			log.error("error on get page as base 64 image", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					log.error("enable to close document", e);
				}
	          }
		}
        return imagePages;
	}
	
	public PdfParameters getPdfParameters(File pdfFile) {
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
			PDPage pdPage = pdDocument.getPage(0);
			PdfParameters pdfParameters = new PdfParameters((int) pdPage.getMediaBox().getWidth(), (int) pdPage.getMediaBox().getHeight(), pdPage.getRotation(), pdDocument.getNumberOfPages());
			return pdfParameters;
		} catch (IOException e) {
			log.error("error on get pdf parameters", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					log.error("enable to close document", e);
				}
	          }
		}
		return null;
	}
	
	public String pageAsBase64Image(File pdfFile, int page) {
		String imagePage = "";
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
	        PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
	        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, pdfToImageDpi, ImageType.RGB);
	        imagePage = fileService.getBase64Image(bufferedImage, pdfFile.getName());
		} catch (IOException e) {
			log.error("error on convert page to base 64 image", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					log.error("enable to close document", e);
				}
	          }
		}
        return imagePage;
	}
	
	public BufferedImage pageAsBufferedImage(File pdfFile, int page) {
		BufferedImage bufferedImage = null;
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
			PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
			bufferedImage = pdfRenderer.renderImageWithDPI(page, pdfToImageDpi, ImageType.RGB);
		} catch (IOException e) {
			log.error("error on convert page to image", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					log.error("enable to close document", e);
				}
	          }
		}
		return bufferedImage;
	}

	public InputStream pageAsInputStream(File pdfFile, int page) throws Exception {
		BufferedImage bufferedImage = pageAsBufferedImage(pdfFile, page);
		InputStream inputStream = fileService.bufferedImageToInputStream(bufferedImage, "png");
		bufferedImage.flush();
		return inputStream; 
		
		
        
	}
	
}
