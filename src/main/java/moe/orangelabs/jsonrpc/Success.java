package moe.orangelabs.jsonrpc;

import moe.orangelabs.json.Json;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Success implements Result {

    @NotNull
    public final Json result;

    @Contract("null->fail")
    public Success(@NotNull Json result) {
        this.result = checkNotNull(result);
    }

    @Override
    public boolean isError() {
        return false;
    }
}
