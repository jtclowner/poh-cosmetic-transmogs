package com.pohcosmetictransmogs;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Animation;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Point;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

import static com.pohcosmetictransmogs.ApiDouble.DEFAULT;
import static org.junit.Assert.*;

public class ManagerTest
{
	@Test
	public void alignmentUsesActualFootprintParityPerAxis()
	{
		for (int targetX = 1; targetX <= 4; targetX++)
		{
			for (int targetY = 1; targetY <= 4; targetY++)
			{
				for (int replacementSize = 1; replacementSize <= 7; replacementSize++)
				{
					for (Catalogue.Alignment alignment : Catalogue.Alignment.values())
					{
						Harness h = new Harness("\"sizeX\":" + replacementSize + ",\"sizeY\":" + replacementSize
							+ ",\"alignment\":\"" + alignment + "\",");
						h.sizeX = targetX;
						h.sizeY = targetY;
						h.manager.addObject(h.object(1));
						assertEquals((10 + targetX) * 64 + ((targetX - replacementSize) & 1) * alignment.x * 64,
							h.only().getLocation().getX());
						assertEquals((10 + targetY) * 64 + ((targetY - replacementSize) & 1) * alignment.y * 64,
							h.only().getLocation().getY());
						h.manager.stop();
					}
				}
			}
		}
	}

	@Test
	public void alignmentRespectsFinalQuarterTurnAndAddsRotatedManualOffsets()
	{
		Harness h = new Harness("\"sizeX\":2,\"sizeY\":3,\"rotation\":512,"
			+ "\"alignment\":\"SOUTH_WEST\",\"offsetX\":128,\"offsetY\":256,");
		h.manager.addObject(h.object(1));
		assertEquals(704 + 128, h.only().getLocation().getX());
		assertEquals(704 - 64 + 256, h.only().getLocation().getY());
		h.manager.stop();

		h = new Harness("\"sizeX\":2,\"sizeY\":3,\"alignment\":\"SOUTH_WEST\","
			+ "\"offsetX\":128,\"offsetY\":256,");
		h.orientation = 512;
		h.manager.addObject(h.object(1));
		assertEquals(704 + 256, h.only().getLocation().getX());
		assertEquals(704 - 64 - 128, h.only().getLocation().getY());
		h.manager.stop();
	}

	@Test
	public void omittedAlignmentPreservesTheOriginalCentre()
	{
		Harness h = new Harness("\"sizeX\":2,\"sizeY\":2,");
		h.manager.addObject(h.object(1));
		assertEquals(704, h.only().getLocation().getX());
		assertEquals(704, h.only().getLocation().getY());
		h.manager.stop();
	}

	@Test
	public void diagonalSquareRotationAndScaleDoNotChangeDeclaredAlignment()
	{
		Harness h = new Harness("\"sizeX\":2,\"sizeY\":2,\"rotation\":256,"
			+ "\"modelScaleX\":900,\"alignment\":\"SOUTH_WEST\",");
		h.manager.addObject(h.object(1));
		assertEquals(640, h.only().getLocation().getX());
		assertEquals(640, h.only().getLocation().getY());
		h.manager.stop();
	}

	@Test
	public void radiusUsesFootprintAndOriginalInteractionsRemainInTheScene()
	{
		Harness h = new Harness("\"sizeX\":3,\"sizeY\":7,\"modelScaleX\":900,");
		GameObject object = h.object(1);
		h.manager.addObject(object);
		assertEquals(444, h.only().getRadius());
		assertFalse(h.manager.shouldDrawObject(object));
		assertTrue(h.manager.getActiveObjects().contains(object));
		h.manager.setHidden(true);
		assertTrue(h.active.isEmpty());
		assertTrue(h.manager.shouldDrawObject(object));
		h.manager.setHidden(false);
		assertEquals(1, h.active.size());
		h.manager.stop();
		assertTrue(h.active.isEmpty());
		assertTrue(h.manager.shouldDrawObject(object));
		assertTrue(h.invalidations > 0);
	}

	@Test
	public void stateChangesRetireOldObjectsUntilDespawn()
	{
		Harness h = new Harness("\"open\":{\"modelIds\":[20]},");
		GameObject closed = h.object(1);
		GameObject open = h.object(2);
		h.manager.addObject(closed);
		Model closedModel = h.only().getModel();
		h.manager.addObject(open);
		assertEquals(1, h.active.size());
		assertNotSame(closedModel, h.only().getModel());
		assertFalse(h.manager.shouldDrawObject(closed));
		assertFalse(h.manager.shouldDrawObject(open));
		h.manager.removeObject(closed);
		assertEquals(1, h.active.size());
		assertFalse(h.manager.shouldDrawObject(open));
		h.manager.removeObject(open);
		assertTrue(h.active.isEmpty());
	}

