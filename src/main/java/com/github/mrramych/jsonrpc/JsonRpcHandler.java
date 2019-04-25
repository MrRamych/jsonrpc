package com.github.mrramych.jsonrpc;

import com.github.mrramych.json.Json;
import com.github.mrramych.json.ParseException;
import com.github.mrramych.json.types.JsonArray;
import com.github.mrramych.json.types.JsonObject;
import com.github.mrramych.json.types.JsonString;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.mrramych.json.Json.object;
import static com.google.common.base.Preconditions.checkNotNull;

public class JsonRpcHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcHandler.class);

    private static final Json RESPONSE_PARSE_ERROR = object(
            "jsonrpc", "2.0",
            "id", null,
            "error", object(
                    "code", Error.PARSE_ERROR,
                    "message", "Parse error")
    );

    private static final Json RESPONSE_INVALID_REQUEST = object(
            "jsonrpc", "2.0",
            "id", null,
            "error", object(
                    "code", Error.INVALID_REQUEST,
                    "message", "Invalid request")
    );

    private static final Json RESPONSE_METHOD_NOT_FOUND = object(
            "jsonrpc", "2.0",
            "id", null,
            "error", object(
                    "code", Error.METHOD_NOT_FOUND,
                    "message", "Method not found")
    );

    private static final Json RESPONSE_INTERNAL_ERROR = object(
            "jsonrpc", "2.0",
            "id", null,
            "error", object(
                    "code", Error.INTERNAL_ERROR,
                    "message", "Internal error")
    );


    private final ConcurrentHashMap<String, MethodHandler> handlers = new ConcurrentHashMap<>();

    public @Nullable
    MethodHandler register(String method, MethodHandler handler) {
        return handlers.put(checkNotNull(method), checkNotNull(handler));
    }

    public @Nullable
    MethodHandler unregister(String method) {
        return handlers.remove(checkNotNull(method));
    }


    public String handle(String request) {
        try {
            Json json = Json.decode(request);
            if (json.isArray()) {
                Json result = handleArray(((JsonArray) json));
                return result == null ? "" : result.toString();
            } else if (json.isObject()) {
                Json result = invokeMethod((JsonObject) json);
                return result == null ? "" : result.toString();
            } else
                return RESPONSE_INTERNAL_ERROR.toString();
        } catch (ParseException e) {
            return RESPONSE_PARSE_ERROR.toString();
        }
    }

    /**
     * Handle batch request.
     *
     * @param requests array of requests
     * @return response or null if all requests are notifications
     */
    @Nullable
    private Json handleArray(JsonArray requests) {
        JsonArray array = new JsonArray();

        requests.forEach(json -> {
            Json result = json.isObject() ? invokeMethod(json.getAsObject()) : RESPONSE_INVALID_REQUEST;
            if (result != null) {
                array.add(result);
            }
        });

        if (array.isEmpty()) {
            return null;
        }

        return array;

    }

    /**
     * Handle single request
     *
     * @param input request
     * @return result or null if request is notification
     */
    @Nullable
    private Json invokeMethod(JsonObject input) {
        @Nullable Json id = input.getOrDefault("id", null);
        try {
            @NotNull String method;
            if (input.containsString("method")) {
                method = input.getString("method").string;
            } else {
                return RESPONSE_INVALID_REQUEST;
            }
            if (!handlers.containsKey(method)) {
                return RESPONSE_METHOD_NOT_FOUND;
            }

            @Nullable Json params = null;
            if (input.containsKey("params")) {
                params = input.get("params");
            }

            if (id == null) {
                handlers.get(method).handle(params, true);
                return null;
            } else {
                JsonObject response = object(
                        "jsonrpc", "2.0",
                        "id", id
                );

                Result result = handlers.get(method).handle(params, false);

                if (result.isError()) {
                    Error error = ((Error) result);

                    JsonObject errorObject = object(
                            "code", ((Error) result).code,
                            "message", error.message
                    );
                    if (error.data != null) {
                        errorObject.put(new JsonString("error"), error.data);
                    }

                    response.put(new JsonString("error"), errorObject);
                } else {
                    response.put(new JsonString("result"), ((Success) result).result);
                }

                return response;
            }
        } catch (Exception e) {
            if (id != null) {
                return RESPONSE_INTERNAL_ERROR;
            } else {
                return null;
            }
        }
    }

}
