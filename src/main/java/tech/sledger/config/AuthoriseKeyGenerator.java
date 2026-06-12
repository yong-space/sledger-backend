package tech.sledger.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import tech.sledger.model.user.User;
import java.lang.reflect.Method;

/**
 * Builds the {@code authorise} cache key as {@code <userId>-<accountId>} using direct Java
 * calls rather than a SpEL key expression. SpEL property navigation (e.g. {@code #auth.principal.id})
 * resolves getters reflectively at runtime, which fails in the GraalVM native image when those
 * getters are absent from the reachability metadata. A KeyGenerator avoids reflection entirely.
 */
@Component("authoriseKeyGenerator")
public class AuthoriseKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        Authentication auth = (Authentication) params[0];
        long accountId = (Long) params[1];
        long userId = ((User) auth.getPrincipal()).getId();
        return userId + "-" + accountId;
    }
}
