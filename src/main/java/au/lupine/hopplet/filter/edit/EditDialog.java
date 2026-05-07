package au.lupine.hopplet.filter.edit;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.compiler.FilterCompiler;
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
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("UnstableApiUsage")
public final class EditDialog {

    private EditDialog() {}

    public static void open(@NonNull Player player, @NonNull EditTarget target) {
        open(player, target, target.name(), null);
    }

    private static void open(@NonNull Player player, @NonNull EditTarget target, @NonNull String text, @Nullable Component message) {
        ItemStack item = new ItemStack(target.icon());
        if (target.component() != null) {
            ItemMeta meta = item.getItemMeta();
            meta.customName(target.component());
            item.setItemMeta(meta);
        }

        List<DialogBody> bodies = new ArrayList<>();
        bodies.add(DialogBody.item(item).build());
        bodies.add(DialogBody.plainMessage(translate(player, "hopplet.dialog.edit_filter.body.need_help")));
        bodies.add(DialogBody.plainMessage(translate(player, "hopplet.dialog.edit_filter.body.documentation")));
        bodies.add(DialogBody.plainMessage(translate(player, "hopplet.dialog.edit_filter.body.discord")));

        if (message != null) bodies.add(DialogBody.plainMessage(translate(player, message)));

        Dialog dialog = Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(translate(player, "hopplet.dialog.edit_filter.title"))
                .body(bodies)
                .inputs(List.of(
                    DialogInput.text("filter_input", translate(player, "hopplet.dialog.edit_filter.input.filter_input"))
                        .initial(text)
                        .maxLength(Hopplet.instance().config().root().node("filter", "edit", "dialog", "max_input_length").getInt())
                        .width(300)
                        .multiline(TextDialogInput.MultilineOptions.create(null, 100))
                        .build()
                ))
                .build()
            )
            .type(DialogType.multiAction(List.of(
                validate(player, target),
                confirm(player, target),
                cancel(player, target)
            )).columns(3).build())
        );

        player.showDialog(dialog);
    }

    private static @NonNull ActionButton validate(@NonNull Player player, @NonNull EditTarget target) {
        return ActionButton.builder(translate(player, "hopplet.dialog.edit_filter.action.validate"))
            .action(DialogAction.customClick((view, audience) -> {
                String input = view.getText("filter_input");
                if (input == null) return;

                Component result;
                try {
                    FilterCompiler.compile(input);
                    result = translate(player, "hopplet.dialog.edit_filter.action.validate.success");
                } catch (FilterCompileException e) {
                    result = e.asComponent();
                }

                open(player, target, input, result);
            }, ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .build()
            ))
            .build();
    }

    private static @NonNull ActionButton confirm(@NonNull Player player, @NonNull EditTarget target) {
        return ActionButton.builder(translate(player, "hopplet.dialog.edit_filter.action.confirm"))
            .action(DialogAction.customClick((view, audience) -> {
                String input = view.getText("filter_input");
                if (input == null) return;

                target.edit(input, player);
                playSound(target.location(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 0.75F, 1.25F, 1.5F);
            }, ClickCallback.Options.builder()
                .uses(1)
                .build()
            ))
            .build();
    }

    private static @NonNull ActionButton cancel(@NonNull Player player, @NonNull EditTarget target) {
        return ActionButton.builder(translate(player, "hopplet.dialog.edit_filter.action.cancel"))
            .action(DialogAction.customClick((view, audience) -> {
                playSound(target.location(), Sound.BLOCK_ANVIL_LAND, 0.3F, 1.25F, 1.5F);
            }, ClickCallback.Options.builder()
                .uses(1)
                .build()
            ))
            .build();
    }

    private static void playSound(@NonNull Location location, @NonNull Sound sound, float volume, float origin, float bound) {
        Hopplet instance = Hopplet.instance();
        instance.getServer().getRegionScheduler().run(instance, location, task -> {
            Random random = new Random();
            location.getWorld().playSound(location, sound, volume, random.nextFloat(origin, bound));
        });
    }

    // https://github.com/PaperMC/Paper/issues/12971
    private static @NonNull Component translate(@NonNull Player player, @NonNull String key) {
        return translate(player, Component.translatable(key));
    }

    private static @NonNull Component translate(@NonNull Player player, @NonNull Component component) {
        return GlobalTranslator.render(component, player.locale());
    }
}
