package com.pohcosmetictransmogs;

import org.junit.Test;

import static org.junit.Assert.*;

public class TargetSpecTest
{
	@Test
	public void stateIsDeclaredByTheTarget()
	{
		TargetSpec target = new TargetSpec();
		target.objectIds = new int[] {10};
		assertFalse(target.isStateful());
		assertFalse(target.isOpen(10));
		target.objectIds = new int[] {10, 11, 12, 13};
		target.openObjectIds = new int[] {11, 13};
		assertTrue(target.isStateful());
		assertFalse(target.isOpen(10));
		assertTrue(target.isOpen(11));
		assertTrue(target.isOpen(13));
		assertFalse(target.isOpen(18809));
	}
}
