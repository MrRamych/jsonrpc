package com.github.mrramych.jsonrpc;

import com.github.mrramych.json.Json;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface MethodHandler {

    @NotNull
    Result handle(@Nullable Json params, boolean isNotification) throws Exception;

}
