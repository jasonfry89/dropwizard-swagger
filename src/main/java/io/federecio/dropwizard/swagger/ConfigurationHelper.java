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

import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.util.HashSet;

/**
 * Wrapper around Dropwizard's configuration and the bundle's config that simplifies getting some
 * information from them.
 *
 * @author Federico Recio
 * @author Flemming Frandsen
 */
public class ConfigurationHelper {

    private final Configuration configuration;
    private final SwaggerBundleConfiguration swaggerBundleConfiguration;

    ConfigurationHelper(Configuration configuration, SwaggerBundleConfiguration swaggerBundleConfiguration) {
        this.configuration = configuration;
        this.swaggerBundleConfiguration = swaggerBundleConfiguration;
    }

    public String getSwaggerViewPath(){
        final String baseUrl = getBaseUrl();
        final String uriPrefix = stripUrlSlashes(this.swaggerBundleConfiguration.getUriPrefix());
        if (baseUrl.equals("/") && uriPrefix.equals("/")) {
            return "";
        }
        return (baseUrl.equals("/") ? "" : baseUrl) + (uriPrefix.equals("/") ? "" : uriPrefix);
    }

    public String getHtmlResourcePath() {
        final String uriPrefix = stripUrlSlashes(this.swaggerBundleConfiguration.getUriPrefix());
        return (uriPrefix.equals("/") ? "" : uriPrefix) + Constants.SWAGGER_PATH;
    }

    public String getSwaggerUriPath() {
        final String jerseyRootPath = getJerseyRootPath();
        return (jerseyRootPath.equals("/") ? "" : jerseyRootPath) + Constants.SWAGGER_URI_PATH;
    }

    public String getSwaggerAPIListingPath(){
        final String uriPrefix = stripUrlSlashes(this.swaggerBundleConfiguration.getUriPrefix());
        return (uriPrefix.equals("/") ? "" : uriPrefix) + "/swagger.{type:json|yaml}";
    }

    public String getBaseUrl() {
        final String applicationContextPath = getApplicationContextPath();
        final String rootPath = getJerseyRootPath();
        if (rootPath.equals("/") && applicationContextPath.equals("/")) {
            return "";
        }
        return (applicationContextPath.equals("/") ? "" : applicationContextPath) + (rootPath.equals("/") ? "" : rootPath);
    }

    public String getSwaggerLoginPathWithBaseUrl() {
        final String baseUrl = getBaseUrl();
        return (baseUrl.equals("/") ? "" : baseUrl) + Constants.SWAGGER_LOGIN_PATH;
    }

    public String getSwaggerName(){
        return "swagger" + getSwaggerViewPath();
    }

    private String getJerseyRootPath() {
        ServerFactory serverFactory = configuration.getServerFactory();
        return serverFactory instanceof SimpleServerFactory
                ? stripUrlSlashes(((SimpleServerFactory) serverFactory).getJerseyRootPath().orElse("/"))
                : stripUrlSlashes(((DefaultServerFactory) serverFactory).getJerseyRootPath().orElse("/"));
    }

    private String getApplicationContextPath() {
        ServerFactory serverFactory = configuration.getServerFactory();
        return serverFactory instanceof SimpleServerFactory
                ? stripUrlSlashes(((SimpleServerFactory) serverFactory).getApplicationContextPath())
                : stripUrlSlashes(((DefaultServerFactory) serverFactory).getApplicationContextPath());
    }

    private String stripUrlSlashes(String urlToStrip) {
        if (urlToStrip.endsWith("/*")) {
            urlToStrip = urlToStrip.substring(0, urlToStrip.length() - 1);
        }

        if (urlToStrip.length() > 1 && urlToStrip.endsWith("/")) {
            urlToStrip = urlToStrip.substring(0, urlToStrip.length() - 1);
        }

        return urlToStrip;
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("/")) return "/" + url;
        return url;
    }

    protected String stripAndNormalizeUrl(String url) {
        return normalizeUrl(stripUrlSlashes(url));
    }

    protected HashSet<String> getApiListingFilters(){
        return swaggerBundleConfiguration.getApiListingFilters();
    }
}