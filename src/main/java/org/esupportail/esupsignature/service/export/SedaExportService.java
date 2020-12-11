//package org.esupportail.esupsignature.service.export;
//
//import eu.europa.esig.dss.jaxb.simplereport.SimpleReport;
//import eu.europa.esig.dss.jaxb.simplereport.XmlSignature;
//import eu.europa.esig.dss.validation.reports.Reports;
//import fr.gouv.vitam.tools.sedalib.core.ArchiveUnit;
//import fr.gouv.vitam.tools.sedalib.core.BinaryDataObject;
//import fr.gouv.vitam.tools.sedalib.core.DataObjectPackage;
//import fr.gouv.vitam.tools.sedalib.inout.SIPBuilder;
//import fr.gouv.vitam.tools.sedalib.metadata.content.*;
//import fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger;
//import org.apache.commons.io.IOUtils;
//import org.esupportail.esupsignature.entity.Document;
//import org.esupportail.esupsignature.entity.Log;
//import org.esupportail.esupsignature.entity.SignRequest;
//import org.esupportail.esupsignature.repository.LogRepository;
//import org.esupportail.esupsignature.service.SignRequestService;
//import org.esupportail.esupsignature.service.ValidationService;
//import org.esupportail.esupsignature.service.utils.file.FileService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.io.*;
//import java.nio.file.Paths;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.Date;
//import java.util.List;
//
//@Service
//public class SedaExportService {
//
//    private static final Logger logger = LoggerFactory.getLogger(SedaExportService.class);
//
//    @Resource
//    SignRequestService signRequestService;
//
//    @Resource
//    LogRepository logRepository;
//
//    @Resource
//    FileService fileService;
//
//    @Resource
//    private ValidationService validationService;
//
//    public InputStream generateSip(SignRequest signRequest) {
//        try {
//            File targetFile = fileService.getTempFile(signRequest.getTitle() + "-seda.zip");
//            Document document = signRequestService.getLastSignedDocument(signRequest);
//            File file = fileService.getTempFile(document.getFileName());
//            OutputStream outputStream = new FileOutputStream(file);
//            IOUtils.copy(document.getInputStream(), outputStream);
//            outputStream.close();
//            Reports reports = validationService.validate(new FileInputStream(file));
//            File validationXml = fileService.getTempFile("validation.xml");
//            FileWriter fw = new java.io.FileWriter(validationXml.getAbsolutePath());
//            fw.write(reports.getXmlDiagnosticData());
//            fw.close();
//
//            SEDALibProgressLogger pl = new SEDALibProgressLogger(logger, SEDALibProgressLogger.GLOBAL);
//            SIPBuilder sb = new SIPBuilder(targetFile.getAbsolutePath(), pl);
//            sb.setAgencies("FRAN_NP_000001", "FRAN_NP_000010", "FRAN_NP_000015", "FRAN_NP_000019");
//            sb.setArchivalAgreement("IC-000001");
//            DataObjectPackage dataObjectPackage = sb.getArchiveTransfer().getDataObjectPackage();
//
//            BinaryDataObject docBinaryDataObject = new BinaryDataObject(dataObjectPackage, Paths.get(file.getAbsolutePath()), file.getName(), "BinaryMaster_1");
//            docBinaryDataObject.extractTechnicalElements(pl);
//            BinaryDataObject signViladationBinaryDataObject = new BinaryDataObject(dataObjectPackage, validationXml.toPath(), validationXml.getName(), "BinaryMaster_1");
//            signViladationBinaryDataObject.extractTechnicalElements(pl);
//
//            ArchiveUnit id1ArchiveUnit = sb.createRootArchiveUnit("ID1", "File", signRequest.getTitle(), signRequest.getComment());
//            List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
//            for (Log log : logs) {
//                Event event = new Event(log.getId().toString(), log.getFinalStatus(), convertToLocalDateTimeViaInstant(log.getLogDate()), log.getReturnCode());
//                event.addNewMetadata("EventTypeCode", log.getFinalStatus() + " by " + log.getEppn());
//                event.addNewMetadata("EventDetail", log.getComment());
//                id1ArchiveUnit.getContent().addMetadata(event);
//            }
//
//
//            ArchiveUnit id2ArchiveUnit = sb.addNewSubArchiveUnit("ID1", "ID2", "Item", "validation.xml", "");
//            SimpleReport simpleReport = reports.getSimpleReportJaxb();
//            for(XmlSignature xmlSignature : simpleReport.getSignature()) {
//                //UserUi user = userRepository.findByEppn(xmlSignature.getSignedBy()).get(0);
//                Signature signature = new Signature();
//                signature.addMetadata(new Signer(xmlSignature.getSignedBy(), convertToLocalDateTimeViaInstant(xmlSignature.getBestSignatureTime())));
//                Validator validator = new Validator("DSS Validator", convertToLocalDateTimeViaInstant(xmlSignature.getBestSignatureTime()));
//                validator.addNewMetadata("Identifier", simpleReport.getPolicy().getPolicyName());
//                signature.addMetadata(validator);
//
//                ReferencedObject referencedObject = new ReferencedObject(
//                        docBinaryDataObject.getInDataObjectPackageId(),
//                        docBinaryDataObject.messageDigest.getValue(),
//                        docBinaryDataObject.messageDigest.getAlgorithm());
//                signature.addMetadata(referencedObject);
//                id2ArchiveUnit.getContent().addMetadata(signature);
//            }
//
//            id1ArchiveUnit.addDataObjectById(docBinaryDataObject.getInDataObjectPackageId());
//            id2ArchiveUnit.addDataObjectById(signViladationBinaryDataObject.getInDataObjectPackageId());
//            sb.generateSIP();
//            InputStream targetInputStream = new FileInputStream(targetFile);
//            targetFile.delete();
//            file.delete();
//            //validationXml.delete();
//            return targetInputStream;
//        } catch (Exception e) {
//            logger.error(e.getMessage(), e);
//            return null;
//        }
//    }
//
//    public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
//        return dateToConvert.toInstant()
//                .atZone(ZoneId.systemDefault())
//                .toLocalDateTime();
//    }
//
//}