	@Test
	public void removingASelectionRestoresOriginalDrawing()
	{
		Harness h = new Harness("");
		Catalogue.current.appearances.get("gem").bindTargets = new String[0];
		h.manager.setCatalogue(Catalogue.current);
		h.manager.setSelections(Map.of("box", "gem"));
		GameObject object = h.object(1);
		h.manager.addObject(object);
		assertFalse(h.manager.shouldDrawObject(object));
		h.manager.setSelections(Collections.emptyMap());
		assertTrue(h.active.isEmpty());
		assertTrue(h.manager.shouldDrawObject(object));
		h.manager.removeObject(object);
		h.manager.setSelections(Map.of("box", "gem"));
		h.manager.addObject(h.object(1));
		assertTrue(h.manager.shouldDrawObject(object));
	}

	@Test
	public void missingModelsRetryWithoutChangingBindings()
	{
		Harness h = new Harness("");
		h.modelsAvailable = false;
		GameObject object = h.object(1);
		h.manager.addObject(object);
		assertTrue(h.active.isEmpty());
		h.modelsAvailable = true;
		h.manager.loadMissingModels();
		assertEquals(1, h.active.size());
		assertFalse(h.manager.shouldDrawObject(object));
	}

	@Test
	public void equalModelIdsInDifferentRecipesHaveIndependentCacheEntries()
	{
		Harness h = new Harness("");
		Catalogue.current = Catalogue.loadCatalogue(RuneLiteAPI.GSON, null,
			new StringReader("{\"targets\":{\"a\":{\"objectIds\":[1]},\"b\":{\"objectIds\":[3]}},"
				+ "\"appearances\":{\"red\":{\"modelIds\":[10],\"recolorFrom\":[127],\"recolorTo\":[730],\"bindTargets\":[\"a\"]},"
				+ "\"blue\":{\"modelIds\":[10],\"recolorFrom\":[127],\"recolorTo\":[44762],\"bindTargets\":[\"b\"]}}}"));
		h.manager.setCatalogue(Catalogue.current);
		h.manager.addObject(h.object(1));
		h.manager.addObject(h.object(3));
		assertEquals(2, h.active.size());
		assertEquals(2, h.lights);
		assertTrue(h.operations.contains("recolor:127:730"));
		assertTrue(h.operations.contains("recolor:127:-20774"));
	}

	@Test
	public void scaleTransitionsKeepTheBottomFixedAndReverse()
	{
		Harness h = new Harness("");
		GameObject closed = h.object(1);
		h.manager.addObject(closed);
		h.manager.removeObject(closed);
		GameObject open = h.object(2);
		h.manager.addObject(open);
		RuneLiteObject replacement = h.only();
		assertNotNull(replacement.getAnimationController());
		replacement.tick(30);
		h.operations.clear();
		replacement.getModel();
		assertTrue(h.operations.contains("scale:160:160:160"));
		assertTrue(h.operations.contains("translate:0:-5:0"));
		h.manager.removeObject(open);
		h.manager.addObject(h.object(1));
		replacement = h.only();
		replacement.tick(30);
		h.operations.clear();
		replacement.getModel();
		assertTrue(h.operations.contains("scale:128:128:128"));
	}

	@Test
	public void animatedModelsAreScaledAfterPosing()
	{
		Harness h = new Harness("\"animationId\":99,\"modelScaleX\":200,\"modelScaleY\":180,\"modelScaleHeight\":150,");
		h.manager.addObject(h.object(1));
		assertFalse(h.operations.stream().anyMatch(s -> s.startsWith("scale:")));
		h.only().getModel();
		assertTrue(h.operations.indexOf("pose") < h.operations.indexOf("scale:200:150:180"));
	}

	@Test
	public void closingReversesTheOpenSpawnAnimation()
	{
		Harness h = new Harness("\"animationId\":99,\"open\":{\"modelIds\":[20],\"animationId\":99,\"spawnAnimationId\":100},");
		GameObject open = h.object(2);
		h.manager.addObject(open);
		h.manager.removeObject(open);
		h.manager.addObject(h.object(1));
		RuneLiteObject replacement = h.only();
		assertEquals(100, replacement.getAnimationController().getAnimation().getId());
		assertEquals(9, replacement.getAnimationController().getFrame());
		replacement.tick(2);
		assertEquals(7, replacement.getAnimationController().getFrame());
		replacement.tick(10);
		assertEquals(99, replacement.getAnimationController().getAnimation().getId());
	}

