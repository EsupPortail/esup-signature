package org.esupportail.esupsignature.web.wssecure;

import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.dto.json.UserSignatureStateDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;

@RestController
@RequestMapping(path = {"/ws-secure/user/fetch", "/ws-secure/otp/fetch"})
public class UserAndOtpUserFetchController {

    private final SignBookService signBookService;
    private final UserService userService;

    public UserAndOtpUserFetchController(SignBookService signBookService, UserService userService) {
        this.signBookService = signBookService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserSignatureStateDto> update(@ModelAttribute("userEppn") String userEppn,
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
        userService.updateUser(authUserEppn, name, firstname, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore, null, false);
        return ResponseEntity.ok(buildUserSignatureStateDto(userEppn, authUserEppn, signRequestId, httpSession));
    }

    @DeleteMapping("/delete-sign/{id}")
    public ResponseEntity<UserSignatureStateDto> deleteSign(@ModelAttribute("userEppn") String userEppn,
                                           @ModelAttribute("authUserEppn") String authUserEppn,
                                           @PathVariable long id,
                                           @RequestParam(value = "signRequestId", required = false) Long signRequestId,
                                           HttpSession httpSession) {
        userService.deleteSign(authUserEppn, id);
        return ResponseEntity.ok(buildUserSignatureStateDto(userEppn, authUserEppn, signRequestId, httpSession));
    }

    private UserSignatureStateDto buildUserSignatureStateDto(String userEppn, String authUserEppn, Long signRequestId, HttpSession httpSession) {
        User user = userService.getFullUserByEppn(authUserEppn);
        return new UserSignatureStateDto(user.getFirstname(), user.getName(), user.getEmail(), user.getSignImagesIds(), getSignImages(signRequestId, userEppn, authUserEppn, httpSession));
    }

    private java.util.List<String> getSignImages(Long signRequestId, String userEppn, String authUserEppn, HttpSession httpSession) {
        if (signRequestId == null) {
            return java.util.Collections.emptyList();
        }
        Object userShareString = httpSession.getAttribute("userShareId");
        Long userShareId = null;
        if (userShareString != null) {
            userShareId = Long.valueOf(userShareString.toString());
        }
        try {
            return signBookService.getSignImagesForSignRequest(signRequestId, userEppn, authUserEppn, userShareId);
        } catch (EsupSignatureUserException | IOException e) {
            return java.util.Collections.emptyList();
        }
    }
}





