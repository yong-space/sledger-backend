package tech.sledger.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import tech.sledger.model.tx.Transaction;
import java.util.Map;
import static java.util.stream.Collectors.toMap;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RequiredArgsConstructor
public class TransactionOpsRepoImpl implements TransactionOpsRepo {
    private final MongoOperations mongoOps;

    record TransactionCount(long _id, long count) {}

    @Override
    public Map<Long, Long> countTransactionsByAccount(long ownerId) {
        LookupOperation lookup = lookup("account", "account.$id", "_id", "a");
        MatchOperation match = match(Criteria.where("a.owner.$id").is(ownerId));
        GroupOperation group = group("account.$id").count().as("count");
        Aggregation aggregate = newAggregation(lookup, match, group);
        return mongoOps.aggregate(aggregate, Transaction.class, TransactionCount.class)
            .getMappedResults()
            .stream().collect(toMap(TransactionCount::_id, TransactionCount::count));
    }
}
