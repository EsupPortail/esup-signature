package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.entity.enums.SearchType;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.extvalue.ExtValueService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
        return extValue.search(SearchType.valueOf(searchType), searchString, searchReturn).stream().limit(15).collect(Collectors.toList());
    }

}
