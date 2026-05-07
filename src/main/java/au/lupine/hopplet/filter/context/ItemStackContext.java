package au.lupine.hopplet.filter.context;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public final class ItemStackContext implements FilterContext {

    private final @NonNull ItemStack stack;

    public ItemStackContext(@NonNull ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public @NonNull ItemStack stack() {
        return stack;
    }
}
