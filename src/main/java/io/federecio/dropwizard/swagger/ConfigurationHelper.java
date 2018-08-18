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

    public ConfigurationHelper(Configuration configuration, SwaggerBundleConfiguration swaggerBundleConfiguration) {
        this.configuration = configuration;
        this.swaggerBundleConfiguration = swaggerBundleConfiguration;
    }

    public String getAssetName() {
        return Constants.SWAGGER_ASSETS_NAME + swaggerBundleConfiguration.getUriPrefix();
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
        final String uriPrefix = stripUrlSlashes(this.swaggerBundleConfiguration.getUriPrefix());
        return (jerseyRootPath.equals("/") ? "" : jerseyRootPath) + (uriPrefix.equals("/") ? "" : uriPrefix) + Constants.SWAGGER_URI_PATH;
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

    public String getSwaggerName(){
        return "swagger" + getSwaggerViewPath();
    }

    private String getJerseyRootPath() {
        String rootPath;
        ServerFactory serverFactory = configuration.getServerFactory();

        if (serverFactory instanceof SimpleServerFactory) {
            rootPath = ((SimpleServerFactory) serverFactory).getJerseyRootPath().get();
        } else {
            rootPath = ((DefaultServerFactory) serverFactory).getJerseyRootPath().get();
        }

        return stripUrlSlashes(rootPath);
    }

    private String getApplicationContextPath() {
        String applicationContextPath;

        ServerFactory serverFactory = configuration.getServerFactory();

        if (serverFactory instanceof SimpleServerFactory) {
            applicationContextPath = ((SimpleServerFactory) serverFactory).getApplicationContextPath();
        } else {
            applicationContextPath = ((DefaultServerFactory) serverFactory).getApplicationContextPath();
        }

        return stripUrlSlashes(applicationContextPath);
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