	@Test
	public void shatterEffectsHandOffAndAreRemovedOnShutdown()
	{
		Harness h = new Harness("\"sizeX\":2,\"sizeY\":2,\"alignment\":\"SOUTH_WEST\","
			+ "\"open\":{\"modelIds\":[20]},\"transitionModelId\":30,\"transitionAnimationId\":100,\"transitionHandoff\":20,");
		GameObject closed = h.object(1);
		h.manager.addObject(closed);
		h.manager.removeObject(closed);
		h.manager.addObject(h.object(2));
		RuneLiteObject effect = h.only();
		effect.tick(2);
		assertEquals(2, h.active.size());
		for (RuneLiteObject part : h.active)
		{
			assertEquals(640, part.getLocation().getX());
			assertEquals(640, part.getLocation().getY());
		}
		effect.tick(8);
		assertEquals(1, h.active.size());
		h.manager.stop();
		assertTrue(h.active.isEmpty());
	}

	@Test
	public void bobbingContinuesThroughTheScaleTransition()
	{
		Harness h = new Harness("\"bobbing\":true,");
		GameObject closed = h.object(1);
		h.manager.addObject(closed);
		h.only().tick(40);
		assertEquals(-4, h.only().getZ());
		h.manager.removeObject(closed);
		h.manager.addObject(h.object(2));
		h.only().tick(40);
		assertEquals(-4, h.only().getZ());
		assertNotNull(h.only().getAnimationController());
	}

	@Test
	public void reloadRebuildsModelsAndRestoresRemovedTargets()
	{
		Harness h = new Harness("");
		GameObject object = h.object(1);
		h.loaded.add(object);
		h.manager.addObject(object);
		Model previous = h.only().getModel();
		h.manager.setCatalogue(Catalogue.current);
		assertEquals(1, h.active.size());
		assertNotSame(previous, h.only().getModel());
		h.manager.setCatalogue(Catalogue.loadCatalogue(RuneLiteAPI.GSON, null, new StringReader("{}")));
		assertTrue(h.active.isEmpty());
		assertTrue(h.manager.shouldDrawObject(object));
	}

	@Test
	public void delayedSpawnWaitsForVisibilityAndRunsOnce()
	{
		Harness h = new Harness("\"animationId\":99,\"spawnAnimationId\":100,\"spawnOnce\":true,");
		h.gameState = GameState.LOADING;
		GameObject object = h.object(1);
		h.manager.addObject(object);
		RuneLiteObject replacement = h.only();
		replacement.tick(100);
		assertEquals(0, replacement.getAnimationController().getFrame());
		h.gameState = GameState.LOGGED_IN;
		replacement.tick(1);
		replacement.tick(10);
		assertEquals(99, replacement.getAnimationController().getAnimation().getId());
		h.manager.refreshColours();
		assertEquals(99, h.only().getAnimationController().getAnimation().getId());
	}

	@Test
	public void staticMirroringReversesWindingAndNormals()
	{
		Harness h = new Harness("\"placements\":{\"box\":{\"flipX\":true}},");
		h.manager.addObject(h.object(1));
		Model model = h.only().getModel();
		assertArrayEquals(new int[] {3}, model.getFaceIndices1());
		assertArrayEquals(new int[] {1}, model.getFaceIndices3());
		assertArrayEquals(new int[] {-4}, model.getVertexNormalsX());
		assertTrue(h.operations.contains("scale:-128:128:128"));
	}

	private static final class Harness
	{
		final Set<RuneLiteObject> active = Collections.newSetFromMap(new IdentityHashMap<>());
		final List<String> operations = new ArrayList<>();
		final List<GameObject> loaded = new ArrayList<>();
		final Client client;
		final WorldView world;
		final PohCosmeticTransmogsManager manager;
		boolean modelsAvailable = true;
		int sizeX = 1;
		int sizeY = 1;
		int orientation;
		int invalidations;
		int lights;
		GameState gameState = GameState.LOGGED_IN;

