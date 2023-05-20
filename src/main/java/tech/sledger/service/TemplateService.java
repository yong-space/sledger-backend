package tech.sledger.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.tx.Template;
import tech.sledger.model.user.User;
import tech.sledger.repo.TemplateRepo;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateRepo templateRepo;

    public List<Template> list(User owner) {
        return templateRepo.findByOwnerOrderByReference(owner);
    }

    @Transactional
    public List<Template> add(User owner, List<Template> templates) {
        Template previous = templateRepo.findFirstByOrderByIdDesc();
        AtomicLong id = new AtomicLong();
        id.set((previous == null) ? 1 : previous.getId() + 1);

        List<Template> processed = templates.stream()
            .peek(template -> {
                template.setOwner(owner);
                template.setId(id.get());
                template.setReference(template.getReference().toLowerCase());
                id.set(id.get() + 1);
            })
            .toList();
        return templateRepo.saveAll(processed);
    }

    @Transactional
    public List<Template> edit(User owner, List<Template> templates) {
        authorise(owner, templates.stream().map(Template::getId).toList());
        return templateRepo.saveAll(templates.stream().peek(t -> t.setOwner(owner)).toList());
    }

    @Transactional
    public void delete(User owner, long templateId) {
        templateRepo.delete(authorise(owner, List.of(templateId)).get(0));
    }

    private List<Template> authorise(User owner, List<Long> templateIds) {
        List<Template> templates = templateRepo.findAllById(templateIds);
        if (templates.size() != templateIds.size()) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to find all templates requested");
        }
        if (templates.parallelStream().anyMatch(t -> t.getOwner().getId() != owner.getId())) {
            throw new ResponseStatusException(UNAUTHORIZED, "You are not the owner of all templates requested");
        }
        return templates;
    }
}
