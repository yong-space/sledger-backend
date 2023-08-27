package tech.sledger.service;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import tech.sledger.service.importer.Importer;
import tech.sledger.service.importer.OcbcImporter;
import tech.sledger.service.importer.UobImporter;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {
    private final TemplateService templateService;
    private final Map<String, Class<? extends Importer>> importerMap = Map.of(
        "OCBC", OcbcImporter.class,
        "UOB", UobImporter.class
    );

    public List<Transaction> process(
        Account account, InputStream inputStream
    ) throws Exception {
        List<Template> templates = templateService.list(account.getOwner());
        if (!importerMap.containsKey(account.getIssuer().getName())) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported issuer");
        }
        return importerMap.get(account.getIssuer().getName())
            .getConstructor().newInstance().process(account, inputStream, templates)
            .stream().map((t) -> {
                if (t instanceof CashTransaction ct) {
                    ct.setRemarks(toProperCase(ct.getRemarks()));
                    return ct;
                }
                return t;
            })
            .sorted(comparing(Transaction::getDate))
            .toList();
    }

    private String toProperCase(String s) {
        final String ACTIONABLE_DELIMITERS = " '-/";

        StringBuilder sb = new StringBuilder();
        boolean capNext = true;

        for (char c : s.toCharArray()) {
            c = (capNext) ? Character.toUpperCase(c) : Character.toLowerCase(c);
            sb.append(c);
            capNext = ACTIONABLE_DELIMITERS.indexOf(c) >= 0;
        }
        return sb.toString();
    }
}
