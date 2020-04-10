package org.esupportail.esupsignature.service.export;

import org.apache.commons.text.StringEscapeUtils;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataExportService {

    @Resource
    private DataRepository dataRepository;

    @Resource
    private LogRepository logRepository;

    public InputStream getCsvDatasFromForms(List<Form> forms) {
        return csvStringToInputStream(toCSV(getDatasToExport(forms)));
    }

    public List<Map<String, String>> getDatasToExport(List<Form> forms) {
        List<Map<String, String>> dataDatas = new ArrayList<>();
        for(Form form: forms) {
            dataDatas.addAll(getDatasToExport(form));
        }
        return  dataDatas;
    }

    public List<Map<String, String>> getDatasToExport(Form form) {
        List<Data> datas = dataRepository.findByForm(form);
        List<Map<String, String>> dataDatas = new ArrayList<>();
        for(Data data : datas) {
            SignBook signBook = data.getSignBook();
            if(signBook != null && signBook.getStatus().equals(SignRequestStatus.completed)) {
                Log lastLog = logRepository.findBySignRequestIdAndFinalStatus(signBook.getSignRequests().get(0).getId(), SignRequestStatus.completed.name()).get(0);
                LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
                toExportDatas.put("form_name", form.getName());
                toExportDatas.put("form_create_date", data.getCreateDate().toString());
                toExportDatas.put("form_create_by", data.getCreateBy());
                toExportDatas.put("form_completed_date", lastLog.getLogDate().toString());
                toExportDatas.put("form_completed_by", lastLog.getEppnFor());
                for (Map.Entry<String, String> entry : data.getDatas().entrySet()) {
                    toExportDatas.put("form_data_" + entry.getKey(), entry.getValue());
                }
                List<Log> logs = logRepository.findBySignRequestId(signBook.getSignRequests().get(0).getId()).stream().sorted(Comparator.comparing(Log::getLogDate)).collect(Collectors.toList());
                int step = 1;
                //for(WorkflowStep workflowStep : signBook.getWorkflowSteps()) {
                for (Log log : logs) {
                    if (log.getFinalStatus().equals(SignRequestStatus.checked.name()) || log.getFinalStatus().equals(SignRequestStatus.signed.name())) {
                        toExportDatas.put("sign_step_" + step + "_user_eppn", log.getEppnFor());
                        toExportDatas.put("sign_step_" + step + "_type", log.getFinalStatus());
                        toExportDatas.put("sign_step_" + step + "_date", log.getLogDate().toString());
                        step++;
                    }
                }
                dataDatas.add(toExportDatas);
            }
        }
        return dataDatas;
    }

    public  String toCSV(List<Map<String, String>> list) {
        List<String> headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            sb.append(headers.get(i));
            sb.append(i == headers.size()-1 ? "\n" : ",");
        }
        for (Map<String, String> map : list) {
            for (int i = 0; i < headers.size(); i++) {
                String value = "";
                if(map.get(headers.get(i)) != null) {
                    value = StringEscapeUtils.escapeCsv(map.get(headers.get(i)));
                }
                sb.append(value);
                sb.append(i == headers.size()-1 ? "\n" : ",");
            }
        }
        return sb.toString();
    }

    public InputStream csvStringToInputStream(String csvString) {
        byte[] bytes = csvString.getBytes();
        return new ByteArrayInputStream(bytes);
    }

}
