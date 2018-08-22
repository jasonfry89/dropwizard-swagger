/**
 * Copyright (C) 2014 Federico Recio
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.federecio.dropwizard.swagger;

import io.swagger.annotations.ApiOperation;
import io.swagger.models.Swagger;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import io.dropwizard.auth.Auth;
import io.swagger.annotations.ApiParam;

/**
 * ApiListingResource has same code as import io.swagger.jaxrs.listing.ApiListingResource
 * but extends a custom BaseApiListingResource
 */

@Path("/swagger.{type:json|yaml}")
public class ApiListingResource extends BaseApiListingResource {

    @Context
    ServletContext context;

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @ApiOperation(value = "The swagger definition in either JSON or YAML", hidden = true)
    public Response getListing(
            @Context Application app,
            @Context ServletConfig sc,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            @PathParam("type") String type,
            @Auth @ApiParam(hidden = true) SwaggerUser user) {

        if (!user.getPermission().getCanAccessSwagger()) return Response.noContent().build();

        Swagger swagger = process(app, context, sc, headers, uriInfo);
        if (swagger == null) return Response.status(404).build();
        String contentType = StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")
                ? "application/yaml" : MediaType.APPLICATION_JSON;
        return Response.ok().entity(swagger).type(contentType).build();
    }
}

