package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.SledgerUser;
import tech.sledger.model.account.Account;
import java.util.List;

public interface AccountRepo extends MongoRepository<Account, Long> {
    Account findFirstByOrderByIdDesc();
    List<Account> findAllByOwnerOrderByName(SledgerUser owner);
}
