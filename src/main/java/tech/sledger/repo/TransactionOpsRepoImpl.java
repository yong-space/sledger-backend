package tech.sledger.repo;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.stage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import tech.sledger.model.dto.CreditCardStatement;
import tech.sledger.model.tx.Transaction;
import java.util.List;

@RequiredArgsConstructor
public class TransactionOpsRepoImpl implements TransactionOpsRepo {
    private final MongoOperations mongoOps;

    @Override
    public List<CreditCardStatement> getCreditCardStatement(long accountId) {
        return mongoOps.aggregate(newAggregation(
            match(Criteria.where("accountId").is(accountId)),
            match(Criteria.where("category").ne("Credit Card Bill")),
            stage("""
                { $group: {
                    _id: { month: { $dateToString: { format: "%Y-%m-01T00:00:00.000Z", date: "$date" } } },
                    amount: { $sum: { $toDouble: "$amount" } },
                    transactions: { $sum: 1 }
                }}
            """),
            stage("{ $replaceRoot: { newRoot: { $mergeObjects: [ \"$_id\", \"$$ROOT\" ] } } }"),
            sort(ASC, "month")
        ), Transaction.class, CreditCardStatement.class).getMappedResults();
    }
}
