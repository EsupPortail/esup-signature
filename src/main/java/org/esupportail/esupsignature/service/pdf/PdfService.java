package org.esupportail.esupsignature.service.pdf;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.imageio.ImageIO;

import org.apache.fontbox.ttf.TrueTypeFont;
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
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.ValidationResult.ValidationError;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

@Service
public class PdfService {

	private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

	@Resource
	private FileService fileService;

	@Resource
	private UserService userService;
	
	@Value("${pdf.width}")
	private int pdfWidth;
	@Value("${pdf.height}")
	private int pdfHeight;
	@Value("${pdf.pdfToImageDpi}")
	private int pdfToImageDpi;
	@Value("${sign.widthThreshold}")
	private int signWidthThreshold;
	
	public File stampImage(File toSignFile, SignRequestParams params, User user, boolean addPage, boolean addDate) throws InvalidPasswordException, IOException {
		//TODO add ip ? + date + nom ?
		SignRequestParams.SignType signType = params.getSignType();
    	PdfParameters pdfParameters = getPdfParameters(toSignFile);
		toSignFile = formatPdf(toSignFile, params, addPage, user);
		try {
			File targetFile =  new File(Files.createTempDir(), toSignFile.getName());
			PDDocument pdDocument = PDDocument.load(toSignFile);
			
			InputStream fontStream = new ClassPathResource("Helvetica.ttf").getInputStream();
			PDFont font = PDTrueTypeFont.loadTTF(pdDocument, fontStream);
			
	        PDPage pdPage = pdDocument.getPage(params.getSignPageNumber() - 1);
	        
			PDImageXObject pdImage;
					
			PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, false, true);
			float height = pdPage.getMediaBox().getHeight();
			float width = pdPage.getMediaBox().getWidth();

			File signImage;
			int xPos = (int) params.getXPos();
			int yPos = (int) params.getYPos();
			//TODO remplace tolocalestring
			//DateFormat format = new SimpleDateFormat("dd MMM YYYY HH:mm:ss", Locale.FRENCH);
			if(signType.equals(SignType.visa)) {
				try {
					//signImage = fileService.stringToImageFile("Visé par\n " + getInitials(user.getFirstname() + " " + user.getName()), "png");
					addText(contentStream, "Visé par " + user.getFirstname() + " " + user.getName(), xPos, yPos, font);
					if(addDate) {
						addText(contentStream, "Le " + new Date().toLocaleString(), xPos, yPos + 20, font);
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					signImage = null;
				}
			} else {
				int topHeight = 0;
				if(addDate) {
					//TODO add nom prenom ?
					addText(contentStream, "Le " + new Date().toLocaleString(), xPos, yPos, font);
					//topHeight = 20;
				}
				signImage = user.getSignImage().getJavaIoFile();
				int[] size = getSignSize(signImage);
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
					contentStream.drawImage(pdImage, xPos, yPos + topHeight, size[0], size[1]);

				} else {
					AffineTransform at = new java.awt.geom.AffineTransform(0, 1, -1, 0, width, 0);
				    contentStream.transform(new Matrix(at));
				    pdImage = PDImageXObject.createFromFileByContent(signImage, pdDocument);
				    contentStream.drawImage(pdImage, xPos, yPos + topHeight - 37 , size[0], size[1]);
				}
			}

			contentStream.close();
			pdDocument.save(targetFile);
			pdDocument.close();
		    return targetFile;
		} catch (IOException e) {
			logger.error("error to add image", e);
		}
		return null;
	}
	
	public void addText(PDPageContentStream contentStream, String text, int xPos, int yPos, PDFont font) throws IOException {
		int fontSize = 8;
		contentStream.beginText();
		contentStream.newLineAtOffset(xPos, 830 - yPos);
		contentStream.setFont(font, fontSize);
		contentStream.showText(text);
		contentStream.endText();
	}
	
	public int[] getSignSize(File signFile) throws IOException {
		BufferedImage bimg = ImageIO.read(signFile);
		int signWidth;
		int signHeight;
		if(bimg.getWidth() <= signWidthThreshold * 2) {
			signWidth = bimg.getWidth() / 2;
			signHeight = bimg.getHeight() / 2;
		} else {
			signWidth =  signWidthThreshold;
			double percent = ((double)signWidthThreshold / (double)bimg.getWidth());
			signHeight = (int) (percent * bimg.getHeight());
		}
		return new int[]{signWidth, signHeight};
	}
	
	public File formatPdf(File toSignFile, SignRequestParams params, boolean addPage, User user) throws InvalidPasswordException, IOException {
		PDDocumentInformation info = new PDDocumentInformation();
        info.setTitle(toSignFile.getName());
        info.setSubject(toSignFile.getName());
        info.setAuthor(user.getFirstname() + " " + user.getName());
        info.setCreator(user.getFirstname() + " " + user.getName());
        info.setProducer("esup-signature");
        info.setKeywords("PDF, signed");
        info.setCreationDate(Calendar.getInstance());
        info.setModificationDate(Calendar.getInstance());
        info.setTrapped("Unknown");
        info.setCustomMetadataValue("swag", "yes");
        
		PDDocument pdDocument = PDDocument.load(toSignFile);
		pdDocument.setDocumentInformation(info);
        pdDocument.setAllSecurityToBeRemoved(true);
        
		if(!SignRequestParams.NewPageType.none.equals(params.getNewPageType()) && addPage) {
    		if(SignRequestParams.NewPageType.onBegin.equals(params.getNewPageType())) {
    			pdDocument = addNewPage(pdDocument, null, 0);
    			params.setSignPageNumber(1);
    		} else {
    			pdDocument = addNewPage(pdDocument, null, -1);
    			params.setSignPageNumber(getPdfParameters(toSignFile).getTotalNumberOfPages());
    		}
    	}
        try {
			useNextSignatureField(pdDocument);
	        convertToPDFA(pdDocument);
        } catch (Exception e) {
			logger.error("format PDF error", e);
			return null;
		}
       
        pdDocument.save(toSignFile);
        pdDocument.close();
    	return toSignFile;
	}
	
	public PDDocument addNewPage(PDDocument pdDocument, String template, int position) {
		try {
			PDDocument targetPDDocument = new PDDocument();
			PDPage newPage = null;
			if(template != null) {
				PDDocument defaultNewPageTemplate = PDDocument.load(new ClassPathResource(template, PdfService.class).getFile());
				if(defaultNewPageTemplate != null) {
					newPage = defaultNewPageTemplate.getPage(0);
				}
			} else {
				PDDocument defaultNewPageTemplate = PDDocument.load(new ClassPathResource("/templates/defaultnewpage.pdf", PdfService.class).getFile());
				if(defaultNewPageTemplate != null) {
					newPage = defaultNewPageTemplate.getPage(0);
				} else {
					newPage = new PDPage(PDRectangle.A4);
				}
			} 			
			if(position == 0) {
				targetPDDocument.addPage(newPage);
			} 
			
			for(PDPage page : pdDocument.getPages()) {
				targetPDDocument.addPage(page);
			}
			
			if(position == -1) {
				targetPDDocument.addPage(newPage);
			}
		    return targetPDDocument;
		} catch (IOException e) {
			logger.error("error to add blank page", e);
		}
		return null;
	}
	
	public boolean checkPdfA(File pdfFile) {
		logger.info("check pdfa validity");
		try {
			PreflightParser parser = new PreflightParser(pdfFile);  
			parser.parse();
			PreflightDocument document = parser.getPreflightDocument();
	        document.validate();  
	        ValidationResult result = document.getResult();  
		    document.close();
	        for(ValidationError v : result.getErrorsList()) {
	        	if(v.getErrorCode().startsWith("7")) {
	        		logger.warn("pdf validation error " + v.getErrorCode() + " : " + v.getDetails());
	        	}
	        	//TODO probleme pdfa non conforme
	        	if(v.getErrorCode().equals("7.1")) {
	        		logger.info("contains PDFA metadata");
	        		return true;
	        	}
	        }
		    XMPMetadata metadata = result.getXmpMetaData();
		    if (metadata == null) {
		    	logger.warn("not complient to PDFA");
		        return false;
		    } else {
		    	PDFAIdentificationSchema id = metadata.getPDFIdentificationSchema();
		    	System.err.println(id.getConformance());
		    	logger.info("complient to PDFA");
		    	return true;
		    }

		} catch (Exception e) {
			logger.error("check error", e);
		}
		return false;
	}
	
	private PDDocument useNextSignatureField(PDDocument pdDocument) throws Exception {
		//on verouille le prochain champ signature (par ordre alphabetique)
        PDDocumentCatalog cat = pdDocument.getDocumentCatalog();
		PDAcroForm pdAcroForm = cat.getAcroForm();
		if(pdAcroForm != null) {
			pdAcroForm.setNeedAppearances(false);
			List<PDSignatureField> signatureFields = pdDocument.getSignatureFields();
			signatureFields = signatureFields.stream().sorted(Comparator.comparing(PDSignatureField::getPartialName)).collect(Collectors.toList());
			for(PDSignatureField pdSignatureField : signatureFields) {
				if(pdSignatureField.getSignature() == null) {
					pdSignatureField.setValue(new PDSignature());
					break;
				}
			}

		}
		return pdDocument;
	}
	
	private PDDocument convertToPDFA(PDDocument pdDocument) throws Exception {
		
		//TODO repair convert...
		PDFont font = PDTrueTypeFont.loadTTF(pdDocument, new ClassPathResource("Helvetica.ttf").getFile());
		PDFont font2 = PDTrueTypeFont.loadTTF(pdDocument, new ClassPathResource("Helvetica-Bold.ttf").getFile());
		PDType0Font.load(pdDocument, new ClassPathResource("Helvetica.ttf").getFile());
		PDType0Font.load(pdDocument, new ClassPathResource("Helvetica-Bold.ttf").getFile());
		
		PDDocumentCatalog cat = pdDocument.getDocumentCatalog();
		
		PDAcroForm pdAcroForm = cat.getAcroForm();
		if(pdAcroForm != null) {
			PDResources pdResources = new PDResources();
			pdAcroForm.setDefaultResources(pdResources);
		    List<PDField> fields = new ArrayList<>(pdAcroForm.getFields());
		    //si plus de champ signature vide on applati
		    if(processFields(fields, pdResources, font)) {
		    	pdAcroForm.flatten();
		    }
		}

		cat.getCOSObject().removeItem(COSName.D);
		COSDictionary trailer = new COSDictionary();
		pdDocument.getDocument().setTrailer(trailer);
		trailer.setItem(COSName.ROOT, pdDocument.getDocumentCatalog());
		cat.getCOSObject().setName(COSName.SMASK, "None");
		cat.getCOSObject().setName(COSName.SUBTYPE, "pdf");
		
		PDDocumentInformation info = pdDocument.getDocumentInformation();
		
        XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();
        
        AdobePDFSchema pdfSchema = xmpMetadata.createAndAddAdobePDFSchema();
        pdfSchema.setKeywords(info.getKeywords());
        pdfSchema.setProducer(info.getProducer());
        
        DublinCoreSchema dublinCoreSchema = xmpMetadata.createAndAddDublinCoreSchema();
        dublinCoreSchema.setTitle(info.getTitle());
        dublinCoreSchema.setDescription(info.getSubject());
        dublinCoreSchema.addCreator(info.getCreator());
        
        XMPBasicSchema xmpBasicSchema = xmpMetadata.createAndAddXMPBasicSchema();
        xmpBasicSchema.setCreatorTool(info.getCreator());
        xmpBasicSchema.setCreateDate(info.getCreationDate());
        xmpBasicSchema.setModifyDate(info.getModificationDate());
        
        PDFAIdentificationSchema pdfaid = xmpMetadata.createAndAddPFAIdentificationSchema();
    	pdfaid.setConformance("B");
        pdfaid.setPart(1);
        pdfaid.setAboutAsSimple("");
        
        XmpSerializer serializer = new XmpSerializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serializer.serialize(xmpMetadata, baos, false);
    
        PDMetadata metadata = new PDMetadata(pdDocument);
        cat.setMetadata(metadata);
        
        metadata.importXMPMetadata(baos.toByteArray());

        InputStream colorProfile = PdfService.class.getResourceAsStream("/sRGB.icc");
        PDOutputIntent intent = new PDOutputIntent(pdDocument, colorProfile);
        intent.setInfo("sRGB IEC61966-2.1");
        intent.setOutputCondition("sRGB IEC61966-2.1");
        intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
        intent.setRegistryName("http://www.color.org");
        cat.addOutputIntent(intent);
        
        return pdDocument;	
	}
	
	private boolean processFields(List<PDField> fields, PDResources resources, PDFont font) {

		boolean noSignFieldEmpty = true; 
		for(PDField f : fields) {
			logger.debug("process :" + f.getFullyQualifiedName() + " : " + f.getFieldType());
			if(f instanceof PDSignatureField) {
        		if(((PDSignatureField) f).getSignature() == null) {
        			noSignFieldEmpty = false;
        		}
        	}
			f.setReadOnly(true);
        	String value = "";
			try {
	        	if (f instanceof PDTextField) {
	        		value = f.getValueAsString();
	        	}
	        	if (f instanceof PDCheckBox) {
	        		PDCheckBox box = (PDCheckBox) f;
	        		if(box.isChecked()) {
	        			value = "Yes";
	        		} else {
	        			value = "Off";
	        		}
	        	}
	        	if(f instanceof PDSignatureField) {
	        		continue;
	        	}
	            f.setValue(value);
	        } catch (IOException e) {
	            if (e.getMessage().matches("Could not find font: /.*")) {
	                String fontName = e.getMessage().replaceAll("^[^/]*/", "");
	                System.out.println("Adding fallback font for: " + fontName);
	                resources.put(COSName.getPDFName(fontName), font);
	                try {
	                    f.setValue(value);
	                } catch (IOException e1) {
	                    logger.error("process fields error", e1);
	                }
	            } else {
	            	logger.error("process fields error", e);
	            }
	        }
	        if (f instanceof PDNonTerminalField) {
	            processFields(((PDNonTerminalField) f).getChildren(), resources, font);
	        }
	    }
		return true;
	}
	
	public int[] getSignFieldCoord(File pdfFile) {
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
			PDPage pdPage = pdDocument.getPage(0);
			List<PDSignatureField> signatureFields = pdDocument.getSignatureFields();
			signatureFields = signatureFields.stream().sorted(Comparator.comparing(PDSignatureField::getPartialName)).collect(Collectors.toList());
			for(PDSignatureField pdSignatureField : signatureFields) {
				if(pdSignatureField.getValue() == null) {
					int[] pos = new int[2];
					pos[0] = (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX();
					pos[1] = (int) pdPage.getBBox().getHeight() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftY() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getHeight();
		    		return pos;
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				pdDocument.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}

		return null;
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
			logger.error("error on get page as base 64 image", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					logger.error("enable to close document", e);
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
			logger.error("error on get pdf parameters", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					logger.error("enable to close document", e);
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
			logger.error("error on convert page to base 64 image", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					logger.error("enable to close document", e);
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
			logger.error("error on convert page to image", e);
		} finally {
			if (pdDocument != null) {
				try {
					pdDocument.close();
				} catch (IOException e) {
					logger.error("enable to close document", e);
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
