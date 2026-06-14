package tech.sledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {
  // txAll (the per-user union) is the only transaction cache — single-account views are derived
  // from it. Mutations only know an accountId, not the owner, so clear all entries; mutations are
  // rare relative to reads, so this is cheap. Mirrors clearAuthCache.
  @CacheEvict(value="txAll", allEntries=true)
  public void clearTxCache(long accountId) {}

  // authorise cache is keyed by principal id + account id, so a single account id
  // cannot target the owning user's entry directly; clear all entries instead.
  // Account edits/deletes are rare and the cache is small, so this is cheap.
  @CacheEvict(value="authorise", allEntries=true)
  public void clearAuthCache(long accountId) {}
}
