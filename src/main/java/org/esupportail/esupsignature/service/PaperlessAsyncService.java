package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service d'archivage Paperless asynchrone.
 * Séparé de PaperlessService pour permettre l'usage de @Async sans auto-invocation (proxy Spring requis).
 */
@Service
public class PaperlessAsyncService {

    private static final Logger logger = LoggerFactory.getLogger(PaperlessAsyncService.class);

    private final PaperlessService paperlessService;
    private final SignRequestService signRequestService;
    private final TargetService targetService;

    public PaperlessAsyncService(PaperlessService paperlessService, SignRequestService signRequestService,
                                 TargetService targetService) {
        this.paperlessService = paperlessService;
        this.signRequestService = signRequestService;
        this.targetService = targetService;
    }

    /**
     * Upload le document signé dans Paperless puis lie le résultat à la demande de signature,
     * entièrement en arrière-plan (thread @Async). L'appelant n'attend pas le résultat.
     *
     * Si targetId/targetUri sont fournis (non null), le webhook REST target est envoyé après
     * l'archivage avec le vrai ID Paperless du document signé.
     */
    @Async
    public CompletableFuture<Long> uploadAndLinkAsync(Long sourceDocumentId, byte[] pdfBytes,
                                                      String filename, Long signRequestId,
                                                      String signataires, String authUserEppn,
                                                      Long targetId, String targetUri, String statusName) {
        try {
            Long newPaperlessId = paperlessService.uploadSignedDocument(
                    sourceDocumentId, pdfBytes, filename, signRequestId, signataires);
            String downloadUrl = paperlessService.getPaperlessUrl().replaceAll("/$", "")
                    + "/api/documents/" + newPaperlessId + "/download/";
            signRequestService.cleanAndLinkWithPaperlessId(signRequestId, downloadUrl, newPaperlessId, authUserEppn);
            logger.info("Archivage Paperless asynchrone réussi signRequest={} → paperlessId={}", signRequestId, newPaperlessId);
            if (targetUri != null) {
                try {
                    targetService.sendRest(targetUri, signRequestId.toString(), statusName, "end", authUserEppn, String.valueOf(newPaperlessId));
                    if (targetId != null) {
                        targetService.markTargetOk(targetId);
                    }
                    signRequestService.updateStatus(signRequestId, SignRequestStatus.valueOf(statusName),
                            "Exporté vers " + targetUri, null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
                    logger.info("Webhook REST target envoyé après archivage Paperless signRequest={} paperlessId={}", signRequestId, newPaperlessId);
                } catch (Exception re) {
                    logger.error("Échec envoi webhook REST target après archivage Paperless pour signRequest={}", signRequestId, re);
                }
            }
            return CompletableFuture.completedFuture(newPaperlessId);
        } catch (Exception e) {
            logger.error("Archivage Paperless asynchrone échoué pour signRequest={}", signRequestId, e);
            // Envoyer quand même le webhook REST vers l'application tierce (CACES etc.)
            // même si Paperless a échoué, pour ne pas bloquer la mise à jour du statut.
            if (targetUri != null) {
                try {
                    logger.warn("Envoi du webhook REST malgré l'échec Paperless pour signRequest={}", signRequestId);
                    targetService.sendRest(targetUri, signRequestId.toString(), statusName, "end", authUserEppn, "");
                    if (targetId != null) {
                        targetService.markTargetOk(targetId);
                    }
                    signRequestService.updateStatus(signRequestId, SignRequestStatus.valueOf(statusName),
                            "Exporté vers " + targetUri + " (archivage Paperless en échec)", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
                } catch (Exception re) {
                    logger.error("Échec envoi webhook REST (fallback) pour signRequest={}", signRequestId, re);
                }
            }
            return CompletableFuture.failedFuture(e);
        }
    }
}
