package org.esupportail.esupsignature.service.export;

import fr.gouv.vitam.tools.sedalib.inout.SIPBuilder;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibException;
import fr.gouv.vitam.tools.sedalib.utils.SEDALibProgressLogger;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.slf4j.LoggerFactory;

import java.io.*;

public class SedaExportService {

    public InputStream generateSip(Document document) throws SEDALibException, IOException {
        File targetFile = File.createTempFile("test-seda.zip", ".zip");
        File file = File.createTempFile(document.getFileName(), ".pdf");
        OutputStream outputStream = new FileOutputStream(file);
        IOUtils.copy(document.getInputStream(), outputStream);
        outputStream.close();
        SEDALibProgressLogger pl = new SEDALibProgressLogger(LoggerFactory.getLogger("sedalibsamples"), SEDALibProgressLogger.OBJECTS_GROUP);
        SIPBuilder sb = new SIPBuilder(targetFile.getAbsolutePath(), pl);
        sb.setAgencies("FRAN_NP_000001", "FRAN_NP_000010", "FRAN_NP_000015", "FRAN_NP_000019");
        sb.setArchivalAgreement("IC-000001");
        sb.createRootArchiveUnit("Racine", "Subseries", "Test","Test export SEDA");
        sb.addFileToArchiveUnit("Racine", file.getAbsolutePath(), "1");
        sb.generateSIP();
        InputStream targetInputStream = new FileInputStream(targetFile);
        targetFile.delete();
        file.delete();
        return targetInputStream;
    }

}
