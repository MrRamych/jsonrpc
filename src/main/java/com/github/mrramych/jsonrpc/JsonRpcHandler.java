package com.github.mrramych.jsonrpc;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonRpcHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(JsonRpcHandler.class);

    private static final String RESPONSE_INVALID_SYNTAX;
    private static final String RESPONSE_INVALID_REQUEST;
    private static final String RESPONSE_INTERNAL_ERROR;
    private static final String ERROR_OBJECT_METHOD_NOT_FOUND;

    static {
        try {
            {
                StringWriter stringWriter = new StringWriter();
                new JsonWriter(stringWriter)
                        .beginObject()
                        .name("jsonrpc").value("2.0")
                        .name("id").nullValue()
                        .name("error")

                        .beginObject()
                        .name("code").value(Error.PARSE_ERROR)
                        .name("message").value("Parse error")
                        .endObject()

                        .endObject()
                        .close();
                RESPONSE_INVALID_SYNTAX = stringWriter.toString();
            }
            {
                StringWriter stringWriter = new StringWriter();
                new JsonWriter(stringWriter)
                        .beginObject()
                        .name("jsonrpc").value("2.0")
                        .name("id").nullValue()
                        .name("error")

                        .beginObject()
                        .name("code").value(Error.INVALID_REQUEST)
                        .name("message").value("Invalid Request")
                        .endObject()

                        .endObject()
                        .close();
                RESPONSE_INVALID_REQUEST = stringWriter.toString();
            }
            {
                StringWriter stringWriter = new StringWriter();
                new JsonWriter(stringWriter)
                        .beginObject()
                        .name("jsonrpc").value("2.0")
                        .name("id").nullValue()
                        .name("error")

                        .beginObject()
                        .name("code").value(Error.INTERNAL_ERROR)
                        .name("message").value("Internal error")
                        .endObject()

                        .endObject()
                        .close();
                RESPONSE_INTERNAL_ERROR = stringWriter.toString();
            }
            {
                StringWriter stringWriter = new StringWriter();
                new JsonWriter(stringWriter)
                        .beginObject()
                        .name("code").value(Error.METHOD_NOT_FOUND)
                        .name("message").value("Method not found")
                        .endObject()
                        .close();
                ERROR_OBJECT_METHOD_NOT_FOUND = stringWriter.toString();
            }
        } catch (IOException e) {
            throw new java.lang.Error(e);
        }
    }

    private final ConcurrentHashMap<String, MethodHandler> handlers = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

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
            JsonElement input = gson.fromJson(request, JsonElement.class);
            if (input.isJsonArray()) {
                String result = handleArray(input.getAsJsonArray());
                return result == null ? "" : result;
            } else if (input.isJsonObject()) {
                String result = invokeMethod((JsonObject) input);
                return result == null ? "" : result;
            } else {
                return RESPONSE_INVALID_REQUEST;
            }
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Exception caught {}", request, e);
            return RESPONSE_INVALID_SYNTAX;
        }
    }

    /**
     * Handle batch request.
     *
     * @param requests array of requests
     * @return response or null if all requests are notifications
     */
    @Nullable
    private String handleArray(JsonArray requests) {
        try {
            StringWriter stringWriter = new StringWriter();
            JsonWriter writer = new JsonWriter(stringWriter);
            writer.beginArray();

            boolean allNotifications = true;
            for (JsonElement jsonElement : requests) {
                if (jsonElement.isJsonObject()) {
                    String result = invokeMethod(jsonElement.getAsJsonObject());
                    if (result != null) {
                        allNotifications = false;
                        writer.jsonValue(result);
                    }
                } else {
                    writer.jsonValue(RESPONSE_INVALID_REQUEST);
                }
            }

            if (allNotifications) {
                return null;
            }

            writer.endArray();
            writer.close();
            return stringWriter.toString();
        } catch (Exception e) {
            throw new java.lang.Error(e);
        }
    }

    /**
     * Handle single request
     *
     * @param input request
     * @return result or null if request is notification
     */
    @Nullable
    private String invokeMethod(JsonObject input) {
        @Nullable JsonElement id = input.get("id");
        try {
            @Nullable String method = null;
            if (input.has("method") ||
                    input.get("method").isJsonPrimitive() ||
                    input.getAsJsonPrimitive("method").isString()) {
                method = input.getAsJsonPrimitive("method").getAsString();
            }

            @Nullable JsonElement params = null;
            if (input.has("params")) {
                params = input.get("params");
            }

            if (id == null) {
                if (method != null && handlers.containsKey(method)) {
                    handlers.get(method).handle(params, true);
                }
                return null;
            } else {
                StringWriter stringWriter = new StringWriter();
                JsonWriter writer = new JsonWriter(stringWriter);
                writer.beginObject();
                writer.name("jsonrpc").value("2.0");
                writer.name("id").jsonValue(id.toString());

                if (method == null) {
                    writer.name("error").jsonValue(ERROR_OBJECT_METHOD_NOT_FOUND);
                } else {
                    Result result = handlers.get(method).handle(params, false);

                    if (result.isError()) {
                        Error error = ((Error) result);

                        writer.name("error");
                        writer.beginObject();
                        writer.name("code").value(error.code);
                        writer.name("message").value(error.message);
                        if (error.data != null) {
                            writer.name("data").jsonValue(error.data.toString());
                        }
                        writer.endObject();
                    } else {
                        Success success = ((Success) result);
                        writer.name("result").jsonValue(success.result.toString());
                    }
                }

                writer.endObject();
                writer.close();
                return stringWriter.toString();
            }
        } catch (Exception e) {
            if (id != null) {
                try {
                    StringWriter stringWriter = new StringWriter();
                    JsonWriter writer = new JsonWriter(stringWriter);
                    writer.beginObject();
                    writer.name("jsonrpc").value("2.0");
                    writer.name("error")
                            .beginObject()
                            .name("code").value(-1)
                            .name("message").value("Server error")
                            .endObject();

                    writer.name("id");
                    writer.jsonValue(gson.toJson(id));
                    writer.endObject();
                    writer.close();
                    return stringWriter.toString();
                } catch (IOException ioe) {
                    throw new java.lang.Error(ioe);
                }
            } else {
                return null;
            }
        }
    }

}
