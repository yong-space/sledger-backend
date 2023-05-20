package tech.sledger.endpoints;

import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.sledger.model.account.Account;
import tech.sledger.model.tx.Transaction;
import tech.sledger.service.ImportService;
import tech.sledger.service.UserService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/import")
public class ImportEndpoints {
    private final ImportService importService;
    private final UserService userService;

    @PostMapping
    public List<Transaction> importUpload(
        Authentication auth,
        @RequestParam("file") MultipartFile file,
        @RequestParam("accountId") long accountId
    ) throws IOException, CsvException {
        Account account = userService.authorise(auth, accountId);
        try (InputStream inputStream = file.getInputStream()) {
            return importService.process(account, inputStream);
        }
    }
}
