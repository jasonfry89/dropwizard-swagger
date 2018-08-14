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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Resource;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * A {@link io.dropwizard.ConfiguredBundle} that provides hassle-free configuration of Swagger and Swagger UI
 * on top of Dropwizard.
 *
 * @author Federico Recio
 * @author Flemming Frandsen
 * @author Tristan Burch
 */
public abstract class SwaggerBundle<T extends Configuration> implements ConfiguredBundle<T> {

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.addBundle(new ViewBundle<Configuration>() {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(final Configuration configuration) {
                return ImmutableMap.of();
            }
        });
    }

    @Override
    public void run(T configuration, Environment environment) {
        SwaggerBundleConfiguration swaggerBundleConfiguration = getSwaggerBundleConfiguration(configuration);
        if (swaggerBundleConfiguration == null) {
            throw new IllegalStateException("You need to provide an instance of SwaggerBundleConfiguration");
        }

        swaggerBundleConfiguration.getSwaggers().forEach(singleSwaggerConfiguration -> {
            ConfigurationHelper configurationHelper = new ConfigurationHelper(configuration, singleSwaggerConfiguration);
            new AssetsBundle(Constants.SWAGGER_RESOURCES_PATH, configurationHelper.getSwaggerUriPath(), null, Constants.SWAGGER_ASSETS_NAME).run(environment);

            // Register the resource that returns the swagger HTML
            String urlPattern = configurationHelper.getUrlPattern();
            Resource.Builder resourceBuilder = Resource
                    .builder()
                    .path(urlPattern);
            resourceBuilder
                    .addMethod("GET")
                    .produces(MediaType.TEXT_HTML)
                    .handledBy((Inflector<ContainerRequestContext, SwaggerView>) containerRequestContext -> new SwaggerView(urlPattern));
            Resource resource = resourceBuilder.build();
            environment.jersey().getResourceConfig().registerResources(resource);
//        environment.jersey().register(new SwaggerResource(urlPattern));

            BeanConfig beanConfig = setUpSwagger(singleSwaggerConfiguration, configurationHelper.getUrlPattern());

            // Register the resource that returns swagger.json
            Resource swaggerJSONResource = Resource
                    .builder(ApiListingResource.class)
                    .path(urlPattern + ".{type:json|yaml}")
                    .build();
            environment.jersey().getResourceConfig().registerResources(swaggerJSONResource);
//        environment.jersey().register(new ApiListingResource());

            // Register the serializers
            environment.jersey().register(new SwaggerSerializers());
            environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

            Swagger swagger = beanConfig.getSwagger();
            environment.getApplicationContext().setAttribute(urlPattern, swagger);
        });
    }

    @SuppressWarnings("unused")
    protected abstract SwaggerBundleConfiguration getSwaggerBundleConfiguration(T configuration);

    private BeanConfig setUpSwagger(SingleSwaggerConfiguration swaggerBundleConfiguration, String urlPattern) {
        BeanConfig config = new BeanConfig();

        if (swaggerBundleConfiguration.getTitle() != null) {
            config.setTitle(swaggerBundleConfiguration.getTitle());
        }

        if (swaggerBundleConfiguration.getVersion() != null) {
            config.setVersion(swaggerBundleConfiguration.getVersion());
        }

        if (swaggerBundleConfiguration.getDescription() != null) {
            config.setDescription(swaggerBundleConfiguration.getDescription());
        }

        if (swaggerBundleConfiguration.getContact() != null) {
            config.setContact(swaggerBundleConfiguration.getContact());
        }

        if (swaggerBundleConfiguration.getLicense() != null) {
            config.setLicense(swaggerBundleConfiguration.getLicense());
        }

        if (swaggerBundleConfiguration.getLicenseUrl() != null) {
            config.setLicenseUrl(swaggerBundleConfiguration.getLicenseUrl());
        }

        if (swaggerBundleConfiguration.getTermsOfServiceUrl() != null) {
            config.setTermsOfServiceUrl(swaggerBundleConfiguration.getTermsOfServiceUrl());
        }

//        if (swaggerBundleConfiguration.getSecurityConfigurations() != null) {
//            for (SecurityConfiguration securityConfiguration : swaggerBundleConfiguration.getSecurityConfigurations()) {
//                if(!securityConfiguration.getType().equals("oauth")) {
//                    throw new Exception("asd");
//                }
//
//                OAuth2Definition definition = new OAuth2Definition();
//                definition.setFlow(securityConfiguration.getFlow());
//                definition.setAuthorizationUrl(securityConfiguration.getAuthorizationUrl());
//                definition.setTokenUrl(securityConfiguration.getTokenUrl());
//                definition.setType(securityConfiguration.getType());
//                config.getSwagger().addSecurityDefinition(securityConfiguration.getName(), definition);
//            }
//        }

        config.setBasePath(urlPattern);

        if (swaggerBundleConfiguration.getResourcePackage() != null) {
            config.setResourcePackage(swaggerBundleConfiguration.getResourcePackage());
        } else {
            throw new IllegalStateException("Resource package needs to be specified for Swagger to correctly detect annotated resources");
        }

        config.setScan(true);

        return config;
    }
}
