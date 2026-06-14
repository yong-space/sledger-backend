package tech.sledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {
  // Per-account tx eviction also drops every user's all-transactions aggregate (txAll). Mutations
  // only know an accountId, not the owner, so a precise per-user evict isn't possible here; clearing
  // all entries is correct and cheap given mutations are rare relative to reads. Mirrors clearAuthCache.
  @Caching(evict = {
    @CacheEvict(value="tx", key="#accountId"),
    @CacheEvict(value="txAll", allEntries=true)
  })
  public void clearTxCache(long accountId) {}

  // authorise cache is keyed by principal id + account id, so a single account id
  // cannot target the owning user's entry directly; clear all entries instead.
  // Account edits/deletes are rare and the cache is small, so this is cheap.
  @CacheEvict(value="authorise", allEntries=true)
  public void clearAuthCache(long accountId) {}
}
