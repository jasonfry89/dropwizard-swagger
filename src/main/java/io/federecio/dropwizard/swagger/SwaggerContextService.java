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
}
