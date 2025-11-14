package org.esupportail.esupsignature.web.wssecure;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.FieldPropertieService;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ws-secure/users")
public class UserWsSecureController {

    private final UserPropertieService userPropertieService;
    private final FieldPropertieService fieldPropertieService;
    private final UserService userService;
    private final RecipientService recipientService;

    public UserWsSecureController(UserPropertieService userPropertieService, FieldPropertieService fieldPropertieService, UserService userService, RecipientService recipientService) {
        this.userPropertieService = userPropertieService;
        this.fieldPropertieService = fieldPropertieService;
        this.userService = userService;
        this.recipientService = recipientService;
    }

    @ResponseBody
    @PostMapping(value ="/check-temp-users")
    private List<User> checkTempUsers(@RequestBody(required = false) List<String> recipientEmails) {
        return userService.checkTempUsers(recipientService.convertRecipientEmailsToStep(recipientEmails).stream().map(WorkflowStepDto::getRecipients).flatMap(List::stream).toList());
    }

    @ResponseBody
    @GetMapping("/get-favorites")
    private Set<User> getFavorites(@ModelAttribute("authUserEppn") String authUserEppn) {
        return userPropertieService.getFavoritesEmails(authUserEppn);
    }

    @ResponseBody
    @GetMapping(value = "/get-favorites/{id}")
    public List<String> getFavorites(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable("id") Long id) {
        return fieldPropertieService.getFavoritesValues(authUserEppn, id);
    }

    @GetMapping("/get-ui-params")
    @ResponseBody
    public Map<UiParams, String> getUiParams(@ModelAttribute("authUserEppn") String authUserEppn) {
        return userService.getUiParams(authUserEppn);
    }

    @GetMapping("/set-ui-params/{key}/{value}")
    @ResponseBody
    public void setUiParams(@ModelAttribute("authUserEppn") String authUserEppn, @PathVariable String key, @PathVariable String value) {
        userService.setUiParams(authUserEppn, UiParams.valueOf(key), value);
    }

    @GetMapping(value = "/get-sign-image/{id}")
    public ResponseEntity<Void> getSignature(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        Map<String, Object> signature = userService.getSignatureByUserAndId(userEppn, id);
        if(signature != null) {
            return getDocumentResponseEntity(response, (byte[]) signature.get("bytes"), (String) signature.get("fileName"), (String) signature.get("contentType"));
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/get-default-image")
    @ResponseBody
    public ResponseEntity<Void> getDefaultImage(@ModelAttribute("authUserEppn") String authUserEppn, HttpServletResponse response) throws IOException {
        return getDocumentResponseEntity(response, userService.getDefaultImage(authUserEppn).readAllBytes(), "default.png", "image/png");
    }

    @GetMapping(value = "/get-default-image-base64")
    @ResponseBody
    public String getDefaultImageBase64(@ModelAttribute("authUserEppn") String authUserEppn) throws IOException {
        return userService.getDefaultImage64(authUserEppn);
    }

    @GetMapping(value = "/get-default-paraphe")
    @ResponseBody
    public ResponseEntity<Void> getDefaultParaphe(@ModelAttribute("authUserEppn") String authUserEppn, HttpServletResponse response) throws IOException {
        return getDocumentResponseEntity(response, userService.getDefaultParaphe(authUserEppn).readAllBytes(), "default.png", "image/png");
    }

    @GetMapping(value = "/get-default-paraphe-base64")
    @ResponseBody
    public String getDefaultParapheBase64(@ModelAttribute("authUserEppn") String authUserEppn) throws IOException {
        return userService.getDefaultParaphe64(authUserEppn);
    }

    @GetMapping(value = "/get-keystore")
    public ResponseEntity<Void> getKeystore(@ModelAttribute("authUserEppn") String authUserEppn, HttpServletResponse response) throws IOException {
        Map<String, Object> keystore = userService.getKeystoreByUser(authUserEppn);
        if(keystore != null) {
            return getDocumentResponseEntity(response, (byte[]) keystore.get("bytes"), keystore.get("fileName").toString(), keystore.get("contentType").toString());
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<Void> getDocumentResponseEntity(HttpServletResponse response, byte[] bytes, String fileName, String contentType) throws IOException {
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"));
        response.setHeader("Cache-Control", "public, max-age=86400");
        response.setContentType(contentType);
        IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
