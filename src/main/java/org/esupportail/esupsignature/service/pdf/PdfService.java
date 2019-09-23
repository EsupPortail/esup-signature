package org.esupportail.esupsignature.service.pdf;

import com.google.common.io.Files;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.esupportail.esupsignature.config.pdf.PdfProperties;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.SignRequestParams.SignType;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Configuration
@EnableConfigurationProperties(PdfProperties.class)
public class PdfService {

	private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

	private PdfProperties pdfProperties;

	public PdfService(PdfProperties pdfProperties) {
		this.pdfProperties = pdfProperties;
	}

	@Resource
	private FileService fileService;


	public File stampImage(File toSignFile, SignRequest signRequest, User user, boolean addPage, boolean addDate) throws InvalidPasswordException, IOException {
		//TODO add ip ? + date + nom ?
		SignRequestParams params = signRequest.getSignRequestParams();
		SignRequestParams.SignType signType = params.getSignType();
    	PdfParameters pdfParameters = getPdfParameters(new FileInputStream(toSignFile));
		toSignFile = formatPdf(toSignFile, params, addPage);
		try {
			PDDocument pdDocument = PDDocument.load(toSignFile);

			PDPage pdPage = pdDocument.getPage(params.getSignPageNumber() - 1);
			PDImageXObject pdImage;
					
			PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);
			float height = pdPage.getMediaBox().getHeight();
			float width = pdPage.getMediaBox().getWidth();
			
			int xPos = (int) params.getXPos();
			int yPos = (int) params.getYPos();
			DateFormat dateFormat = new SimpleDateFormat("dd MMMM YYYY HH:mm:ss", Locale.FRENCH);
			InputStream signImage = new FileInputStream(PdfService.class.getResource("/sceau.png").getFile());
			if(signType.equals(SignType.visa)) {
				try {
					addText(contentStream, "Visé par " + user.getFirstname() + " " + user.getName(), xPos, yPos, PDType1Font.HELVETICA);
					if(addDate) {
						addText(contentStream, "Le " + dateFormat.format(new Date()), xPos, yPos + 20, PDType1Font.HELVETICA);
					}
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			} else {
				//TODO add nom prenom ?
				//addText(contentStream, "Signé par " + user.getFirstname() + " " + user.getName(), xPos, yPos, PDType1Font.HELVETICA);
				if(addDate) {
					addText(contentStream, "Le " + dateFormat.format(new Date()), xPos, yPos, PDType1Font.HELVETICA);
					//topHeight = 20;
				}
				signImage = user.getSignImage().getInputStream();
			}
			int topHeight = 0;
            BufferedImage bufferedImage = ImageIO.read(signImage);
			int[] size = getSignSize(bufferedImage);
			if(pdfParameters.getRotation() == 0) {
				AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
		        tx.translate(0, - bufferedImage.getHeight(null));
		        AffineTransformOp op = new AffineTransformOp(tx,AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		        bufferedImage = op.filter(bufferedImage, null);
				File flipedSignImage = File.createTempFile("preview", ".png");
				ImageIO.write(bufferedImage, "png", flipedSignImage);
				pdImage = PDImageXObject.createFromFileByContent(flipedSignImage, pdDocument);
				flipedSignImage.delete();
				contentStream.transform(new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, height)));
				contentStream.drawImage(pdImage, xPos, yPos + topHeight, size[0], size[1]);
			} else {
				AffineTransform at = new java.awt.geom.AffineTransform(0, 1, -1, 0, width, 0);
			    contentStream.transform(new Matrix(at));
				File flipedSignImage = File.createTempFile("preview", ".png");
				ImageIO.write(bufferedImage, "png", flipedSignImage);
				pdImage = PDImageXObject.createFromFileByContent(flipedSignImage, pdDocument);
				flipedSignImage.delete();
			    contentStream.drawImage(pdImage, xPos, yPos + topHeight - 37 , size[0], size[1]);
			}
			
			contentStream.close();
			pdDocument.save(toSignFile);
			pdDocument.close();
		    try {
	    		return writeMetadatas(convertGS(toSignFile), signRequest);
			} catch (Exception e) {
				logger.error("enable to convert to pdf A", e);
			}
		} catch (IOException e) {
			logger.error("error to add image", e);
		}
		return null;
	}
	
	public File formatPdf(File file, SignRequestParams params, boolean addPage) throws InvalidPasswordException, IOException {
        
		if(!SignRequestParams.NewPageType.none.equals(params.getNewPageType()) && addPage) {
			PDDocument pdDocument = PDDocument.load(file);
			if(SignRequestParams.NewPageType.onBegin.equals(params.getNewPageType())) {
    			pdDocument = addNewPage(pdDocument, null, 0);
    			params.setSignPageNumber(1);
    		} else {
    			pdDocument = addNewPage(pdDocument, null, -1);
    			params.setSignPageNumber(getPdfParameters(new FileInputStream(file)).getTotalNumberOfPages());
    		}
	        pdDocument.save(file);
	        pdDocument.close();
		}

    	return file;
	}
	
	public File writeMetadatas(File file, SignRequest signRequest){
		
		try {
			PDDocument pdDocument = PDDocument.load(file);
			pdDocument.setVersion(1.7f);
			
			COSDictionary trailer = pdDocument.getDocument().getTrailer();
			COSDictionary rootDictionary = trailer.getCOSDictionary(COSName.ROOT);
			rootDictionary.setItem(COSName.TYPE, COSName.CATALOG);
			rootDictionary.setItem(COSName.VERSION, COSName.getPDFName("1.7"));
			
			PDDocumentInformation info = pdDocument.getDocumentInformation();
	        info.setTitle(file.getName());
	        info.setSubject(file.getName());
	        info.setAuthor(signRequest.getCreateBy());
	        info.setCreator("GhostScript");
	        info.setProducer("esup-signature");
	        info.setKeywords("pdf, signed, " + file.getName());
	        if(info.getCreationDate() == null) {
				info.setCreationDate(Calendar.getInstance());
			}
	        info.setModificationDate(Calendar.getInstance());
	        pdDocument.setDocumentInformation(info);
	
			PDDocumentCatalog cat = pdDocument.getDocumentCatalog();
	
			XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();
	        
	        AdobePDFSchema pdfSchema = xmpMetadata.createAndAddAdobePDFSchema();
	        pdfSchema.setKeywords(info.getKeywords());
	        pdfSchema.setProducer(info.getProducer());
	        
	        DublinCoreSchema dublinCoreSchema = xmpMetadata.createAndAddDublinCoreSchema();
	        dublinCoreSchema.setTitle(info.getTitle());
	        dublinCoreSchema.setDescription(info.getSubject());
	        dublinCoreSchema.addCreator(info.getAuthor());
	        
	        XMPBasicSchema xmpBasicSchema = xmpMetadata.createAndAddXMPBasicSchema();
	        xmpBasicSchema.setCreatorTool(info.getCreator());
	        xmpBasicSchema.setCreateDate(info.getCreationDate());
	        xmpBasicSchema.setModifyDate(info.getModificationDate());
	        
	        PDFAIdentificationSchema pdfaid = xmpMetadata.createAndAddPFAIdentificationSchema();
	    	pdfaid.setConformance("B");
	        pdfaid.setPart(pdfProperties.getPdfALevel());
	        pdfaid.setAboutAsSimple("");
	        
	        XmpSerializer serializer = new XmpSerializer();
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        serializer.serialize(xmpMetadata, baos, true);
	    
	        PDMetadata metadata = new PDMetadata(pdDocument);
	        metadata.importXMPMetadata(baos.toByteArray());
	        cat.setMetadata(metadata);
	        pdDocument.save(file);
			pdDocument.close();
		} catch (IOException | BadFieldValueException | TransformerException e) {
			logger.error("error on write metadatas", e);
		}
        return file;	
	}
	
	public File convertGS(File file) throws IOException {
		
    	if(!isPdfAComplient(file)) {
		    File targetFile =  File.createTempFile(fileService.getNameOnly(file.getName()), "." + fileService.getExtension(file.getName()));
		    String defFile =  PdfService.class.getResource("/PDFA_def.ps").getFile();
		    String cmd = pdfProperties.getPathToGS() + "gs -dPDFA=" + pdfProperties.getPdfALevel() + " -dBATCH -dNOPAUSE -sColorConversionStrategy=RGB -sDEVICE=pdfwrite -dPDFACompatibilityPolicy=1 -sOutputFile='" + targetFile.getAbsolutePath() + "' '" + defFile + "' '" + file.getAbsolutePath() + "'";
	    	logger.info("GhostScript PDF/A convertion : " + cmd);
	    	
	    	ProcessBuilder processBuilder = new ProcessBuilder();
	    	processBuilder.command("bash", "-c", cmd);
	    	processBuilder.directory(new File("/tmp"));
	    	try {
	    		Process process = processBuilder.start();
	    		StringBuilder output = new StringBuilder();
	    		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    		String line;
	    		while ((line = reader.readLine()) != null) {
	    			output.append(line + "\n");
	    		}

	    		int exitVal = process.waitFor();
	    		if (exitVal == 0) {
	    			logger.info(output.toString());
	    			logger.info("Convert success");
	    		} else {
	    			logger.warn(output.toString());
	    			logger.warn("Convert fail");
	    			return null;
	    		}
	    	} catch (IOException | InterruptedException e) {
	    		logger.error("GhostScript launc error", e);
	    	}
	    	file.delete();
		    return targetFile;
    	} else {
    		return file;
    	}
	}
	
	public boolean isPdfAComplient(File pdfFile) {
		if("success".equals(checkPDFA(pdfFile, false).get(0))) {
			return true;
		}
		return false;
	}
	
	public List<String> checkPDFA(File pdfFile, boolean fillResults) {
		List<String> result = new ArrayList<>();
		VeraGreenfieldFoundryProvider.initialise();

		try {
			PDFAParser parser = Foundries.defaultInstance().createParser(pdfFile);
			PDFAValidator validator = Foundries.defaultInstance().createValidator(parser.getFlavour(), false);
		    ValidationResult validationResult = validator.validate(parser);
		    if (validationResult.isCompliant()) {
		    	result.add("success");
		    	result.add("File is complient with PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
		    } else {
		    	result.add("danger");
		    	result.add("File is not complient with PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
		    	if(fillResults) {
					for (TestAssertion test : validationResult.getTestAssertions()) {
						result.add(test.getRuleId().getClause() + " : " + test.getMessage());
					}
				}
		    }
			validator.close();
		    parser.close();
		} catch (Exception e) {
			logger.error("check error", e);
		}
		return result;
	}
	
	@Deprecated
	private PDDocument useNextSignatureField(PDDocument pdDocument) throws Exception {
		//on verouille le prochain champ signature (par ordre d'apparition)
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
				PDDocument defaultNewPageTemplate = PDDocument.load(new ClassPathResource("/templates/pdf/defaultnewpage.pdf", PdfService.class).getFile());
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
	
	private boolean checkSignFieldsEmpty(File file) throws InvalidPasswordException, IOException {
		PDDocument pdDocument = PDDocument.load(file);
		PDDocumentCatalog cat = pdDocument.getDocumentCatalog();
		List<PDField> fields = cat.getAcroForm().getFields();
		boolean signFieldEmpty = false; 
		for(PDField f : fields) {
			logger.debug("process :" + f.getFullyQualifiedName() + " : " + f.getFieldType());
			if(f instanceof PDSignatureField) {
        		if(((PDSignatureField) f).getSignature() == null) {
        			signFieldEmpty = true;
        		}
        	}
		}
		pdDocument.close();
		logger.info("sign field empty : " + signFieldEmpty);
		return signFieldEmpty;
	}
	
	@Deprecated
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
		return noSignFieldEmpty;
	}

	public void addText(PDPageContentStream contentStream, String text, int xPos, int yPos, PDFont font) throws IOException {
		int fontSize = 8;
		contentStream.beginText();
		contentStream.newLineAtOffset(xPos, 830 - yPos);
		contentStream.setFont(font, fontSize);
		contentStream.showText(text);
		contentStream.endText();
	}

	public int[] getSignSize(BufferedImage bimg) throws IOException {
		int signWidth;
		int signHeight;
		if(bimg.getWidth() <= pdfProperties.getSignWidthThreshold() * 2) {
			signWidth = bimg.getWidth() / 2;
			signHeight = bimg.getHeight() / 2;
		} else {
			signWidth =  pdfProperties.getSignWidthThreshold();
			double percent = ((double) pdfProperties.getSignWidthThreshold() / (double)bimg.getWidth());
			signHeight = (int) (percent * bimg.getHeight());
		}
		return new int[]{signWidth, signHeight};
	}

	public int[] getSignSize(InputStream signFile) throws IOException {
		BufferedImage bimg = ImageIO.read(signFile);
		return getSignSize(bimg);
	}
	
	/*
	public File formatSignFields(File file, long signNumber, SignRequestParams parameters) throws InvalidPasswordException, IOException {
		PDDocument pdDocument = PDDocument.load(file);
		PDPage pdPage = pdDocument.getPage(0);
		List<PDSignatureField> pdSignatureFields = pdDocument.getSignatureFields();
		PDSignatureField pdSignatureField = pdSignatureFields.get((int) signNumber);
		if(pdSignatureField.getSignature() == null) {
			PDAnnotationWidget widget = pdSignatureField.getWidgets().get(0);
			int[] pos = new int[2];
			pos[0] = (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX();
			pos[1] = (int) pdPage.getBBox().getHeight() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftY() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getHeight();
			PDRectangle rect = new PDRectangle(pos[0], pos[1], 100, 75);
			widget.getRectangle().
		}
		pdDocument.saveIncrementalForExternalSigning(new FileOutputStream(file));
		pdDocument.close();
		return file;
	}
	*/
	public int[] getSignFieldCoord(InputStream pdfFile, long signNumber) {
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
			PDPage pdPage = pdDocument.getPage(0);
			List<PDSignatureField> pdSignatureFields = pdDocument.getSignatureFields();
			//pdSignatureFields = pdSignatureFields.stream().sorted(Comparator.comparing(PDSignatureField::getPartialName)).collect(Collectors.toList());
			if(pdSignatureFields.size() < signNumber + 1) {
				return null;
			}
			PDSignatureField pdSignatureField = pdSignatureFields.get((int) signNumber);
			if(pdSignatureField.getValue() == null) {
				int[] pos = new int[2];
				pos[0] = (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX();
				pos[1] = (int) pdPage.getBBox().getHeight() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftY() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getHeight();
	    		return pos;
			}
		} catch (IOException e) {
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
	
	public PDSignatureField getPDSignatureFieldName(InputStream pdfFile, long signNumber) {
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
			List<PDSignatureField> pdSignatureFields = pdDocument.getSignatureFields();
			return pdSignatureFields.get((int) signNumber);
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
		        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(i, pdfProperties.getPdfToImageDpi(), ImageType.RGB);
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
	
	public PdfParameters getPdfParameters(InputStream pdfFile) {
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
	        BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, pdfProperties.getPdfToImageDpi(), ImageType.RGB);
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
	
	public BufferedImage pageAsBufferedImage(InputStream pdfFile, int page) {
		BufferedImage bufferedImage = null;
		PDDocument pdDocument = null;
		try {
			pdDocument = PDDocument.load(pdfFile);
			PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
			bufferedImage = pdfRenderer.renderImageWithDPI(page, pdfProperties.getPdfToImageDpi(), ImageType.RGB);
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

	public InputStream pageAsInputStream(InputStream pdfFile, int page) throws Exception {
		BufferedImage bufferedImage = pageAsBufferedImage(pdfFile, page);
		InputStream inputStream = fileService.bufferedImageToInputStream(bufferedImage, "png");
		bufferedImage.flush();
		return inputStream; 

	}
	
}