		Harness(String fields)
		{
			Scene scene = ApiDouble.of(Scene.class, (name, args) ->
			{
				if (name.equals("getTiles"))
				{
					Tile tile = ApiDouble.of(Tile.class, (n, a) -> n.equals("getGameObjects")
						? loaded.toArray(new GameObject[0]) : DEFAULT);
					return new Tile[][][] {{{tile}}};
				}
				throw new AssertionError("Original scene mutation: " + name);
			});
			world = ApiDouble.of(WorldView.class, (name, args) ->
			{
				switch (name)
				{
					case "getId": return WorldView.TOPLEVEL;
					case "getScene": return scene;
					case "worldViews": return ApiDouble.of(IndexedObjectSet.class,
						(n, a) -> n.equals("iterator") ? Collections.emptyIterator() : DEFAULT);
					default: return DEFAULT;
				}
			});
			DrawCallbacks callbacks = ApiDouble.of(DrawCallbacks.class, (name, args) ->
			{
				if (name.equals("invalidateZone")) { invalidations++; }
				return DEFAULT;
			});
			client = ApiDouble.of(Client.class, (name, args) ->
			{
				switch (name)
				{
					case "isGpu": return true;
					case "getDrawCallbacks": return callbacks;
					case "getTopLevelWorldView":
					case "getWorldView": return world;
					case "createRuneLiteObject": return new RuneLiteObject(client());
					case "registerRuneLiteObject": active.add((RuneLiteObject) args[0]); return null;
					case "removeRuneLiteObject": active.remove(args[0]); return null;
					case "isRuneLiteObjectRegistered": return active.contains(args[0]);
					case "loadModelData": return modelsAvailable ? modelData() : null;
					case "mergeModels": return args[0] instanceof Model[] ? model() : modelData();
					case "applyTransformations": operations.add("pose"); return model();
					case "loadAnimation": return animation((int) args[0]);
					case "getGameState": return gameState;
					case "getCameraFpX": return 704f;
					case "getViewportWidth": return 800;
					case "getViewportHeight": return 600;
					case "getScale": return 512;
					default: return DEFAULT;
				}
			});
			Catalogue.current = Catalogue.loadCatalogue(RuneLiteAPI.GSON, null, new StringReader(
				"{\"targets\":{\"box\":{\"objectIds\":[1,2],\"openObjectIds\":[2],\"scaleTransition\":true}},"
				+ "\"appearances\":{\"gem\":{" + fields + "\"modelIds\":[10],\"bindTargets\":[\"box\"]}}}"));
			manager = new PohCosmeticTransmogsManager(client, new PohCosmeticTransmogsConfig() {});
			manager.start(Collections.emptyMap(), false);
		}

		Client client() { return client; }

		RuneLiteObject only()
		{
			assertEquals(1, active.size());
			return active.iterator().next();
		}

		GameObject object(int id)
		{
			return ApiDouble.of(GameObject.class, (name, args) ->
			{
				switch (name)
				{
					case "getId": return id;
					case "getWorldView": return world;
					case "getSceneMinLocation": return new Point(5, 5);
					case "getSceneMaxLocation": return new Point(4 + sizeX, 4 + sizeY);
					case "sizeX": return sizeX;
					case "sizeY": return sizeY;
					case "getOrientation": return orientation;
					default: return DEFAULT;
				}
			});
		}

		ModelData modelData()
		{
			return ApiDouble.of(ModelData.class, (name, args) ->
			{
				if (name.equals("light")) { lights++; return model(); }
				record(name, args);
				return DEFAULT;
			});
		}

		Model model()
		{
			int[] first = {1}, third = {3}, normals = {4};
			return ApiDouble.of(Model.class, (name, args) ->
			{
				if (name.equals("getXYZMag")) { throw new AssertionError("Radius must not use model bounds"); }
				if (name.equals("getBottomY")) { return 20; }
				if (name.equals("getFaceIndices1")) { return first; }
				if (name.equals("getFaceIndices3")) { return third; }
				if (name.equals("getVertexNormalsX")) { return normals; }
				record(name, args);
				return DEFAULT;
			});
		}

		void record(String name, Object[] args)
		{
			if (name.equals("scale") || name.equals("translate") || name.equals("recolor"))
			{
				StringBuilder text = new StringBuilder(name);
				for (Object arg : args) { text.append(':').append(arg); }
				operations.add(text.toString());
			}
		}

		Animation animation(int id)
		{
			return ApiDouble.of(Animation.class, (name, args) ->
			{
				switch (name)
				{
					case "getId": return id;
					case "isMayaAnim": return true;
					case "getDuration":
					case "getFrameStep": return 10;
					default: return DEFAULT;
				}
			});
		}
	}
}
