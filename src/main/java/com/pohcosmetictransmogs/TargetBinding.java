package com.pohcosmetictransmogs;

/** One selected recipe shared by every scene ID of a logical target. */
final class TargetBinding
{
	final TargetSpec target;
	final Catalogue.Recipe appearance;

	TargetBinding(TargetSpec target, Catalogue.Recipe appearance)
	{
		this.target = target;
		this.appearance = appearance;
	}

	Catalogue.Definition state(int objectId)
	{
		return target.isStateful() ? appearance.state(target.isOpen(objectId)) : appearance;
	}

	Catalogue.Calibration calibration(int sizeX, int sizeY)
	{
		return Catalogue.ModelFactory.calibration(appearance, target,
			target.sizeX > 0 ? target.sizeX : sizeX,
			target.sizeY > 0 ? target.sizeY : sizeY);
	}
}
