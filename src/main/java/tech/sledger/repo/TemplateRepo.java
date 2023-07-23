package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.tx.Template;
import java.util.List;

public interface TemplateRepo extends MongoRepository<Template, Long> {
    List<Template> findByOwnerIdOrderByReference(long ownerId);
    Template findFirstByOrderByIdDesc();
}
