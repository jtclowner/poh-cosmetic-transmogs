package com.pohcosmetictransmogs;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(PohCosmeticTransmogsConfig.GROUP)
public interface PohCosmeticTransmogsConfig extends Config
{
	String GROUP = "pohfurnituretransmogs";

	@ConfigSection(
		name = "Entrance",
		description = "PoH entrance portal appearance",
		position = 10
	)
	String ENTRANCE_SECTION = "entrance";

	@ConfigSection(
		name = "Costume room",
		description = "Costume-room furniture appearances",
		position = 20
	)
	String COSTUME_ROOM_SECTION = "costumeRoom";

	@ConfigSection(
		name = "Recolours",
		description = "Colour overrides for supported appearances",
		position = 30
	)
	String RECOLOURS_SECTION = "recolours";

	@ConfigSection(
		name = "Miscellaneous",
		description = "Other appearance options",
		position = 40
	)
	String MISC_SECTION = "miscellaneous";

	@ConfigItem(
		keyName = "hideFurnitureTransmogs",
		name = "Hide furniture transmogs",
		description = "Temporarily show the original PoH furniture",
		position = 0
	)
	default boolean hideFurnitureTransmogs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "outlineOriginalClickbox",
		name = "Outline original shape",
		description = "Outline the original furniture's interactive shape",
		position = 1
	)
	default boolean outlineOriginalShape()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "originalClickboxColor",
		name = "Outline colour",
		description = "Colour of the original furniture shape outline",
		position = 2
	)
	default Color originalShapeColor()
	{
		return Color.CYAN;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "originalClickboxWidth",
		name = "Outline thickness",
		description = "Thickness of the original furniture shape outline",
		position = 3
	)
	default int originalShapeWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "entrancePortal",
		name = "Entrance appearance",
		description = "Replacement for the PoH entrance portal",
		position = 0,
		section = ENTRANCE_SECTION
	)
	default EntranceAppearance entrancePortal()
	{
		return EntranceAppearance.ORIGINAL;
	}

	@ConfigItem(
		keyName = "portalColour",
		name = "Portal colour",
		description = "Colour of supported portal energy",
		position = 1,
		section = ENTRANCE_SECTION
	)
	default AppearanceColour portalColour()
	{
		return AppearanceColour.NATIVE;
	}

	@ConfigItem(
		keyName = "recolourPortalsInAllPositions",
		name = "Recolour all portals",
		description = "Also recolour portals used outside the entrance position",
		position = 2,
		section = ENTRANCE_SECTION
	)
	default boolean recolourPortalsInAllPositions()
	{
		return false;
	}

	@ConfigItem(
		keyName = "treasureChest",
		name = "Treasure chest",
		description = "Replacement for all treasure-chest tiers and open/closed states",
		position = 0,
		section = COSTUME_ROOM_SECTION
	)
	default ChestAppearance treasureChest()
	{
		return ChestAppearance.ORIGINAL;
	}

	@ConfigItem(
		keyName = "capeRack",
		name = "Cape rack",
		description = "Replacement for all six cape-rack tiers",
		position = 1,
		section = COSTUME_ROOM_SECTION
	)
	default StorageAppearance capeRack()
	{
		return StorageAppearance.ORIGINAL;
	}

	@ConfigItem(
		keyName = "fancyDressBox",
		name = "Fancy dress box",
		description = "Replacement for all fancy-dress-box tiers and open/closed states",
		position = 2,
		section = COSTUME_ROOM_SECTION
	)
	default ChestAppearance fancyDressBox()
	{
		return ChestAppearance.ORIGINAL;
	}

	@ConfigItem(
		keyName = "armourCase",
		name = "Armour case",
		description = "Replacement for all armour-case tiers and open/closed states",
		position = 3,
		section = COSTUME_ROOM_SECTION
	)
	default ArmourAppearance armourCase()
	{
		return ArmourAppearance.ORIGINAL;
	}

	@ConfigItem(
		keyName = "toyBox",
		name = "Toy box",
		description = "Replacement for all toy-box tiers and open/closed states",
		position = 4,
		section = COSTUME_ROOM_SECTION
	)
	default ChestAppearance toyBox()
	{
		return ChestAppearance.ORIGINAL;
	}

	@ConfigItem(
		keyName = "magicWardrobe",
		name = "Magic wardrobe",
		description = "Replacement for all magic-wardrobe tiers and open/closed states",
		position = 5,
		section = COSTUME_ROOM_SECTION
	)
	default WardrobeAppearance magicWardrobe()
	{
		return WardrobeAppearance.ORIGINAL;
	}

	@ConfigItem(
		keyName = "recolourColour",
		name = "Selected colour",
		description = "Colour used by the enabled recolours below",
		position = 0,
		section = RECOLOURS_SECTION
	)
	default AppearanceColour recolourColour()
	{
		return AppearanceColour.PURPLE;
	}

	@ConfigItem(
		keyName = "recolourCoxCrystals",
		name = "Recolour crystals",
		description = "Recolour supported crystal appearances",
		position = 1,
		section = RECOLOURS_SECTION
	)
	default boolean recolourCrystals()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recolourTobChest",
		name = "Recolour ToB chest",
		description = "Recolour the purple glow surrounding the ToB monumental chest",
		position = 2,
		section = RECOLOURS_SECTION
	)
	default boolean recolourTobChest()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recolourGauntletChest",
		name = "Recolour Gauntlet chest",
		description = "Recolour the Gauntlet reward chest accents",
		position = 3,
		section = RECOLOURS_SECTION
	)
	default boolean recolourGauntletChest()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recolourDeadmanChest",
		name = "Recolour Deadman chest",
		description = "Recolour the Deadman supply chest panels",
		position = 4,
		section = RECOLOURS_SECTION
	)
	default boolean recolourDeadmanChest()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recolourToaSarcophagus",
		name = "Recolour ToA containers",
		description = "Recolour supported ToA chest and sarcophagus accents",
		position = 5,
		section = RECOLOURS_SECTION
	)
	default boolean recolourToaContainers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recolourNodePortal",
		name = "Match Node Portal to account",
		description = "Match the Node Portal helm to the logged-in account type",
		position = 0,
		section = MISC_SECTION
	)
	default boolean recolourNodePortal()
	{
		return false;
	}

	interface AppearanceOption
	{
		String getAppearanceKey();

		static <T extends Enum<T> & AppearanceOption> T find(T[] options, String appearanceKey)
		{

			for (T option : options)
			{
				if (option.getAppearanceKey().equals(appearanceKey))
				{
					return option;
				}
			}
			return options[0];
		}

	}

	@Getter
	@RequiredArgsConstructor
	enum AppearanceColour
	{
		// Orange is deliberately unavailable for entrance-portal recolouring. It is
		// reserved for the Wilderness house theme unlocked by a Deadman reward scroll.
		NATIVE(-1, "Native"),
		RED(0, "Red"),
		YELLOW(10, "Yellow"),
		GREEN(14, "Green"),
		BLUE(42, "Blue"),
		PURPLE(52, "Purple");

		private final int hue;
		private final String displayName;

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum EntranceAppearance implements AppearanceOption
	{
		ORIGINAL(""),
		A_KINGDOM_DIVIDED_PORTAL("a_kingdom_divided_portal"),
		ABYSSAL_PORTAL("abyssal_portal"),
		CASTLE_DRAKAN_PORTAL("castle_drakan_portal"),
		CHRISTMAS_PORTAL("christmas_portal"),
		CIRCLE_1("circle_1"),
		CIRCLE_2("circle_2"),
		CLAN_HALL_PORTAL("clan_hall_portal"),
		COX_OLM_BARRIER("cox_olm_barrier"),
		DT2_PORTAL("dt2_portal"),
		ELVEN_PORTAL("elven_portal"),
		EVIL_CHICKEN_LAIR_PORTAL("evil_chicken_lair_portal"),
		FEROX_DARK_PORTAL("ferox_dark_portal"),
		FEROX_LIGHT_PORTAL("ferox_light_portal"),
		FEROX_RED_PORTAL("ferox_red_portal"),
		GAUNTLET_PORTAL("gauntlet_portal"),
		NODE_PORTAL("node_portal"),
		GOLEM_PORTAL("golem_portal"),
		GWENITH_GLIDE_PORTAL("gwenith_glide_portal"),
		INFERNAL_RIFT("infernal_rift"),
		ISLE_OF_SOULS_DARK_PORTAL("isle_of_souls_dark_portal"),
		ISLE_OF_SOULS_DEAD_TREES("isle_of_souls_dead_trees"),
		ISLE_OF_SOULS_LIGHT_PORTAL("isle_of_souls_light_portal"),
		MYTHS_GUILD_PORTAL("myths_guild_portal"),
		PEST_CONTROL_BLUE_PORTAL("pest_control_blue_portal"),
		PEST_CONTROL_PURPLE_PORTAL("pest_control_purple_portal"),
		PEST_CONTROL_RED_PORTAL("pest_control_red_portal"),
		PEST_CONTROL_YELLOW_PORTAL("pest_control_yellow_portal"),
		PRIFDDINAS_AGILITY_PORTAL("prifddinas_agility_portal"),
		RAGING_ECHOES_PORTAL("raging_echoes_portal"),
		RECIPE_FOR_DISASTER_PORTAL("recipe_for_disaster_portal"),
		SEPULCHRE_PORTAL("sepulchre_portal"),
		SHADOW_REALM_PORTAL("shadow_realm_portal"),
		TROUBLE_BREWING_PORTAL("trouble_brewing_portal"),
		WIZARDS_TOWER_PORTAL("wizards_tower_portal");

		private final String appearanceKey;

		@Override
		public String toString()
		{
			return Catalogue.name(appearanceKey);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum StorageAppearance implements AppearanceOption
	{
		ORIGINAL(""),
		COX_BANK_CHEST("cox_bank_chest"),
		COX_CRYSTAL_BOMB("cox_crystal_bomb"),
		COX_ANCIENT_CHEST("cox_ancient_chest"),
		COX_CRYSTAL_CHEST("cox_crystal_chest"),
		COX_LARGE_CRYSTAL("cox_large_crystal"),
		COX_LARGE_STORAGE("cox_large_storage"),
		COX_MASSIVE_STORAGE("cox_massive_storage"),
		COX_MEDIUM_STORAGE("cox_medium_storage"),
		COX_SMALL_STORAGE("cox_small_storage"),
		COX_THIEVING_CHEST("cox_thieving_chest"),
		CRYSTAL_OUTCROP_1("crystal_outcrop"),
		COLOURLESS_CRYSTAL("colourless_crystal"),
		DEADMAN_SUPPLY_CHEST("deadman_supply_chest"),
		GAUNTLET_REWARD_CHEST("gauntlet_reward_chest"),
		TOA_CHEST_1("toa_chest_1"),
		TOA_CHEST_2("toa_chest_2"),
		TOB_TELEPORT_CRYSTAL("tob_teleport_crystal");

		private final String appearanceKey;

		@Override
		public String toString()
		{
			return Catalogue.name(appearanceKey);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum ArmourAppearance implements AppearanceOption
	{
		ORIGINAL(""),
		ARMOUR_DISPLAY("armour_display"),
		ARMADYL_ARMOUR("armadyl_armour"),
		BANDOS_ARMOUR("bandos_armour"),
		JUSTICIAR_ARMOUR("justiciar_armour"),
		COX_BANK_CHEST("cox_bank_chest"),
		COX_ANCIENT_CHEST("cox_ancient_chest"),
		COX_LARGE_CRYSTAL("cox_large_crystal"),
		COX_CRYSTAL_CHEST("cox_crystal_chest"),
		COX_LARGE_STORAGE("cox_large_storage"),
		COX_MASSIVE_STORAGE("cox_massive_storage"),
		COX_MEDIUM_STORAGE("cox_medium_storage"),
		COX_SMALL_STORAGE("cox_small_storage"),
		COX_THIEVING_CHEST("cox_thieving_chest"),
		COX_CRYSTAL_BOMB("cox_crystal_bomb"),
		CRYSTAL_OUTCROP_1("crystal_outcrop"),
		COLOURLESS_CRYSTAL("colourless_crystal"),
		GAUNTLET_REWARD_CHEST("gauntlet_reward_chest"),
		TOA_CHEST_1("toa_chest_1"),
		TOA_CHEST_2("toa_chest_2");

		private final String appearanceKey;

		@Override
		public String toString()
		{
			return Catalogue.name(appearanceKey);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum ChestAppearance implements AppearanceOption
	{
		ORIGINAL(""),
		DEADMAN_SUPPLY_CHEST("deadman_supply_chest"),
		GAUNTLET_REWARD_CHEST("gauntlet_reward_chest"),
		TOA_CHEST_1("toa_chest_1"),
		TOA_CHEST_2("toa_chest_2"),
		TOA_SARCOPHAGUS("toa_sarcophagus"),
		TOB_MONUMENTAL_CHEST("tob_monumental_chest"),
		COX_ANCIENT_CHEST("cox_ancient_chest"),
		COX_BANK_CHEST("cox_bank_chest"),
		COX_SMALL_STORAGE("cox_small_storage"),
		COX_MEDIUM_STORAGE("cox_medium_storage"),
		COX_LARGE_STORAGE("cox_large_storage"),
		COX_MASSIVE_STORAGE("cox_massive_storage"),
		COX_THIEVING_CHEST("cox_thieving_chest"),
		COX_CRYSTAL_CHEST("cox_crystal_chest"),
		COX_CRYSTAL_BOMB("cox_crystal_bomb"),
		COX_LARGE_CRYSTAL("cox_large_crystal"),
		COLOURLESS_CRYSTAL("colourless_crystal"),
		CRYSTAL_OUTCROP_1("crystal_outcrop");

		private final String appearanceKey;

		@Override
		public String toString()
		{
			return Catalogue.name(appearanceKey);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum WardrobeAppearance implements AppearanceOption
	{
		ORIGINAL(""),
		CHRISTMAS_PORTAL("christmas_portal"),
		DT2_PORTAL("dt2_portal"),
		ELVEN_PORTAL("elven_portal"),
		EVIL_CHICKEN_LAIR_PORTAL("evil_chicken_lair_portal"),
		FEROX_DARK_PORTAL("ferox_dark_portal"),
		FEROX_LIGHT_PORTAL("ferox_light_portal"),
		FEROX_RED_PORTAL("ferox_red_portal"),
		GAUNTLET_PORTAL("gauntlet_portal"),
		GOLEM_PORTAL("golem_portal"),
		ISLE_OF_SOULS_DARK_PORTAL("isle_of_souls_dark_portal"),
		ISLE_OF_SOULS_DEAD_TREES("isle_of_souls_dead_trees"),
		ISLE_OF_SOULS_LIGHT_PORTAL("isle_of_souls_light_portal"),
		MYTHS_GUILD_PORTAL("myths_guild_portal"),
		PRIFDDINAS_AGILITY_PORTAL("prifddinas_agility_portal"),
		RAGING_ECHOES_PORTAL("raging_echoes_portal");

		private final String appearanceKey;

		@Override
		public String toString()
		{
			return Catalogue.name(appearanceKey);
		}
	}
}
