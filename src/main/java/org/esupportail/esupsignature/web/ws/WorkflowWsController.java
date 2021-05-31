package org.esupportail.esupsignature.web.ws;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/ws/workflows")
public class WorkflowWsController {

    @Resource
    WorkflowService workflowService;

    @Resource
    UserService userService;

    @CrossOrigin
    @PostMapping(value = "/new")
    public ResponseEntity<String> create(@RequestParam Workflow workflow, @RequestParam String[] types, @RequestParam List<String> managers) {
        try {
            User user = userService.getByEppn(workflow.getCreateBy().getEppn());
            workflow = workflowService.createWorkflow(workflow.getTitle(), workflow.getDescription(), user);
            workflowService.update(workflow, user, types, managers);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EsupSignatureException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Workflow get(@PathVariable Long id) {
        return workflowService.getById(id);
    }

    @CrossOrigin
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Workflow> getAll() {
        return workflowService.getAllWorkflows();
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        Workflow workflow = workflowService.getById(id);
        try {
            workflowService.delete(workflow);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EsupSignatureException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @CrossOrigin
    @PostMapping("/{id}")
    public ResponseEntity<String> update(@RequestParam Workflow workflow, @RequestParam String[] types, @RequestParam List<String> managers) {
        User user = userService.getByEppn(workflow.getCreateBy().getEppn());
        workflowService.update(workflow, user, types, managers);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
