package fr.neocle.flexgateway.api.provider;

import com.google.gson.JsonElement;

import java.util.Map;

/**
 * Interface for providing data through the API
 */
public interface DataProvider {

    /**
     * Get the data endpoint name (will be accessible at /api/ext/{extension}/{endpoint})
     */
    String getEndpoint();

    /**
     * Get data for this endpoint
     * @param parameters Query parameters from the request
     * @return JSON data to return
     */
    JsonElement getData(Map<String, String> parameters);

    /**
     * Get endpoint description for API documentation
     */
    String getDescription();

    /**
     * Get supported query parameters
     */
    default String[] getSupportedParameters() {
        return new String[0];
    }

    /**
     * Check if this endpoint requires specific permissions
     */
    default String getRequiredPermission() {
        return null;
    }
}