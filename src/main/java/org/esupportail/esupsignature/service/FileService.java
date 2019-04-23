package org.esupportail.esupsignature.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.Files;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class FileService {
	
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);
	
	public MultipartFile toMultipartFile(File file, String mimeType) {
		try {
			return new MockMultipartFile(file.getName(), file.getName(), mimeType, new FileInputStream(file));
		} catch (IOException e) {
			logger.error("enable to convert to multipartfile", e);
		}
		return null;
	}
	
	public File inputStreamToFile(InputStream inputStream, String name) throws IOException {
		File file = new File(Files.createTempDir(), name);
		OutputStream outputStream = new FileOutputStream(file);
		IOUtils.copy(inputStream, outputStream);
		outputStream.close();
		return file;
	}

	public File multipartPdfToFile(MultipartFile multiPartFile) throws IOException	{
		File file = File.createTempFile(multiPartFile.getOriginalFilename(), ".pdf");
	    file.createNewFile(); 
	    FileOutputStream fos = new FileOutputStream(file); 
	    fos.write(multiPartFile.getBytes());
	    fos.close(); 
	    return file;
	}
	
	public File fromBase64Image(String base64Image, String name) throws IOException {
		File fileImage = File.createTempFile(name, ".png");
		ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(base64Image.substring(base64Image.lastIndexOf(',') + 1).trim()));
        BufferedImage image = ImageIO.read(bis);
        ImageIO.write(image, "png", fileImage);
        return fileImage;
	}
	
	public File resize(File img, int newW, int newH) throws IOException {
	    return resize(ImageIO.read(img), newW, newH);
	}
	
	public File resize(BufferedImage img, int newW, int newH) throws IOException {
		File fileImage = File.createTempFile("img", ".png");
	    BufferedImage thumbnail = Thumbnails.of(img).height(newH).asBufferedImage();
	    ImageIO.write(thumbnail, "png", fileImage);	
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
	
	public String getExtension(File file) {
		return FilenameUtils.getExtension(file.getName());
	}
	
	public String getNameOnly(File file) {
		return FilenameUtils.getBaseName(file.getName());
	}
	
	public String getBase64Image(Document file) throws IOException, SQLException {
		BufferedImage imBuff = ImageIO.read(file.getBigFile().getBinaryFile().getBinaryStream());
		return getBase64Image(imBuff, file.getFileName());
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

	public File notFoundImageToInputStream(String type) throws IOException {
		return stringToImageFile("PAGE NOT FOUND", type);
	}
	
	public File stringToImageFile(String text, String type) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        FontRenderContext fc = g2d.getFontRenderContext();
        Font font = new Font("Courier", Font.PLAIN, 14);
        Rectangle2D bounds = font.getStringBounds(text, fc);
        image = new BufferedImage(100, 75, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) image.getGraphics();
        g2d.setColor(Color.white);
	    g2d.fillRect(0, 0, 100, 75);
	    g2d.setColor(Color.black);
		g2d.drawString(text, 0, (int)-bounds.getY());
	    g2d.dispose();
	    ImageIO.write(image, type, os);
		return inputStreamToFile(new ByteArrayInputStream(os.toByteArray()), "paraphe." + type);
	}
	
	public File renameFile(File file, String name) {
		File newfile = new File(Files.createTempDir(), name);
		boolean result = file.renameTo(newfile);
		if (result) {
			return newfile;
		} else {
			return null;
		}
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
	
}
