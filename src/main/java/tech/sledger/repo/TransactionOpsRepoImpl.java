package tech.sledger.repo;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.stage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import tech.sledger.model.account.Account;
import tech.sledger.model.dto.CreditCardStatement;
import tech.sledger.model.dto.MonthlyBalance;
import tech.sledger.model.tx.Transaction;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class TransactionOpsRepoImpl implements TransactionOpsRepo {
    private final MongoOperations mongoOps;

    @Override
    public List<CreditCardStatement> getCreditCardBills(long accountId) {
        return mongoOps.aggregate(newAggregation(
            match(Criteria.where("accountId").is(accountId)),
            match(Criteria.where("category").ne("Credit Card Bill")),
            stage("""
                { $group: {
                    _id: "$billingMonth",
                    amount: { $sum: { $toDouble: "$amount" } },
                    transactions: { $sum: 1 }
                }}
            """),
            sort(ASC, "_id")
        ), Transaction.class, CreditCardStatement.class).getMappedResults();
    }

    @Override
    public List<MonthlyBalance> getBalanceHistory(List<Long> accountIds, Instant from, Instant to) {
        return mongoOps.aggregate(newAggregation(
            match(new Criteria().andOperator(
                Criteria.where("accountId").in(accountIds),
                Criteria.where("date").gte(from),
                Criteria.where("date").lte(to)
            )),
            stage("""
                { $group: {
                    _id: {
                        accountId: "$accountId",
                        month: { $dateToString: { format: "%Y-%m-01T00:00:00.000Z", date: "$date" } }
                    },
                    balance: { $last: "$balance" }
                }}
            """),
            project()
                .andInclude("balance")
                .andExclude("_id")
                .andExpression("$_id.month").as("month")
                .andExpression("$_id.accountId").as("accountId"),
            sort(ASC, "month", "accountId")
        ), Transaction.class, MonthlyBalance.class).getMappedResults();
    }
}
