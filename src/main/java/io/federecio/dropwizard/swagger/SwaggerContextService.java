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

import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.models.Swagger;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;

/**
 * created getSwagger(String uriPath), so that swagger is sourced from ServletContext
 * Removed isInitialized and will be using Swagger object stored in ServletContext
 */

public class SwaggerContextService extends io.swagger.jaxrs.config.SwaggerContextService {
    private String configId;
    private String contextId;
    private String basePath;
    private ServletConfig sc;

    public SwaggerContextService(ServletConfig sc, String basePath) {
        this.basePath = normalizeBasePath(basePath);
        this.sc = sc;
    }

    public Swagger getSwagger(String uriPath) {
        Swagger swagger = null;
        if (isServletConfigAvailable(sc)) swagger = (Swagger) sc.getServletContext().getAttribute("swagger" + uriPath);
        if (swagger == null) swagger = (Swagger) getSwaggerFromConfig();
        if (swagger == null) swagger = new Swagger();
        return swagger;
    }

    private Object getSwaggerFromConfig() {
        String configIdKey;
        if (configId != null) {
            configIdKey = CONFIG_ID_PREFIX + configId;
        } else if (contextId != null) {
            configIdKey = CONFIG_ID_PREFIX + contextId;
        } else {
            if (isServletConfigAvailable(sc)) {
                configIdKey = (sc.getInitParameter(CONFIG_ID_KEY) != null) ? CONFIG_ID_PREFIX + sc.getInitParameter(CONFIG_ID_KEY) : null;
                if (configIdKey == null) {
                    boolean usePathBasedConfig = Boolean.valueOf(sc.getInitParameter(USE_PATH_BASED_CONFIG));
                    if (usePathBasedConfig && StringUtils.isNotBlank(basePath)) {
                        configIdKey = CONFIG_ID_PREFIX + basePath;
                    } else {
                        configIdKey = (sc.getInitParameter(CONTEXT_ID_KEY) != null) ? CONFIG_ID_PREFIX + sc.getInitParameter(CONTEXT_ID_KEY) : CONFIG_ID_DEFAULT;
                    }
                }
            } else {
                configIdKey = CONFIG_ID_DEFAULT;
            }
        }
        return SwaggerConfigLocator.getInstance().getSwagger(configIdKey);
    }

    /**
     * Normalize base path to the canonical form by adding trailing and leading slashes
     * @param basePath base path to normalize
     * @return normalized base path
     */
    private static String normalizeBasePath(final String basePath) {
        if (basePath == null) return basePath;

        String normalizedBasePath = basePath.trim();
        if (!normalizedBasePath.startsWith("/")) normalizedBasePath = "/" + normalizedBasePath;
        if (!normalizedBasePath.endsWith("/")) normalizedBasePath = normalizedBasePath + "/";

        return normalizedBasePath;
    }
}
