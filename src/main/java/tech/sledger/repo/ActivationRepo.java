package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.user.Activation;
import tech.sledger.model.user.User;

public interface ActivationRepo extends MongoRepository<Activation, String> {
    Activation findFirstByUser(User user);
}
