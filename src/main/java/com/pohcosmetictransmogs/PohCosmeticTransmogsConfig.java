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
		int getObjectId();

		static <T extends Enum<T> & AppearanceOption> T find(T[] options, int objectId)
		{
			int id = PohAppearanceCatalog.canonicalSelectionId(objectId);
			for (T option : options)
			{
				if (option.getObjectId() == id)
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
		ORIGINAL(-1),
		A_KINGDOM_DIVIDED_PORTAL(41807),
		ABYSSAL_PORTAL(49242),
		CASTLE_DRAKAN_PORTAL(61216),
		CHRISTMAS_PORTAL(46442),
		CIRCLE_1(47346),
		CIRCLE_2(4408),
		CLAN_HALL_PORTAL(41724),
		COX_OLM_BARRIER(29879),
		DT2_PORTAL(12334),
		ELVEN_PORTAL(34947),
		EVIL_CHICKEN_LAIR_PORTAL(12260),
		FEROX_DARK_PORTAL(26642),
		FEROX_LIGHT_PORTAL(26732),
		FEROX_RED_PORTAL(26727),
		GAUNTLET_PORTAL(36081),
		NODE_PORTAL(42819),
		GOLEM_PORTAL(6282),
		GWENITH_GLIDE_PORTAL(58942),
		INFERNAL_RIFT(56397),
		ISLE_OF_SOULS_DARK_PORTAL(40460),
		ISLE_OF_SOULS_DEAD_TREES(40461),
		ISLE_OF_SOULS_LIGHT_PORTAL(40476),
		MYTHS_GUILD_PORTAL(31618),
		PEST_CONTROL_BLUE_PORTAL(1748),
		PEST_CONTROL_PURPLE_PORTAL(1747),
		PEST_CONTROL_RED_PORTAL(1750),
		PEST_CONTROL_YELLOW_PORTAL(1749),
		PRIFDDINAS_AGILITY_PORTAL(36240),
		RAGING_ECHOES_PORTAL(56074),
		RECIPE_FOR_DISASTER_PORTAL(12355),
		SEPULCHRE_PORTAL(38829),
		SHADOW_REALM_PORTAL(33037),
		TROUBLE_BREWING_PORTAL(15996),
		WIZARDS_TOWER_PORTAL(43765);

		private final int objectId;

		@Override
		public String toString()
		{
			return PohAppearanceCatalog.name(objectId);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum StorageAppearance implements AppearanceOption
	{
		ORIGINAL(-1),
		COX_BANK_CHEST(47419),
		COX_CRYSTAL_BOMB(29766),
		COX_ANCIENT_CHEST(30028),
		COX_CRYSTAL_CHEST(30027),
		COX_LARGE_CRYSTAL(29794),
		COX_LARGE_STORAGE(29780),
		COX_MASSIVE_STORAGE(37978),
		COX_MEDIUM_STORAGE(29779),
		COX_SMALL_STORAGE(29770),
		COX_THIEVING_CHEST(29742),
		CRYSTAL_OUTCROP_1(4928),
		COLOURLESS_CRYSTAL(29757),
		DEADMAN_SUPPLY_CHEST(33125),
		GAUNTLET_REWARD_CHEST(36087),
		TOA_CHEST_1(44788),
		TOA_CHEST_2(41696),
		TOB_TELEPORT_CRYSTAL(32996);

		private final int objectId;

		@Override
		public String toString()
		{
			return PohAppearanceCatalog.name(objectId);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum ArmourAppearance implements AppearanceOption
	{
		ORIGINAL(-1),
		ARMOUR_DISPLAY(21447),
		ARMADYL_ARMOUR(31904),
		BANDOS_ARMOUR(31903),
		JUSTICIAR_ARMOUR(33012),
		COX_BANK_CHEST(47419),
		COX_ANCIENT_CHEST(30028),
		COX_LARGE_CRYSTAL(29794),
		COX_CRYSTAL_CHEST(30027),
		COX_LARGE_STORAGE(29780),
		COX_MASSIVE_STORAGE(37978),
		COX_MEDIUM_STORAGE(29779),
		COX_SMALL_STORAGE(29770),
		COX_THIEVING_CHEST(29742),
		COX_CRYSTAL_BOMB(29766),
		CRYSTAL_OUTCROP_1(4928),
		COLOURLESS_CRYSTAL(29757),
		GAUNTLET_REWARD_CHEST(36087),
		TOA_CHEST_1(44788),
		TOA_CHEST_2(41696);

		private final int objectId;

		@Override
		public String toString()
		{
			return PohAppearanceCatalog.name(objectId);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum ChestAppearance implements AppearanceOption
	{
		ORIGINAL(-1),
		DEADMAN_SUPPLY_CHEST(33125),
		GAUNTLET_REWARD_CHEST(36087),
		TOA_CHEST_1(44788),
		TOA_CHEST_2(41696),
		TOA_SARCOPHAGUS(44825),
		TOB_MONUMENTAL_CHEST(32991),
		COX_ANCIENT_CHEST(30028),
		COX_BANK_CHEST(47419),
		COX_SMALL_STORAGE(29770),
		COX_MEDIUM_STORAGE(29779),
		COX_LARGE_STORAGE(29780),
		COX_MASSIVE_STORAGE(37978),
		COX_THIEVING_CHEST(29742),
		COX_CRYSTAL_CHEST(30027),
		COX_CRYSTAL_BOMB(29766),
		COX_LARGE_CRYSTAL(29794),
		COLOURLESS_CRYSTAL(29757),
		CRYSTAL_OUTCROP_1(4928);

		private final int objectId;

		@Override
		public String toString()
		{
			return PohAppearanceCatalog.name(objectId);
		}
	}

	@Getter
	@RequiredArgsConstructor
	enum WardrobeAppearance implements AppearanceOption
	{
		ORIGINAL(-1),
		CHRISTMAS_PORTAL(46442),
		DT2_PORTAL(12334),
		ELVEN_PORTAL(34947),
		EVIL_CHICKEN_LAIR_PORTAL(12260),
		FEROX_DARK_PORTAL(26642),
		FEROX_LIGHT_PORTAL(26732),
		FEROX_RED_PORTAL(26727),
		GAUNTLET_PORTAL(36081),
		GOLEM_PORTAL(6282),
		ISLE_OF_SOULS_DARK_PORTAL(40460),
		ISLE_OF_SOULS_DEAD_TREES(40461),
		ISLE_OF_SOULS_LIGHT_PORTAL(40476),
		MYTHS_GUILD_PORTAL(31618),
		PRIFDDINAS_AGILITY_PORTAL(36240),
		RAGING_ECHOES_PORTAL(56074);

		private final int objectId;

		@Override
		public String toString()
		{
			return PohAppearanceCatalog.name(objectId);
		}
	}
}
