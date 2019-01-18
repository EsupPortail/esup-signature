package org.esupportail.esupsignature.service;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.domain.Document;
import org.springframework.stereotype.Service;

@Service
public class FileService {
	
	public File inputStreamToFile(InputStream inputStream, String prefix, String suffix) throws IOException {
		File file = File.createTempFile(prefix, suffix);
		OutputStream outputStream = new FileOutputStream(file);
		IOUtils.copy(inputStream, outputStream);
		outputStream.close();
		return file;
	}

	public File fromBase64Image(String base64Image, String name) throws IOException, SQLException {
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
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();
	    ImageIO.write(dimg, "png", fileImage);
	    return fileImage;
	}  
	
	public String getBase64Image(Document file) throws IOException, SQLException {
		BufferedImage imBuff = ImageIO.read(file.getBigFile().getBinaryFile().getBinaryStream());
		return getBase64Image(imBuff, file.getFileName());
	}
	
	public String getBase64Image(BufferedImage imBuff, String name) throws IOException, SQLException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(imBuff, "png", baos);
        baos.flush();
        String out = DatatypeConverter.printBase64Binary(baos.toByteArray());
        baos.close();
        return out;
        
	}
}
