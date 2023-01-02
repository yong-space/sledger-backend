package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.account.AccountIssuer;

public interface AccountIssuerRepo extends MongoRepository<AccountIssuer, Long> {
    AccountIssuer findFirstByOrderByIdDesc();
    AccountIssuer findFirstByName(String name);
}
