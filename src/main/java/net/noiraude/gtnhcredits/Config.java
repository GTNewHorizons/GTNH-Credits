package net.noiraude.gtnhcredits;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import org.jetbrains.annotations.NotNull;

public final class Config {

    private static final String SECTION_MENU_BUTTON = "menu_button";
    private static final String SECTION_CREDITS_SCREEN = "credits_screen";

    private static Config instance;

    // menu_button
    public final boolean menuButtonEnabled;
    /** X position. >= 0: absolute. < 0: margin from right edge (button right = screen_width - abs(buttonX)). */
    public final int menuButtonX;
    /** Y position. >= 0: absolute. < 0: margin from bottom edge (button bottom = screen_height - abs(buttonY)). */
    public final int menuButtonY;
    public final @NotNull String menuButtonIcon;

    // credits_screen
    /** Resource location of the logo texture, in {@code domain:path} format. */
    public final @NotNull String creditsScreenLogo;
    /** Minimum fuzzy-match score for a person name to appear in fuzzy filter results. Lower is stricter. */
    public final double fuzzyThreshold;

    private Config(@NotNull File configDir) {
        @NotNull
        Configuration cfg = new Configuration(new File(configDir, "gtnh-credits.cfg"));

        menuButtonEnabled = cfg
            .getBoolean("enabled", SECTION_MENU_BUTTON, false, "Show the Credits button in the vanilla main menu.");
        menuButtonX = cfg.getInt(
            "buttonX",
            SECTION_MENU_BUTTON,
            -18,
            -10000,
            10000,
            "Button X position. >= 0: relative to left. < 0: relative to right.");
        menuButtonY = cfg.getInt(
            "buttonY",
            SECTION_MENU_BUTTON,
            -18,
            -10000,
            10000,
            "Button Y position. >= 0: relative to top < 0: relative to bottom.");
        menuButtonIcon = cfg.getString(
            "icon",
            SECTION_MENU_BUTTON,
            "",
            "Resource location of the button icon texture (domain:path). Leave empty for none.");

        creditsScreenLogo = cfg.getString(
            "logo",
            SECTION_CREDITS_SCREEN,
            GTNHCredits.MODID + ":textures/gui/credits/logo.png",
            "Resource location of the credits screen logo texture (domain:path).");
        fuzzyThreshold = cfg.getFloat(
            "fuzzyThreshold",
            SECTION_CREDITS_SCREEN,
            30.0f,
            0.0f,
            1000.0f,
            "Minimum fuzzy-match score for a person name to appear in fuzzy filter results. Lower is stricter.");
        if (cfg.hasChanged()) cfg.save();
    }

    public static void synchronizeConfiguration(@NotNull File configDir) {
        if (instance != null) throw new IllegalStateException("Config already initialized");
        instance = new Config(configDir);
    }

    public static @NotNull Config getInstance() {
        if (instance == null) throw new IllegalStateException("Config not yet initialized");
        return instance;
    }
}
