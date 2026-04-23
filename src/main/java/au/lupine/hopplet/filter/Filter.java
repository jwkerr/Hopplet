package au.lupine.hopplet.filter;

import au.lupine.hopplet.filter.exception.FilterCompileException;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class Filter {

    private final @NonNull Node root;

    private Filter(@NonNull Node root) {
        this.root = root;
    }

    /// @return `true` if the filter accepts the item in the specified {@link Context}.
    public boolean test(@NonNull Context context) {
        return root.evaluate(context);
    }

    public static final class Compiler {

        /// Compiles a raw string into a {@link Filter}.
        /// @return A compiled Filter, or `null` if the specified string is null, empty, or contains only whitespace.
        /// @throws FilterCompileException Thrown if the specified string is non-empty but has a compilation error.
        public static @Nullable Filter compile(@Nullable String raw) throws FilterCompileException {
            if (raw == null || raw.isBlank()) return null;

            List<Token> tokens = Tokeniser.tokenise(raw);
            Node root = new Parser(tokens).parse();

            return new Filter(root);
        }

        public static @Nullable Filter compile(@Nullable Component component) throws FilterCompileException {
            return compile(serialise(component));
        }

        public static @Nullable Filter compile(@NonNull Hopper hopper) throws FilterCompileException {
            return compile(hopper.customName());
        }

        public static @Nullable Filter compile(@NonNull HopperMinecart hopper) throws FilterCompileException {
            return compile(hopper.customName());
        }

        private static @Nullable String serialise(@Nullable Component component) {
            return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
        }

        private static final class Tokeniser {
            private static List<Token> tokenise(@NonNull String input) throws FilterCompileException {
                List<Token> tokens = new ArrayList<>();
                int i = 0;

                while (i < input.length()) {
                    char character = input.charAt(i);

                    if (Character.isWhitespace(character)) {
                        i++;
                        continue;
                    }

                    switch (character) {
                        case '(' -> { tokens.add(new Token(TokenType.LPAREN, "(", i)); i++; }
                        case ')' -> { tokens.add(new Token(TokenType.RPAREN, ")", i)); i++; }
                        case '&' -> { tokens.add(new Token(TokenType.AND, "&", i)); i++; }
                        case '|' -> { tokens.add(new Token(TokenType.OR, "|", i)); i++; }
                        case '!' -> { tokens.add(new Token(TokenType.NOT, "!", i)); i++; }
                        case ',' -> { tokens.add(new Token(TokenType.COMMA, ",", i)); i++; }
                        case '"' -> {
                            int start = i;
                            i++;

                            int stringStart = i;
                            while (i < input.length() && input.charAt(i) != '"') i++;

                            if (i >= input.length()) {
                                throw new FilterCompileException(
                                    Component.translatable(
                                        "hopplet.filter.compilation.exception.unterminated_string",
                                        Argument.numeric("position", start + 1)
                                    )
                                );
                            }

                            String string = input.substring(stringStart, i);
                            tokens.add(new Token(TokenType.STRING, string, start));
                            i++;
                        }
                        default -> {
                            int start = i;
                            while (i < input.length() && !TokenType.delimiter(input.charAt(i))) i++;

                            String ident = input.substring(start, i);
                            tokens.add(new Token(TokenType.IDENT, ident, start));
                        }
                    }
                }

                tokens.add(new Token(TokenType.EOF, "", input.length()));
                return tokens;
            }
        }

        private static final class Parser {

            private final @NonNull List<Token> tokens;
            private int position = 0;

            public Parser(@NonNull List<Token> tokens) {
                this.tokens = tokens;
            }

            private @NonNull Token peek() {
                return tokens.get(position);
            }

            private @NonNull Token advance() {
                return tokens.get(position++);
            }

            private @NonNull Token expect(@NonNull TokenType type) throws FilterCompileException {
                Token token = peek();

                if (token.type != type) throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.compilation.exception.expected_token",
                        Argument.string("expected", type.display()),
                        Argument.string("got", token.type.display()),
                        Argument.numeric("position", token.position + 1)
                    )
                );

                return advance();
            }

            public @NonNull Node parse() throws FilterCompileException {
                Node node = or();
                expect(TokenType.EOF);
                return node;
            }

            private @NonNull Node call() throws FilterCompileException {
                Token name = expect(TokenType.IDENT);
                expect(TokenType.LPAREN);

                List<String> arguments = new ArrayList<>();
                if (peek().type != TokenType.RPAREN) {
                    arguments.add(argument());

                    while (peek().type == TokenType.COMMA) {
                        advance();
                        arguments.add(argument());
                    }
                }

                expect(TokenType.RPAREN);

                String functionName = name.text;
                Function<?> function = Function.ofNamespacedOrName(functionName);
                if (function == null) throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.compilation.exception.unknown_function",
                        Argument.string("name", functionName),
                        Argument.numeric("position", name.position + 1)
                    )
                );

                return Node.Call.of(function, arguments);
            }

            private @NonNull String argument() throws FilterCompileException {
                Token token = peek();

                if (token.type == TokenType.IDENT || token.type == TokenType.STRING) {
                    advance();
                    return token.text;
                }

                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.compilation.exception.expected_argument",
                        Argument.string("got", token.type.display()),
                        Argument.numeric("position", token.position + 1)
                    )
                );
            }

            private @NonNull Node and() throws FilterCompileException {
                Node left = not();

                while(peek().type == TokenType.AND) {
                    advance();
                    Node right = not();
                    left = new Node.And(left, right);
                }

                return left;
            }

            private @NonNull Node or() throws FilterCompileException {
                Node left = and();

                while(peek().type == TokenType.OR) {
                    advance();
                    Node right = and();
                    left = new Node.Or(left, right);
                }

                return left;
            }

            private @NonNull Node not() throws FilterCompileException {
                if (peek().type == TokenType.NOT) {
                    advance();
                    return new Node.Not(not());
                }

                return primary();
            }

            private @NonNull Node primary() throws FilterCompileException {
                Token token = peek();

                if (token.type == TokenType.LPAREN) {
                    advance();

                    Node inner = or();
                    expect(TokenType.RPAREN);

                    return inner;
                }

                if (token.type == TokenType.IDENT) return call();

                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.compilation.exception.unexpected_token",
                        Argument.string("token", token.type.display()),
                        Argument.numeric("position", token.position + 1)
                    )
                );
            }
        }

        private enum TokenType {
            IDENT(null),
            STRING(null),
            LPAREN('('),
            RPAREN(')'),
            AND('&'),
            OR('|'),
            NOT('!'),
            COMMA(','),
            EOF(null);

            private static final Set<Character> DELIMITERS = Arrays.stream(values())
                .map(type -> type.character)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

            private final @Nullable Character character;

            TokenType(@Nullable Character character) {
                this.character = character;
            }

            public static boolean delimiter(char character) {
                return Character.isWhitespace(character) || DELIMITERS.contains(character);
            }

            public @NonNull String display() {
                return switch (this) {
                    case IDENT -> "identifier";
                    case STRING -> "string";
                    case LPAREN -> "(";
                    case RPAREN -> ")";
                    case AND -> "&";
                    case OR -> "|";
                    case NOT -> "!";
                    case COMMA -> ",";
                    case EOF -> "end of filter";
                };
            }
        }

        private record Token(@NonNull TokenType type, @NonNull String text, int position) {}
    }

    private sealed interface Node {

        boolean evaluate(@NonNull Context context);

        record Call<ArgumentType>(@NonNull Function<ArgumentType> function, @NonNull ArgumentType argument) implements Node {
            @Override public boolean evaluate(@NonNull Context context) {
                return function.test(context, argument);
            }

            static <ArgumentType> @NonNull Call<ArgumentType> of(@NonNull Function<ArgumentType> function, @NonNull List<String> arguments) throws FilterCompileException {
                ArgumentType compiled = function.compile(arguments);
                return new Call<>(function, compiled);
            }
        }

        record And(@NonNull Node left, @NonNull Node right) implements Node {
            @Override public boolean evaluate(@NonNull Context context) {
                return left.evaluate(context) && right.evaluate(context);
            }
        }

        record Or(@NonNull Node left, @NonNull Node right) implements Node {
            @Override public boolean evaluate(@NonNull Context context) {
                return left.evaluate(context) || right.evaluate(context);
            }
        }

        record Not(@NonNull Node inner) implements Node {
            @Override public boolean evaluate(@NonNull Context context) {
                return !inner.evaluate(context);
            }
        }

        record Constant(boolean value) implements Node {
            @Override public boolean evaluate(@NonNull Context context) {
                return value;
            }
        }
    }

    public static final class Cache {

        // Using multiple nested maps allows invalidating a full world/chunk at once, doubt there's any noticeable difference in lookup speed
        // an important assumption is that only the region thread responsible for the chunk will access the final int to object map
        public static final Map<UUID, Map<Long, AbstractInt2ObjectMap<Filter>>> BLOCK_CACHE = new ConcurrentHashMap<>();
        public static final Map<UUID, Filter> ENTITY_CACHE = new ConcurrentHashMap<>();

        private static final java.util.function.Function<UUID, Map<Long, AbstractInt2ObjectMap<Filter>>> NEW_WORLD_CHUNK_MAP = ignored -> new ConcurrentHashMap<>();
        private static final java.util.function.Function<Long, AbstractInt2ObjectMap<Filter>> NEW_CHUNK_MAP = ignored -> new Int2ObjectOpenHashMap<>();

        // Generic methods

        public static void cache(final UUID worldUUID, final int x, final int y, final int z, final Filter filter) {
            BLOCK_CACHE
                    .computeIfAbsent(worldUUID, NEW_WORLD_CHUNK_MAP)
                    .computeIfAbsent(Chunk.getChunkKey(x >> 4, z >> 4), NEW_CHUNK_MAP)
                    .put(packChunkRelativeCoords(x, y, z), filter);
        }

        private static @Nullable AbstractInt2ObjectMap<Filter> getChunkFilterMap(final UUID worldUUID, final int x, final int z) {
            final Map<Long, AbstractInt2ObjectMap<Filter>> chunkMap = BLOCK_CACHE.get(worldUUID);
            if (chunkMap == null) return null;

            return chunkMap.get(Chunk.getChunkKey(x >> 4, z >> 4));
        }

        public static @Nullable Filter get(final UUID worldUUID, final int x, final int y, final int z) {
            final AbstractInt2ObjectMap<Filter> filterMap = getChunkFilterMap(worldUUID, x, z);
            if (filterMap == null) return null;

            return filterMap.get(packChunkRelativeCoords(x, y, z));
        }

        public static void invalidate(final UUID worldUUID, final int x, final int y, final int z){
            final AbstractInt2ObjectMap<Filter> filterMap = getChunkFilterMap(worldUUID, x, z);
            if (filterMap != null) filterMap.remove(packChunkRelativeCoords(x, y, z));
        }

        // Location methods

        public static void cache(@NonNull Location location, @NonNull Filter filter) {
            cache(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), filter);
        }

        public static @Nullable Filter get(@NonNull Location location) {
            return get(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        public static void invalidate(@NonNull Location location) {
            invalidate(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }

        // Block methods

        public static void cache(@NonNull Block block, @NonNull Filter filter) {
            cache(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), filter);
        }

        public static @Nullable Filter get(@NonNull Block block) {
            return get(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        public static void invalidate(@NonNull Block block) {
            invalidate(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        public static void cache(@NonNull Hopper hopper, @NonNull Filter filter) {
            cache(hopper.getWorld().getUID(), hopper.getX(), hopper.getY(), hopper.getZ(), filter);
        }

        public static @Nullable Filter get(@NonNull Hopper hopper) {
            return get(hopper.getWorld().getUID(), hopper.getX(), hopper.getY(), hopper.getZ());
        }

        public static @Nullable Filter getOrCompile(@NonNull Hopper hopper) throws FilterCompileException {
            final AbstractInt2ObjectMap<Filter> map = BLOCK_CACHE
                    .computeIfAbsent(hopper.getWorld().getUID(), NEW_WORLD_CHUNK_MAP)
                    .computeIfAbsent(Chunk.getChunkKey(hopper.getX() >> 4, hopper.getZ() >> 4), NEW_CHUNK_MAP);

            final int packed = packChunkRelativeCoords(hopper.getX(), hopper.getY(), hopper.getZ());

            Filter filter = map.get(packed);
            if (filter != null) return filter;

            Filter compiled = Filter.Compiler.compile(hopper);
            if (compiled != null) map.put(packed, compiled);

            return compiled;
        }

        public static void invalidate(@NonNull Hopper hopper) {
            invalidate(hopper.getWorld().getUID(), hopper.getX(), hopper.getY(), hopper.getZ());
        }

        // Hopper minecart methods

        public static void cache(@NonNull UUID uuid, @NonNull Filter filter) {
            ENTITY_CACHE.put(uuid, filter);
        }

        public static @Nullable Filter get(@NonNull UUID uuid) {
            return ENTITY_CACHE.get(uuid);
        }

        public static void invalidate(@NonNull UUID uuid) {
            ENTITY_CACHE.remove(uuid);
        }

        public static void cache(@NonNull HopperMinecart hopper, @NonNull Filter filter) {
            cache(hopper.getUniqueId(), filter);
        }

        public static @Nullable Filter get(@NonNull HopperMinecart hopper) {
            return get(hopper.getUniqueId());
        }

        public static @Nullable Filter getOrCompile(@NonNull HopperMinecart hopper) throws FilterCompileException {
            Filter filter = get(hopper);
            if (filter != null) return filter;

            Filter compiled = Filter.Compiler.compile(hopper);
            if (compiled != null) cache(hopper, compiled);

            return compiled;
        }

        public static void invalidate(@NonNull HopperMinecart hopper) {
            invalidate(hopper.getUniqueId());
        }

        /**
         * Packs chunk relative x and z coords and the y coordinate into a single integer.
         *
         * @param x The world or chunk relative x coordinate.
         * @param y The world relative y coordinate, the maximum supported range is from 2^23 - 1 to -2^23
         * @param z The world or chunk relative z coordinate.
         * @return A packed integer that uniquely identifies these coords in a chunk.
         */
        public static int packChunkRelativeCoords(final int x, @Range(from = -8_388_608, to = 8_388_607) final int y, final int z) {
            return x & 0xF // mask x and z with 0xF (15) to ensure they are within range
                    | (z & 0xF) << 4 // z is put into the next 4 bits
                    | (y & 0xFFFFFF) << 8; // and y is put into the remaining 24 bits after x and z
        }
    }

    public static final class Context {

        private final @NonNull ItemStack stack;
        private final @Nullable Item item;
        private final @Nullable Inventory source;
        private final @NonNull Inventory destination;

        private Context(@NonNull ItemStack stack, @Nullable Item item, @Nullable Inventory source, @NonNull Inventory destination) {
            if (item != null && !item.getItemStack().equals(stack)) throw new IllegalStateException("Specified ItemStack does not match the ItemStack of Item.");

            this.stack = stack;
            this.item = item;
            this.source = source;
            this.destination = destination;
        }

        public static @NonNull Builder builder() {
            return new Builder();
        }

        public @NonNull ItemStack stack() {
            return stack;
        }

        public @Nullable Item item() {
            return item;
        }

        public @Nullable Inventory source() {
            return source;
        }

        public @NonNull Inventory destination() {
            return destination;
        }

        public static final class Builder {

            private ItemStack stack;
            private @Nullable Item item;
            private @Nullable Inventory source;
            private Inventory destination;

            private Builder() {}

            public @NonNull Context build() {
                return new Context(stack, item, source, destination);
            }

            public @NonNull Builder stack(@NonNull ItemStack stack) {
                this.stack = stack;
                return this;
            }

            public @NonNull Builder item(@Nullable Item item) {
                this.item = item;
                if (item != null) this.stack = item.getItemStack();
                return this;
            }

            public @NonNull Builder source(@Nullable Inventory source) {
                this.source = source;
                return this;
            }

            public @NonNull Builder destination(@NonNull Inventory destination) {
                this.destination = destination;
                return this;
            }
        }
    }
}
