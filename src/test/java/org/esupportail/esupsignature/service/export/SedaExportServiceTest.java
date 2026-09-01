package org.esupportail.esupsignature.service.export;

import fr.gouv.vitam.tools.sedalib.droid.DroidIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SedaExportServiceTest {

    @TempDir
    Path configDir;

    @Test
    void preparesDroidConfigurationFromSedalibResources() throws Exception {
        SedaExportService.prepareDroidConfiguration(configDir, DroidIdentifier.class.getClassLoader());

        assertTrue(Files.size(configDir.resolve("DROID_SignatureFile_V97.xml")) > 0);
        assertTrue(Files.size(configDir.resolve("container-signature-20201001.xml")) > 0);
    }
}
