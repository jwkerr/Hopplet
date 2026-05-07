package au.lupine.hopplet.filter.context;

import org.bukkit.inventory.Inventory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface InventoryTransferContext extends FilterContext {

    @Nullable Inventory source();

    @NonNull Inventory destination();
}
