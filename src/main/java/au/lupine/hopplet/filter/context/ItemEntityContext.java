package au.lupine.hopplet.filter.context;

import org.bukkit.entity.Item;
import org.jspecify.annotations.NonNull;

public interface ItemEntityContext extends FilterContext {

    @NonNull Item item();
}
