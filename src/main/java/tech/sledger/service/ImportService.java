package tech.sledger.service;

import static java.util.Comparator.comparing;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Template;
import tech.sledger.model.tx.Transaction;
import tech.sledger.service.importer.GrabImporter;
import tech.sledger.service.importer.Importer;
import tech.sledger.service.importer.OcbcImporter;
import tech.sledger.service.importer.UobImporter;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {
    private final TemplateService templateService;
    private final Map<String, Class<? extends Importer>> importerMap = Map.of(
        "OCBC", OcbcImporter.class,
        "UOB", UobImporter.class,
        "Grab", GrabImporter.class
    );

    public List<Transaction> process(
        Account account, InputStream inputStream
    ) throws Exception {
        String issuerName = account.getIssuer().getName();
        if (!importerMap.containsKey(issuerName)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported issuer");
        }
        AtomicInteger index = new AtomicInteger(1);

        boolean isGrab = issuerName.equals("Grab");
        List<Template> templates = templateService
            .list(account.getOwner())
            .stream()
            .filter(t -> isGrab == t.getCategory().equals("Location"))
            .toList();

        return importerMap.get(issuerName)
            .getConstructor().newInstance().process(account, inputStream, templates)
            .stream()
            .sorted(comparing(Transaction::getDate))
            .peek(t -> t.setId(index.getAndIncrement()))
            .toList();
    }
}
