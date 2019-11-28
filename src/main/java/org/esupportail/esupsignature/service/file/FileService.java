package org.esupportail.esupsignature.service.file;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Map;

@Service
public class FileService {
	
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	public MultipartFile toMultipartFile(InputStream file, String name, String mimeType) {
		try {
			return new MockMultipartFile(name, name, mimeType, file);
		} catch (IOException e) {
			logger.error("enable to convert to multipartfile", e);
		}
		return null;
	}

	public File fromBase64Image(String base64Image, String name) throws IOException {
		File fileImage = File.createTempFile(name, ".png");
		ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(base64Image.substring(base64Image.lastIndexOf(',') + 1).trim()));
        BufferedImage image = ImageIO.read(bis);
        ImageIO.write(image, "png", fileImage);
        return fileImage;
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

	public InputStream bufferedImageToInputStream(BufferedImage image, String type) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(image, type, os);
		return new ByteArrayInputStream(os.toByteArray());
	}

	public InputStream notFoundImageToInputStream(String type) throws IOException {
		return stringToImageFile("PAGE NOT\n FOUND", type);
	}
	
	public InputStream stringToImageFile(String text, String type) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedImage  image = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = (Graphics2D) image.getGraphics();
        Font font = new Font("Helvetica", Font.PLAIN, 40); 
        graphics2D.setColor(Color.white);
	    graphics2D.fillRect(0, 0, 200, 150);
	    graphics2D.setColor(Color.black);
	    Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
	    map.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
	    font = font.deriveFont(map);
	    graphics2D.setFont(font);
	    graphics2D.setRenderingHint(
	            RenderingHints.KEY_TEXT_ANTIALIASING,
	            RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
	    int lineWeight = 15;
	    for (String line : text.split("\n")) {
	    	graphics2D.drawString(line, 5, lineWeight += graphics2D.getFontMetrics().getHeight());
	        //graphics2D.drawString(text, 0, 40);
	    }
	    graphics2D.dispose();
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
	
	public String base64Transparence(String base64Image) {
		BufferedImage image = makeColorTransparent(base64StringToImg(base64Image));
		return imgToBase64String(image, "png");
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
	
	public BufferedImage makeColorTransparent(BufferedImage im)
	   {
		final ImageFilter filter = new RGBImageFilter() {

			@Override
			public int filterRGB(int x, int y, int rgb) {
				final Color filterColor = new Color(rgb);

				if(colorsAreSimilar(filterColor, Color.WHITE, 40)) {
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

}
