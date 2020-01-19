package moe.orangelabs.jsonrpc;

import moe.orangelabs.json.Json;
import moe.orangelabs.json.types.JsonArray;
import moe.orangelabs.json.types.JsonObject;
import org.assertj.core.api.Condition;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static moe.orangelabs.json.Json.*;
import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonRpcHandlerTest {

    @Test
    public void test() {
        final AtomicBoolean wasInvoked = new AtomicBoolean(false);

        MethodHandler method = (argument, isNotification) -> {
            assertThat(argument.getAsString().string).isEqualTo("argument");
            assertThat(isNotification).isFalse();
            assertThat(wasInvoked).withFailMessage("Method was called twice").isFalse();
            wasInvoked.set(true);
            return new Success(number(100));
        };

        JsonRpcHandler handler = new JsonRpcHandler();
        handler.register("method", method);
        JsonObject object = Json.parse(
                handler.handle("{\"jsonrpc\":\"2.0\",\"method\":\"method\",\"params\":\"argument\",\"id\":-1}")
        ).getAsObject();
        assertThat(object).isEqualTo(object(
                "jsonrpc", "2.0",
                "id", -1,
                "result", 100
        ));
    }

    @Test
    public void testBatch() {
        final AtomicBoolean wasInvoked1 = new AtomicBoolean(false);
        final AtomicBoolean wasInvoked2 = new AtomicBoolean(false);

        MethodHandler method1 = (argument, isNotification) -> {
            assertThat(argument.getAsString().string).isEqualTo("argument1");
            assertThat(isNotification).isFalse();
            assertThat(wasInvoked1).withFailMessage("Method 1 was called twice").isFalse();
            wasInvoked1.set(true);
            return new Success(number(10));
        };

        MethodHandler method2 = (argument, isNotification) -> {
            assertThat(argument.getAsString().string).isEqualTo("argument2");
            assertThat(isNotification).isFalse();
            assertThat(wasInvoked2).withFailMessage("Method 2 was called twice").isFalse();
            wasInvoked2.set(true);
            return new Success(number(30));
        };

        JsonRpcHandler handler = new JsonRpcHandler();
        handler.register("method1", method1);
        handler.register("method2", method2);
        JsonArray array = Json.parse(
                handler.handle("[" +
                        "{\"jsonrpc\":\"2.0\",\"method\":\"method1\",\"params\":\"argument1\",\"id\":1}," +
                        "{\"jsonrpc\":\"2.0\",\"method\":\"method2\",\"params\":\"argument2\",\"id\":2}" +
                        "]"
                )
        ).getAsArray();

        assertThat(array.size()).isEqualTo(2);

        JsonObject object1 = object(
                "jsonrpc", "2.0",
                "result", 10,
                "id", 1
        );
        JsonObject object2 = object(
                "jsonrpc", "2.0",
                "result", 30,
                "id", 2
        );

        assertThat(array).is(anyOf(
                new Condition<Object>(t -> t.equals(array(object1, object2)), array(object1, object2).toString()),
                new Condition<Object>(t -> t.equals(array(object2, object1)), array(object2, object1).toString())
        ));
    }

}
