package moe.orangelabs.jsonrpc;

import moe.orangelabs.json.Json;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Error implements Result {

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    public final int code;
    @NotNull
    public final String message;
    @Nullable
    public final Json data;

    @Contract("_,null,_->fail")
    public Error(int code, @NotNull String message, @Nullable Json data) {
        this.code = code;
        this.message = checkNotNull(message);
        this.data = data;
    }

    @Contract("_,null->fail")
    public Error(int code, @NotNull String message) {
        this.code = code;
        this.message = checkNotNull(message);
        this.data = null;
    }

    @Override
    public boolean isError() {
        return true;
    }
}
