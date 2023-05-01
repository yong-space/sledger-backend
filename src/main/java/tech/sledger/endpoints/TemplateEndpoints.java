package tech.sledger.endpoints;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.sledger.model.tx.Template;
import tech.sledger.model.user.User;
import tech.sledger.service.TemplateService;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/template")
public class TemplateEndpoints {
    private final TemplateService templateService;

    @GetMapping
    public List<Template> listTemplates(Authentication auth) {
        return templateService.list((User) auth.getPrincipal());
    }

    @PostMapping
    public List<Template> addTemplate(Authentication auth, @RequestBody List<Template> templates) {
        return templateService.add((User) auth.getPrincipal(), templates);
    }

    @PutMapping
    public List<Template> editTemplate(Authentication auth, @RequestBody List<Template> templates) {
        return templateService.edit((User) auth.getPrincipal(), templates);
    }

    @DeleteMapping("/{templateId}")
    public void deleteTemplate(Authentication auth, @PathVariable long templateId) {
        templateService.delete((User) auth.getPrincipal(), templateId);
    }
}
