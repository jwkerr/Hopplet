package au.lupine.hopplet.filter.compiler;

import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.context.FilterContext;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import org.jspecify.annotations.NonNull;

import java.util.List;

public sealed interface Node {

    boolean evaluate(@NonNull FilterContext context);

    record Call<ArgumentType>(@NonNull Function<ArgumentType> function, @NonNull ArgumentType argument) implements Node {
        @Override public boolean evaluate(@NonNull FilterContext context) {
            return function.test(context, argument);
        }

        static <ArgumentType> @NonNull Call<ArgumentType> of(@NonNull Function<ArgumentType> function, @NonNull List<String> arguments) throws FilterCompileException {
            ArgumentType compiled = function.compile(arguments);
            return new Call<>(function, compiled);
        }
    }

    record And(@NonNull Node left, @NonNull Node right) implements Node {
        @Override public boolean evaluate(@NonNull FilterContext context) {
            return left.evaluate(context) && right.evaluate(context);
        }
    }

    record Or(@NonNull Node left, @NonNull Node right) implements Node {
        @Override public boolean evaluate(@NonNull FilterContext context) {
            return left.evaluate(context) || right.evaluate(context);
        }
    }

    record Not(@NonNull Node inner) implements Node {
        @Override public boolean evaluate(@NonNull FilterContext context) {
            return !inner.evaluate(context);
        }
    }

    record Constant(boolean value) implements Node {
        @Override public boolean evaluate(@NonNull FilterContext context) {
            return value;
        }
    }
}
