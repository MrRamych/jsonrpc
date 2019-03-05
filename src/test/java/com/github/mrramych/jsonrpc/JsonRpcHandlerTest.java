package com.github.mrramych.jsonrpc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonRpcHandlerTest {

    @Test
    public void test() {
        final AtomicBoolean wasInvoked = new AtomicBoolean(false);

        MethodHandler method = (argument, isNotification) -> {
            assertThat(argument.getAsString()).isEqualTo("argument");
            assertThat(isNotification).isFalse();
            assertThat(wasInvoked).withFailMessage("Method was called twice").isFalse();
            wasInvoked.set(true);
            return new Success(new JsonPrimitive(100));
        };

        JsonRpcHandler handler = new JsonRpcHandler();
        handler.register("method", method);
        JsonObject object = new Gson().fromJson(
                handler.handle("{\"jsonrpc\":\"2.0\",\"method\":\"method\",\"params\":\"argument\",\"id\":-1}"),
                JsonObject.class
        );
        assertThat(object.getAsJsonPrimitive("jsonrpc").getAsString()).isEqualTo("2.0");
        assertThat(object.getAsJsonPrimitive("id").getAsInt()).isEqualTo(-1);
        assertThat(object.getAsJsonPrimitive("result").getAsInt()).isEqualTo(100);
    }

    @Test
    public void testBatch() {
        final AtomicBoolean wasInvoked1 = new AtomicBoolean(false);
        final AtomicBoolean wasInvoked2 = new AtomicBoolean(false);

        MethodHandler method1 = (argument, isNotification) -> {
            assertThat(argument.getAsString()).isEqualTo("argument1");
            assertThat(isNotification).isFalse();
            assertThat(wasInvoked1).withFailMessage("Method 1 was called twice").isFalse();
            wasInvoked1.set(true);
            return new Success(new JsonPrimitive(10));
        };

        MethodHandler method2 = (argument, isNotification) -> {
            assertThat(argument.getAsString()).isEqualTo("argument2");
            assertThat(isNotification).isFalse();
            assertThat(wasInvoked2).withFailMessage("Method 2 was called twice").isFalse();
            wasInvoked2.set(true);
            return new Success(new JsonPrimitive(30));
        };

        JsonRpcHandler handler = new JsonRpcHandler();
        handler.register("method1", method1);
        handler.register("method2", method2);
        JsonArray object = new Gson().fromJson(
                handler.handle("[" +
                        "{\"jsonrpc\":\"2.0\",\"method\":\"method1\",\"params\":\"argument1\",\"id\":1}," +
                        "{\"jsonrpc\":\"2.0\",\"method\":\"method2\",\"params\":\"argument2\",\"id\":2}" +
                        "]"
                ),
                JsonArray.class
        );

        assertThat(object.getAsJsonArray().size()).isEqualTo(2);

        if (object.get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsInt() == 1) {
            assertThat(object.get(0).getAsJsonObject().getAsJsonPrimitive("jsonrpc").getAsString()).isEqualTo("2.0");
            assertThat(object.get(0).getAsJsonObject().getAsJsonPrimitive("result").getAsInt()).isEqualTo(10);
        } else {
            assertThat(object.get(1).getAsJsonObject().getAsJsonPrimitive("jsonrpc").getAsString()).isEqualTo("2.0");
            assertThat(object.get(1).getAsJsonObject().getAsJsonPrimitive("result").getAsInt()).isEqualTo(10);
        }

        if (object.get(0).getAsJsonObject().getAsJsonPrimitive("id").getAsInt() == 2) {
            assertThat(object.get(0).getAsJsonObject().getAsJsonPrimitive("jsonrpc").getAsString()).isEqualTo("2.0");
            assertThat(object.get(0).getAsJsonObject().getAsJsonPrimitive("result").getAsInt()).isEqualTo(30);
        } else {
            assertThat(object.get(1).getAsJsonObject().getAsJsonPrimitive("jsonrpc").getAsString()).isEqualTo("2.0");
            assertThat(object.get(1).getAsJsonObject().getAsJsonPrimitive("result").getAsInt()).isEqualTo(30);
        }
    }

}
