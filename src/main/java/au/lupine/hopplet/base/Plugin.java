package au.lupine.hopplet.base;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public abstract class Plugin extends JavaPlugin {

    private final @NonNull List<Listener> listeners = new ArrayList<>();
    private final @NonNull List<ScheduledTask> tasks = new ArrayList<>();
    private Config config;

    @Override
    public final void onLoad() {
        config = new Config(this);
        translations();
        load();
    }

    public void load() {}

    @Override
    public final void onEnable() {
        initialise();
    }

    public final void initialise() {
        config.reload();

        enable();

        PluginManager pm = this.getServer().getPluginManager();
        listeners.forEach(listener -> pm.registerEvents(listener, this));
    }

    public void enable() {}

    @Override
    public final void onDisable() {
        terminate();
    }

    public final void terminate() {
        disable();

        listeners.forEach(HandlerList::unregisterAll);
        listeners.clear();

        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    public void disable() {}

    public void reload() {
        terminate();
        initialise();
    }

    public @NonNull Config config() {
        return config;
    }

    public @NonNull Map<String, Object> nodes() {
        return Map.of();
    }

    public final void listeners(@NonNull Listener... listeners) {
        this.listeners.addAll(List.of(listeners));
    }

    public final void tasks(@NonNull ScheduledTask... tasks) {
        this.tasks.addAll(List.of(tasks));
    }

    private void translations() {
        MiniMessageTranslationStore store = MiniMessageTranslationStore.create(Key.key(namespace(), "translations"));

        for (Locale locale : readLocalesFromJar()) {
            try {
                ResourceBundle bundle = ResourceBundle
                    .getBundle(
                        "lang.Bundle",
                        locale,
                        getClassLoader(),
                        UTF8ResourceBundleControl.utf8ResourceBundleControl()
                    );

                store.registerAll(locale, bundle, false);
            } catch (MissingResourceException ignored) {}
        }

        GlobalTranslator.translator().addSource(store);
    }

    private @NonNull Collection<Locale> readLocalesFromJar() {
        Set<Locale> locales = new HashSet<>();
        locales.add(Locale.of("en", "US"));

        try {
            URL root = Plugin.class.getResource("");
            if (root == null) return locales;

            try (FileSystem fs = FileSystems.newFileSystem(root.toURI(), Collections.emptyMap()); Stream<Path> stream  = Files.list(fs.getRootDirectories().iterator().next().resolve("/lang"))) {
                stream.map(path -> path.getFileName().toString())
                        .filter(fileName -> fileName.startsWith("Bundle_") && fileName.endsWith(".properties"))
                        .map(fileName -> fileName.substring("Bundle_".length(), fileName.lastIndexOf(".")).split("_")) // assumes all bundle files contain both a language + country
                        .map(locale -> Locale.of(locale[0], locale[1]))
                        .forEach(locales::add);
            }
        } catch (URISyntaxException | IOException e) {
            getSLF4JLogger().warn("Failed to read resource bundle jars from the plugin jar", e);
        }

        return locales;
    }

    public static final class Config {

        private final Plugin owner;

        private final @NonNull GsonConfigurationLoader loader;
        private @NonNull ConfigurationNode root;

        public Config(@NonNull Plugin owner) {
            this.owner = owner;

            Path file = owner.getDataFolder()
                .toPath()
                .resolve("config.json");

            try {
                Files.createDirectories(file.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            GsonConfigurationLoader.Builder builder = GsonConfigurationLoader.builder();
            builder.path(file);
            loader = builder.build();

            try {
                root = Files.exists(file) ? loader.load() : loader.createNode();
                defaults(root, owner.nodes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            save();
        }

        @SuppressWarnings("unchecked")
        private static void defaults(@NonNull ConfigurationNode node, Map<String, Object> defaults) throws SerializationException {
            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                ConfigurationNode child = node.node(entry.getKey());
                Object value = entry.getValue();

                if (value instanceof Map<?, ?> nested) {
                    defaults(child, (Map<String, Object>) nested);
                } else if (child.virtual()) {
                    child.set(value);
                }
            }
        }

        public @NonNull Plugin owner() {
            return owner;
        }

        public @NonNull ConfigurationNode root() {
            return root;
        }

        public void save() {
            try {
                loader.save(root);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void reload() {
            try {
                root = loader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
