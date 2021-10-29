package org.esupportail.esupsignature.web.wssecure;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.FieldPropertieService;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValueService;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/ws-secure/users")
public class UserWsController {

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private FieldPropertieService fieldPropertieService;

    @Resource
    private UserService userService;

    @Autowired(required = false)
    private SmsService smsService;

    @Resource
    private ExtValueService extValueService;

    @GetMapping(value="/search-extvalue")
    @ResponseBody
    public List<Map<String, Object>> searchValue(@RequestParam(value="searchType") String searchType, @RequestParam(value="searchString") String searchString, @RequestParam(value = "serviceName") String serviceName, @RequestParam(value = "searchReturn") String searchReturn) {
        ExtValue extValue = extValueService.getExtValueServiceByName(serviceName);
        List<Map<String, Object>> values = extValue.search(searchType, searchString, searchReturn);
        return values.stream().sorted(Comparator.comparing(v -> v.values().iterator().next().toString())).collect(Collectors.toList());
    }

    @ResponseBody
    @GetMapping("/get-favorites")
    private List<String> getFavorites(@ModelAttribute("authUserEppn") String authUserEppn) {
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


    @ResponseBody
    @PostMapping(value ="/check-temp-users")
    private List<User> checkTempUsers(@RequestBody(required = false) List<String> recipientEmails) {
        if (recipientEmails!= null && recipientEmails.size() > 0) {
            List<User> users = userService.getTempUsersFromRecipientList(recipientEmails);
            if (smsService != null) {
                return users;
            } else {
                if (users.size() > 0) {
                    return null;
                }
            }
        }
        return new ArrayList<>();
    }

    @GetMapping(value = "/get-sign-image/{id}")
    public ResponseEntity<Void> getSignature(@ModelAttribute("userEppn") String userEppn, @PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        Map<String, Object> signature = userService.getSignatureByUserAndId(userEppn, id);
        return getDocumentResponseEntity(response, (byte[]) signature.get("bytes"), signature.get("fileName").toString(), (String) signature.get("contentType"));
    }

    @GetMapping(value = "/get-keystore")
    public ResponseEntity<Void> getKeystore(@ModelAttribute("authUserEppn") String authUserEppn, HttpServletResponse response) throws IOException {
        Map<String, Object> keystore = userService.getKeystoreByUser(authUserEppn);
        return getDocumentResponseEntity(response, (byte[]) keystore.get("bytes"), keystore.get("fileName").toString(), keystore.get("contentType").toString());
    }


    private ResponseEntity<Void> getDocumentResponseEntity(HttpServletResponse response, byte[] bytes, String fileName, String contentType) throws IOException {
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()));
        response.setContentType(contentType);
        IOUtils.copy(new ByteArrayInputStream(bytes), response.getOutputStream());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
