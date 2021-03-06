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
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.util.ReflectionUtils;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A {@link ConfiguredBundle} that provides hassle-free configuration of Swagger and Swagger UI
 * on top of Dropwizard.
 *
 * @author Federico Recio
 * @author Flemming Frandsen
 * @author Tristan Burch
 */
public abstract class SwaggerBundle<T extends Configuration> implements ConfiguredBundle<T> {

    private static Logger logger = LoggerFactory.getLogger(SwaggerBundle.class);
    private static boolean assetBundleRegistered = false;

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
    public void run(T configuration, Environment environment) throws Exception {
        List<SwaggerBundleConfiguration> swaggerBundleConfigurations = getSwaggerBundleConfigurations(configuration);
        if (swaggerBundleConfigurations.isEmpty()) {
            throw new IllegalStateException("Provide at least one instance of SwaggerBundleConfiguration");
        }
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        environment.jersey().register(new SwaggerSerializers());

        // Register the resource that authenticate swagger login
        Resource.Builder swaggerLoginBuilder = Resource
                .builder()
                .path(Constants.SWAGGER_LOGIN_PATH);
        try {
            swaggerLoginBuilder.addMethod("POST")
                    .produces(MediaType.TEXT_PLAIN)
                    .handledBy(getAuthenticator(configuration), Authenticator.class.getMethod("getToken", String.class, String.class));
        } catch (NoSuchMethodException e) {
            logger.error("get token method not found in Authenticator.class");
        }
        Resource swaggerLoginResource = swaggerLoginBuilder.build();
        environment.jersey().getResourceConfig().registerResources(swaggerLoginResource);

        swaggerBundleConfigurations.forEach(swaggerConfig -> {
            ConfigurationHelper configurationHelper = new ConfigurationHelper(configuration, swaggerConfig);
            //Register asset bundle resources
            if (!assetBundleRegistered) {
                new AssetsBundle(
                        Constants.SWAGGER_RESOURCES_PATH,
                        configurationHelper.getSwaggerUriPath(),
                        null,
                        Constants.SWAGGER_ASSETS_NAME).run(environment);
                assetBundleRegistered = true;
            }

            // Register the resource that returns the swagger HTML
            Resource.Builder swaggerHtmlBuilder = Resource
                    .builder()
                    .path(configurationHelper.getHtmlResourcePath());
            swaggerHtmlBuilder.addMethod("GET")
                    .produces(MediaType.TEXT_HTML)
                    .handledBy((Inflector<ContainerRequestContext, SwaggerView>) containerRequestContext -> new
                            SwaggerView(configurationHelper.getBaseUrl(), configurationHelper.getSwaggerViewPath(), configurationHelper.getSwaggerLoginPathWithBaseUrl()));
            Resource swaggerHtmlResource = swaggerHtmlBuilder.build();
            environment.jersey().getResourceConfig().registerResources(swaggerHtmlResource);

            BeanConfig beanConfig = setUpSwagger(swaggerConfig, configurationHelper.getBaseUrl());
            // Register the resource that returns the swagger Listing json
            Resource swaggerListingResource = Resource
                    .builder(ApiListingResource.class)
                    .path(configurationHelper.getSwaggerAPIListingPath())
                    .build();
            environment.jersey().getResourceConfig().registerResources(swaggerListingResource);

            applyApiListingFilter(beanConfig, configurationHelper);
            environment.getApplicationContext().setAttribute(configurationHelper.getSwaggerName(), beanConfig.getSwagger());
        });
    }

    protected abstract List<SwaggerBundleConfiguration> getSwaggerBundleConfigurations(T configuration);

    protected abstract Authenticator getAuthenticator(T configuration);

