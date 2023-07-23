package tech.sledger.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import tech.sledger.model.account.Account;
import java.util.List;
import java.util.Map;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RequiredArgsConstructor
public class AccountOpsRepoImpl implements AccountOpsRepo {
    private final MongoOperations mongoOps;

    @Override
    public List<Map> getAccountsWithMetrics(long ownerId) {
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
                    { $project: { balance: 1, transactions: 1, ordinaryBalance: 1, specialBalance: 1, medisaveBalance: 1  } }
                  ],
                  as: "transactionLookup"
                }
            }
            """);
        Aggregation aggregate = newAggregation(
            match(new Criteria("owner.$id").is(ownerId)),
            lookup,
            stage("{ $replaceRoot: { newRoot: { $mergeObjects: [ { $arrayElemAt: [ \"$transactionLookup\", 0 ] }, \"$$ROOT\" ] } } }"),
            lookup("accountIssuer", "issuer.$id", "_id", "issuerLookup"),
            unwind("$issuerLookup"),
            sort(ASC, "type", "issuerLookup.name", "name"),
            stage("{ $addFields: { id: \"$_id\", issuerId: \"$issuerLookup._id\" } }"),
            stage("{ $project: { transactionLookup: 0, issuerLookup: 0, owner: 0, issuer: 0, _id: 0 } }")
        );
        return mongoOps.aggregate(aggregate, Account.class, Map.class).getMappedResults();
    }

    @Override
    public List<String> getTopStrings(long ownerId, String field, String q) {
        return mongoOps.aggregate(newAggregation(
                match(new Criteria("owner.$id").is(ownerId)),
                lookup("transaction", "_id", "accountId", "lookup"),
                unwind("$lookup"),
                replaceRoot("$lookup"),
                match(new Criteria(field).regex(q, "i")),
                group("$" + field).count().as("count"),
                sort(DESC, "count").and(ASC, "_id"),
                limit(5),
                project("_id")
            ), Account.class, Map.class).getMappedResults()
            .stream().map(m -> (String) m.get("_id")).toList();
    }
}
