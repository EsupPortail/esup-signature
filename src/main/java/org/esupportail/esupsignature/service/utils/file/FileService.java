package org.esupportail.esupsignature.service.utils.file;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

@Service
public class FileService {
	
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	public String readFileToString(String path) {
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource resource = resourceLoader.getResource(path);
		return asString(resource);
	}

	public String asString(Resource resource) {
		try (Reader reader = new InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
			return FileCopyUtils.copyToString(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public File inputStreamToTempFile(InputStream inputStream, String name) throws IOException {
		File file = getTempFile("tmp_" + name);
		OutputStream outputStream = new FileOutputStream(file);
		IOUtils.copy(inputStream, outputStream);
		outputStream.close();
		inputStream.close();
		return file;
	}

	public ByteArrayOutputStream copyInputStream(InputStream inputStream) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			inputStream.transferTo(baos);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos;
	}

	public MockMultipartFile toMultipartFile(InputStream file, String name, String mimeType) {
		try {
			return new MockMultipartFile(name, name, mimeType, file);
		} catch (IOException e) {
			logger.error("unable to convert to multipartfile", e);
		}
		return null;
	}

	public InputStream fromBase64Image(String base64Image) {
		return new ByteArrayInputStream(Base64.getDecoder().decode(base64Image.substring(base64Image.lastIndexOf(',') + 1).trim()));
	}

	public String getContentType(File file) {
		try {
			return java.nio.file.Files.probeContentType(file.toPath());
		} catch (IOException e) {
			logger.error("can't get content type", e);
		}
		return null;
	}

	public String getExtension(String name) {
		return FilenameUtils.getExtension(name);
	}
	
	public String getNameOnly(String name) {
		return FilenameUtils.getBaseName(name);
	}

	public File getTempFile(String name) {
		try {
			File tempFile = File.createTempFile(getNameOnly(name), getExtension(name));
			tempFile.deleteOnExit();
			return tempFile;
		} catch (IOException e) {
			logger.error("unable to create temp file", e);
		}
		return null;
	}


	public String getBase64Image(Document document) throws IOException {
		BufferedImage imBuff = ImageIO.read(document.getInputStream());
		return getBase64Image(imBuff, document.getFileName());
	}

	public String getBase64Image(InputStream is, String name) throws IOException {
		BufferedImage imBuff = ImageIO.read(is);
		return getBase64Image(imBuff, name);
	}

	public String getBase64Image(BufferedImage imBuff, String name) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imBuff, "png", baos);
        baos.flush();
        String out = DatatypeConverter.printBase64Binary(baos.toByteArray());
        baos.close();
        return out;
	}

