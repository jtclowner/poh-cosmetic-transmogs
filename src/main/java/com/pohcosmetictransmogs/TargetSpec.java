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

}
