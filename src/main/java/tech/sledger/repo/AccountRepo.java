package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import java.util.List;

public interface AccountRepo extends MongoRepository<Account, Long>, AccountOpsRepo {
    Account findFirstById(long id);
    Account findFirstByOrderByIdDesc();
    List<Account> findAllByIssuer(AccountIssuer issuer);
}
