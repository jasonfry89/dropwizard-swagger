package io.federecio.dropwizard.swagger;

import java.security.Principal;

public interface SwaggerUser extends Principal {
    public SwaggerPermission getPermission();
}

