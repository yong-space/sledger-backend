package tech.sledger.endpoints;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.sledger.model.tx.Template;
import tech.sledger.model.user.User;
import tech.sledger.service.TemplateService;
import java.util.List;

@Slf4j
@Validated
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
    public List<Template> addTemplate(
        Authentication auth,
        @RequestBody List<@Valid Template> templates
    ) {
        return templateService.add((User) auth.getPrincipal(), templates);
    }

    @PutMapping
    public List<Template> editTemplate(
        Authentication auth,
        @RequestBody List<@Valid Template> templates
    ) {
        return templateService.edit((User) auth.getPrincipal(), templates);
    }

    @DeleteMapping("/{templateId}")
    public void deleteTemplate(Authentication auth, @PathVariable long templateId) {
        templateService.delete((User) auth.getPrincipal(), templateId);
    }
}
