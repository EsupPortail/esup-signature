package org.esupportail.esupsignature.service.utils.file;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {
	
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	private static final Float initialFontSize = 12f;

	private final String[] faImages = {"check-solid", "times-solid", "circle-regular", "minus-solid"};

	private final GlobalProperties globalProperties;

    public FileService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public ByteArrayOutputStream copyInputStream(InputStream inputStream) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			inputStream.transferTo(baos);
		} catch (IOException e) {
			logger.error("unable to copy input stream", e);
		}
		return baos;
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

	public InputStream bufferedImageToInputStream(BufferedImage image, String type) throws IOException {
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

	public InputStream addTextToImage(InputStream imageStream, SignRequestParams signRequestParams, SignType signType, User user, Date date, Boolean otp) throws IOException {
		InputStream textAddedInputStream = imageStream;
		if(signRequestParams.getAddExtra()) {
			int qualityFactor = globalProperties.getSignatureImageDpi() / 100;
			final BufferedImage signImage = ImageIO.read(imageStream);
			int extraWidth = 0;
			int extraHeight = 0;
			int nbExtra = 0;
			if (signRequestParams.getExtraType()) nbExtra++;
			if (signRequestParams.getExtraName()) nbExtra++;
			if (signRequestParams.getExtraDate()) nbExtra++;
			if (StringUtils.hasText(signRequestParams.getExtraText())) {
				nbExtra += signRequestParams.getExtraText().split("\\s*\n\\s*").length;
			} else if (signRequestParams.getIsExtraText()) {
                nbExtra++;
            }
			if (!signRequestParams.getExtraOnTop()) {
				extraWidth = 400;
			} else {
				extraHeight = 17 * nbExtra;
			}
			int widthOffset = (int) (extraWidth * signRequestParams.getSignScale() * qualityFactor * globalProperties.getFixFactor());
			int heightOffset = (int) (extraHeight  * signRequestParams.getSignScale() * qualityFactor * globalProperties.getFixFactor());
			int width = (int) ((200 + extraWidth)  * signRequestParams.getSignScale() * qualityFactor * globalProperties.getFixFactor());
			int height = (int) ((100 + extraHeight)  * signRequestParams.getSignScale() * qualityFactor * globalProperties.getFixFactor());
			signRequestParams.setSignWidth(Math.round((float) width / qualityFactor / globalProperties.getFixFactor() / signRequestParams.getSignScale()));
			signRequestParams.setSignHeight(Math.round((float) height / qualityFactor / globalProperties.getFixFactor() / signRequestParams.getSignScale()));
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//			changeColor(signImage, 0, 0, 0, signRequestParams.getRed(), signRequestParams.getGreen(), signRequestParams.getBlue());
			Graphics2D graphics2D = (Graphics2D) image.getGraphics();
			if(signRequestParams.getExtraOnTop()) {
				graphics2D.drawImage(signImage, 0, heightOffset, width, height - heightOffset, null);
			} else {
				graphics2D.drawImage(signImage, 0, 0, widthOffset, height, null);
			}
			graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int lineCount = 0;
			Map<TextAttribute, Object> attributes = new Hashtable<>();
			int fontSize = (int) (10f * qualityFactor * signRequestParams.getSignScale() * globalProperties.getFixFactor());
			setQualityParams(graphics2D);
			try {
				Font font = Font.createFont(Font.TRUETYPE_FONT, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream()).deriveFont(Font.PLAIN).deriveFont((float) fontSize);
				font = font.deriveFont(attributes);
				graphics2D.setFont(font);
				graphics2D.setPaint(Color.black);
				FontMetrics fm = graphics2D.getFontMetrics();
				int lineHeight = Math.round(fontSize);
				if(signRequestParams.getExtraType()) {
					String typeSign = "Signature";
					if(signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa)) typeSign = "Visa";
					if(otp!= null && otp) {
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
				if(StringUtils.hasText(signRequestParams.getExtraText())) {
					List<String> text = List.of(signRequestParams.getExtraText().split("\\s*\n\\s*"));
					for (String line : text) {
						if (lineCount == 0) {
							graphics2D.drawString(new String(line.getBytes(), StandardCharsets.UTF_8), widthOffset, fm.getHeight());
						} else {
							graphics2D.drawString(new String(line.getBytes(), StandardCharsets.UTF_8), widthOffset, fm.getHeight() + lineHeight * lineCount);
						}
						lineCount++;
					}
				}
				graphics2D.dispose();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ImageIO.write(image, "png", outputStream);
				textAddedInputStream = new ByteArrayInputStream(outputStream.toByteArray());
			} catch (FontFormatException e) {
				logger.error("unable to get font", e);
			}
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

	public void addImageWatermark(InputStream watermarkImageFile, InputStream sourceImageFile, ByteArrayOutputStream destImageFile, boolean extraOnTop) {
		try {
			BufferedImage sourceImage = ImageIO.read(sourceImageFile);
			BufferedImage watermarkImage = ImageIO.read(watermarkImageFile);
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

	public InputStream getFileFromUrl(String url) throws IOException, URISyntaxException {
		return new URI(url).toURL().openStream();
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

	public InputStream getDefaultImage(String name, String firstname, String email, boolean print) throws IOException {
		float fixFactor = globalProperties.getFixFactor();
		if(!print) {
			fixFactor = 1f;
		}
		BufferedImage bufferedImage = new BufferedImage(Math.round(600 / fixFactor), Math.round(300 / fixFactor), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = bufferedImage.createGraphics();
		graphics2D.setColor(new Color(0f,0f,0f,0f ));
		Rectangle rect = new Rectangle();
		rect.setRect(0, 0, 600 / fixFactor, 300 / fixFactor);
		graphics2D.fillRect(0, 0, Math.round(600 / fixFactor), Math.round(300 / fixFactor));
		setQualityParams(graphics2D);
		String word;
		if(StringUtils.hasText(firstname) && StringUtils.hasText(name)) {
			if (name.length() >= firstname.length()) {
				word = name;
			} else {
				word = firstname;
			}
		} else {
			word = email;
		}
		try {
			Font font;
			int fontSize;
			if(StringUtils.hasText(firstname) && StringUtils.hasText(name)) {
				font = Font.createFont(Font.TRUETYPE_FONT, new ClassPathResource("/static/fonts/Signature.ttf").getInputStream()).deriveFont(Font.BOLD);
				fontSize = findFontSize(word, Math.round(250 / fixFactor), font);
			} else {
				font = Font.createFont(Font.TRUETYPE_FONT, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream()).deriveFont(Font.BOLD);
				fontSize = findFontSize(email, Math.round(500 / fixFactor), font);
			}
			font = font.deriveFont((float) fontSize);
			graphics2D.setFont(font);
			graphics2D.setColor(Color.BLACK);
			FontMetrics fm = graphics2D.getFontMetrics();
			int y = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
			int lineHeight = Math.round((float) fontSize / 1.5f);
			if(StringUtils.hasText(firstname) && StringUtils.hasText(name)) {
				graphics2D.drawString(StringUtils.capitalize(firstname), 250 / fixFactor, y - lineHeight);
				graphics2D.drawString(StringUtils.capitalize(name), 250 / fixFactor, y + lineHeight);
			} else {
				graphics2D.drawString(email, 10 / fixFactor, y - lineHeight);
			}
			graphics2D.dispose();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "png", outputStream);
			return new ByteArrayInputStream(outputStream.toByteArray());
		} catch (FontFormatException e) {
			logger.warn("unable to get font");
			throw new EsupSignatureRuntimeException("unable to get font", e);
		}
	}

	public InputStream getDefaultParaphe(String name, String firstname, String email, boolean print) throws IOException {
		float fixFactor = globalProperties.getFixFactor();
		if(!print) {
			fixFactor = 1f;
		}
		BufferedImage bufferedImage = new BufferedImage(Math.round(600 / fixFactor), Math.round(300 / fixFactor), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics2D = bufferedImage.createGraphics();
		graphics2D.setColor(new Color(0f,0f,0f,0f ));
		Rectangle rect = new Rectangle();
		rect.setRect(0, 0, 600 / fixFactor, 300 / fixFactor);
		graphics2D.fillRect(0, 0, Math.round(600 / fixFactor), Math.round(300 / fixFactor));
		setQualityParams(graphics2D);
		String word = email;
		if(StringUtils.hasText(firstname) && StringUtils.hasText(name)) {
			word = (firstname.charAt(0)  + "" + name.charAt(0)).toUpperCase();
		}
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, new ClassPathResource("/static/fonts/Signature.ttf").getInputStream()).deriveFont(Font.BOLD);
			int fontSize = findFontSize(word, Math.round(250 / fixFactor), font);
			font = font.deriveFont((float) fontSize);
			graphics2D.setFont(font);
			graphics2D.setColor(Color.BLACK);
			FontMetrics fm = graphics2D.getFontMetrics();
			int y = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
			int lineHeight = Math.round((float) fontSize / 1.5f);
			graphics2D.drawString(word, 250 / fixFactor, y - lineHeight);
			graphics2D.dispose();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(bufferedImage, "png", outputStream);
			return new ByteArrayInputStream(outputStream.toByteArray());
		} catch (FontFormatException e) {
			logger.warn("unable to get font");
			throw new EsupSignatureRuntimeException("unable to get font", e);
		}
	}

	private int findFontSize(String word, int maxWidth, Font font) {
		int maxSize = 80;
		Graphics graphics = createGraphics();
		FontMetrics metrics = graphics.getFontMetrics(font);
		while (font.getSize() < maxSize) {
			int largeurTexte = metrics.stringWidth(word);
			if (largeurTexte >= maxWidth) {
				return font.getSize();
			}
			font = font.deriveFont((float) (font.getSize() + 1));
			metrics = graphics.getFontMetrics(font);
		}
		return font.getSize();
	}

	private Graphics createGraphics() {
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		return g2d;
	}

	public InputStream getFaImageByIndex(int index) throws IOException {
		return new ClassPathResource("/static/images/"+ faImages[Math.abs(index) - 1] + ".png").getInputStream();
	}

	public String getFileChecksum(InputStream inputStream) throws IOException {
		return DigestUtils.sha3_256Hex(inputStream);
	}

	public byte[] zipDocuments(Map<InputStream, String> inputStreams) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
		int i = 0;
		for(Map.Entry<InputStream, String> inputStream : inputStreams.entrySet()) {
			zipOutputStream.putNextEntry(new ZipEntry(i + "-" + inputStream.getValue()));
			IOUtils.copy(inputStream.getKey(), zipOutputStream);
			zipOutputStream.write(inputStream.getKey().readAllBytes());
			zipOutputStream.closeEntry();
			i++;
		}
		zipOutputStream.close();
		return outputStream.toByteArray();
	}

	public InputStream resizeImage(InputStream signImage, float width, float height) throws IOException {
		BufferedImage originalImage = ImageIO.read(signImage);
		if (originalImage == null) {
			throw new IOException("Invalid image input");
		}

		float originalWidth = originalImage.getWidth();
		float originalHeight = originalImage.getHeight();

		float widthRatio = width / originalWidth;
		float heightRatio = height / originalHeight;
		float scale = Math.min(widthRatio, heightRatio);

		int newWidth = Math.round(originalWidth * scale);
		int newHeight = Math.round(originalHeight * scale);

		// redimensionner l'image
		Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

		// créer une nouvelle image avec fond transparent
		BufferedImage outputImage = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = outputImage.createGraphics();
		g2d.setComposite(AlphaComposite.Clear);
		g2d.fillRect(0, 0, (int) width, (int) height);

		// centrer l'image
		int x = ((int) width - newWidth) / 2;
		int y = ((int) height - newHeight) / 2;
		g2d.setComposite(AlphaComposite.SrcOver);
		g2d.drawImage(scaledImage, x, y, null);
		g2d.dispose();

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(outputImage, "png", os);
		return new ByteArrayInputStream(os.toByteArray());
	}
}
