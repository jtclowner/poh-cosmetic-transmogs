package com.pohcosmetictransmogs;

import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.AppearanceOption;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.ArmourAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.ChestAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.EntranceAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.StorageAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.WardrobeAppearance;
import java.util.HashSet;
import java.util.Set;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PohCosmeticTransmogsTest
{
	@BeforeClass
	public static void loadCatalogue()
	{
		PohAppearanceCatalog.initialize(RuneLiteAPI.GSON, null, null);
	}

	@Test
	public void bundledRecipesHaveValidModelsAndPlacements()
	{
		Set<Integer> ids = new HashSet<>();
		assertFalse(PohAppearanceCatalog.catalogue().isEmpty());
		for (PohAppearanceCatalog.Recipe recipe : PohAppearanceCatalog.catalogue())
		{
			assertNotNull(recipe.name);
			assertFalse(recipe.name.trim().isEmpty());
			assertTrue(recipe.name, ids.add(recipe.source.getObjectId()));
			for (PohAppearanceCatalog.Definition state : new PohAppearanceCatalog.Definition[]
				{recipe.source, recipe.closed, recipe.open})
			{
				assertTrue(recipe.name, state.getModelIds().length > 0);
				assertTrue(recipe.name, state.getSizeX() > 0 && state.getSizeY() > 0);
				assertTrue(recipe.name, state.getModelScaleX() > 0
					&& state.getModelScaleHeight() > 0 && state.getModelScaleY() > 0);
				assertEquals(recipe.name, state.getRecolorFrom().length, state.getRecolorTo().length);
			}
			for (PohAppearanceCatalog.Calibration fit : recipe.placements.values())
			{
				assertTrue(recipe.name, fit.getScaleX() > 0 && fit.getScaleHeight() > 0 && fit.getScaleY() > 0);
			}
		}
	}

	@Test
	public void allowedPlacementsUseExplicitFitsOrCrystalOutcropFitting()
	{
		PohAppearanceCatalog.get(30027);
		for (PohFurniture furniture : PohFurniture.values())
		{
			for (PohAppearanceCatalog.Recipe recipe : PohAppearanceCatalog.catalogue())
			{
				int id = recipe.source.getObjectId();
				if (PohFurniture.isAllowed(furniture, id))
				{
					if (PohAppearanceCatalog.placement(furniture, id) == null)
					{
						assertEquals(furniture + ":" + id, 4928, id);
						PohAppearanceCatalog.Calibration fit = calibration(furniture, id);
						assertEquals(furniture == PohFurniture.FANCY_DRESS_BOX ? 256
							: furniture == PohFurniture.TREASURE_CHEST || furniture == PohFurniture.TOY_BOX ? 160 : 128,
							fit.getScaleX());
						assertEquals(128, fit.getScaleHeight());
						assertEquals(furniture == PohFurniture.FANCY_DRESS_BOX ? 256 : 128, fit.getScaleY());
					}
				}
			}
		}
	}

	@Test
	public void chestRecipesCanShareASourceWithoutSharingTheirClosedState()
	{
		PohAppearanceCatalog.Recipe crystal = PohAppearanceCatalog.recipe(30027);
		PohAppearanceCatalog.Recipe chest = PohAppearanceCatalog.recipe(30028);
		assertEquals("CoX Crystal Chest", crystal.name);
		assertEquals("CoX Chest", chest.name);
		assertEquals(32752, crystal.transitionModelId);
		assertEquals(7506, crystal.transitionAnimationId);
		assertEquals(20, crystal.transitionHandoff);
		assertEquals(-1, chest.transitionModelId);
		assertTrue(PohAppearanceCatalog.recipe(29766).bobbing);
		assertFalse(chest.bobbing);
		assertEquals(crystal.open.getObjectId(), chest.source.getObjectId());
		assertArrayEquals(new int[] {32752, 32755}, crystal.closed.getModelIds());
		assertArrayEquals(new int[] {32755}, chest.closed.getModelIds());
		assertArrayEquals(new int[] {32756}, chest.open.getModelIds());
		assertNotEquals(crystal.placements.get(PohFurniture.FANCY_DRESS_BOX),
			chest.placements.get(PohFurniture.FANCY_DRESS_BOX));
	}

	@Test
	public void authoritativeCalibrationsAreCompiledIn()
	{
		PohAppearanceCatalog.Calibration portal = calibration(PohFurniture.ENTRANCE_PORTAL, 41807);
		assertEquals(1024, portal.getRotation());
		assertEquals(232, portal.getScaleX());
		assertEquals(84, portal.getOffsetHeight());

		PohAppearanceCatalog.Calibration chest = calibration(PohFurniture.TREASURE_CHEST, 30027);
		assertEquals(512, chest.getRotation());
		PohAppearanceCatalog.Calibration openChest = calibration(PohFurniture.TREASURE_CHEST, 30028);
		assertEquals(chest.getScaleX(), openChest.getScaleX());
		assertEquals(chest.getScaleHeight(), openChest.getScaleHeight());
		assertEquals(chest.getScaleY(), openChest.getScaleY());
		assertEquals(128, chest.getScaleX());
		assertEquals(192, chest.getScaleY());
		assertEquals(16, chest.getOffsetY());

		PohAppearanceCatalog.Calibration sarcophagus = calibration(PohFurniture.TREASURE_CHEST, 44825);
		assertTrue(sarcophagus.isFlipX());
		assertFalse(calibration(PohFurniture.TOY_BOX, 44825).isFlipX());
		assertEquals(-85, sarcophagus.signedScaleX());
		assertEquals(80, sarcophagus.getScaleY());
		assertEquals(
			calibration(PohFurniture.TOY_BOX, 29742).getRotation() & 2047,
			calibration(PohFurniture.TREASURE_CHEST, 29742).getRotation() & 2047);
		assertEquals(calibration(PohFurniture.TOY_BOX, 29742),
			calibration(PohFurniture.TREASURE_CHEST, 29742));
		assertNotEquals(calibration(PohFurniture.TOY_BOX, 44825), sarcophagus);
		assertNotNull(PohAppearanceCatalog.get(33012));
		assertArrayEquals(new int[] {35412}, PohAppearanceCatalog.get(33012).getModelIds());
		assertNull(PohAppearanceCatalog.get(33013));
		assertEquals(1024, calibration(PohFurniture.ARMOUR_CASE, 33012).getRotation());
	}

	@Test
	public void dropdownAllowlistsMatchCataloguePlacements()
	{
		assertEquals(34, EntranceAppearance.values().length - 1);
		assertEquals(17, StorageAppearance.values().length - 1);
		assertEquals(19, ArmourAppearance.values().length - 1);
		assertEquals(18, ChestAppearance.values().length - 1);
		assertEquals(15, WardrobeAppearance.values().length - 1);

		assertTrue(PohFurniture.isAllowed(PohFurniture.TREASURE_CHEST, 32991));
		assertFalse(PohFurniture.isAllowed(PohFurniture.CAPE_RACK, 32991));
		assertFalse(PohFurniture.isAllowed(PohFurniture.ARMOUR_CASE, 33125));
		assertFalse(PohFurniture.isAllowed(PohFurniture.MAGIC_WARDROBE, 41724));
		assertFalse(PohFurniture.isAllowed(PohFurniture.MAGIC_WARDROBE, 44825));
		assertTrue(PohFurniture.isAllowed(PohFurniture.MAGIC_WARDROBE, 6282));
		for (int id : new int[] {4928})
		{
			assertTrue(PohFurniture.isAllowed(PohFurniture.CAPE_RACK, id));
			assertTrue(PohFurniture.isAllowed(PohFurniture.ARMOUR_CASE, id));
			assertTrue(PohFurniture.isAllowed(PohFurniture.TOY_BOX, id));
			assertTrue(PohFurniture.isAllowed(PohFurniture.TREASURE_CHEST, id));
			assertTrue(PohFurniture.isAllowed(PohFurniture.FANCY_DRESS_BOX, id));
			assertFalse(PohFurniture.isAllowed(PohFurniture.MAGIC_WARDROBE, id));
			assertFalse(PohFurniture.isAllowed(PohFurniture.ENTRANCE_PORTAL, id));
		}
		assertTrue(PohFurniture.isAllowed(PohFurniture.CAPE_RACK, 32996));
		assertFalse(PohFurniture.isAllowed(PohFurniture.ARMOUR_CASE, 32996));
		assertFalse(PohFurniture.isAllowed(PohFurniture.TOY_BOX, 32996));
		assertFalse(PohFurniture.isAllowed(PohFurniture.TREASURE_CHEST, 32996));
		assertFalse(PohFurniture.isAllowed(PohFurniture.FANCY_DRESS_BOX, 32996));
	}

	@Test
	public void everyDropdownChoiceHasACompiledRecipe()
	{
		assertRecipes(EntranceAppearance.values());
		assertRecipes(StorageAppearance.values());
		assertRecipes(ArmourAppearance.values());
		assertRecipes(ChestAppearance.values());
		assertRecipes(WardrobeAppearance.values());
	}

	@Test
	public void nodeAndCoxRecolourPalettesAreExplicit()
	{
		assertArrayEquals(new short[] {652, 908, 916, 920, 926},
			PohAppearanceCatalog.get(61216).getPortalColours());
		assertArrayEquals(new short[] {(short) 38040, (short) 38053, (short) 38309, (short) 38315},
			PohAppearanceCatalog.get(42819).getNodeColours());
		assertEquals(11, PohAppearanceCatalog.get(42819).getNodeGreyColours().length);
		assertEquals(4, PohAppearanceCatalog.get(29794).getCrystalColours().length);
		assertEquals(4, PohAppearanceCatalog.get(29757).getCrystalColours().length);
		assertEquals(3, PohAppearanceCatalog.get(29766).getCrystalColours().length);
		assertEquals(2, PohAppearanceCatalog.get(30027).getCrystalColours().length);
		short[] gauntletChestColours = {
			(short) 32916, (short) 32922, (short) 32926, (short) 32200,
			(short) 29518, (short) 29526, (short) 31192,
			(short) 26776};
		assertArrayEquals(gauntletChestColours,
			PohAppearanceCatalog.get(36087).getGauntletColours());
		assertArrayEquals(gauntletChestColours,
			PohAppearanceCatalog.get(36088).getGauntletColours());
		assertArrayEquals(new short[] {(short) 55219},
			PohAppearanceCatalog.get(32991).getTobColours());
		assertArrayEquals(PohAppearanceCatalog.get(32991).getTobColours(),
			PohAppearanceCatalog.get(41746).getTobColours());
		assertArrayEquals(new short[] {(short) 54177},
			PohAppearanceCatalog.get(4928).getCrystalColours());
		assertArrayEquals(new short[] {
			(short) 53582, (short) 52403, (short) 52407, (short) 52416,
			(short) 52424, (short) 51515, (short) 51484},
			PohAppearanceCatalog.get(32996).getCrystalColours());
		assertArrayEquals(new short[] {(short) 960, (short) 794, (short) 914},
			PohAppearanceCatalog.get(33125).getDeadmanColours());
		assertArrayEquals(PohAppearanceCatalog.get(33125).getDeadmanColours(),
			PohAppearanceCatalog.get(31583).getDeadmanColours());
		assertEquals(23, PohAppearanceCatalog.get(44825).getToaColours().length);
		short[] toaChestColours = PohAppearanceCatalog.get(44788).getToaColours();
		assertEquals(17, toaChestColours.length);
		assertArrayEquals(toaChestColours,
			PohAppearanceCatalog.get(44789).getToaColours());
		assertArrayEquals(toaChestColours,
			PohAppearanceCatalog.get(41696).getToaColours());
		assertArrayEquals(toaChestColours,
			PohAppearanceCatalog.get(44791).getToaColours());
		for (int chestColour : new int[] {6315, 6348, 6592, 6674, 6817, 6819,
			6823, 6825, 6827, 6833, 6837, 6839, 6848, 6856, 6864, 6868, 6872})
		{
			assertTrue(contains(toaChestColours, (short) chestColour));
		}
		for (int coinColour : new int[] {7384, 7690, 7349, 7343, 7506, 7500,
			7492, 8123, 7616, 5943, 6986})
		{
			assertFalse(contains(PohAppearanceCatalog.get(44825).getToaColours(),
				(short) coinColour));
			assertFalse(contains(toaChestColours, (short) coinColour));
		}
		for (int retainedColour : new int[] {5281, 5293, 6336, 6379, 6976})
		{
			assertFalse(contains(PohAppearanceCatalog.get(44825).getToaColours(),
				(short) retainedColour));
		}
		assertEquals(14, PohCosmeticTransmogsConfig.AppearanceColour.GREEN.getHue());
	}

	@Test
	public void crystalModelsUseCalibratedTransforms()
	{
		assertArrayEquals(new int[] {4895}, PohAppearanceCatalog.get(4928).getModelIds());
		assertEquals("Crystal Outcrop", PohAppearanceCatalog.name(4928));
		assertNull(PohAppearanceCatalog.get(4927));
		for (PohFurniture slot : PohFurniture.values())
		{
			assertFalse(PohFurniture.isAllowed(slot, 4927));
		}
		assertArrayEquals(new int[] {35449}, PohAppearanceCatalog.get(32996).getModelIds());
		assertEquals(160, calibration(PohFurniture.TREASURE_CHEST, 4928).getScaleX());
		assertEquals(96, calibration(PohFurniture.TREASURE_CHEST, 4928).getScaleY());
		assertEquals(calibration(PohFurniture.TREASURE_CHEST, 4928),
			calibration(PohFurniture.TOY_BOX, 4928));

		PohAppearanceCatalog.Calibration crystal =
			calibration(PohFurniture.CAPE_RACK, 32996);
		assertEquals(128, crystal.getScaleX());
		assertEquals(128, crystal.getScaleHeight());
		assertEquals(128, crystal.getScaleY());

		assertEquals(192, calibration(PohFurniture.ARMOUR_CASE, 29766).getScaleX());
		assertEquals(192, calibration(PohFurniture.CAPE_RACK, 29766).getScaleX());
		assertEquals(180, calibration(PohFurniture.ARMOUR_CASE, 29766).getScaleHeight());
		assertEquals(-4, calibration(PohFurniture.ARMOUR_CASE, 29766).getOffsetHeight());
		assertEquals(180, calibration(PohFurniture.CAPE_RACK, 29766).getScaleHeight());
		assertEquals(336, calibration(PohFurniture.TOY_BOX, 29766).getScaleX());
		assertEquals(210, calibration(PohFurniture.TOY_BOX, 29766).getScaleHeight());
		assertEquals(-4, calibration(PohFurniture.TOY_BOX, 29766).getOffsetHeight());
		assertEquals(336, calibration(PohFurniture.TREASURE_CHEST, 29766).getScaleX());
		assertEquals(210, calibration(PohFurniture.TREASURE_CHEST, 29766).getScaleHeight());
		assertEquals(384, calibration(PohFurniture.FANCY_DRESS_BOX, 29766).getScaleX());
		assertEquals(240, calibration(PohFurniture.FANCY_DRESS_BOX, 29766).getScaleHeight());

		PohAppearanceCatalog.Calibration tob =
			calibration(PohFurniture.FANCY_DRESS_BOX, 32991);
		assertEquals(100, tob.getScaleX());
		assertEquals(100, tob.getScaleHeight());
		assertEquals(100, tob.getScaleY());
	}

	@Test
	public void openStatesUseTheirPairedModels()
	{
		assertEquals(41746, state(32991, 18809));
		assertEquals(47420, state(47419, 18809));
		assertEquals(29743, state(29742, 18809));
		assertEquals(44789, state(44788, 18809));
		assertEquals(44791, state(41696, 18809));
		assertEquals(36088, state(36087, 18809));
		assertEquals(31583, state(33125, 18809));
		assertEquals(44934, state(44825, 18809));
		assertEquals(30028, state(30027, 18809));
		assertEquals(32991, PohAppearanceCatalog.canonicalSelectionId(41746));
		assertEquals(47419, PohAppearanceCatalog.canonicalSelectionId(47420));
		assertEquals(29742, PohAppearanceCatalog.canonicalSelectionId(29743));
		assertEquals(44788, PohAppearanceCatalog.canonicalSelectionId(44789));
		assertEquals(41696, PohAppearanceCatalog.canonicalSelectionId(44791));
		assertEquals(36087, PohAppearanceCatalog.canonicalSelectionId(36088));
		assertEquals(33125, PohAppearanceCatalog.canonicalSelectionId(31583));
		assertEquals(44825, PohAppearanceCatalog.canonicalSelectionId(44934));
		assertEquals(30028, PohAppearanceCatalog.canonicalSelectionId(30028));
		assertNotEquals(PohAppearanceCatalog.state(30028, false), PohAppearanceCatalog.state(30028, true));
		assertArrayEquals(new int[] {32755},
			PohAppearanceCatalog.state(30028, false).getModelIds());
		assertArrayEquals(new int[] {32756},
			PohAppearanceCatalog.state(30028, true).getModelIds());
		assertEquals(-512, calibration(PohFurniture.FANCY_DRESS_BOX, 30027).getRotation());
		assertEquals(512, calibration(PohFurniture.FANCY_DRESS_BOX, 30028).getRotation());
		assertEquals(9505, PohAppearanceCatalog.get(44934).getSpawnAnimationId());
		assertEquals(14167, PohAppearanceCatalog.get(61216).getSpawnAnimationId());
		assertEquals(7823, PohAppearanceCatalog.get(33125).getSpawnAnimationId());
		assertTrue(PohAppearanceCatalog.get(33125).isSpawnOnce());
		assertNotEquals(PohAppearanceCatalog.state(33125, false), PohAppearanceCatalog.state(33125, true));
		assertEquals(PohAppearanceCatalog.state(29766, false), PohAppearanceCatalog.state(29766, true));
		assertEquals(-1, calibration(PohFurniture.CAPE_RACK, 33125).getOffsetHeight());
		assertEquals(-1, calibration(PohFurniture.FANCY_DRESS_BOX, 33125).getOffsetHeight());
		assertEquals(-1, calibration(PohFurniture.TREASURE_CHEST, 33125).getOffsetHeight());
	}

	@Test
	public void targetFamiliesContainEveryConstructedVariant()
	{
		assertEquals(2, PohFurniture.ENTRANCE_PORTAL.getObjectIds().length);
		assertEquals(6, PohFurniture.CAPE_RACK.getObjectIds().length);
		assertEquals(6, PohFurniture.FANCY_DRESS_BOX.getObjectIds().length);
		assertEquals(6, PohFurniture.ARMOUR_CASE.getObjectIds().length);
		assertEquals(14, PohFurniture.MAGIC_WARDROBE.getObjectIds().length);
		assertEquals(6, PohFurniture.TOY_BOX.getObjectIds().length);
		assertEquals(6, PohFurniture.TREASURE_CHEST.getObjectIds().length);
		assertEquals(PohFurniture.TREASURE_CHEST, PohFurniture.fromObjectId(18808));
		assertEquals(PohFurniture.ENTRANCE_PORTAL, PohFurniture.fromObjectId(4525));
		assertEquals(PohFurniture.ENTRANCE_PORTAL, PohFurniture.fromObjectId(60789));
		assertNull(PohFurniture.fromObjectId(30028));
	}

	@Test
	public void renderingHelpersRemainBounded()
	{
		assertTrue(PohCosmeticTransmogsManager.isVisibleLevel(0, 0));
		assertTrue(PohCosmeticTransmogsManager.isVisibleLevel(1, 1));
		assertTrue(PohCosmeticTransmogsManager.isVisibleLevel(2, 2));
		assertTrue(PohCosmeticTransmogsManager.isVisibleLevel(1, 2));
		assertFalse(PohCosmeticTransmogsManager.isVisibleLevel(1, 0));
		assertFalse(PohCosmeticTransmogsManager.isVisibleLevel(2, 0));
		assertFalse(PohCosmeticTransmogsManager.isVisibleLevel(0, 1));
		assertFalse(PohCosmeticTransmogsManager.isVisibleLevel(2, 1));
	}

	private static PohAppearanceCatalog.Calibration calibration(PohFurniture furniture, int objectId)
	{
		PohAppearanceCatalog.Definition definition = PohAppearanceCatalog.get(objectId);
		assertNotNull(definition);
		return PohAppearanceCatalog.ModelFactory.calibration(furniture,
			furniture.getSizeX(), furniture.getSizeY(), definition);
	}

	private static int state(int sourceObjectId, int targetObjectId)
	{
		return PohAppearanceCatalog.state(sourceObjectId, PohFurniture.isOpenState(targetObjectId)).getObjectId();
	}

	private static <T extends Enum<T> & AppearanceOption> void assertRecipes(T[] appearances)
	{
		for (T appearance : appearances)
		{
			if (appearance.getObjectId() >= 0)
			{
				PohAppearanceCatalog.Definition definition =
					PohAppearanceCatalog.get(appearance.getObjectId());
				assertNotNull(appearance.toString(), definition);
				assertTrue(appearance.toString(), definition.getModelIds().length > 0);
			}
		}
	}

	private static boolean contains(short[] values, short value)
	{
		for (short candidate : values)
		{
			if (candidate == value)
			{
				return true;
			}
		}
		return false;
	}
}
