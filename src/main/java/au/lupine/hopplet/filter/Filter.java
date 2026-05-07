package au.lupine.hopplet.filter;

import au.lupine.hopplet.filter.compiler.FilterCompiler;
import au.lupine.hopplet.filter.compiler.Node;
import au.lupine.hopplet.filter.context.FilterContext;
import au.lupine.hopplet.filter.context.ItemStackContext;
import au.lupine.hopplet.filter.edit.EditTarget;
import au.lupine.hopplet.filter.edit.HopperEditTarget;
import au.lupine.hopplet.filter.edit.HopperMinecartEditTarget;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Filter {

    /// This style must be present for a filter to be valid.
    public static Style style = Style.style(NamedTextColor.GOLD);

    private final @NonNull Node root;
    private final @NonNull String raw;

    public Filter(@NonNull Node root, @NonNull String raw) {
        this.root = root;
        this.raw = raw;
    }

    public @NonNull String raw() {
        return raw;
    }

    public static @Nullable Filter of(@NonNull String raw) throws FilterCompileException {
        return FilterCompiler.compile(raw);
    }

    public static @Nullable Filter of(@Nullable Component component) throws FilterCompileException {
        return FilterCompiler.compile(component);
    }

    public static @Nullable Filter of(@NonNull Hopper hopper) throws FilterCompileException {
        return FilterCompiler.compile(hopper);
    }

    public static @Nullable Filter of(@NonNull HopperMinecart hopper) throws FilterCompileException {
        return FilterCompiler.compile(hopper);
    }

    /// @return `true` if the filter accepts the specified {@link FilterContext}.
    public boolean test(@NonNull FilterContext context) {
        return root.evaluate(context);
    }

    /// @return `true` if the filter accepts the specified {@link ItemStack}.
    public boolean test(@NonNull ItemStack stack) {
        return test(new ItemStackContext(stack));
    }

    public static void edit(@NonNull EditTarget target, @NonNull Filter filter) {
        target.edit(filter);
    }

    public static void edit(@NonNull Hopper hopper, @NonNull Filter filter) {
        edit(new HopperEditTarget(hopper), filter);
    }

    public static void edit(@NonNull HopperMinecart hopper, @NonNull Filter filter) {
        edit(new HopperMinecartEditTarget(hopper), filter);
    }
}
