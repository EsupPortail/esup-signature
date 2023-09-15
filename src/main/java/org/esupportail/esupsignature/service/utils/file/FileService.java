package org.esupportail.esupsignature.service.utils.file;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

@Service
public class FileService {
	
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	private final String[] faImages = {"check-solid", "times-solid", "circle-regular", "minus-solid"};

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
			StringBuilder prefix = new StringBuilder(getNameOnly(name));
			while(prefix.length() < 3) {
				prefix.append("_");
			}
			File tempFile = File.createTempFile(prefix.toString(), getExtension(name));
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
	
	public static BufferedImage toBufferedImage(Image image)  {
		if (image instanceof BufferedImage) return (BufferedImage) image;
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
	        return os.toString(StandardCharsets.ISO_8859_1);
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

	public InputStream addTextToImage(InputStream imageStream, SignRequestParams signRequestParams, SignType signType, User user, Date date, double fixFactor) throws IOException {
		InputStream textAddedInputStream = imageStream;
		String[] arr = signRequestParams.getExtraText().split("\\s*\n\\s*");
		List<String> text = Arrays.asList(arr);
		if(signRequestParams.getAddExtra()) {
			int qualityFactor = 3;
			final BufferedImage signImage = ImageIO.read(imageStream);
			int widthOffset = (int) (signRequestParams.getExtraWidth() * qualityFactor * fixFactor);
			int heightOffset = (int) (signRequestParams.getExtraHeight() * qualityFactor * fixFactor);
			int width = (int) (signRequestParams.getSignWidth() * qualityFactor * fixFactor);
			int height = (int) (signRequestParams.getSignHeight() * qualityFactor * fixFactor);

			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			changeColor(signImage, 0, 0, 0, signRequestParams.getRed(), signRequestParams.getGreen(), signRequestParams.getBlue());
			Graphics2D graphics2D = (Graphics2D) image.getGraphics();
			if(signRequestParams.getExtraOnTop()) {
				graphics2D.drawImage(signImage, 0, heightOffset, width, height - heightOffset, null);
			} else {
				graphics2D.drawImage(signImage, 0, 0, widthOffset, height, null);
			}
			graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int lineCount = 0;
			Map<TextAttribute, Object> attributes = new Hashtable<>();
			int fontSize = (int) (12 * qualityFactor * signRequestParams.getSignScale() * fixFactor);
			setQualityParams(graphics2D);
			Font font = null;
			try {
				font = Font.createFont(Font.TRUETYPE_FONT, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream()).deriveFont(Font.PLAIN).deriveFont((float) fontSize);
			} catch (FontFormatException e) {
				e.printStackTrace();
			}
			font = font.deriveFont(attributes);
			graphics2D.setFont(font);
			graphics2D.setPaint(Color.black);
			FontMetrics fm = graphics2D.getFontMetrics();
			int lineHeight = Math.round(fontSize + fontSize * .5f);
			if(signRequestParams.getExtraType()) {
				String typeSign = "Signature calligraphique";
				if(signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa)) typeSign = "Visa";
				if(signType.equals(SignType.certSign) || signType.equals(SignType.nexuSign)) typeSign = "Signature électronique";
				if(user.getRoles().contains("ROLE_OTP")) {
					if(user.getPhone() != null) {
						typeSign = "Signature OTP : " + user.getPhone();
					} else {
						typeSign = "Signature OTP";
					}
				}
				graphics2D.drawString(typeSign, widthOffset, fm.getHeight());
				lineCount++;
			}
			if(signRequestParams.getExtraName()) {
				if(lineCount == 0) {
					graphics2D.drawString(user.getFirstname() + " " + user.getName(), widthOffset, fm.getHeight());
				} else {
					graphics2D.drawString(user.getFirstname() + " " + user.getName(), widthOffset, fm.getHeight() + lineHeight * lineCount);
				}

				lineCount++;
			}
			if(signRequestParams.getExtraDate()) {
				DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss XXX", Locale.FRENCH);
				if(lineCount == 0) {
					graphics2D.drawString("le " + dateFormat.format(date), widthOffset, fm.getHeight());
				} else {
					graphics2D.drawString("le " + dateFormat.format(date), widthOffset, fm.getHeight() + lineHeight * lineCount);
				}
				lineCount++;
			}
			for (String line : text) {
				if(lineCount == 0) {
					graphics2D.drawString(new String(line.getBytes(), StandardCharsets.UTF_8), widthOffset, fm.getHeight());
				} else {
					graphics2D.drawString(new String(line.getBytes(), StandardCharsets.UTF_8), widthOffset, fm.getHeight() + lineHeight * lineCount);
				}
				lineCount++;
			}
//			graphics2D.drawString("", 0, fm.getHeight() * lineCount + 1);
			graphics2D.dispose();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			textAddedInputStream = new ByteArrayInputStream(outputStream.toByteArray());
		}
		return textAddedInputStream;
	}

