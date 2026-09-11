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
		Catalogue.current = Catalogue.loadCatalogue(RuneLiteAPI.GSON, null, Catalogue.openCatalogueReader());
	}

	@Test
	public void recipesHaveValidModelsAndPlacements()
	{
		Set<String> ids = new HashSet<>();
		assertFalse(Catalogue.current.appearances.values().isEmpty());
		for (Catalogue.Recipe recipe : Catalogue.current.appearances.values())
		{
			assertNotNull(recipe.name);
			assertFalse(recipe.name.trim().isEmpty());
			assertTrue(recipe.name, ids.add(recipe.key));
			for (Catalogue.Definition state : new Catalogue.Definition[]
				{recipe, recipe.closed, recipe.open})
			{
				assertTrue(recipe.name, state.getModelIds().length > 0);
				assertTrue(recipe.name, state.getSizeX() > 0 && state.getSizeY() > 0);
				assertTrue(recipe.name, state.getModelScaleX() > 0
					&& state.getModelScaleHeight() > 0 && state.getModelScaleY() > 0);
				assertEquals(recipe.name, state.getRecolorFrom().length, state.getRecolorTo().length);
			}
			for (Catalogue.Calibration fit : recipe.placements.values())
			{
				assertTrue(recipe.name, fit.getScaleX() > 0 && fit.getScaleHeight() > 0 && fit.getScaleY() > 0);
			}
		}
	}

	@Test
	public void allowedPlacementsUseExplicitFitsOrCrystalOutcropFitting()
	{
		definition(30027);
		for (PohTargetSlot furniture : PohTargetSlot.values())
		{
			for (Catalogue.Recipe recipe : Catalogue.current.appearances.values())
			{
				int id = recipe.getSourceObjectId();
				if (isAllowed(furniture, id))
				{
					if (Catalogue.current.appearances.get(key(id)).placements.get(furniture.getTargetKey()) == null)
					{
						assertEquals(furniture + ":" + id, 4928, id);
						Catalogue.Calibration fit = calibration(furniture, id);
						assertEquals(furniture == PohTargetSlot.FANCY_DRESS_BOX ? 256
							: furniture == PohTargetSlot.TREASURE_CHEST || furniture == PohTargetSlot.TOY_BOX ? 160 : 128,
							fit.getScaleX());
						assertEquals(128, fit.getScaleHeight());
						assertEquals(furniture == PohTargetSlot.FANCY_DRESS_BOX ? 256 : 128, fit.getScaleY());
					}
				}
			}
		}
	}

	@Test
	public void chestRecipesCanShareASourceWithoutSharingTheirClosedState()
	{
		Catalogue.Recipe crystal = Catalogue.current.appearances.get("cox_crystal_chest");
		Catalogue.Recipe chest = Catalogue.current.appearances.get("cox_ancient_chest");
		assertEquals("CoX Crystal Chest", crystal.name);
		assertEquals("CoX Chest", chest.name);
		assertEquals(32752, crystal.transitionModelId);
		assertEquals(7506, crystal.transitionAnimationId);
		assertEquals(20, crystal.transitionHandoff);
		assertEquals(-1, chest.transitionModelId);
		assertTrue(Catalogue.current.appearances.get("cox_crystal_bomb").bobbing);
		assertFalse(chest.bobbing);
		assertEquals(crystal.open.getSourceObjectId(), chest.getSourceObjectId());
		assertArrayEquals(new int[] {32752, 32755}, crystal.closed.getModelIds());
		assertArrayEquals(new int[] {32755}, chest.closed.getModelIds());
		assertArrayEquals(new int[] {32756}, chest.open.getModelIds());
		assertNotEquals(crystal.placements.get("fancy_dress_box"),
			chest.placements.get("fancy_dress_box"));
	}

	@Test
	public void authoritativeCalibrationsAreCompiledIn()
	{
		Catalogue.Calibration portal = calibration(PohTargetSlot.ENTRANCE_PORTAL, 41807);
		assertEquals(1024, portal.getRotation());
		assertEquals(232, portal.getScaleX());
		assertEquals(84, portal.getOffsetHeight());

		Catalogue.Calibration chest = calibration(PohTargetSlot.TREASURE_CHEST, 30027);
		assertEquals(512, chest.getRotation());
		Catalogue.Calibration openChest = calibration(PohTargetSlot.TREASURE_CHEST, 30028);
		assertEquals(chest.getScaleX(), openChest.getScaleX());
		assertEquals(chest.getScaleHeight(), openChest.getScaleHeight());
		assertEquals(chest.getScaleY(), openChest.getScaleY());
		assertEquals(128, chest.getScaleX());
		assertEquals(192, chest.getScaleY());
		assertEquals(16, chest.getOffsetY());

		Catalogue.Calibration sarcophagus = calibration(PohTargetSlot.TREASURE_CHEST, 44825);
		assertTrue(sarcophagus.isFlipX());
		assertFalse(calibration(PohTargetSlot.TOY_BOX, 44825).isFlipX());
		assertEquals(-85, sarcophagus.signedScaleX());
		assertEquals(80, sarcophagus.getScaleY());
		assertEquals(
			calibration(PohTargetSlot.TOY_BOX, 29742).getRotation() & 2047,
			calibration(PohTargetSlot.TREASURE_CHEST, 29742).getRotation() & 2047);
		assertEquals(calibration(PohTargetSlot.TOY_BOX, 29742),
			calibration(PohTargetSlot.TREASURE_CHEST, 29742));
		assertNotEquals(calibration(PohTargetSlot.TOY_BOX, 44825), sarcophagus);
		assertNotNull(definition(33012));
		assertArrayEquals(new int[] {35412}, definition(33012).getModelIds());
		assertNull(definition(33013));
		assertEquals(1024, calibration(PohTargetSlot.ARMOUR_CASE, 33012).getRotation());
	}

	@Test
	public void dropdownAllowlistsMatchCataloguePlacements()
	{
		assertEquals(34, EntranceAppearance.values().length - 1);
		assertEquals(17, StorageAppearance.values().length - 1);
		assertEquals(19, ArmourAppearance.values().length - 1);
		assertEquals(18, ChestAppearance.values().length - 1);
		assertEquals(15, WardrobeAppearance.values().length - 1);

		assertTrue(isAllowed(PohTargetSlot.TREASURE_CHEST, 32991));
		assertFalse(isAllowed(PohTargetSlot.CAPE_RACK, 32991));
		assertFalse(isAllowed(PohTargetSlot.ARMOUR_CASE, 33125));
		assertFalse(isAllowed(PohTargetSlot.MAGIC_WARDROBE, 41724));
		assertFalse(isAllowed(PohTargetSlot.MAGIC_WARDROBE, 44825));
		assertTrue(isAllowed(PohTargetSlot.MAGIC_WARDROBE, 6282));
		for (int id : new int[] {4928})
		{
			assertTrue(isAllowed(PohTargetSlot.CAPE_RACK, id));
			assertTrue(isAllowed(PohTargetSlot.ARMOUR_CASE, id));
			assertTrue(isAllowed(PohTargetSlot.TOY_BOX, id));
			assertTrue(isAllowed(PohTargetSlot.TREASURE_CHEST, id));
			assertTrue(isAllowed(PohTargetSlot.FANCY_DRESS_BOX, id));
			assertFalse(isAllowed(PohTargetSlot.MAGIC_WARDROBE, id));
			assertFalse(isAllowed(PohTargetSlot.ENTRANCE_PORTAL, id));
		}
		assertTrue(isAllowed(PohTargetSlot.CAPE_RACK, 32996));
		assertFalse(isAllowed(PohTargetSlot.ARMOUR_CASE, 32996));
		assertFalse(isAllowed(PohTargetSlot.TOY_BOX, 32996));
		assertFalse(isAllowed(PohTargetSlot.TREASURE_CHEST, 32996));
		assertFalse(isAllowed(PohTargetSlot.FANCY_DRESS_BOX, 32996));
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
			definition(61216).getPortalColours());
		assertArrayEquals(new short[] {(short) 38040, (short) 38053, (short) 38309, (short) 38315},
			definition(42819).getNodeColours());
		assertEquals(11, definition(42819).getNodeGreyColours().length);
		assertEquals(4, definition(29794).getCrystalColours().length);
		assertEquals(4, definition(29757).getCrystalColours().length);
		assertEquals(3, definition(29766).getCrystalColours().length);
		assertEquals(2, definition(30027).getCrystalColours().length);
		short[] gauntletChestColours = {
			(short) 32916, (short) 32922, (short) 32926, (short) 32200,
			(short) 29518, (short) 29526, (short) 31192,
			(short) 26776};
		assertArrayEquals(gauntletChestColours,
			definition(36087).getGauntletColours());
		assertArrayEquals(gauntletChestColours,
			definition(36088).getGauntletColours());
		assertArrayEquals(new short[] {(short) 55219},
			definition(32991).getTobColours());
		assertArrayEquals(definition(32991).getTobColours(),
			definition(41746).getTobColours());
		assertArrayEquals(new short[] {(short) 54177},
			definition(4928).getCrystalColours());
		assertArrayEquals(new short[] {
			(short) 53582, (short) 52403, (short) 52407, (short) 52416,
			(short) 52424, (short) 51515, (short) 51484},
			definition(32996).getCrystalColours());
		assertArrayEquals(new short[] {(short) 960, (short) 794, (short) 914},
			definition(33125).getDeadmanColours());
		assertArrayEquals(definition(33125).getDeadmanColours(),
			definition(31583).getDeadmanColours());
		assertEquals(23, definition(44825).getToaColours().length);
		short[] toaChestColours = definition(44788).getToaColours();
		assertEquals(17, toaChestColours.length);
		assertArrayEquals(toaChestColours,
			definition(44789).getToaColours());
		assertArrayEquals(toaChestColours,
			definition(41696).getToaColours());
		assertArrayEquals(toaChestColours,
			definition(44791).getToaColours());
		for (int chestColour : new int[] {6315, 6348, 6592, 6674, 6817, 6819,
			6823, 6825, 6827, 6833, 6837, 6839, 6848, 6856, 6864, 6868, 6872})
		{
			assertTrue(contains(toaChestColours, (short) chestColour));
		}
		for (int coinColour : new int[] {7384, 7690, 7349, 7343, 7506, 7500,
			7492, 8123, 7616, 5943, 6986})
		{
			assertFalse(contains(definition(44825).getToaColours(),
				(short) coinColour));
			assertFalse(contains(toaChestColours, (short) coinColour));
		}
		for (int retainedColour : new int[] {5281, 5293, 6336, 6379, 6976})
		{
			assertFalse(contains(definition(44825).getToaColours(),
				(short) retainedColour));
		}
		assertEquals(14, PohCosmeticTransmogsConfig.AppearanceColour.GREEN.getHue());
	}

	@Test
	public void crystalModelsUseCalibratedTransforms()
	{
		assertArrayEquals(new int[] {4895}, definition(4928).getModelIds());
		assertEquals("Crystal Outcrop", Catalogue.name(key(4928)));
		assertNull(definition(4927));
		for (PohTargetSlot slot : PohTargetSlot.values())
		{
			assertFalse(isAllowed(slot, 4927));
		}
		assertArrayEquals(new int[] {35449}, definition(32996).getModelIds());
		assertEquals(160, calibration(PohTargetSlot.TREASURE_CHEST, 4928).getScaleX());
		assertEquals(96, calibration(PohTargetSlot.TREASURE_CHEST, 4928).getScaleY());
		assertEquals(calibration(PohTargetSlot.TREASURE_CHEST, 4928),
			calibration(PohTargetSlot.TOY_BOX, 4928));

		Catalogue.Calibration crystal =
			calibration(PohTargetSlot.CAPE_RACK, 32996);
		assertEquals(128, crystal.getScaleX());
		assertEquals(128, crystal.getScaleHeight());
		assertEquals(128, crystal.getScaleY());

		assertEquals(192, calibration(PohTargetSlot.ARMOUR_CASE, 29766).getScaleX());
		assertEquals(192, calibration(PohTargetSlot.CAPE_RACK, 29766).getScaleX());
		assertEquals(180, calibration(PohTargetSlot.ARMOUR_CASE, 29766).getScaleHeight());
		assertEquals(-4, calibration(PohTargetSlot.ARMOUR_CASE, 29766).getOffsetHeight());
		assertEquals(180, calibration(PohTargetSlot.CAPE_RACK, 29766).getScaleHeight());
		assertEquals(336, calibration(PohTargetSlot.TOY_BOX, 29766).getScaleX());
		assertEquals(210, calibration(PohTargetSlot.TOY_BOX, 29766).getScaleHeight());
		assertEquals(-4, calibration(PohTargetSlot.TOY_BOX, 29766).getOffsetHeight());
		assertEquals(336, calibration(PohTargetSlot.TREASURE_CHEST, 29766).getScaleX());
		assertEquals(210, calibration(PohTargetSlot.TREASURE_CHEST, 29766).getScaleHeight());
		assertEquals(384, calibration(PohTargetSlot.FANCY_DRESS_BOX, 29766).getScaleX());
		assertEquals(240, calibration(PohTargetSlot.FANCY_DRESS_BOX, 29766).getScaleHeight());

		Catalogue.Calibration tob =
			calibration(PohTargetSlot.FANCY_DRESS_BOX, 32991);
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
		assertNotEquals(stateDefinition(30028, false), stateDefinition(30028, true));
		assertArrayEquals(new int[] {32755},
			stateDefinition(30028, false).getModelIds());
		assertArrayEquals(new int[] {32756},
			stateDefinition(30028, true).getModelIds());
		assertEquals(-512, calibration(PohTargetSlot.FANCY_DRESS_BOX, 30027).getRotation());
		assertEquals(512, calibration(PohTargetSlot.FANCY_DRESS_BOX, 30028).getRotation());
		assertEquals(9505, definition(44934).getSpawnAnimationId());
		assertEquals(14167, definition(61216).getSpawnAnimationId());
		assertEquals(7823, definition(33125).getSpawnAnimationId());
		assertTrue(definition(33125).isSpawnOnce());
		assertNotEquals(stateDefinition(33125, false), stateDefinition(33125, true));
		assertEquals(stateDefinition(29766, false), stateDefinition(29766, true));
		assertEquals(-1, calibration(PohTargetSlot.CAPE_RACK, 33125).getOffsetHeight());
		assertEquals(-1, calibration(PohTargetSlot.FANCY_DRESS_BOX, 33125).getOffsetHeight());
		assertEquals(-1, calibration(PohTargetSlot.TREASURE_CHEST, 33125).getOffsetHeight());
	}

	@Test
	public void targetFamiliesContainEveryConstructedVariant()
	{
		assertEquals(2, target(PohTargetSlot.ENTRANCE_PORTAL).objectIds.length);
		assertEquals(6, target(PohTargetSlot.CAPE_RACK).objectIds.length);
		assertEquals(6, target(PohTargetSlot.FANCY_DRESS_BOX).objectIds.length);
		assertEquals(6, target(PohTargetSlot.ARMOUR_CASE).objectIds.length);
		assertEquals(14, target(PohTargetSlot.MAGIC_WARDROBE).objectIds.length);
		assertEquals(6, target(PohTargetSlot.TOY_BOX).objectIds.length);
		assertEquals(6, target(PohTargetSlot.TREASURE_CHEST).objectIds.length);
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

	private static Catalogue.Calibration calibration(PohTargetSlot furniture, int objectId)
	{
		Catalogue.Definition definition = definition(objectId);
		assertNotNull(definition);
		return Catalogue.ModelFactory.calibration(Catalogue.current.appearances.get(key(objectId)),
			target(furniture), target(furniture).sizeX, target(furniture).sizeY);
	}

	private static int state(int sourceObjectId, int targetObjectId)
	{
		return stateDefinition(sourceObjectId, isOpen(targetObjectId)).getSourceObjectId();
	}

	private static <T extends Enum<T> & AppearanceOption> void assertRecipes(T[] appearances)
	{
		for (T appearance : appearances)
		{
			if (!appearance.getAppearanceKey().isEmpty())
			{
				Catalogue.Definition definition =
					Catalogue.current.appearances.get(appearance.getAppearanceKey());
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
	private static TargetSpec target(PohTargetSlot slot)
	{
		return Catalogue.current.targets.get(slot.getTargetKey());
	}

	private static boolean isAllowed(PohTargetSlot slot, int id)
	{
		String key = key(id);
		return !key.isEmpty() && ((AppearanceOption) slot.option(key)).getAppearanceKey().equals(key);
	}

	private static String key(int id)
	{
		for (Catalogue.Recipe recipe : Catalogue.current.appearances.values())
		{
			if (recipe.sourceObjectId == id)
			{
				return recipe.key;
			}
		}
		return "";
	}

	private static Catalogue.Definition definition(int id)
	{
		for (Catalogue.Recipe recipe : Catalogue.current.appearances.values())
		{
			for (Catalogue.Definition state : new Catalogue.Definition[] {recipe, recipe.closed, recipe.open})
			{
				if (state.sourceObjectId == id)
				{
					return state;
				}
			}
		}
		return null;
	}

	private static Catalogue.Definition stateDefinition(int id, boolean opened)
	{
		return Catalogue.current.appearances.get(key(id)).state(opened);
	}

	private static boolean isOpen(int id)
	{
		return Catalogue.current.targets.values().stream().anyMatch(target -> target.isOpen(id));
	}}
