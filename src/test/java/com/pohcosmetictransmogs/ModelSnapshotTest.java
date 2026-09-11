package com.pohcosmetictransmogs;

import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Model data is checked independently of its catalogue representation. */
public class ModelSnapshotTest
{
	@Test
	public void modelStatesPreserveGeometryAnimationsAndColours() throws Exception
	{
		Catalogue catalogue = Catalogue.loadCatalogue(RuneLiteAPI.GSON, null, Catalogue.openCatalogueReader());
		Properties expected = new Properties();
		try (InputStream input = getClass().getResourceAsStream("/model-states.properties"))
		{
			expected.load(input);
		}
		Set<String> checked = new HashSet<>();
		for (Catalogue.Recipe recipe : catalogue.appearances.values())
		{
			for (Catalogue.Definition state : new Catalogue.Definition[] {recipe, recipe.closed, recipe.open})
			{
				JsonObject value = new JsonObject();
				value.add("geometry", RuneLiteAPI.GSON.toJsonTree(new int[] {state.sourceObjectId,
					state.sizeX, state.sizeY, state.getScaleX(), state.getScaleHeight(), state.getScaleY(),
					state.animationId, state.spawnAnimationId, state.spawnOnce ? 1 : 0}));
				value.add("models", RuneLiteAPI.GSON.toJsonTree(state.modelIds));
				value.add("from", RuneLiteAPI.GSON.toJsonTree(state.recolours.keySet()));
				value.add("to", RuneLiteAPI.GSON.toJsonTree(state.recolours.values()));
				value.add("palettes", RuneLiteAPI.GSON.toJsonTree(new short[][] {
					state.colours(Catalogue.ColourChannel.PORTAL), state.colours(Catalogue.ColourChannel.CRYSTALS),
					state.colours(Catalogue.ColourChannel.TOB_CHEST), state.colours(Catalogue.ColourChannel.GAUNTLET_CHEST),
					state.colours(Catalogue.ColourChannel.DEADMAN_CHEST), state.colours(Catalogue.ColourChannel.TOA_CONTAINERS),
					state.colours(Catalogue.ColourChannel.NODE_TRIM), state.colours(Catalogue.ColourChannel.NODE_BODY)}));
				value.addProperty("saturation", state.portalSaturation);
				byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8));
				StringBuilder hash = new StringBuilder();
				for (byte b : digest) { hash.append(String.format("%02x", b & 255)); }
				String key = recipe.key + ":" + recipe.stateKey(state);
				assertEquals(key, expected.getProperty(key), hash.toString());
				checked.add(key);
			}
		}
		assertEquals(expected.size(), checked.size());
	}
}
