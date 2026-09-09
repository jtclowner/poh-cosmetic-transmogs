package com.pohcosmetictransmogs;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

class PohCosmeticTransmogsOverlay extends Overlay
{
	private final PohCosmeticTransmogsConfig config;
	private final PohCosmeticTransmogsManager manager;
	private final Client client;
	private final Map<TileObject, Shape> shapes = new IdentityHashMap<>();
	private int cameraX = Integer.MIN_VALUE;
	private int cameraY;
	private int cameraZ;
	private int cameraPitch;
	private int cameraYaw;
	private int viewportWidth;
	private int viewportHeight;
	private int viewportXOffset;
	private int viewportYOffset;
	private int viewportScale;
	private int strokeWidth = -1;
	private Stroke stroke;

	@Inject
	private PohCosmeticTransmogsOverlay(
		PohCosmeticTransmogsConfig config, PohCosmeticTransmogsManager manager, Client client)
	{
		this.config = config;
		this.manager = manager;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public synchronized Dimension render(Graphics2D graphics)
	{
		refreshProjectionCache();
		graphics.setColor(config.originalShapeColor());
		int configuredStrokeWidth = config.originalShapeWidth();
		if (strokeWidth != configuredStrokeWidth)
		{
			strokeWidth = configuredStrokeWidth;
			stroke = new BasicStroke(configuredStrokeWidth);
		}
		graphics.setStroke(stroke);
		WorldView topLevel = client.getTopLevelWorldView();
		Set<TileObject> activeObjects = manager.getActiveObjects();
		shapes.keySet().removeIf(object -> !activeObjects.contains(object));
		for (TileObject object : activeObjects)
		{
			WorldView worldView = object.getWorldView();
			if (worldView == null || object.getPlane() != worldView.getPlane())
			{
				continue;
			}
			WorldEntity worldEntity = topLevel == null
				? null : topLevel.worldEntities().byIndex(worldView.getId());
			if (worldEntity != null && worldEntity.isHiddenForOverlap())
			{
				continue;
			}
			Shape shape = shapes.get(object);
			if (shape == null)
			{
				shape = object.getClickbox();
				if (shape != null)
				{
					shapes.put(object, shape);
				}
			}
			if (shape != null)
			{
				graphics.draw(shape);
			}
		}
		return null;
	}

	synchronized void reset()
	{
		shapes.clear();
		cameraX = Integer.MIN_VALUE;
	}

	private void refreshProjectionCache()
	{
		int newCameraX = client.getCameraX();
		int newCameraY = client.getCameraY();
		int newCameraZ = client.getCameraZ();
		int newCameraPitch = client.getCameraPitch();
		int newCameraYaw = client.getCameraYaw();
		int newViewportWidth = client.getViewportWidth();
		int newViewportHeight = client.getViewportHeight();
		int newViewportXOffset = client.getViewportXOffset();
		int newViewportYOffset = client.getViewportYOffset();
		int newViewportScale = client.getScale();
		boolean viewChanged = cameraX != newCameraX || cameraY != newCameraY || cameraZ != newCameraZ
			|| cameraPitch != newCameraPitch || cameraYaw != newCameraYaw
			|| viewportWidth != newViewportWidth || viewportHeight != newViewportHeight
			|| viewportXOffset != newViewportXOffset || viewportYOffset != newViewportYOffset
			|| viewportScale != newViewportScale;
		if (viewChanged)
		{
			shapes.clear();
			cameraX = newCameraX;
			cameraY = newCameraY;
			cameraZ = newCameraZ;
			cameraPitch = newCameraPitch;
			cameraYaw = newCameraYaw;
			viewportWidth = newViewportWidth;
			viewportHeight = newViewportHeight;
			viewportXOffset = newViewportXOffset;
			viewportYOffset = newViewportYOffset;
			viewportScale = newViewportScale;
		}
	}
}
