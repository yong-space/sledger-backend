package tech.sledger.model.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.sledger.model.validation.ComplexPassword;
import tech.sledger.model.validation.NoAliasedEmails;
import tech.sledger.model.validation.Sanitised;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Profile {
    @NotNull(message = "Display name cannot be null")
    @Size(min = 2, max = 20, message = "Display name should be between 2 and 20 characters")
    @Sanitised(message = "Display name has invalid characters")
    private String displayName;

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    @Size(max = 50, message = "Email should be at most 50 characters")
    @Sanitised(message = "Email has invalid characters")
    @NoAliasedEmails
    private String username;

    private String password;

    @Size(min = 8, max = 100, message = "Password should be between 8 and 100 characters")
    @ComplexPassword
    private String newPassword;
}
