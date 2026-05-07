package au.lupine.hopplet.filter.context;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public interface FilterContext {

    @NonNull ItemStack stack();
}
