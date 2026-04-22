package au.lupine.hopplet.listener;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public final class FilterEditListener implements Listener {

    @EventHandler
    public void on(@NonNull PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        if (!player.getInventory().getItemInMainHand().isEmpty()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        BlockState state = block.getState();
        if (!(state instanceof Hopper)) return;

        BlockBreakEvent bbe = new BlockBreakEvent(block, player);
        if (!bbe.callEvent()) return; // Player does not have permission to edit this hopper

        event.setCancelled(true);

        Component existing = ((Hopper) state).customName();
        String name = existing == null ? "" : PlainTextComponentSerializer.plainText().serialize(existing);

        Location location = block.getLocation();

        Consumer<String> confirm = input -> {
            Hopplet instance = Hopplet.instance();

            instance.getServer().getRegionScheduler().run(instance, location, task -> {
                Block target = location.getBlock();
                if (!(target.getState(false) instanceof Hopper hopper)) return;

                String cleaned = Filter.Compiler.clean(input);

                Filter.Cache.invalidate(hopper);
                try {
                    Filter filter = Filter.Compiler.compile(cleaned);
                    if (filter != null) Filter.Cache.cache(hopper, filter);
                } catch (FilterCompileException e) {
                    player.sendMessage(e);
                }

                hopper.customName(cleaned.isEmpty() ? null : Component.text(cleaned));

                hopper.setTransferCooldown(20);
                hopper.update();

                playSound(location, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.75F, 1.25F, 1.5F);
            });
        };

        Runnable cancel = () -> playSound(location, Sound.BLOCK_ANVIL_LAND, 0.3F, 1.25F, 1.5F);

        player.showDialog(dialog(player, name, Material.HOPPER, confirm, cancel));
    }

    @EventHandler
    public void on(@NonNull PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof HopperMinecart hopper)) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        if (!player.getInventory().getItemInMainHand().isEmpty()) return;

        BlockBreakEvent bbe = new BlockBreakEvent(hopper.getLocation().getBlock(), player);
        if (!bbe.callEvent()) return;

        event.setCancelled(true);

        Component existing = hopper.customName();
        String name = existing == null ? "" : PlainTextComponentSerializer.plainText().serialize(existing);

        Consumer<String> confirm = input -> {
            Location location = hopper.getLocation();

            Hopplet instance = Hopplet.instance();

            instance.getServer().getRegionScheduler().run(instance, location, task -> {
                if (!hopper.isValid()) return;

                String cleaned = Filter.Compiler.clean(input);

                Filter.Cache.invalidate(hopper);
                try {
                    Filter filter = Filter.Compiler.compile(cleaned);
                    if (filter != null) Filter.Cache.cache(hopper, filter);
                } catch (FilterCompileException e) {
                    player.sendMessage(e);
                }

                hopper.customName(cleaned.isEmpty() ? null : Component.text(cleaned));
                playSound(location, Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.75F, 1.25F, 1.5F);
            });
        };

        Runnable cancel = () -> {
            Location location = hopper.getLocation();
            playSound(location, Sound.BLOCK_ANVIL_LAND, 0.3F, 1.25F, 1.5F);
        };

        player.showDialog(dialog(player, name, Material.HOPPER_MINECART, confirm, cancel));
    }

    private @NonNull Dialog dialog(@NonNull Player player, @NonNull String text, @NonNull Material icon, @NonNull Consumer<String> confirm, @NonNull Runnable cancel) {
        return dialog(player, text, icon, null, confirm, cancel);
    }

    @SuppressWarnings("UnstableApiUsage")
    private @NonNull Dialog dialog(@NonNull Player player, @NonNull String text, @NonNull Material icon, @Nullable Component message, @NonNull Consumer<String> confirm, @NonNull Runnable cancel) {
        ItemStack item = new ItemStack(icon);
        if (!text.isBlank()) {
            ItemMeta meta = item.getItemMeta();
            meta.customName(Component.text(text));
            item.setItemMeta(meta);
        }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(item).build());
        bodies.add(DialogBody.plainMessage(translate(player, "hopplet.dialog.edit_filter.body.need_help")));
        bodies.add(DialogBody.plainMessage(translate(player, "hopplet.dialog.edit_filter.body.documentation")));
        bodies.add(DialogBody.plainMessage(translate(player, "hopplet.dialog.edit_filter.body.discord")));

        if (message != null) bodies.add(DialogBody.plainMessage(message));

        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(translate(player, "hopplet.dialog.edit_filter.title"))
                .body(bodies)
                .inputs(List.of(
                    DialogInput.text("filter_input", translate(player, "hopplet.dialog.edit_filter.input.filter_input"))
                        .initial(text)
                        .maxLength(512)
                        .width(300)
                        .multiline(TextDialogInput.MultilineOptions.create(null, 100))
                        .build()
                ))
                .build()
            )
            .type(DialogType.multiAction(
                    List.of(
                        ActionButton.builder(translate(player, "hopplet.dialog.edit_filter.action.validate"))
                            .action(DialogAction.customClick((view, audience) -> {
                                    String input = view.getText("filter_input");
                                    if (input == null) return;

                                    Component result;
                                    try {
                                        Filter.Compiler.compile(input);
                                        result = translate(player, "hopplet.dialog.edit_filter.action.validate.success");
                                    } catch (FilterCompileException e) {
                                        result = e.asComponent();
                                    }

                                    player.showDialog(dialog(player, input, icon, result, confirm, cancel));
                                }, ClickCallback.Options.builder()
                                    .uses(ClickCallback.UNLIMITED_USES)
                                    .build()
                            ))
                            .build(),
                        ActionButton.builder(translate(player, "hopplet.dialog.edit_filter.action.confirm"))
                            .action(DialogAction.customClick((view, audience) -> {
                                    String input = view.getText("filter_input");
                                    if (input == null) return;

                                    confirm.accept(input);
                                }, ClickCallback.Options.builder()
                                    .uses(1)
                                    .build()
                            ))
                            .build(),
                        ActionButton.builder(translate(player, "hopplet.dialog.edit_filter.action.cancel"))
                            .action(DialogAction.customClick((view, audience) -> cancel.run(), ClickCallback.Options.builder()
                                .uses(1)
                                .build()
                            ))
                            .build()
                    ))
                .columns(3)
                .build()
            )
        );
    }

    // https://github.com/PaperMC/Paper/issues/12971
    private @NonNull Component translate(@NonNull Player player, @NonNull String key) {
        return GlobalTranslator.render(Component.translatable(key), player.locale());
    }

    private void playSound(@NonNull Location location, @NonNull Sound sound, float volume, float origin, float bound) {
        Hopplet instance = Hopplet.instance();
        instance.getServer().getRegionScheduler().run(instance, location, task -> {
            Random random = new Random();
            location.getWorld().playSound(location, sound, volume, random.nextFloat(origin, bound));
        });
    }
}
