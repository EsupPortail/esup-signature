package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.extvalue.ExtValueService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RequestMapping("user/user-ws")
@Controller
@Transactional
public class UserWsController {

    @Resource
    private ExtValueService extValueService;

    @GetMapping(value="/search-extvalue")
    @ResponseBody
    public List<Map<String, Object>> searchValue(@RequestParam(value="searchType") String searchType, @RequestParam(value="searchString") String searchString, @RequestParam(value = "serviceName") String serviceName, @RequestParam(value = "searchReturn") String searchReturn) {
        ExtValue extValue = extValueService.getExtValueServiceByName(serviceName);
        List<Map<String, Object>> values = extValue.search(searchType, searchString, searchReturn);
        return values.stream().sorted(Comparator.comparing(v -> v.values().iterator().next().toString())).collect(Collectors.toList());
    }

}
