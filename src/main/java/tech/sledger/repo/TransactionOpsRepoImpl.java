package tech.sledger.repo;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.stage;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
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
        Criteria creditCardBill = Criteria.where("_id.category").ne("Credit Card Bill");
        return mongoOps.aggregate(newAggregation(
            match(Criteria.where("accountId").is(accountId)),
            group("billingMonth", "category")
                .sum(ConvertOperators.valueOf("amount").convertToDouble()).as("totalAmount")
                .count().as("transactions"),
            group("_id.billingMonth")
                .sum("$totalAmount").as("net")
                .sum(Cond.when(creditCardBill).then("$totalAmount").otherwise(0))
                    .as("amount")
                .sum(Cond.when(creditCardBill).then("$transactions").otherwise(0))
                    .as("transactions"),
            _ -> Document.parse("""
            {
                $setWindowFields: {
                    sortBy: { _id: 1 },
                    output: {
                        balance: {
                            $sum: "$net",
                            window: { documents: ["unbounded", "current"] }
                        }
                    }
                }
            }
            """)
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
