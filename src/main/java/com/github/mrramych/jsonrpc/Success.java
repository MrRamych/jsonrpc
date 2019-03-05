package com.github.mrramych.jsonrpc;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Success implements Result {

    @NotNull
    public final JsonElement result;

    @Contract("null->fail")
    public Success(@NotNull JsonElement result) {
        this.result = checkNotNull(result);
    }

    @Override
    public boolean isError() {
        return false;
    }
}
