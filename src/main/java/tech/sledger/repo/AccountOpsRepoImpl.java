package tech.sledger.repo;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.lookup;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.replaceRoot;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.stage;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import tech.sledger.model.account.Account;
import tech.sledger.model.dto.AccountDTO;
import tech.sledger.model.dto.Insight;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class AccountOpsRepoImpl implements AccountOpsRepo {
    private final MongoOperations mongoOps;

    @Override
    public List<AccountDTO> getAccountsWithMetrics(long ownerId) {
        AggregationOperation lookup = stage("""
            {
                $lookup: {
                  from: "transaction",
                  let: { accountId: "$_id" },
                  pipeline: [
                    { $match: { $expr: { $eq: [ "$$accountId", "$accountId" ] } } },
                    {
                        $group: {
                            _id: "$accountId",
                            maxDate: { $max: "$date" },
                            transactions: { $sum: 1 },
                            ordinaryBalance: { $sum: { $toDouble: "$ordinaryAmount" } },
                            specialBalance: { $sum: { $toDouble: "$specialAmount" } },
                            medisaveBalance: { $sum: { $toDouble: "$medisaveAmount" } }
                        }
                    },
                    {
                      $lookup: {
                        from: "transaction",
                        let: { accountId: "$_id", maxDate: "$maxDate" },
                        pipeline: [
                          { $match: { $expr: { $and: [
                            { $eq: [ "$accountId", "$$accountId" ] },
                            { $eq: [ "$date", "$$maxDate" ] }
                          ] } } },
                          { $project: { _id: 0, balance: 1 } }
                        ],
                        as: "balanceLookup"
                      }
                    },
                    { $replaceRoot: { newRoot: { $mergeObjects: [ { $arrayElemAt: [ "$balanceLookup", 0 ] }, "$$ROOT" ] } } },
                    {
                        $project: {
                            balance: 1,
                            transactions: 1,
                            ordinaryBalance: { $cond: { if: { $eq: [ "$ordinaryBalance", 0 ] }, then: null, else: "$ordinaryBalance" }},
                            specialBalance: { $cond: { if: { $eq: [ "$specialBalance", 0 ] }, then: null, else: "$specialBalance" }},
                            medisaveBalance: { $cond: { if: { $eq: [ "$medisaveBalance", 0 ] }, then: null, else: "$medisaveBalance" }}
                        }
                    }
                  ],
                  as: "transactionLookup"
                }
            }
            """);
        Aggregation aggregate = newAggregation(
            match(Criteria.where("owner.$id").is(ownerId)),
            lookup,
            stage("{ $replaceRoot: { newRoot: { $mergeObjects: [ { $arrayElemAt: [ \"$transactionLookup\", 0 ] }, \"$$ROOT\" ] } } }"),
            lookup("accountIssuer", "issuer.$id", "_id", "issuerLookup"),
            unwind("$issuerLookup"),
            sort(ASC, "type", "issuerLookup.name", "name"),
            stage("{ $addFields: { issuerId: \"$issuerLookup._id\" } }"),
            stage("{ $project: { transactionLookup: 0, issuerLookup: 0, owner: 0, issuer: 0 } }")
        );
        return mongoOps.aggregate(aggregate, Account.class, AccountDTO.class).getMappedResults();
    }

    @Override
    public List<String> getTopStrings(long ownerId, String field, String q) {
        return mongoOps.aggregate(newAggregation(
                match(Criteria.where("owner.$id").is(ownerId)),
                lookup("transaction", "_id", "accountId", "transactions"),
                unwind("$transactions"),
                replaceRoot("$transactions"),
                match(Criteria.where(field).regex(q, "i")),
                group("$" + field).count().as("count"),
                sort(DESC, "count").and(ASC, "_id"),
                limit(5),
                project("_id")
            ), Account.class, Map.class).getMappedResults()
            .stream().map(m -> (String) m.get("_id")).toList();
    }

    @Override
    public List<Insight> getInsights(long ownerId, Instant from, Instant to) {
        Criteria criteria = new Criteria().andOperator(
            Criteria.where("category").exists(true),
            Criteria.where("category").ne(""),
            Criteria.where("category").regex("^(?!Transient|Credit Card Bill$).*$", "i"),
            Criteria.where("date").gte(from),
            Criteria.where("date").lte(to)
        );
        return mongoOps.aggregate(newAggregation(
            match(Criteria.where("owner.$id").is(ownerId)),
            lookup("transaction", "_id", "accountId", "transactions"),
            unwind("$transactions"),
            replaceRoot("$transactions"),
            match(criteria),
            stage("""
                { $group: {
                    _id: {
                        category: "$category",
                        subCategory: "$subCategory",
                        month: {
                            $dateToString: { format: "%Y-%m-01T00:00:00.000Z", date: "$date" }
                        }
                    },
                    total: { $sum: { $toDouble: "$amount" } },
                    transactions: { $sum: 1 }
                }}
            """),
            stage("{ $replaceRoot: { newRoot: { $mergeObjects: [ \"$_id\", \"$$ROOT\" ] } } }"),
            sort(ASC, "month", "category")
        ), Account.class, Insight.class).getMappedResults();
    }
}
