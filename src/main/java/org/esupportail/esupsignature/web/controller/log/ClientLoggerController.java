package org.esupportail.esupsignature.web.controller.log;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.web.controller.user.UserController;
import org.esupportail.esupsignature.web.controller.ws.json.JsonClientSideError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RequestMapping("log")
@Controller
public class ClientLoggerController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping(consumes = {"application/json"})
    public ResponseEntity<Boolean> log(@RequestBody JsonClientSideError jsonClientSideError, HttpServletRequest httpServletRequest) {
        long contentLength = httpServletRequest.getContentLengthLong();
        if (contentLength > Long.parseLong("1000")) {
            logger.warn("This request is too big and its content will not be logged. Headers");
        } else {
            logger.warn("Client-side error occurred : " + jsonClientSideError);
        }
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

}
