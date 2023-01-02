package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.SledgerUser;

public interface SledgerUserRepo extends MongoRepository<SledgerUser, Long> {
    SledgerUser findFirstByOrderByIdDesc();
    SledgerUser findFirstByUsername(String username);
}
