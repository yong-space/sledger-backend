package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.tx.Template;
import tech.sledger.model.user.User;
import java.util.List;

public interface TemplateRepo extends MongoRepository<Template, Long> {
    List<Template> findByOwnerOrderByReference(User user);
    Template findFirstByOrderByIdDesc();
}
