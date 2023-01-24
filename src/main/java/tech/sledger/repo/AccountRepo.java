package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountIssuer;
import tech.sledger.model.user.User;
import java.util.List;

public interface AccountRepo extends MongoRepository<Account, Long> {
    Account findFirstByOrderByIdDesc();
    List<Account> findAllByOwnerOrderByName(User owner);
    List<Account> findAllByIssuer(AccountIssuer issuer);
}
