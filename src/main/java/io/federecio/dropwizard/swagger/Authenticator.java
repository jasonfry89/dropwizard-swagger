/**
 * Copyright (C) 2014 Federico Recio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.federecio.dropwizard.swagger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

public abstract class Authenticator {

    public abstract String login(String username, String password, UriInfo uriInfo);

    public String getToken(
            @HeaderParam(value = "username") String username,
            @HeaderParam(value = "password") String password,
            @Context UriInfo uriInfo) {
        String token = login(username, password, uriInfo);
        if (token == null || token.equals("")) throw new NotAuthorizedException("Not Authorized");
        return token;
    }
}