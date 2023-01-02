package tech.sledger.model.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Registration {
    String username;
    String password;
    String password2;
}
