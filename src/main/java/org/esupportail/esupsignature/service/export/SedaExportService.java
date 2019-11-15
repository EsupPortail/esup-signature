package org.esupportail.esupsignature.service.export;

import eu.europa.esig.dss.jaxb.simplereport.SimpleReport;
import eu.europa.esig.dss.jaxb.simplereport.XmlSignature;
import eu.europa.esig.dss.validation.reports.DetailedReport;
import eu.europa.esig.dss.validation.reports.Reports;
import fr.gouv.vitam.tools.sedalib.core.BinaryDataObject;
import fr.gouv.vitam.tools.sedalib.core.DataObjectGroup;
import fr.gouv.vitam.tools.sedalib.core.DataObjectPackage;
import fr.gouv.vitam.tools.sedalib.inout.SIPBuilder;
import fr.gouv.vitam.tools.sedalib.metadata.SEDAMetadata;
import fr.gouv.vitam.tools.sedalib.metadata.content.*;
import fr.gouv.vitam.tools.sedalib.metadata.management.LogBook;
import fr.gouv.vitam.tools.sedalib.metadata.management.Management;
import fr.gouv.vitam.tools.sedalib.metadata.namedtype.DigestType;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.ValidationService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.esupportail.esupsignature.web.controller.user.SignRequestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class SedaExportService {

    private static final Logger logger = LoggerFactory.getLogger(SedaExportService.class);

    @Resource
    SignService signService;

    @Resource
    SignRequestService signRequestService;

    @Resource
    LogRepository logRepository;

    @Resource
    UserRepository userRepository;

    @Resource
    private ValidationService validationService;

    public InputStream generateSip(SignRequest signRequest) {
        try {
            File targetFile = File.createTempFile("test-seda.zip", ".zip");
            Document document = signRequestService.getLastSignedDocument(signRequest);
            File file = File.createTempFile(document.getFileName(), ".pdf");
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(document.getInputStream(), outputStream);
            outputStream.close();
            SEDALibProgressLogger pl = new SEDALibProgressLogger(logger, SEDALibProgressLogger.OBJECTS_GROUP);
            SIPBuilder sb = new SIPBuilder(targetFile.getAbsolutePath(), pl);
            sb.setAgencies("FRAN_NP_000001", "FRAN_NP_000010", "FRAN_NP_000015", "FRAN_NP_000019");
            sb.setArchivalAgreement("IC-000001");
            sb.createRootArchiveUnit("Racine", "Subseries", "Test", "Test export SEDA");
            sb.addFileToArchiveUnit("Racine", file.getAbsolutePath(), "1");
            Management management = sb.getManagement("Racine");
            Content content = sb.getContent("Racine");
            List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
            LogBook logBook = new LogBook();
            for (Log log : logs) {
                Event event = new Event(log.getId().toString(), log.getFinalStatus(), convertToLocalDateTimeViaInstant(log.getLogDate()), log.getReturnCode());
                event.addNewMetadata("EventTypeCode", log.getFinalStatus() + " by " + log.getEppn());
                event.addNewMetadata("EventDetail", log.getComment());
                logBook.addMetadata(event);
            }
            management.addMetadata(logBook);

            Reports reports = validationService.validate(new MockMultipartFile(document.getFileName(), document.getInputStream()));
            DetailedReport detailedReport = reports.getDetailedReport();
            SimpleReport simpleReport = reports.getSimpleReportJaxb();
            for(XmlSignature xmlSignature : simpleReport.getSignature()) {
                //User user = userRepository.findByEppn(xmlSignature.getSignedBy()).get(0);
                Signature signature = new Signature();
                signature.addMetadata(new Signer(xmlSignature.getSignedBy(), convertToLocalDateTimeViaInstant(xmlSignature.getBestSignatureTime())));
                DataObjectPackage dataObjectPackage = sb.findArchiveUnit("Racine").getDataObjectPackage();
                for(BinaryDataObject binaryDataObject : dataObjectPackage.getBdoInDataObjectPackageIdMap().values()){
                    ReferencedObject referencedObject = new ReferencedObject(
                            binaryDataObject.getInDataObjectPackageId(),
                            binaryDataObject.messageDigest.getValue(),
                            binaryDataObject.messageDigest.getAlgorithm());
                    signature.addMetadata(referencedObject);
                    content.addMetadata(signature);
                }
            }

            sb.generateSIP();
            InputStream targetInputStream = new FileInputStream(targetFile);
            targetFile.delete();
            file.delete();
            return targetInputStream;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

}
