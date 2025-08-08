package fr.neocle.flexgateway.api.handler;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Interface for handling POST/PUT/DELETE actions through the API
 */
public interface ActionHandler {

    /**
     * Get the action endpoint name (will be accessible at /api/ext/{extension}/{endpoint})
     */
    String getEndpoint();

    /**
     * Handle the action
     * @param method HTTP method (POST, PUT, DELETE)
     * @param parameters Query parameters
     * @param body Request body as JsonObject (null if no body)
     * @return Response data
     */
    JsonObject handleAction(String method, Map<String, String> parameters, JsonObject body);

    /**
     * Get supported HTTP methods for this endpoint
     */
    String[] getSupportedMethods();

    /**
     * Get endpoint description
     */
    String getDescription();

    /**
     * Check if this action requires specific permissions
     */
    default String getRequiredPermission() {
        return null;
    }
}