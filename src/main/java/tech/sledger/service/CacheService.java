package tech.sledger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheService {
  @CacheEvict(value="tx", key="#accountId")
  public void clearTxCache(long accountId) {}
}
