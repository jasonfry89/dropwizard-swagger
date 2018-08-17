package io.federecio.dropwizard.swagger;

import io.swagger.config.FilterFactory;
import io.swagger.config.Scanner;
import io.swagger.config.SwaggerConfig;
import io.swagger.core.filter.SpecFilter;
import io.swagger.core.filter.SwaggerSpecFilter;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.JaxrsScanner;
import io.swagger.jaxrs.config.ReaderConfigUtils;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The process() was modified to allow calling a customized ctxService.getSwagger(path)
 * Removed isInitialized and will be using Swagger object stored in ServletContext
 */

public class BaseApiListingResource extends io.swagger.jaxrs.listing.BaseApiListingResource {
    private static volatile ConcurrentMap<String, Boolean> initializedScanner = new ConcurrentHashMap<>();
    private static volatile ConcurrentMap<String, Boolean> initializedConfig = new ConcurrentHashMap<>();
    private static Logger LOGGER = LoggerFactory.getLogger(io.swagger.jaxrs.listing.BaseApiListingResource.class);


    protected Swagger process(
            Application app,
            ServletContext servletContext,
            ServletConfig sc,
            HttpHeaders headers,
            UriInfo uriInfo) {
        String path = getPath(uriInfo);

        SwaggerContextService ctxService = (SwaggerContextService) new SwaggerContextService()
                .withServletConfig(sc)
                .withBasePath(uriInfo.getBaseUri().getPath());

        Swagger swagger = ctxService.getSwagger(path);

        synchronized (ApiListingResource.class) {
            if (SwaggerContextService.isScannerIdInitParamDefined(sc)) {
                if (!initializedScanner.containsKey(sc.getServletName() + "_" + SwaggerContextService.getScannerIdFromInitParam(sc))) {
                    swagger = scan(app, servletContext, sc, uriInfo);
                }
            } else if (SwaggerContextService.isConfigIdInitParamDefined(sc)) {
                if (!initializedConfig.containsKey(sc.getServletName() + "_" + SwaggerContextService.getConfigIdFromInitParam(sc))) {
                    swagger = scan(app, servletContext, sc, uriInfo);
                }
            } else if (SwaggerContextService.isUsePathBasedConfigInitParamDefined(sc)) {
                if (!initializedConfig.containsKey(sc.getServletName() + "_" + ctxService.getBasePath())) {
                    swagger = scan(app, servletContext, sc, uriInfo);
                }
            }
        }

        if (swagger != null) {
            SwaggerSpecFilter filterImpl = FilterFactory.getFilter();
            if (filterImpl != null) {
                SpecFilter f = new SpecFilter();
                swagger = f.filter(swagger, filterImpl, getQueryParams(uriInfo.getQueryParameters()), getCookies(headers),
                        getHeaders(headers));
            }
        }
        return swagger;
    }

    private static synchronized Swagger scan(Application app, ServletContext context, ServletConfig sc, UriInfo uriInfo) {
        Swagger swagger = null;

        io.swagger.jaxrs.config.SwaggerContextService ctxService = new io.swagger.jaxrs.config.SwaggerContextService()
                .withServletConfig(sc)
                .withBasePath(uriInfo.getBaseUri().getPath());

        Scanner scanner = ctxService.getScanner();
        if (scanner != null) {
            SwaggerSerializers.setPrettyPrint(scanner.getPrettyPrint());
            swagger = new io.swagger.jaxrs.config.SwaggerContextService()
                    .withServletConfig(sc)
                    .withBasePath(uriInfo.getBaseUri().getPath())
                    .getSwagger();
            Set<Class<?>> classes;
            if (scanner instanceof JaxrsScanner) {
                JaxrsScanner jaxrsScanner = (JaxrsScanner) scanner;
                classes = jaxrsScanner.classesFromContext(app, sc);
            } else {
                classes = scanner.classes();
            }
            if (classes != null) {
                Reader reader = new Reader(swagger, ReaderConfigUtils.getReaderConfig(context));
                swagger = reader.read(classes);
                if (scanner instanceof SwaggerConfig) {
                    swagger = ((SwaggerConfig) scanner).configure(swagger);
                } else {
                    SwaggerConfig swaggerConfig = ctxService.getConfig();
                    if (swaggerConfig != null) {
                        LOGGER.debug("configuring swagger with " + swaggerConfig);
                        swaggerConfig.configure(swagger);
                    } else {
                        LOGGER.debug("no configurator");
                    }
                }
                new io.swagger.jaxrs.config.SwaggerContextService()
                        .withServletConfig(sc)
                        .withBasePath(uriInfo.getBaseUri().getPath())
                        .updateSwagger(swagger);
            }
        }

        if (io.swagger.jaxrs.config.SwaggerContextService.isScannerIdInitParamDefined(sc)) {
            initializedScanner.put(sc.getServletName() + "_" + io.swagger.jaxrs.config.SwaggerContextService.getScannerIdFromInitParam(sc), true);
        } else if (io.swagger.jaxrs.config.SwaggerContextService.isConfigIdInitParamDefined(sc)) {
            initializedConfig.put(sc.getServletName() + "_" + io.swagger.jaxrs.config.SwaggerContextService.getConfigIdFromInitParam(sc), true);
        } else if (io.swagger.jaxrs.config.SwaggerContextService.isUsePathBasedConfigInitParamDefined(sc)) {
            initializedConfig.put(sc.getServletName() + "_" + ctxService.getBasePath(), true);
        }

        return swagger;
    }

    private static Map<String, List<String>> getQueryParams(MultivaluedMap<String, String> params) {
        Map<String, List<String>> output = new HashMap<>();
        if (params != null) {
            for (String key : params.keySet()) {
                List<String> values = params.get(key);
                output.put(key, values);
            }
        }
        return output;
    }

    private static Map<String, String> getCookies(HttpHeaders headers) {
        Map<String, String> output = new HashMap<>();
        if (headers != null) {
            for (String key : headers.getCookies().keySet()) {
                Cookie cookie = headers.getCookies().get(key);
                output.put(key, cookie.getValue());
            }
        }
        return output;
    }

    private static Map<String, List<String>> getHeaders(HttpHeaders headers) {
        Map<String, List<String>> output = new HashMap<>();
        if (headers != null) {
            for (String key : headers.getRequestHeaders().keySet()) {
                List<String> values = headers.getRequestHeaders().get(key);
                output.put(key, values);
            }
        }
        return output;
    }

    private String getPath(UriInfo uriInfo) {
        return uriInfo.getRequestUri().getPath().replace("/swagger.json", "");
    }
}

