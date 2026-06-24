package org.esupportail.esupsignature.web.wssecure;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.ui.global.UserSignatureStateDto;
import org.esupportail.esupsignature.dto.ui.global.UiDataDto;
import org.esupportail.esupsignature.dto.ui.global.UiHomeDto;
import org.esupportail.esupsignature.dto.ui.global.UiUserLookupDto;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.service.ui.UiFetchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ws-secure/ui")
public class UiFetchController {

    private final UiFetchService uiFetchService;

    public UiFetchController(UiFetchService uiFetchService) {
        this.uiFetchService = uiFetchService;
    }

    @GetMapping(value = "/ui-data", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UiDataDto> getUiData(@ModelAttribute("userEppn") String userEppn,
                                               @ModelAttribute("authUserEppn") String authUserEppn,
                                               HttpSession httpSession) {
        return ResponseEntity.ok(uiFetchService.buildUiData(userEppn, authUserEppn, httpSession));
    }

    @GetMapping(value = "/home", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UiHomeDto> getHomeBootstrap(@ModelAttribute("userEppn") String userEppn,
                                                      @ModelAttribute("authUserEppn") String authUserEppn,
                                                      @RequestParam(value = "formId", required = false) Long formId,
                                                      @RequestParam(value = "workflowId", required = false) Long workflowId) {
        return ResponseEntity.ok(uiFetchService.buildUiHomeBootstrap(userEppn, authUserEppn, formId, workflowId));
    }

    @PutMapping(value = "/ui-data/{object}/{key}/{value}")
    public ResponseEntity<Void> setUiDataValue(@ModelAttribute("authUserEppn") String authUserEppn,
                                               @PathVariable String object,
                                               @PathVariable String key,
                                               @PathVariable String value) {
        if (!uiFetchService.setUiDataValue(authUserEppn, object, key, value)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/temp-users/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UiUserLookupDto>> checkTempUsers(@RequestBody(required = false) List<String> recipientEmails) {
        return ResponseEntity.ok(uiFetchService.checkTempUsers(recipientEmails));
    }

    @GetMapping(value = "/favorites/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UiUserLookupDto>> getFavoriteUsers(@ModelAttribute("authUserEppn") String authUserEppn) {
        return ResponseEntity.ok(uiFetchService.getFavoriteUsers(authUserEppn));
    }

    @GetMapping(value = "/favorites/fields/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getFavoriteFieldValues(@ModelAttribute("authUserEppn") String authUserEppn,
                                                               @PathVariable("id") Long id) {
        return ResponseEntity.ok(uiFetchService.getFavoriteFieldValues(authUserEppn, id));
    }

    @GetMapping(value = "/signatures/{id}")
    public ResponseEntity<Void> getSignature(@ModelAttribute("userEppn") String userEppn,
                                             @PathVariable("id") Long id,
                                             HttpServletResponse response) throws IOException {
        Map<String, Object> signature = uiFetchService.getSignatureDocument(userEppn, id);
        if (signature == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return getDocumentResponseEntity(response, (byte[]) signature.get("bytes"), (String) signature.get("fileName"), (String) signature.get("contentType"));
    }

    @GetMapping(value = "/signatures/default-image")
    public ResponseEntity<Void> getDefaultImage(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                HttpServletResponse response) throws IOException {
        return getDocumentResponseEntity(response, uiFetchService.getDefaultImage(userEppn).readAllBytes(), "default.png", "image/png");
    }

    @GetMapping(value = "/signatures/default-paraphe")
    public ResponseEntity<Void> getDefaultParaphe(@ModelAttribute("userEppn") String userEppn, @ModelAttribute("authUserEppn") String authUserEppn,
                                                  HttpServletResponse response) throws IOException {
        return getDocumentResponseEntity(response, uiFetchService.getDefaultParaphe(userEppn).readAllBytes(), "default.png", "image/png");
    }

    @GetMapping(value = "/keystore")
    public ResponseEntity<Void> getKeystore(@ModelAttribute("authUserEppn") String authUserEppn,
                                            HttpServletResponse response) throws IOException {
        Map<String, Object> keystore = uiFetchService.getKeystore(authUserEppn);
        if (keystore == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return getDocumentResponseEntity(response, (byte[]) keystore.get("bytes"), keystore.get("fileName").toString(), keystore.get("contentType").toString());
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/profile")
    public ResponseEntity<UserSignatureStateDto> updateProfile(@ModelAttribute("userEppn") String userEppn,
                                                               @ModelAttribute("authUserEppn") String authUserEppn,
                                                               @RequestParam(value = "signImageBase64", required = false) String signImageBase64,
                                                               @RequestParam(value = "name") String name,
                                                               @RequestParam(value = "firstname") String firstname,
                                                               @RequestParam(value = "signRequestId", required = false) Long signRequestId,
                                                               @RequestParam(value = "emailAlertFrequency", required = false) EmailAlertFrequency emailAlertFrequency,
                                                               @RequestParam(value = "emailAlertHour", required = false) Integer emailAlertHour,
                                                               @RequestParam(value = "emailAlertDay", required = false) DayOfWeek emailAlertDay,
                                                               @RequestParam(value = "multipartKeystore", required = false) MultipartFile multipartKeystore,
                                                               HttpSession httpSession) throws Exception {
        return ResponseEntity.ok(uiFetchService.updateProfile(userEppn, authUserEppn, signImageBase64, name, firstname, signRequestId, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore, httpSession));
    }

    @DeleteMapping(value = "/profile/signatures/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserSignatureStateDto> deleteSignature(@ModelAttribute("userEppn") String userEppn,
                                                                 @ModelAttribute("authUserEppn") String authUserEppn,
                                                                 @PathVariable long id,
                                                                 @RequestParam(value = "signRequestId", required = false) Long signRequestId,
                                                                 HttpSession httpSession) {
        return ResponseEntity.ok(uiFetchService.deleteSignature(userEppn, authUserEppn, id, signRequestId, httpSession));
    }

    @PostMapping(value = "/warnings/read")
    public ResponseEntity<Void> warningReaded(@ModelAttribute("authUserEppn") String authUserEppn) {
        uiFetchService.markWarningsRead(authUserEppn);
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Void> getDocumentResponseEntity(HttpServletResponse response, byte[] bytes, String fileName, String contentType) throws IOException {
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));
        response.setHeader("Cache-Control", "private, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType(contentType);
        IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}

