package net.noiraude.gtnhcredits.repository;

import static net.noiraude.gtnhcredits.GTNHCredits.LOG;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.noiraude.gtnhcredits.GTNHCredits;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.parser.CreditsParseException;
import net.noiraude.libcredits.parser.CreditsParser;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class CreditsRepository {

    private static final ResourceLocation LOCATION = new ResourceLocation(GTNHCredits.MODID, "credits.json");

    static CreditsDocument load() {
        try (InputStream is = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(LOCATION)
            .getInputStream()) {
            return CreditsParser.parse(is);
        } catch (FileNotFoundException e) {
            LOG.info("No credits resource provided at {}", LOCATION);
            return CreditsDocument.empty();
        } catch (IOException e) {
            LOG.error("Failed to load credits from {}", LOCATION, e);
            return CreditsDocument.empty();
        } catch (CreditsParseException e) {
            LOG.error("Credits data from {} is invalid: {}", LOCATION, e.getMessage());
            return CreditsDocument.empty();
        }
    }
}
