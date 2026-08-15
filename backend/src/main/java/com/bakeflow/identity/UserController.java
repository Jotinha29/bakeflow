package com.bakeflow.identity;

import static com.bakeflow.identity.IdentityDtos.UserInput;
import static com.bakeflow.identity.IdentityDtos.UserPage;
import static com.bakeflow.identity.IdentityDtos.UserUpdate;
import static com.bakeflow.identity.IdentityDtos.UserView;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAuthority('SCOPE_USER_READ')")
public class UserController {
    private final IdentityService service;

    public UserController(IdentityService service) { this.service = service; }

    @GetMapping
    UserPage list(@RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.users(search, active, role, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_USER_WRITE')")
    UserView create(@Valid @RequestBody UserInput input) { return service.create(input); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_USER_WRITE')")
    UserView update(@PathVariable UUID id, @Valid @RequestBody UserUpdate input) {
        return service.update(id, input);
    }
}
