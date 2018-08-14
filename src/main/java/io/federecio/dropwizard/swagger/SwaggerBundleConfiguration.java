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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.models.auth.SecuritySchemeDefinition;

import java.util.List;
import java.util.Objects;

/**
 * For the meaning of all these properties please refer to Swagger documentation or {@link io.swagger.jaxrs.config.BeanConfig}
 *
 * @author Tristan Burch
 * @author Federico Recio
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SwaggerBundleConfiguration {

    @JsonProperty
    private List<SingleSwaggerConfiguration> swaggers;

    public List<SingleSwaggerConfiguration> getSwaggers() {
        return swaggers;
    }

    public void setSwaggers(List<SingleSwaggerConfiguration> swaggers) {
        this.swaggers = swaggers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SwaggerBundleConfiguration that = (SwaggerBundleConfiguration) o;
        return Objects.equals(swaggers, that.swaggers);
    }

    @Override
    public int hashCode() {

        return Objects.hash(swaggers);
    }

    @Override
    public String toString() {
        return "SwaggerBundleConfiguration{" +
                "swaggers=" + swaggers +
                '}';
    }
}
