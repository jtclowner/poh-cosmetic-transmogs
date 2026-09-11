package com.pohcosmetictransmogs;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

import static org.junit.Assert.*;

public class CatalogueTest
{
	@Test
	public void everyReaderUsesTheSameCacheAwareNormalization()
	{
		ObjectComposition composition = ApiDouble.of(ObjectComposition.class, (name, args) ->
			name.equals("getSizeX") ? 3 : name.equals("getSizeY") ? 2 : ApiDouble.DEFAULT);
		int[] lookups = {0};
		Client client = ApiDouble.of(Client.class, (name, args) ->
		{
			if (name.equals("getObjectDefinition")) { lookups[0]++; return composition; }
			return ApiDouble.DEFAULT;
		});
		Catalogue catalogue = Catalogue.loadCatalogue(RuneLiteAPI.GSON, client,
			new StringReader("{\"appearances\":{\"a\":{\"sourceObjectId\":42,\"sizeX\":6,\"sizeY\":8}}}"),
			new StringReader("{\"appearances\":{\"b\":{\"sourceObjectId\":42,\"sizeX\":6,\"sizeY\":8}}}"));
		assertEquals(2, lookups[0]);
		for (Catalogue.Recipe recipe : catalogue.appearances.values())
		{
			assertEquals(6, recipe.sizeX);
			assertEquals(8, recipe.sizeY);
			assertEquals(256, recipe.modelScaleX);
			assertEquals(512, recipe.modelScaleY);
			assertEquals(256, recipe.modelScaleHeight);
		}
	}

	@Test
	public void readersAccumulateEntriesInOrder()
	{
		Catalogue catalogue = read(
			"{\"targets\":{\"box\":{\"objectIds\":[1,2],\"openObjectIds\":[2]}}}",
			"{\"appearances\":{\"gem\":{\"modelIds\":[10],\"bindTargets\":[\"box\"]}}}",
			"{\"appearances\":{\"gem\":{\"name\":\"Gem\",\"modelIds\":[11],\"bindTargets\":[\"box\"]}}}");
		assertEquals(1, catalogue.targets.size());
		assertEquals(1, catalogue.appearances.size());
		assertArrayEquals(new int[] {11}, catalogue.appearances.get("gem").modelIds);
		Map<Integer, TargetBinding> bindings = catalogue.bind(Collections.emptyMap());
		assertSame(bindings.get(1), bindings.get(2));
		assertTrue(bindings.get(1).target.isStateful());
	}

	@Test
	public void placementsDoNotCreateBindings()
	{
		Catalogue catalogue = read("{\"targets\":{\"box\":{\"objectIds\":[1]}},"
			+ "\"appearances\":{\"gem\":{\"modelIds\":[10],\"placements\":{\"box\":{\"offsetY\":12}}}}}");
		assertTrue(catalogue.bind(Collections.emptyMap()).isEmpty());
		TargetBinding binding = catalogue.bind(Map.of("box", "gem")).get(1);
		assertEquals(12, binding.calibration(1, 1).offsetY);
	}

	@Test
	public void automaticBindingsUseTheSameTypeAsSelectionFallbacks()
	{
		Catalogue catalogue = read("{\"targets\":{\"a\":{\"objectIds\":[1,2]},"
			+ "\"b\":{\"objectIds\":[3]},\"c\":{\"objectIds\":[4]}},\"appearances\":{"
			+ "\"gem\":{\"modelIds\":[10],\"bindTargets\":[\"a\",\"b\"],"
			+ "\"placements\":{\"a\":{\"scaleX\":200},\"b\":{\"scaleX\":300}}},"
			+ "\"other\":{\"modelIds\":[20]}}}");
		Map<Integer, TargetBinding> bindings = catalogue.bind(Map.of("a", "other", "c", "other"));
		assertEquals("gem", bindings.get(1).appearance.key);
		assertSame(bindings.get(1), bindings.get(2));
		assertSame(bindings.get(1).appearance, bindings.get(3).appearance);
		assertEquals(200, bindings.get(1).calibration(1, 1).scaleX);
		assertEquals(300, bindings.get(3).calibration(1, 1).scaleX);
		assertEquals("other", bindings.get(4).appearance.key);
	}

