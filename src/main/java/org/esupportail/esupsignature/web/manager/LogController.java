package org.esupportail.esupsignature.web.manager;

import org.esupportail.esupsignature.domain.Log;
import org.springframework.roo.addon.web.mvc.controller.scaffold.RooWebScaffold;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/manager/logs")
@Controller
@RooWebScaffold(path = "manager/logs", create = false, delete = false, update = false, formBackingObject = Log.class)
public class LogController {

}
