package com.github.mrramych.jsonrpc;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public interface MethodHandler {

    @NotNull
    Result handle(@Nullable JsonElement params, boolean isNotification) throws Exception;

}
