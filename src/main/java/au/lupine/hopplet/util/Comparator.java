package au.lupine.hopplet.util;

import au.lupine.hopplet.filter.exception.FilterCompileException;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.jspecify.annotations.NonNull;

public final class Comparator {

    private final @NonNull Type type;
    private final double value;

    public Comparator(@NonNull Type type, double value) {
        this.type = type;
        this.value = value;
    }

    public @NonNull Type type() {
        return type;
    }

    public double value() {
        return value;
    }

    public static @NonNull Comparator of(@NonNull Type type, double value) {
        return new Comparator(type, value);
    }

    public static @NonNull Comparator of(double value) {
        return new Comparator(Type.EQUAL_TO, value);
    }

    /// Converts the specified text into a comparator as represented by the text.
    ///
    /// Examples:
    /// - `=3600 -> Comparator.of(Type.EQUAL_TO, 3600)`
    /// - `15 -> Comparator.of(Type.EQUAL_TO, 15)`
    /// - `>= -> FilterCompileException`
    /// - `>=a -> FilterCompileException`
    /// - `>=30 -> Comparator.of(Type.GREATER_THAN_OR_EQUAL_TO, 30)`
    /// @return A comparator from the specified text.
    /// @throws FilterCompileException If the specified text is not a valid representation of a comparator.
    public static @NonNull Comparator of(@NonNull String text) throws FilterCompileException {
        for (Type type : Type.values()) {
            String symbol = type.symbol;

            if (text.startsWith(symbol)) return new Comparator(type, parse(text.substring(symbol.length()), text));
        }

        return new Comparator(Type.EQUAL_TO, parse(text, text));
    }

    /// Converts the specified text into a `Pair<String, Comparator>` by "splitting" it at the comparator.
    ///
    /// Examples:
    /// - `fire_aspect>=1 -> Pair.of("fire_aspect", Comparator.of(Type.GREATER_THAN_OR_EQUAL_TO, 1))`
    /// - `=1 -> FilterCompileException`
    /// - `fire_aspect -> Pair.of("fire_aspect", def)`
    /// - `fire_aspect>= -> FilterCompileException`
    /// @return A pair of a string and a comparator from the specified text, or the default comparator if none is specified.
    /// @throws FilterCompileException If the specified text is not a valid representation of a comparator.
    public static @NonNull Pair<String, Comparator> split(@NonNull String text, @NonNull Comparator def) throws FilterCompileException {
        String normalised = text.toLowerCase().replaceAll("\\s", "");

        for (Type type : Type.values()) {
            String symbol = type.symbol;

            int index = normalised.indexOf(symbol);
            if (index < 0) continue;

            String string = normalised.substring(0, index);
            return Pair.of(
                string,
                Comparator.of(type, parse(normalised.substring(index + symbol.length()), text))
            );
        }

        return Pair.of(text, def);
    }

    private static double parse(@NonNull String text, @NonNull String input) throws FilterCompileException {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.default.compilation.exception.comparator.invalid_value",
                    Argument.string("input", input)
                )
            );
        }
    }

    public boolean test(double value) {
        return switch (type) {
            case NOT_EQUAL -> value != this.value;
            case LESS_THAN_OR_EQUAL_TO -> value <= this.value;
            case GREATER_THAN_OR_EQUAL_TO -> value >= this.value;
            case EQUAL_TO -> value == this.value;
            case LESS_THAN -> value < this.value;
            case GREATER_THAN -> value > this.value;
        };
    }

    public void wholeValueOrThrow(@NonNull String input) throws FilterCompileException {
        if (value != Math.floor(value)) throw new FilterCompileException(
            Component.translatable(
                "hopplet.filter.function.default.compilation.exception.comparator.value_not_whole",
                Argument.string("input", input)
            )
        );
    }

    public enum Type {

        NOT_EQUAL("!="),
        LESS_THAN_OR_EQUAL_TO("<="),
        GREATER_THAN_OR_EQUAL_TO(">="),
        EQUAL_TO("="),
        LESS_THAN("<"),
        GREATER_THAN(">");

        private final @NonNull String symbol;

        Type(@NonNull String symbol) {
            this.symbol = symbol;
        }
    }

    @Override
    public @NonNull String toString() {
        return type.symbol + value;
    }
}
