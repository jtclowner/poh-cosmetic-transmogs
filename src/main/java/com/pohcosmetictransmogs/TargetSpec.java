package com.pohcosmetictransmogs;

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
	FitMode defaultFitMode = FitMode.NONE;

	enum FitMode
	{
		NONE,
		FOOTPRINT
	}

	boolean isOpen(int objectId)
	{
		for (int id : openObjectIds)
		{
			if (id == objectId)
			{
				return true;
			}
		}
		return false;
	}

	boolean isStateful()
	{
		return openObjectIds.length > 0;
	}

}
