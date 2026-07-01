package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class PaperlessService {

    private static final Logger logger = LoggerFactory.getLogger(PaperlessService.class);

    private static final int TASK_POLL_MAX_ATTEMPTS = 20;
    private static final long TASK_POLL_DELAY_MS = 1500;

    /** Délais maximum pour les appels HTTP vers Paperless (évite de bloquer indéfiniment le thread de requête). */
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public PaperlessService(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public String getPaperlessUrl() {
        return userService.getSystemUiParam(UiParams.paperlessUrl);
    }

    public String getPaperlessToken() {
        return userService.getSystemUiParam(UiParams.paperlessToken);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(getPaperlessUrl()) && StringUtils.hasText(getPaperlessToken());
    }

    @Transactional
    public void saveConfig(String url, String token) {
        userService.setSystemUiParam(UiParams.paperlessUrl, url);
        userService.setSystemUiParam(UiParams.paperlessToken, token);
    }

    @Transactional
    public void saveCustomFieldsConfig(String sourceFieldId, String signedFieldId,
                                       String esignRequestFieldId, String signatureDateFieldId,
                                       String signataireFieldId) {
        userService.setSystemUiParam(UiParams.paperlessSourceFieldId, sourceFieldId);
        userService.setSystemUiParam(UiParams.paperlessSignedFieldId, signedFieldId);
        userService.setSystemUiParam(UiParams.paperlessEsignRequestFieldId, esignRequestFieldId);
        userService.setSystemUiParam(UiParams.paperlessSignatureDateFieldId, signatureDateFieldId);
        userService.setSystemUiParam(UiParams.paperlessSignataireFieldId, signataireFieldId);
    }

    public String getCustomFieldId(UiParams param) {
        return userService.getSystemUiParam(param);
    }

    private String baseUrl() {
        String url = getPaperlessUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + getPaperlessToken());
        return headers;
    }

    public Map<String, Object> testConnection() {
        if (!isConfigured()) {
            return Map.of("success", false, "message", "URL ou token non configuré");
        }
        try {
            org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
            factory.setReadTimeout(READ_TIMEOUT_MS);
            RestTemplate restTemplate = new RestTemplate(factory);
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            String testUrl = baseUrl() + "/api/documents/?page_size=1";
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return Map.of("success", true, "message", "Connexion réussie à Paperless-ngx");
            } else {
                return Map.of("success", false, "message", "Réponse inattendue : " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.warn("Paperless connection test failed: {}", e.getMessage());
            return Map.of("success", false, "message", "Erreur de connexion : " + e.getMessage());
        }
    }

    /**
     * Retourne le JSON complet d'un document Paperless (métadonnées, tags, permissions, custom fields…).
     */
    public JsonNode fetchDocumentMeta(Long paperlessDocumentId) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Paperless non configuré : URL ou token manquant");
        }
        String metaUrl = baseUrl() + "/api/documents/" + paperlessDocumentId + "/";
        URL url = new URI(metaUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Token " + getPaperlessToken());
        conn.connect();
        if (conn.getResponseCode() != 200) {
            throw new IllegalArgumentException("Document Paperless introuvable (id=" + paperlessDocumentId + ") : HTTP " + conn.getResponseCode());
        }
        String json = new String(conn.getInputStream().readAllBytes());
        conn.disconnect();
        return objectMapper.readTree(json);
    }

    /**
     * Fetches a document from Paperless by its ID and returns it as a MultipartFile.
     * Calls /api/documents/{id}/ for metadata (filename) then /api/documents/{id}/download/ for content.
     */
    public MultipartFile fetchDocument(Long paperlessDocumentId) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Paperless non configuré : URL ou token manquant");
        }
        String token = getPaperlessToken();
        String base = baseUrl();

        String metaUrl = base + "/api/documents/" + paperlessDocumentId + "/";
        URL metaUrlObj = new URI(metaUrl).toURL();
        HttpURLConnection metaConn = (HttpURLConnection) metaUrlObj.openConnection();
        metaConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        metaConn.setReadTimeout(READ_TIMEOUT_MS);
        metaConn.setRequestMethod("GET");
        metaConn.setRequestProperty("Authorization", "Token " + token);
        metaConn.connect();
        if (metaConn.getResponseCode() != 200) {
            throw new IllegalArgumentException("Document Paperless introuvable (id=" + paperlessDocumentId + ") : HTTP " + metaConn.getResponseCode());
        }
        String metaJson = new String(metaConn.getInputStream().readAllBytes());
        metaConn.disconnect();

        JsonNode meta = objectMapper.readTree(metaJson);
        String originalFilename = meta.path("original_file_name").asText(paperlessDocumentId + ".pdf");

        String downloadUrl = base + "/api/documents/" + paperlessDocumentId + "/download/";
        URL dlUrl = new URI(downloadUrl).toURL();
        HttpURLConnection dlConn = (HttpURLConnection) dlUrl.openConnection();
        dlConn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        dlConn.setReadTimeout(READ_TIMEOUT_MS);
        dlConn.setRequestMethod("GET");
        dlConn.setRequestProperty("Authorization", "Token " + token);
        dlConn.connect();
        if (dlConn.getResponseCode() != 200) {
            throw new IllegalArgumentException("Téléchargement Paperless impossible (id=" + paperlessDocumentId + ") : HTTP " + dlConn.getResponseCode());
        }
        String contentType = dlConn.getContentType();
        if (contentType == null) contentType = "application/pdf";
        byte[] bytes = dlConn.getInputStream().readAllBytes();
        dlConn.disconnect();

        logger.info("Fetched Paperless document id={} filename='{}' size={} bytes", paperlessDocumentId, originalFilename, bytes.length);
        return new DssMultipartFile("multipartFiles", originalFilename, contentType, bytes);
    }

    /**
     * Uploade le PDF signé dans Paperless en reprenant les métadonnées du document source,
     * puis applique owner, permissions et custom fields de traçabilité via PATCH.
     *
     * @param sourceDocumentId  ID Paperless du document original (avant signature)
     * @param signedPdf         contenu binaire du PDF signé
     * @param filename          nom de fichier à donner dans Paperless
     * @param signRequestId     ID de la demande eSign (traçabilité)
     * @param signataires       email(s) des signataires (traçabilité)
     * @return l'ID Paperless du nouveau document créé
     */
    public Long uploadSignedDocument(Long sourceDocumentId, byte[] signedPdf, String filename,
                                     Long signRequestId, String signataires) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Paperless non configuré : URL ou token manquant");
        }

        JsonNode sourceMeta = fetchDocumentMeta(sourceDocumentId);

        String taskId = postDocumentMultipart(signedPdf, filename, sourceMeta);
        logger.info("Paperless upload task_id={} for signRequest={}", taskId, signRequestId);

        Long newDocId = waitForTaskCompletion(taskId);
        logger.info("Paperless signed document created id={} for signRequest={}", newDocId, signRequestId);

        patchDocumentMetadata(newDocId, sourceMeta, signRequestId, sourceDocumentId, signataires);

        linkSourceToSigned(sourceDocumentId, sourceMeta, newDocId);

        return newDocId;
    }

    /**
     * POST multipart vers /api/documents/post_document/ avec les métadonnées structurelles du document source.
     * Retourne le task_id Paperless.
     */
    private String postDocumentMultipart(byte[] pdfBytes, String filename, JsonNode sourceMeta) throws Exception {
        String boundary = "----EsupSignBoundary" + System.currentTimeMillis();
        String uploadUrl = baseUrl() + "/api/documents/post_document/";

        URL url = new URI(uploadUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Token " + getPaperlessToken());
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (java.io.OutputStream out = conn.getOutputStream()) {
            writePart(out, boundary, "document", filename, "application/pdf", pdfBytes);

            String sourceTitle = sourceMeta.path("title").asText("");
            if (StringUtils.hasText(sourceTitle)) {
                writeFieldPart(out, boundary, "title", "[SIGNÉ] " + sourceTitle);
            } else if (StringUtils.hasText(filename)) {
                writeFieldPart(out, boundary, "title", "[SIGNÉ] " + filename);
            }

            if (!sourceMeta.path("correspondent").isNull() && sourceMeta.path("correspondent").isNumber()) {
                writeFieldPart(out, boundary, "correspondent", sourceMeta.path("correspondent").asText());
            }
            if (!sourceMeta.path("document_type").isNull() && sourceMeta.path("document_type").isNumber()) {
                writeFieldPart(out, boundary, "document_type", sourceMeta.path("document_type").asText());
            }
            if (!sourceMeta.path("storage_path").isNull() && sourceMeta.path("storage_path").isNumber()) {
                writeFieldPart(out, boundary, "storage_path", sourceMeta.path("storage_path").asText());
            }

            JsonNode tags = sourceMeta.path("tags");
            if (tags.isArray()) {
                for (JsonNode tag : tags) {
                    writeFieldPart(out, boundary, "tags", tag.asText());
                }
            }

            writeFieldPart(out, boundary, "created",
                    LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

            out.write(("--" + boundary + "--\r\n").getBytes());
        }

        int status = conn.getResponseCode();
        if (status != 200 && status != 201 && status != 202) {
            String err = new String(conn.getErrorStream() != null ? conn.getErrorStream().readAllBytes() : new byte[0]);
            conn.disconnect();
            throw new IllegalStateException("Paperless upload failed HTTP " + status + ": " + err);
        }
        String responseBody = new String(conn.getInputStream().readAllBytes());
        conn.disconnect();

        // Paperless retourne le task_id (UUID string) en texte brut ou JSON
        String taskId = responseBody.trim().replace("\"", "");
        if (taskId.isEmpty()) {
            throw new IllegalStateException("Paperless n'a pas retourné de task_id");
        }
        return taskId;
    }

    /**
     * Interroge GET /api/tasks/{taskId}/ jusqu'à obtenir le related_document ou un échec.
     */
    private Long waitForTaskCompletion(String taskId) throws Exception {
        String taskUrl = baseUrl() + "/api/tasks/?task_id=" + taskId;
        for (int i = 0; i < TASK_POLL_MAX_ATTEMPTS; i++) {
            Thread.sleep(TASK_POLL_DELAY_MS);
            URL url = new URI(taskUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Token " + getPaperlessToken());
            conn.connect();
            String body = new String(conn.getInputStream().readAllBytes());
            conn.disconnect();

            JsonNode response = objectMapper.readTree(body);
            JsonNode tasks = response.isArray() ? response : response.path("results");
            if (tasks.isArray() && tasks.size() > 0) {
                JsonNode task = tasks.get(0);
                String taskStatus = task.path("status").asText("");
                if ("SUCCESS".equalsIgnoreCase(taskStatus)) {
                    JsonNode relatedDoc = task.path("related_document");
                    if (!relatedDoc.isNull() && relatedDoc.isNumber()) {
                        return relatedDoc.asLong();
                    }
                    throw new IllegalStateException("Paperless task SUCCESS mais related_document absent");
                } else if ("FAILURE".equalsIgnoreCase(taskStatus)) {
                    String result = task.path("result").asText("raison inconnue");
                    throw new IllegalStateException("Paperless task FAILURE : " + result);
                }
            }
            logger.debug("Paperless task {} : en attente (tentative {}/{})", taskId, i + 1, TASK_POLL_MAX_ATTEMPTS);
        }
        throw new IllegalStateException("Paperless task " + taskId + " : délai dépassé après " + TASK_POLL_MAX_ATTEMPTS + " tentatives");
    }

    /**
     * PATCH /api/documents/{id}/ pour appliquer owner, permissions et custom fields de traçabilité.
     */
    private void patchDocumentMetadata(Long newDocId, JsonNode sourceMeta,
                                       Long signRequestId, Long sourceDocumentId,
                                       String signataires) throws Exception {
        ObjectNode patch = objectMapper.createObjectNode();

        if (!sourceMeta.path("owner").isNull() && sourceMeta.path("owner").isNumber()) {
            patch.set("owner", sourceMeta.path("owner"));
        }

        JsonNode sourcePerms = sourceMeta.path("permissions");
        if (!sourcePerms.isMissingNode() && !sourcePerms.isNull()) {
            ObjectNode setPerms = objectMapper.createObjectNode();
            ObjectNode view = objectMapper.createObjectNode();
            view.set("users", sourcePerms.path("view").path("users"));
            view.set("groups", sourcePerms.path("view").path("groups"));
            ObjectNode change = objectMapper.createObjectNode();
            change.set("users", sourcePerms.path("change").path("users"));
            change.set("groups", sourcePerms.path("change").path("groups"));
            setPerms.set("view", view);
            setPerms.set("change", change);
            patch.set("set_permissions", setPerms);
        }

        ArrayNode customFields = objectMapper.createArrayNode();
        addCustomField(customFields, UiParams.paperlessSourceFieldId, sourceDocumentId != null ? sourceDocumentId.toString() : null);
        addCustomField(customFields, UiParams.paperlessEsignRequestFieldId, signRequestId != null ? signRequestId.toString() : null);
        addCustomField(customFields, UiParams.paperlessSignatureDateFieldId,
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        addCustomField(customFields, UiParams.paperlessSignataireFieldId, signataires);
        if (customFields.size() > 0) {
            patch.set("custom_fields", customFields);
        }

        sendPatch(baseUrl() + "/api/documents/" + newDocId + "/", patch);
        logger.info("Paperless PATCH applied on new doc id={}", newDocId);
    }

    /**
     * PATCH sur le document source pour y ajouter l'ID du document signé (lien bidirectionnel).
     */
    private void linkSourceToSigned(Long sourceDocumentId, JsonNode sourceMeta, Long newDocId) {
        String signedFieldId = getCustomFieldId(UiParams.paperlessSignedFieldId);
        if (!StringUtils.hasText(signedFieldId)) {
            return;
        }
        try {
            ArrayNode existingFields = objectMapper.createArrayNode();
            JsonNode srcCustomFields = sourceMeta.path("custom_fields");
            if (srcCustomFields.isArray()) {
                for (JsonNode cf : srcCustomFields) {
                    existingFields.add(cf.deepCopy());
                }
            }
            ObjectNode signedEntry = objectMapper.createObjectNode();
            signedEntry.put("field", Integer.parseInt(signedFieldId));
            signedEntry.put("value", newDocId.toString());
            existingFields.add(signedEntry);

            ObjectNode patch = objectMapper.createObjectNode();
            patch.set("custom_fields", existingFields);
            sendPatch(baseUrl() + "/api/documents/" + sourceDocumentId + "/", patch);
            logger.info("Paperless source doc id={} linked to signed doc id={}", sourceDocumentId, newDocId);
        } catch (Exception e) {
            logger.warn("Impossible de lier le document source Paperless {} au signé {} : {}", sourceDocumentId, newDocId, e.getMessage());
        }
    }

    private void addCustomField(ArrayNode array, UiParams param, String value) {
        if (!StringUtils.hasText(value)) return;
        String fieldId = getCustomFieldId(param);
        if (!StringUtils.hasText(fieldId)) return;
        ObjectNode entry = objectMapper.createObjectNode();
        entry.put("field", Integer.parseInt(fieldId));
        entry.put("value", value);
        array.add(entry);
    }

    private void sendPatch(String urlStr, ObjectNode body) throws Exception {
        URL url = new URI(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("PATCH");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Token " + getPaperlessToken());
        conn.setRequestProperty("Content-Type", "application/json");
        byte[] jsonBytes = objectMapper.writeValueAsBytes(body);
        conn.setRequestProperty("Content-Length", String.valueOf(jsonBytes.length));
        try (java.io.OutputStream out = conn.getOutputStream()) {
            out.write(jsonBytes);
        }
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            String err = new String(conn.getErrorStream() != null ? conn.getErrorStream().readAllBytes() : new byte[0]);
            conn.disconnect();
            throw new IllegalStateException("Paperless PATCH " + urlStr + " failed HTTP " + status + ": " + err);
        }
        conn.disconnect();
    }

    private void writePart(java.io.OutputStream out, String boundary,
                           String fieldName, String filename, String contentType,
                           byte[] data) throws Exception {
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n" +
                "Content-Type: " + contentType + "\r\n\r\n";
        out.write(header.getBytes());
        out.write(data);
        out.write("\r\n".getBytes());
    }

    private void writeFieldPart(java.io.OutputStream out, String boundary,
                                String fieldName, String value) throws Exception {
        String part = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n" +
                value + "\r\n";
        out.write(part.getBytes());
    }

    public InputStream fetchDocumentWithAuth(String documentUrl) throws Exception {
        String token = getPaperlessToken();
        URL url = new URI(documentUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        if (StringUtils.hasText(token)) {
            conn.setRequestProperty("Authorization", "Token " + token);
        }
        conn.connect();
        return conn.getInputStream();
    }

    public boolean isPaperlessUrl(String url) {
        String paperlessUrl = getPaperlessUrl();
        return StringUtils.hasText(paperlessUrl) && StringUtils.hasText(url) && url.startsWith(paperlessUrl);
    }
}
