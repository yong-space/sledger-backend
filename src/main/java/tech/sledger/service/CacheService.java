package tech.sledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import tech.sledger.model.account.Account;

@Service
@Slf4j
public class CacheService {
  @CacheEvict(value="tx", key="#account.id")
  public void clearTxCache(Account account) {}
}
