package org.esupportail.esupsignature.dss;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import org.esupportail.esupsignature.dss.model.TimestampDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class DssUtils {

	private static final Logger logger = LoggerFactory.getLogger(DssUtils.class);

	private DssUtils() {
	}

	public static DSSDocument toDSSDocument(InputStream inputStream) {
		try {
			if ((inputStream != null) && inputStream.available() > 0) {
				DSSDocument document = new InMemoryDocument(inputStream);
				return document;
			}
		} catch (IOException e) {
			logger.error("Cannot read  file : " + e.getMessage(), e);
		}
		return null;
	}

	public static DSSDocument toDSSDocument(MultipartFile multipartFile) {
		try {
			if ((multipartFile != null) && !multipartFile.isEmpty()) {
				DSSDocument document = new InMemoryDocument(multipartFile.getBytes(), multipartFile.getOriginalFilename());
				return document;
			}
		} catch (IOException e) {
			logger.error("Cannot read  file : " + e.getMessage(), e);
		}
		return null;
	}

	public static List<DSSDocument> toDSSDocuments(List<MultipartFile> documentsToSign) {
		List<DSSDocument> dssDocuments = new ArrayList<DSSDocument>();
		for (MultipartFile multipartFile : documentsToSign) {
			DSSDocument dssDocument = toDSSDocument(multipartFile);
			if (dssDocument != null) {
				dssDocuments.add(dssDocument);
			}
		}
		return dssDocuments;
	}

	public static TimestampDTO fromTimestampToken(TimestampToken token) {
		TimestampDTO dto = new TimestampDTO();
		dto.setBase64Timestamp(Utils.toBase64(token.getEncoded()));
		dto.setCanonicalizationMethod(token.getCanonicalizationMethod());
		dto.setType(token.getTimeStampType());
		return dto;
	}

	public static TimestampToken toTimestampToken(TimestampDTO dto) {
		try {
			TimestampToken token = new TimestampToken(Utils.fromBase64(dto.getBase64Timestamp()), dto.getType());
			token.setCanonicalizationMethod(dto.getCanonicalizationMethod());
			return token;
		} catch (Exception e) {
			throw new DSSException("Unable to retrieve the timestamp", e);
		}
	}

}
