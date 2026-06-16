package com.milkdromeda.aiassistant.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // Settings (including the API key) live in their own folder under the game's
    // config directory. That directory is untouched when you replace the mod jar,
    // so your key and preferences carry over when you update the mod.
    private static final Path CONFIG_DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("ai-assistant");
    private static final Path CONFIG_PATH = CONFIG_DIR.resolve("config.json");
    // Older builds stored a single file here; it's migrated automatically.
    private static final Path LEGACY_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("ai-assistant.json");

    private static ModConfig instance;

    public String hfToken = "";
    public String hfModel = "mistralai/Mistral-7B-Instruct-v0.2";
    // Modern HuggingFace router endpoint (OpenAI-compatible chat completions).
    // The old api-inference.huggingface.co endpoint is deprecated and causes
    // connection errors. You can point this at any OpenAI-compatible API
    // (HuggingFace, OpenAI, a local Ollama/LM Studio server, etc.).
    public String apiUrl = "https://router.huggingface.co/v1/chat/completions";
    public int maxNewTokens = 512;
    public double temperature = 0.7;
    public boolean debugLogging = false;
    // Lower = faster, snappier action execution (ticks between plan steps).
    public int actionTickDelay = 8;
    public double followDistance = 4.0;
    public double guardRadius = 16.0;
    // Self-preservation: flee/heal-up when health drops below this fraction.
    public double fleeHealthPercent = 0.25;
    // When true the assistant may execute Minecraft commands as part of a plan
    // (e.g. /setblock for redstone, /fill, /give). This is what lets it "do
    // almost anything". Gated to a permission level and a denylist for safety.
    public boolean allowCommands = true;
    // Permission level for commands the assistant runs (vanilla: 2 = command
    // block tier — allows /setblock, /fill, /summon, /give, /tp, /effect, but
    // NOT server-admin commands like /op or /stop, which need level 3-4).
    public int commandPermissionLevel = 2;
    // When true the assistant listens to normal chat and reacts to commands
    // like "Ethan, follow me" or "help me mine this tree" without needing /ai.
    public boolean chatListening = true;
    // When true the assistant analyses *every* chat message with the language
    // model to decide if you need it — so you don't have to use its name or any
    // exact command words. Requires an API token; ignored if chatListening off.
    public boolean activeMode = true;
    // Default name given to a freshly summoned assistant.
    public String defaultName = "Ethan";
    // Default skin for a freshly summoned assistant: "default"/"steve", a
    // "namespace:path.png" texture, or a name under
    // assets/ai-assistant/textures/entity/skins/<name>.png.
    public String defaultSkin = "default";

    // Safety cap: automatically stop a running task after this many seconds, so a
    // task stuck in an endless loop can't keep running (and lagging) forever.
    // Ongoing activities like patrol/guard count against this too. 0 = no limit.
    public int maxTaskSeconds = 300;

    public static ModConfig get() {
        if (instance == null) load();
        return instance;
    }

    /** Loads settings, migrating the legacy file and surviving missing/corrupt data. */
    public static void load() {
        Path source = Files.exists(CONFIG_PATH) ? CONFIG_PATH
                : (Files.exists(LEGACY_PATH) ? LEGACY_PATH : null);
        if (source != null) {
            try (Reader r = Files.newBufferedReader(source)) {
                ModConfig loaded = GSON.fromJson(r, ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    instance.normalize();
                    save();   // (re)write into the folder, migrating the legacy file across
                    return;
                }
            } catch (Exception e) {
                // Don't lose a recoverable key: keep the bad file as .bak, then fall
                // back to defaults rather than failing to start.
                backup(source);
                System.err.println("[AI-Assistant] Couldn't read config (" + e.getMessage()
                        + "); starting from defaults. Previous file kept as .bak");
            }
        }
        instance = new ModConfig();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, w);
            }
        } catch (IOException e) {
            System.err.println("[AI-Assistant] Failed to save config: " + e.getMessage());
        }
    }

    /** Fills in sensible defaults for any field that came back null/blank/invalid. */
    private void normalize() {
        if (hfToken == null) hfToken = "";
        if (hfModel == null || hfModel.isBlank()) hfModel = "mistralai/Mistral-7B-Instruct-v0.2";
        if (apiUrl == null || apiUrl.isBlank()) apiUrl = "https://router.huggingface.co/v1/chat/completions";
        if (defaultName == null || defaultName.isBlank()) defaultName = "Ethan";
        if (defaultSkin == null || defaultSkin.isBlank()) defaultSkin = "default";
        if (maxTaskSeconds < 0) maxTaskSeconds = 0;
    }

    private static void backup(Path source) {
        try {
            Files.copy(source, source.resolveSibling(source.getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }

    public boolean hasApiToken() {
        return hfToken != null && !hfToken.isBlank();
    }
}