	public static InputStream bufferedImageToInputStream(BufferedImage image, String type) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(image, type, os);
		return new ByteArrayInputStream(os.toByteArray());
	}

	public void copyFile(File source, File dest) throws IOException {
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	        is = new FileInputStream(source);
	        os = new FileOutputStream(dest);
	        byte[] buffer = new byte[1024];
	        int length;
	        while ((length = is.read(buffer)) > 0) {
	            os.write(buffer, 0, length);
	        }
	    } finally {
	        is.close();
	        os.close();
	    }
	}
	
	public InputStream base64Transparence(String base64Image) {
		BufferedImage image = makeColorTransparent(base64StringToImg(base64Image));
		String base64ImageTransparent = imgToBase64String(image, "png");
		return fromBase64Image(base64ImageTransparent);
	}
	
	
	public static boolean colorsAreSimilar(final Color c1, final Color c2, final int tolerance) {
		int r1 = c1.getRed();
		int g1 = c1.getGreen();
		int b1 = c1.getBlue();
		int r2 = c2.getRed();
		int g2 = c2.getGreen();
		int b2 = c2.getBlue();

		return ((r2 - tolerance <= r1) && (r1 <= r2 + tolerance) &&
				(g2 - tolerance <= g1) && (g1 <= g2 + tolerance) &&
				(b2 - tolerance <= b1) && (b1 <= b2 + tolerance));
	}
	
	public BufferedImage makeColorTransparent(BufferedImage im) {
		final ImageFilter filter = new RGBImageFilter() {

			@Override
			public int filterRGB(int x, int y, int rgb) {
				final Color filterColor = new Color(rgb);

				if(colorsAreSimilar(filterColor, Color.WHITE, 20)) {
					return 0x00FFFFFF & rgb;
				} else {
					return rgb;
				}
			}
		};
		final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
		return toBufferedImage(Toolkit.getDefaultToolkit().createImage(ip));
	}
	
	public static BufferedImage toBufferedImage(Image image) 
	  { 
	  if (image instanceof BufferedImage) return (BufferedImage) image; 
	 
	  // This code ensures that all the pixels in the image are loaded 
	  image = new ImageIcon(image).getImage(); 
	 
	  BufferedImage bimage = new BufferedImage(image.getWidth(null),image.getHeight(null), 
	    BufferedImage.TYPE_INT_ARGB); 
	  Graphics g = bimage.createGraphics(); 
	  g.drawImage(image,0,0,null); 
	  g.dispose(); 
	  return bimage; 
	  } 
	
	public static String imgToBase64String(RenderedImage img, String formatName) {
	    final ByteArrayOutputStream os = new ByteArrayOutputStream();
	    try {
	        ImageIO.write(img, formatName, Base64.getEncoder().wrap(os));
	        return os.toString(StandardCharsets.ISO_8859_1.name());
	    } catch (final IOException ioe) {
	        throw new UncheckedIOException(ioe);
	    }
	}

	public static BufferedImage base64StringToImg(String base64Image) {
	    try {
	        return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Image.substring(base64Image.lastIndexOf(',') + 1).trim())));
	    } catch (final IOException ioe) {
	        throw new UncheckedIOException(ioe);
	    }
	}

	public InputStream addTextToImage(InputStream imageStream, SignRequestParams signRequestParams) throws IOException {
		InputStream textAddedInputStream = imageStream;
		String[] arr = signRequestParams.getExtraText().split("\\s*\n\\s*");
		List<String> text = Arrays.asList(arr);
		if(text.size() > 0) {
			final BufferedImage signImage = ImageIO.read(imageStream);
			int widthOffset = 0;
			int heightOffset = 0;
			if(signRequestParams.getAddExtra()) {
				if(signRequestParams.getExtraOnTop()) {
					heightOffset = (int) Math.round((signRequestParams.getSignHeight() / 0.75 / signRequestParams.getSignScale()) - (signImage.getHeight() / 3));
				} else {
					widthOffset = (int) Math.round((signRequestParams.getSignWidth() / 0.75 / signRequestParams.getSignScale()) - (signImage.getWidth() / 3));
				}
			}
			BufferedImage  image = new BufferedImage(signImage.getWidth() + (widthOffset * 3),  signImage.getHeight() + (heightOffset * 3), BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics2D = (Graphics2D) image.getGraphics();
			graphics2D.drawImage(signImage, 0, heightOffset * 3, null);
			graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int lineCount = 1;
			Map<TextAttribute, Object> map = new Hashtable<>();
			int fontSize = 35;
			map.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
			Font font = new Font("DejaVu Sans Condensed", Font.PLAIN, fontSize);
			font = font.deriveFont(map);
			graphics2D.setFont(font);
			graphics2D.setPaint(Color.black);
			for (String line : text) {
				FontMetrics fm = graphics2D.getFontMetrics();
				graphics2D.drawString(new String(line.getBytes(), StandardCharsets.UTF_8), widthOffset * 3, fm.getHeight() * lineCount);
				lineCount++;
			}
			FontMetrics fm = graphics2D.getFontMetrics();
			graphics2D.drawString("", widthOffset * 3, fm.getHeight() * lineCount + 1);
			graphics2D.dispose();
			File fileImage = getTempFile("sign.png");
			ImageIO.write(image, "png", fileImage);
			textAddedInputStream = new FileInputStream(fileImage);
		}
		return textAddedInputStream;
	}

	public void addImageWatermark(InputStream watermarkImageFile, InputStream sourceImageFile, File destImageFile, Color color) {
		try {
			BufferedImage sourceImage = ImageIO.read(sourceImageFile);
			BufferedImage watermarkImage = ImageIO.read(watermarkImageFile);
			changeColor(watermarkImage, 255, 255, 255, 0, 0, 0);
			changeColor(watermarkImage, 0, 0, 0, color.getRed(), color.getGreen(), color.getBlue());
			Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();
			AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f);
			g2d.setComposite(alphaChannel);
			int topLeftX = (sourceImage.getWidth() - watermarkImage.getWidth()) / 2;
			int topLeftY = (sourceImage.getHeight() - watermarkImage.getHeight()) / 2;
			g2d.drawImage(watermarkImage, topLeftX, topLeftY, null);
			ImageIO.write(sourceImage, "png", destImageFile);
			g2d.dispose();
		} catch (IOException ex) {
			logger.error(ex.getMessage());
		}
	}

	public InputStream svgToPng(InputStream svgInputStream) throws IOException {
		File file = getTempFile("sceau.png");
		ImageIO.write(ImageIO.read(svgInputStream), "PNG", file);
		return new FileInputStream(file);
	}

	public File getFileFromUrl(String url) throws IOException {
		File file = getTempFile(url.split("/")[url.split("/").length-1]);
		FileUtils.copyURLToFile(new URL(url), file);
		return file;
	}

	public boolean isFileContainsText(File file, String text) {
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if(line.contains(text)) {
					return true;
				}
			}
		} catch(FileNotFoundException e) {
			logger.error(e.getMessage());
		}
		return false;
	}

    public Map<String, Object> getFileResponse(byte[] bytes, String fileName, String contentType) {
        Map<String, Object> fileResponse = new HashMap<>();
        fileResponse.put("inputStream", new ByteArrayInputStream(bytes));
        fileResponse.put("fileName", fileName);
        fileResponse.put("contentType", contentType);
        return fileResponse;
    }

	public void changeColor(BufferedImage imgBuf, int oldRed, int oldGreen, int oldBlue, int newRed, int newGreen, int newBlue) {

		int RGB_MASK = 0x00ffffff;
		int ALPHA_MASK = 0xff000000;

		int oldRGB = oldRed << 16 | oldGreen << 8 | oldBlue;
		int toggleRGB = oldRGB ^ (newRed << 16 | newGreen << 8 | newBlue);

		int w = imgBuf.getWidth();
		int h = imgBuf.getHeight();

		int[] rgb = imgBuf.getRGB(0, 0, w, h, null, 0, w);
		for (int i = 0; i < rgb.length; i++) {
			if ((rgb[i] & RGB_MASK) == oldRGB) {
				rgb[i] ^= toggleRGB;
			}
		}
		imgBuf.setRGB(0, 0, w, h, rgb, 0, w);
	}
}
