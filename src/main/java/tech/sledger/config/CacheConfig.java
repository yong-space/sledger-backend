package tech.sledger.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.util.Collection;

/**
 * Caffeine-backed caches with explicit bounds so the heap can't grow unbounded.
 *
 * <ul>
 *   <li><b>tx</b> — per-account transaction listings, keyed by account id.</li>
 *   <li><b>txAll</b> — the all-accounts ("account 0") Cash/Credit listing, keyed by user id.
 *       Rows can be held in both caches at once — accepted trade-off for keeping reads and
 *       eviction simple.</li>
 *   <li><b>authorise</b> — tiny account-ownership entries; a generous size cap + TTL is enough.</li>
 * </ul>
 *
 * Both transaction caches are weighed by transaction count so {@code maximumWeight} bounds memory
 * directly, plus an idle-reclaim TTL so inactive entries don't linger.
 *
 * Unregistered cache names fall back to an unbounded Caffeine cache (none are used today).
 */
@Configuration
public class CacheConfig {
    /** Cap on total cached transactions per transaction cache (weight = list size). Tune for the
     *  pod's heap budget; must exceed the largest single user's history to keep them cacheable. */
    private static final long MAX_CACHED_TRANSACTIONS = 100_000;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        // Both caches only ever hold non-null transaction lists, so weigh by list size directly.
        manager.registerCustomCache("tx", Caffeine.newBuilder()
            .maximumWeight(MAX_CACHED_TRANSACTIONS)
            .weigher((Object key, Object value) -> Math.max(1, ((Collection<?>) value).size()))
            .expireAfterAccess(Duration.ofMinutes(30))
            .build());
        manager.registerCustomCache("txAll", Caffeine.newBuilder()
            .maximumWeight(MAX_CACHED_TRANSACTIONS)
            .weigher((Object key, Object value) -> Math.max(1, ((Collection<?>) value).size()))
            .expireAfterAccess(Duration.ofMinutes(30))
            .build());
        manager.registerCustomCache("authorise", Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build());
        return manager;
    }
}
