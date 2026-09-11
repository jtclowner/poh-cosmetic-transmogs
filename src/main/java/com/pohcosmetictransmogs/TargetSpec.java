package com.pohcosmetictransmogs;

import java.util.Arrays;

/** Scene IDs and placement semantics for one logical target. */
final class TargetSpec
{
	String key;
	int[] objectIds = {};
	int[] openObjectIds = {};
	int sizeX;
	int sizeY;
	int orientationOffset;
	boolean scaleTransition;
	boolean portalRecolour;

	boolean isOpen(int objectId)
	{
		return Arrays.stream(openObjectIds).anyMatch(id -> id == objectId);
	}

	boolean isStateful()
	{
		return openObjectIds.length > 0;
	}

	static TargetSpec from(PohFurniture slot)
	{
		TargetSpec target = new TargetSpec();
		target.key = slot.name().toLowerCase(java.util.Locale.ROOT);
		target.objectIds = slot.getObjectIds();
		target.openObjectIds = Arrays.stream(target.objectIds).filter(PohFurniture::isOpenState).toArray();
		target.sizeX = slot.getSizeX();
		target.sizeY = slot.getSizeY();
		target.orientationOffset = slot.getOrientationOffset();
		target.scaleTransition = slot != PohFurniture.ARMOUR_CASE && slot != PohFurniture.MAGIC_WARDROBE;
		target.portalRecolour = slot == PohFurniture.ENTRANCE_PORTAL;
		return target;
	}
}