    private BeanConfig setUpSwagger(SwaggerBundleConfiguration configuration, String baseUrl) {
        BeanConfig config = new BeanConfig();

        if (configuration.getTitle() != null) {
            config.setTitle(configuration.getTitle());
        }

        if (configuration.getVersion() != null) {
            config.setVersion(configuration.getVersion());
        }

        if (configuration.getDescription() != null) {
            config.setDescription(configuration.getDescription());
        }

        if (configuration.getContact() != null) {
            config.setContact(configuration.getContact());
        }

        if (configuration.getLicense() != null) {
            config.setLicense(configuration.getLicense());
        }

        if (configuration.getLicenseUrl() != null) {
            config.setLicenseUrl(configuration.getLicenseUrl());
        }

        if (configuration.getTermsOfServiceUrl() != null) {
            config.setTermsOfServiceUrl(configuration.getTermsOfServiceUrl());
        }

        // The host provided is of the form http://mything.com, https://mything.com, or mything.com
        // Swagger requires us to split the http(s) component from the host
        if(configuration.getHost() != null) {
            final String host = configuration.getHost();
            final String httpPrefix = "http://";
            final boolean startsWithHTTP = host.startsWith(httpPrefix);
            final String httpsPrefix = "https://";
            final boolean startsWithHTTPS = host.startsWith(httpsPrefix);
            if(startsWithHTTP) {
                final String hostName = host.substring(httpPrefix.length());
                config.setHost(hostName);
                config.setSchemes(new String[] { "http" });
            } else if(startsWithHTTPS) {
                final String hostName = host.substring(httpsPrefix.length());
                config.setHost(hostName);
                config.setSchemes(new String[] { "https" });
            } else {
                config.setHost(host);
            }
        }

        config.setBasePath(baseUrl);

        if (configuration.getResourcePackage() == null) {
            throw new IllegalStateException("Resource package needs to be specified for Swagger to correctly detect annotated resources");
        }
        config.setResourcePackage(configuration.getResourcePackage());
        config.setScan(true);
        return config;
    }


    private void applyApiListingFilter(BeanConfig beanConfig, ConfigurationHelper configurationHelper) {
        LinkedHashMap<String, io.swagger.models.Path> allowedEndpoints = getAllowedEnpoints(beanConfig, configurationHelper);
        beanConfig.getSwagger().setPaths(allowedEndpoints);
    }

    private LinkedHashMap<String, io.swagger.models.Path> getAllowedEnpoints(
            BeanConfig beanConfig,
            ConfigurationHelper configurationHelper) {

        LinkedHashMap<String, io.swagger.models.Path> allowedEndpoints = new LinkedHashMap<>();
        HashSet<String> resourceFilters = configurationHelper.getApiListingFilters();
        Set<Class<?>> classes = beanConfig.classes();

        for (Class<?> klazz : classes) {
            HashSet<String> classFilters = getFilters(ReflectionUtils.getAnnotation(klazz, ApiListing.class));
            Path apiPath = ReflectionUtils.getAnnotation(klazz, javax.ws.rs.Path.class);
            Method methods[] = klazz.getMethods();

            for (Method method : methods) {
                boolean includeEndpoint = resourceFilters.isEmpty()
                        || classFilters.stream().anyMatch(resourceFilters::contains)
                        || getFilters(ReflectionUtils.getAnnotation(method, ApiListing.class)).stream().anyMatch(resourceFilters::contains);
                if (includeEndpoint) {
                    Path methodPath = ReflectionUtils.getAnnotation(method, Path.class);
                    String operationPath = getPath(apiPath, methodPath, configurationHelper);
                    io.swagger.models.Path path = beanConfig.getSwagger().getPaths().get(operationPath);
                    if (!operationPath.equals("") && path != null) allowedEndpoints.put(operationPath, path);
                }
            }
        }
        return allowedEndpoints;
    }

    private HashSet<String> getFilters(ApiListing filter) {
        if (filter == null) return new HashSet<>();
        return new HashSet<>(Arrays.asList(filter.values()));
    }

    private String getPath(Path classLevelPath, Path methodLevelPath, ConfigurationHelper configurationHelper) {
        if (classLevelPath == null && methodLevelPath == null) return "";
        String classPath = "/";
        String methodPath = "/";

        if (classLevelPath != null) classPath = configurationHelper.stripAndNormalizeUrl(classLevelPath.value());
        if (methodLevelPath != null) methodPath = configurationHelper.stripAndNormalizeUrl(methodLevelPath.value());

        if (classPath.equals("/") && methodPath.equals("/")) return "";
        return (classPath.equals("/") ? "" : classPath) + (methodPath.equals("/") ? "" : methodPath);
    }
}
