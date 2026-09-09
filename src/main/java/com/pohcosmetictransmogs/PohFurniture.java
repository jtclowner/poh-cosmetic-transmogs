package com.pohcosmetictransmogs;

import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.AppearanceOption;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.ArmourAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.ChestAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.EntranceAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.StorageAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.WardrobeAppearance;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;

enum PohFurniture
{
	// Only the entrance portal and costume-room furniture are eligible targets.
	// Official "GP sink" or gamemode reward cosmetic targets (portal-space portals,
	// portal nexuses, scrying pools, spirit trees, thrones, house styles and achievement
	// gallery/leagues hall accomplishment furniture) will remain outside this target allowlist.
	ENTRANCE_PORTAL("entrancePortal", 2, 2, 0, 4525),
	CAPE_RACK("capeRack", 1, 1, 0, 18766, 18767, 18768, 18769, 18770, 18771),
	TREASURE_CHEST("treasureChest", 2, 1, -1024, 18804, 18805, 18806, 18807, 18808, 18809),
	FANCY_DRESS_BOX("fancyDressBox", 2, 2, 0, 18772, 18773, 18774, 18775, 18776, 18777),
	ARMOUR_CASE("armourCase", 1, 1, 512, 18778, 18779, 18780, 18781, 18782, 18783),
	TOY_BOX("toyBox", 2, 1, -1024, 18798, 18799, 18800, 18801, 18802, 18803),
	MAGIC_WARDROBE("magicWardrobe", 1, 3, 512,
		18784, 18785, 18786, 18787, 18788, 18789, 18790,
		18791, 18792, 18793, 18794, 18795, 18796, 18797);

	private static final Map<Integer, PohFurniture> BY_OBJECT_ID;
	private static final Set<Integer> OPEN_STATE_OBJECT_IDS = new HashSet<>(Arrays.asList(
		18773, 18775, 18777,
		18779, 18781, 18783,
		18785, 18787, 18789, 18791, 18793, 18795, 18797,
		18799, 18801, 18803,
		18805, 18807, 18809));

	static
	{
		Map<Integer, PohFurniture> byId = new HashMap<>();
		for (PohFurniture furniture : values())
		{
			for (int objectId : furniture.objectIds)
			{
				if (byId.put(objectId, furniture) != null)
				{
					throw new IllegalStateException("Duplicate PoH furniture object id " + objectId);
				}
			}
		}
		BY_OBJECT_ID = Collections.unmodifiableMap(byId);
	}

	@Getter(AccessLevel.PACKAGE)
	private final String configKey;
	@Getter(AccessLevel.PACKAGE)
	private final int sizeX;
	@Getter(AccessLevel.PACKAGE)
	private final int sizeY;
	@Getter(AccessLevel.PACKAGE)
	private final int orientationOffset;
	private final int[] objectIds;

	PohFurniture(String configKey, int sizeX, int sizeY, int orientationOffset, int... objectIds)
	{
		this.configKey = configKey;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.orientationOffset = orientationOffset;
		this.objectIds = objectIds;
	}

	static PohFurniture fromConfigKey(String key)
	{
		for (PohFurniture furniture : values())
		{
			if (furniture.configKey.equals(key))
			{
				return furniture;
			}
		}
		return null;
	}

	static PohFurniture fromObjectId(int objectId)
	{
		return BY_OBJECT_ID.get(objectId);
	}

	static int orientationOffset(int objectId)
	{
		PohFurniture furniture = fromObjectId(objectId);
		return furniture == null ? 0 : furniture.orientationOffset;
	}

	static boolean isOpenState(int objectId)
	{
		return OPEN_STATE_OBJECT_IDS.contains(objectId);
	}

	static Map<PohFurniture, Integer> emptySelections()
	{
		Map<PohFurniture, Integer> selections = new EnumMap<>(PohFurniture.class);
		for (PohFurniture furniture : values())
		{
			selections.put(furniture, -1);
		}
		return selections;
	}

	static boolean isAllowed(PohFurniture furniture, int sourceObjectId)
	{
		return sourceObjectId == -1 || ((AppearanceOption) furniture.option(sourceObjectId)).getObjectId() != -1;
	}

	Enum<?> option(int sourceObjectId)
	{
		switch (this)
		{
			case ENTRANCE_PORTAL: return AppearanceOption.find(EntranceAppearance.values(), sourceObjectId);
			case MAGIC_WARDROBE: return AppearanceOption.find(WardrobeAppearance.values(), sourceObjectId);
			case TREASURE_CHEST:
			case TOY_BOX:
			case FANCY_DRESS_BOX: return AppearanceOption.find(ChestAppearance.values(), sourceObjectId);
			case ARMOUR_CASE: return AppearanceOption.find(ArmourAppearance.values(), sourceObjectId);
			default: return AppearanceOption.find(StorageAppearance.values(), sourceObjectId);
		}
	}

	int getRepresentativeOpenObjectId()
	{
		for (int objectId : objectIds)
		{
			if (isOpenState(objectId))
			{
				return objectId;
			}
		}
		return -1;
	}

	int[] getObjectIds()
	{
		return Arrays.copyOf(objectIds, objectIds.length);
	}
}
