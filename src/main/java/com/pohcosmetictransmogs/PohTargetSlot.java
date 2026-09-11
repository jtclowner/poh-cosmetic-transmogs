package com.pohcosmetictransmogs;

import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.AppearanceOption;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.ArmourAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.ChestAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.EntranceAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.StorageAppearance;
import com.pohcosmetictransmogs.PohCosmeticTransmogsConfig.WardrobeAppearance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Configuration slots reference target keys; target mechanics belong to the catalogue. */
@Getter
@RequiredArgsConstructor
enum PohTargetSlot
{
	ENTRANCE_PORTAL("entrancePortal", "entrance_portal"),
	CAPE_RACK("capeRack", "cape_rack"),
	TREASURE_CHEST("treasureChest", "treasure_chest"),
	FANCY_DRESS_BOX("fancyDressBox", "fancy_dress_box"),
	ARMOUR_CASE("armourCase", "armour_case"),
	TOY_BOX("toyBox", "toy_box"),
	MAGIC_WARDROBE("magicWardrobe", "magic_wardrobe");

	private final String configKey;
	private final String targetKey;

	static boolean isConfigKey(String key)
	{
		for (PohTargetSlot slot : values())
		{
			if (slot.configKey.equals(key))
			{
				return true;
			}
		}
		return false;
	}
	Enum<?> option(String appearanceKey)
	{
		switch (this)
		{
			case ENTRANCE_PORTAL: return AppearanceOption.find(EntranceAppearance.values(), appearanceKey);
			case MAGIC_WARDROBE: return AppearanceOption.find(WardrobeAppearance.values(), appearanceKey);
			case TREASURE_CHEST:
			case TOY_BOX:
			case FANCY_DRESS_BOX: return AppearanceOption.find(ChestAppearance.values(), appearanceKey);
			case ARMOUR_CASE: return AppearanceOption.find(ArmourAppearance.values(), appearanceKey);
			default: return AppearanceOption.find(StorageAppearance.values(), appearanceKey);
		}
	}

}
