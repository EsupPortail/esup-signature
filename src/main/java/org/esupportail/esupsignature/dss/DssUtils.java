package org.esupportail.esupsignature.dss;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.DigestDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.dss.ws.dto.TimestampDTO;
import eu.europa.esig.dss.ws.signature.common.TimestampTokenConverter;
import org.esupportail.esupsignature.dss.config.MultipartResolverProvider;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dss.model.OriginalFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class DssUtils {

	private static final Logger LOG = LoggerFactory.getLogger(DssUtils.class);

	private DssUtils() {
	}

	public static DSSDocument toDSSDocument(MultipartFile multipartFile) {
		try {
			if ((multipartFile != null) && !multipartFile.isEmpty()) {
				if (multipartFile.getSize() > MultipartResolverProvider.getInstance().getMaxFileSize()) {
					throw new MaxUploadSizeExceededException(MultipartResolverProvider.getInstance().getMaxFileSize());
				}
				return new InMemoryDocument(multipartFile.getBytes(), multipartFile.getOriginalFilename());
			}
		} catch (IOException e) {
			LOG.error("Cannot read file : " + e.getMessage(), e);
		}
		return null;
	}

	public static List<DSSDocument> toDSSDocuments(List<DssMultipartFile> documentsToSign) {
		List<DSSDocument> dssDocuments = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(documentsToSign)) {
			for (MultipartFile multipartFile : documentsToSign) {
				DSSDocument dssDocument = toDSSDocument(multipartFile);
				if (dssDocument != null) {
					dssDocuments.add(dssDocument);
				}
			}
		}
		return dssDocuments;
	}

	public static TimestampDTO fromTimestampToken(TimestampToken token) {
		return TimestampTokenConverter.toTimestampDTO(token);
	}

	public static TimestampToken toTimestampToken(TimestampDTO dto) {
		return TimestampTokenConverter.toTimestampToken(dto);
	}

	public static List<DSSDocument> originalFilesToDSSDocuments(List<OriginalFile> originalFiles) {
		List<DSSDocument> dssDocuments = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(originalFiles)) {
			for (OriginalFile originalDocument : originalFiles) {
				if (originalDocument.isNotEmpty()) {
					DSSDocument dssDocument;
					if (originalDocument.getCompleteFile() != null) {
						dssDocument = DssUtils.toDSSDocument(originalDocument.getCompleteFile());
					} else {
						dssDocument = new DigestDocument(originalDocument.getDigestAlgorithm(),
								originalDocument.getBase64Digest(), originalDocument.getFilename());
					}
					dssDocuments.add(dssDocument);
					LOG.debug("OriginalDocument with name {} added", originalDocument.getFilename());
				}
			}
		}
		LOG.debug("OriginalDocumentsLoaded : {}", dssDocuments.size());
		return dssDocuments;
	}

	public static boolean isCollectionNotEmpty(List<DssMultipartFile> documents) {
		if (Utils.isCollectionNotEmpty(documents)) {
			for (MultipartFile multipartFile : documents) {
				if (multipartFile != null && !multipartFile.isEmpty()) {
					// return true if at least one file is not empty
					return true;
				}
			}
		}
		return false;
	}

	public static CertificateToken toCertificateToken(MultipartFile certificateFile) {
		try {
			if (certificateFile != null && !certificateFile.isEmpty()) {
				return DSSUtils.loadCertificate(certificateFile.getBytes());
			}
		} catch (DSSException | IOException e) {
			LOG.warn("Cannot convert file to X509 Certificate", e);
			throw new DSSException("Unsupported certificate or file format!");
		}
		return null;
	}

	public static CertificateSource toCertificateSource(List<MultipartFile> certificateFiles) {
		CertificateSource certSource = null;
		if (Utils.isCollectionNotEmpty(certificateFiles)) {
			certSource = new CommonCertificateSource();
			for (MultipartFile file : certificateFiles) {
				CertificateToken certificateChainItem = DssUtils.toCertificateToken(file);
				if (certificateChainItem != null) {
					certSource.addCertificate(certificateChainItem);
				}
			}
		}
		return certSource;
	}

}