	@Test
	public void modelMetadataDoesNotDefineAppearanceIdentity()
	{
		Catalogue catalogue = read("{\"appearances\":{"
			+ "\"red\":{\"sourceObjectId\":1,\"modelIds\":[10],\"recolorFrom\":[127],\"recolorTo\":[730]},"
			+ "\"blue\":{\"sourceObjectId\":1,\"modelIds\":[10],\"recolorFrom\":[127],\"recolorTo\":[44762]},"
			+ "\"explicit\":{\"modelIds\":[10],\"modelScaleX\":200}}}");
		assertEquals(3, catalogue.appearances.size());
		assertNotSame(catalogue.appearances.get("red"), catalogue.appearances.get("blue"));
		assertEquals(-1, catalogue.appearances.get("explicit").sourceObjectId);
		assertEquals(200, catalogue.appearances.get("explicit").modelScaleX);
	}

	@Test
	public void missingStatesUseTheDefaultAndOnlyStatefulTargetsSelectStates()
	{
		Catalogue catalogue = read("{\"targets\":{\"static\":{\"objectIds\":[1]},"
			+ "\"container\":{\"objectIds\":[2,3],\"openObjectIds\":[3]}},\"appearances\":{"
			+ "\"gem\":{\"modelIds\":[10],\"open\":{\"modelIds\":[11],\"animationId\":99},"
			+ "\"bindTargets\":[\"static\",\"container\"]}}}");
		Catalogue.Recipe gem = catalogue.appearances.get("gem");
		assertSame(gem, gem.closed);
		assertEquals(-1, gem.animationId);
		assertEquals(99, gem.open.animationId);
		Map<Integer, TargetBinding> bindings = catalogue.bind(Collections.emptyMap());
		assertSame(gem, bindings.get(1).state(1));
		assertSame(gem, bindings.get(2).state(2));
		assertSame(gem.open, bindings.get(3).state(3));
	}

	@Test
	public void targetAndPlacementFitModesRemainIndependentOfModelScale()
	{
		Catalogue catalogue = read("{\"targets\":{\"box\":{\"objectIds\":[1],\"defaultFitMode\":\"FOOTPRINT\"}},"
			+ "\"appearances\":{\"gem\":{\"sizeX\":1,\"sizeY\":1,\"modelIds\":[10],"
			+ "\"modelScaleX\":200,\"modelScaleY\":180,\"modelScaleHeight\":150}}}");
		TargetBinding binding = catalogue.bind(Map.of("box", "gem")).get(1);
		Catalogue.Calibration fit = binding.calibration(2, 3);
		assertEquals(400, fit.scaleX);
		assertEquals(540, fit.scaleY);
		assertEquals(150, fit.scaleHeight);
		Catalogue.Calibration placement = new Catalogue.Calibration();
		placement.fitMode = TargetSpec.FitMode.NONE;
		placement.scaleX = 300;
		binding.appearance.placements.put("box", placement);
		assertEquals(300, binding.calibration(2, 3).scaleX);
		assertEquals(1, binding.appearance.sizeX);
		assertEquals(200, binding.appearance.modelScaleX);
	}

	@Test
	public void nullReadersAreOptionalAndConsumedReadersAreClosed()
	{
		boolean[] closed = {false};
		StringReader reader = new StringReader("{}")
		{
			@Override
			public void close()
			{
				closed[0] = true;
				super.close();
			}
		};
		Catalogue catalogue = Catalogue.loadCatalogue(RuneLiteAPI.GSON, null, null, reader, new StringReader("  "));
		assertTrue(closed[0]);
		assertTrue(catalogue.targets.isEmpty());
		assertTrue(catalogue.appearances.isEmpty());
	}

	private static Catalogue read(String... inputs)
	{
		StringReader[] readers = new StringReader[inputs.length];
		for (int i = 0; i < inputs.length; i++)
		{
			readers[i] = new StringReader(inputs[i]);
		}
		return Catalogue.loadCatalogue(RuneLiteAPI.GSON, null, readers);
	}
}
