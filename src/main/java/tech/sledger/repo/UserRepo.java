package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.user.User;

public interface UserRepo extends MongoRepository<User, Long> {
    User findFirstByOrderByIdDesc();
    User findFirstByUsername(String username);
}
