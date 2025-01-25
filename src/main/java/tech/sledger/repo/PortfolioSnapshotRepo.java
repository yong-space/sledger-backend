package tech.sledger.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import tech.sledger.model.portfolio.PortfolioSnapshot;
import tech.sledger.model.user.User;

public interface PortfolioSnapshotRepo extends MongoRepository<PortfolioSnapshot, Long> {
    PortfolioSnapshot findFirstByOwner(User user);
}
