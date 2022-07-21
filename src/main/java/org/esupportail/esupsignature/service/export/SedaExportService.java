package org.esupportail.esupsignature.service.export;

import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.validation.reports.Reports;
import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
import fr.gouv.vitam.tools.sedalib.core.BinaryDataObject;
import fr.gouv.vitam.tools.sedalib.core.DataObjectPackage;
import fr.gouv.vitam.tools.sedalib.inout.SIPBuilder;
import fr.gouv.vitam.tools.sedalib.metadata.content.*;
import fr.gouv.vitam.tools.sedalib.metadata.management.*;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.repository.LogRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class SedaExportService {

    private static final Logger logger = LoggerFactory.getLogger(SedaExportService.class);

    @Resource
    SignRequestService signRequestService;

    @Resource
    LogRepository logRepository;

    @Resource
    FileService fileService;

    @Resource
    private ValidationService validationService;

    @Transactional
    public InputStream generateSip(Long signRequestId) {
        Date date = new Date();
        SignRequest signRequest = signRequestService.getById(signRequestId);
        try {
            File targetFile = fileService.getTempFile(signRequest.getTitle() + "-seda.zip");
            Document document = signRequest.getLastSignedDocument();
            File file = fileService.getTempFile(document.getFileName());
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(document.getInputStream(), outputStream);
            outputStream.close();
            Reports reports = validationService.validate(new FileInputStream(file), null);

            File validationXml = fileService.getTempFile("validation.xml");
            FileWriter fw = new java.io.FileWriter(validationXml.getAbsolutePath());
            if(reports != null) {
                fw.write(reports.getXmlDiagnosticData());
            }
            fw.close();

            SEDALibProgressLogger pl = new SEDALibProgressLogger(logger, SEDALibProgressLogger.GLOBAL);
            SIPBuilder sb = new SIPBuilder(targetFile.getAbsolutePath(), pl);
            sb.setAgencies("FRAN_NP_000001", "FRAN_NP_000010", "FRAN_NP_000015", "FRAN_NP_000019");
            sb.setArchivalAgreement("IC-000001");
            DataObjectPackage dataObjectPackage = sb.getArchiveTransfer().getDataObjectPackage();
            BinaryDataObject signValidationBinaryDataObject = null;
            if(reports != null && reports.getXmlSimpleReport() != null) {
                signValidationBinaryDataObject = new BinaryDataObject(dataObjectPackage, validationXml.toPath(), validationXml.getName(), "BinaryMaster_1");
                signValidationBinaryDataObject.extractTechnicalElements(pl);
            }
            ArchiveUnit id1ArchiveUnit = sb.createRootArchiveUnit("ID1", "File", signRequest.getTitle(), "");
            List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
            Management management = new Management();
            LogBook logBook = new LogBook();
            for (Log log : logs) {
                Event event = new Event(log.getId().toString(), log.getFinalStatus(), convertToLocalDateTimeViaInstant(log.getLogDate()), log.getReturnCode());
                event.addNewMetadata("EventTypeCode", log.getFinalStatus() + " by " + log.getEppn());
                event.addNewMetadata("EventDetail", log.getComment());
                logBook.addMetadata(event);
            }
            management.addMetadata(logBook);
            management.addMetadata(new AccessRule("ACC-00001", convertToLocalDateViaInstant(date)));
            ReuseRule reuseRule = new ReuseRule();
            reuseRule.setPreventInheritance(true);
            management.addMetadata(reuseRule);
            management.addMetadata(new ClassificationRule());
            id1ArchiveUnit.setManagement(management);
            BinaryDataObject docBinaryDataObject = new BinaryDataObject(dataObjectPackage, Paths.get(file.getAbsolutePath()), file.getName(), "BinaryMaster_1");
            id1ArchiveUnit.addDataObjectById(docBinaryDataObject.getInDataObjectPackageId());

            if(reports != null) {
                ArchiveUnit id2ArchiveUnit = sb.addNewSubArchiveUnit("ID1", "ID2", "Item", "validation.xml", "");
                docBinaryDataObject.extractTechnicalElements(pl);
                SimpleReport simpleReport = reports.getSimpleReport();
                for (String signatureId : simpleReport.getSignatureIdList()) {
                    Signature signature = new Signature();
                    signature.addMetadata(new Signer(simpleReport.getSignedBy(signatureId), convertToLocalDateTimeViaInstant(simpleReport.getBestSignatureTime(signatureId))));
                    Validator validator = new Validator("DSS Validator", convertToLocalDateTimeViaInstant(date));
                    validator.addNewMetadata("Identifier", "DSS");
                    signature.addMetadata(validator);
                    ReferencedObject referencedObject = new ReferencedObject(
                            docBinaryDataObject.getInDataObjectPackageId(),
                            docBinaryDataObject.messageDigest.getValue(),
                            docBinaryDataObject.messageDigest.getAlgorithm());
                    signature.addMetadata(referencedObject);
                    id2ArchiveUnit.getContent().addMetadata(signature);
                }
                id2ArchiveUnit.addDataObjectById(signValidationBinaryDataObject.getInDataObjectPackageId());
            }

            sb.generateSIP();
            InputStream targetInputStream = new FileInputStream(targetFile);
            targetFile.delete();
            file.delete();
//            validationXml.delete();
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

    public LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

}
