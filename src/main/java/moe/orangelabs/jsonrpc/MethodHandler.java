package moe.orangelabs.jsonrpc;

import moe.orangelabs.json.Json;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface MethodHandler {

    @NotNull
    Result handle(@Nullable Json params, boolean isNotification) throws Exception;

}
