package com.pohcosmetictransmogs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CalibrationTest
{
	@Test
	public void everySelectablePlacementRetainsItsEffectiveTransform() throws Exception
	{
		Catalogue catalogue = Catalogue.loadCatalogue(RuneLiteAPI.GSON, null, Catalogue.openCatalogueReader());
		try (InputStreamReader reader = new InputStreamReader(
			getClass().getResourceAsStream("/calibrations.json"), StandardCharsets.UTF_8))
		{
			JsonObject expected = RuneLiteAPI.GSON.fromJson(reader, JsonObject.class);
			assertEquals(139, expected.size());
			for (Map.Entry<String, JsonElement> entry : expected.entrySet())
			{
				String[] keys = entry.getKey().split(":");
				TargetSpec target = catalogue.targets.get(keys[0]);
				Catalogue.Recipe recipe = catalogue.appearances.get(keys[1]);
				Catalogue.Calibration actual = Catalogue.ModelFactory.calibration(recipe, target,
					target.sizeX, target.sizeY);
				Catalogue.Calibration calibration = RuneLiteAPI.GSON.fromJson(entry.getValue(),
					Catalogue.Calibration.class);
				calibration.fitMode = actual.fitMode;
				assertEquals(entry.getKey(), calibration, actual);
			}
		}
	}
}