	private void setQualityParams(Graphics2D graphics2D) {
		graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		graphics2D.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	public void addImageWatermark(InputStream watermarkImageFile, InputStream sourceImageFile, ByteArrayOutputStream destImageFile, Color color, boolean extraOnTop) {
		try {
			BufferedImage sourceImage = ImageIO.read(sourceImageFile);
			BufferedImage watermarkImage = ImageIO.read(watermarkImageFile);
//			changeColor(watermarkImage, 255, 255, 255, 0, 0, 0);
//			changeColor(watermarkImage, 0, 0, 0, color.getRed(), color.getGreen(), color.getBlue());
			Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();
			AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f);
			g2d.setComposite(alphaChannel);
			double factor = (double) sourceImage.getWidth() / watermarkImage.getWidth();
			int width = sourceImage.getWidth();
			int height = (int) (watermarkImage.getHeight() * factor);
			if(!extraOnTop) {
				factor = (double) sourceImage.getHeight() / watermarkImage.getHeight();
				width = (int) (watermarkImage.getWidth() * factor);
				height = sourceImage.getHeight();
			}
			g2d.drawImage(watermarkImage, 0, sourceImage.getHeight() - height, width, height, null);
			ImageIO.write(sourceImage, "png", destImageFile);
			g2d.dispose();
		} catch (IOException ex) {
			logger.error(ex.getMessage());
		}
	}

	public InputStream getFileFromUrl(String url) throws IOException {
		return new URL(url).openStream();
	}

	public boolean isFileContainsText(InputStream file, String text) {
		Scanner scanner = new Scanner(file);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if(line.contains(text)) {
				return true;
			}
		}
		return false;
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

	public InputStream getDefaultImage(String name, String firstname, int factor) throws IOException {
		BufferedImage bufferedImage = new BufferedImage(600 * factor, 300 * factor, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = bufferedImage.createGraphics();
		graphics2D.setColor(new Color(0f,0f,0f,0f ));
		Rectangle rect = new Rectangle();
		rect.setRect(0, 0, 600 * factor, 300 * factor);
		graphics2D.fillRect(0, 0, 600 * factor, 300 * factor);
		setQualityParams(graphics2D);
		float fontSize = (float) 300 * factor / Math.max(firstname.length(), name.length());
		Font font = null;
		try {
			font = Font.createFont(Font.TRUETYPE_FONT, new ClassPathResource("/static/fonts/Signature.ttf").getInputStream()).deriveFont(Font.BOLD).deriveFont(fontSize);
		} catch (FontFormatException e) {
			logger.warn("unable to get font");
		}
		graphics2D.setFont(font);
		graphics2D.setColor(Color.BLACK);
		FontMetrics fm = graphics2D.getFontMetrics();
		int y = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
		int lineHeight = Math.round(fontSize);
		graphics2D.drawString(StringUtils.capitalize(firstname), 300 * factor, y - lineHeight);
		graphics2D.drawString(StringUtils.capitalize(name), 300 * factor, y + lineHeight);
		graphics2D.dispose();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "png", outputStream);
		return new ByteArrayInputStream(outputStream.toByteArray());
	}

	public InputStream getFaImageByIndex(int index) throws IOException {
		return new ClassPathResource("/static/images/"+ faImages[Math.abs(index) - 1] + ".png").getInputStream();
	}

	public String getFileChecksum(InputStream inputStream) throws IOException {
		return DigestUtils.sha3_256Hex(inputStream);
	}

}
