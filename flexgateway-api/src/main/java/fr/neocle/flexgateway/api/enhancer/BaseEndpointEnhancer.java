package fr.neocle.flexgateway.api.enhancer;

import com.google.gson.JsonObject;

/**
 * Interface for extensions to enhance base plugin endpoints
 * Extensions can add multiple keys to the same base endpoint
 */
public interface BaseEndpointEnhancer {

    /**
     * Get the base endpoint this enhancer applies to
     * Examples: "server", "players", "player", "worlds", "world", "messages"
     */
    String getBaseEndpoint();

    /**
     * Enhance the base endpoint response with additional data
     * You can add multiple properties directly to the response object
     * @param response The response object to enhance (modify this directly)
     * @param originalData The original response data (read-only reference)
     * @param identifier Optional identifier (e.g., player name for /api/player/{name})
     * @param parameters Query parameters from the request
     */
    void enhanceResponse(JsonObject response, JsonObject originalData, String identifier, java.util.Map<String, String> parameters);

    /**
     * Get enhancement description for documentation
     */
    String getDescription();

    /**
     * Priority for this enhancement (higher numbers = processed later)
     * Useful if multiple extensions enhance the same endpoint
     */
    default int getPriority() {
        return 100;
    }
}