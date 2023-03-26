package tech.sledger.repo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import tech.sledger.model.account.Account;
import tech.sledger.model.account.AccountDTO;
import java.util.List;
import java.util.Map;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@RequiredArgsConstructor
public class AccountOpsRepoImpl implements AccountOpsRepo {
    private final MongoOperations mongoOps;

    @Override
    public List<AccountDTO> getAccountsWithMetrics(long ownerId) {
        AggregationOperation matchOwner = stage("{ $match: { \"owner.$id\": " + ownerId + " } }");
        AggregationOperation lookup = stage("""
            {
                $lookup: {
                  from: "transaction",
                  let: { accountId: "$_id" },
                  pipeline: [
                    { $match: { $expr: { $eq: [ "$$accountId", "$account.$id" ] } } },
                    { $group: { _id: "$account.$id", transactions: { $sum: 1 }, maxDate: { $max: "$date" } } },
                    {
                      $lookup: {
                        from: "transaction",
                        let: { accountId: "$_id", maxDate: "$maxDate" },
                        pipeline: [
                          { $match: { $expr: { $and: [
                            { $eq: [ "$account.$id", "$$accountId" ] },
                            { $eq: [ "$date", "$$maxDate" ] }
                          ] } } },
                          { $project: { _id: 0, balance: 1 } }
                        ],
                        as: "balanceLookup"
                      }
                    },
                    { $replaceRoot: { newRoot: { $mergeObjects: [ { $arrayElemAt: [ "$balanceLookup", 0 ] }, "$$ROOT" ] } } },
                    { $project: { balance: 1, transactions: 1 } }
                  ],
                  as: "lookup"
                }
            }
            """);
        AggregationOperation replaceRoot = stage("{ $replaceRoot: { newRoot: { $mergeObjects: [ { $arrayElemAt: [ \"$lookup\", 0 ] }, \"$$ROOT\" ] } } }");
        AggregationOperation project = stage("{ $project: { lookup: 0 } }");
        Aggregation aggregate = newAggregation(matchOwner, lookup, replaceRoot, project);
        return mongoOps.aggregate(aggregate, Account.class, AccountDTO.class).getMappedResults();
    }

    @Override
    public List<String> getTopRemarks(long ownerId, String q) {
        return mongoOps.aggregate(newAggregation(
            match(new Criteria("owner.$id").is(ownerId)),
            lookup("transaction", "_id", "account.$id", "lookup"),
            unwind("$lookup"),
            replaceRoot("$lookup"),
            match(new Criteria("remarks").regex(q, "i")),
            group("$remarks").count().as("count"),
            sort(Sort.Direction.DESC, "count").and(Sort.Direction.ASC, "_id"),
            limit(5),
            project("_id")
        ), Account.class, Map.class).getMappedResults()
        .stream().map(m -> (String) m.get("_id")).toList();
    }

    @Override
    public List<String> getTopCategories(long ownerId, String q) {
        return mongoOps.aggregate(newAggregation(
                match(new Criteria("owner.$id").is(ownerId)),
                lookup("transaction", "_id", "account.$id", "lookup"),
                unwind("$lookup"),
                replaceRoot("$lookup"),
                match(new Criteria("category").regex(q, "i")),
                group("category").count().as("count"),
                sort(Sort.Direction.DESC, "count").and(Sort.Direction.ASC, "_id"),
                limit(5),
                project("_id")
            ), Account.class, Map.class).getMappedResults()
            .stream().map(m -> (String) m.get("_id")).toList();
    }
}
