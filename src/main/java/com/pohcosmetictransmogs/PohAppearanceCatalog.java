package com.pohcosmetictransmogs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.ObjectComposition;
import net.runelite.api.gameval.VarbitID;

/** Resolves appearance definitions and builds their RuneLite models. */
final class PohAppearanceCatalog
{
	private static final int NATIVE_MODEL_SCALE = 128;
	private static final int[] NO_IDS = {};
	private static final short[] NO_COLOURS = {};
	private static List<Recipe> catalogue = Collections.emptyList();
	private static Map<Integer, Recipe> recipeIndex = Collections.emptyMap();
	private static Map<Integer, Definition> definitions = Collections.emptyMap();
	private static Map<Integer, Definition> targets = Collections.emptyMap();
	private PohAppearanceCatalog()
	{
	}

	static synchronized void loadCatalogue(Gson gson)
	{
		if (!catalogue.isEmpty())
		{
			return;
		}
		try (InputStream stream = PohAppearanceCatalog.class.getResourceAsStream("/appearances.json"))
		{
			if (stream == null)
			{
				throw new IllegalStateException("Missing appearances.json");
			}
			try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
			{
				catalogue = Collections.unmodifiableList(readRecipes(gson, reader, null));
				indexCatalogue(catalogue);
			}
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("Unable to load appearances.json", ex);
		}
	}

	static List<Recipe> catalogue()
	{
		return catalogue;
	}

	static void initialize(Gson gson, @Nullable Client client, @Nullable String json)
	{
		loadCatalogue(gson);
		Map<Integer, Recipe> values = new LinkedHashMap<>();
		for (Recipe recipe : catalogue)
		{
			values.put(recipe.source.objectId, recipe);
		}
		if (json != null)
		{
			for (Recipe recipe : readRecipes(gson, new StringReader(json), client))
			{
				values.put(recipe.source.objectId, recipe);
			}
		}
		indexCatalogue(values.values());
	}

	private static List<Recipe> readRecipes(Gson gson, Reader reader, @Nullable Client client)
	{
		List<Recipe> values = new ArrayList<>();
		for (Map.Entry<String, JsonElement> group : gson.fromJson(reader, JsonObject.class).entrySet())
		{
			for (Recipe recipe : gson.fromJson(group.getValue(), Recipe[].class))
			{
				for (Definition state : new Definition[] {recipe.source, recipe.closed, recipe.open})
				{
					if (state != null)
					{
						state.normalize(client == null ? null : client.getObjectDefinition(state.objectId));
					}
				}
				if (recipe.closed == null)
				{
					recipe.closed = recipe.source;
				}
				if (recipe.open == null)
				{
					recipe.open = recipe.source;
				}
				values.add(recipe);
			}
		}
		return values;
	}

	@Nullable
	static Definition get(int sourceObjectId)
	{
		return definitions.get(sourceObjectId);
	}

	static String name(int objectId)
	{
		return objectId == -1 ? "Original" : recipeIndex.get(objectId).name;
	}

	static Map<Integer, Definition> targetDefinitions()
	{
		return targets;
	}

	static int canonicalSelectionId(int sourceObjectId)
	{
		Recipe recipe = recipeIndex.get(sourceObjectId);
		return recipe == null ? sourceObjectId : recipe.source.objectId;
	}

	@Nullable
	static Recipe recipe(int appearanceId)
	{
		return recipeIndex.get(appearanceId);
	}

	@Nullable
	static Definition state(int appearanceId, boolean open)
	{
		Recipe recipe = recipe(canonicalSelectionId(appearanceId));
		return recipe == null ? null : recipe.state(open);
	}

	@Nullable
	static Calibration placement(PohFurniture furniture, int appearanceId)
	{
		Recipe recipe = recipe(canonicalSelectionId(appearanceId));
		return recipe == null ? null : recipe.placements.get(furniture);
	}

	static int orientationCorrection(@Nullable PohFurniture furniture, int sourceObjectId)
	{
		int correction = furniture == PohFurniture.ENTRANCE_PORTAL ? 1024 : 0;
		int canonicalId = canonicalSelectionId(sourceObjectId);
		if (canonicalId == 44788 || canonicalId == 41696)
		{
			return 1024;
		}
		if (canonicalId == 47419)
		{
			if (furniture == PohFurniture.TREASURE_CHEST
				|| furniture == PohFurniture.TOY_BOX
				|| furniture == PohFurniture.FANCY_DRESS_BOX)
			{
				return 1024;
			}
			return 1536;
		}
		if (canonicalId == 29742)
		{
			if (furniture == PohFurniture.TREASURE_CHEST || furniture == PohFurniture.TOY_BOX)
			{
				return 512;
			}
			if (furniture == PohFurniture.FANCY_DRESS_BOX)
			{
				return 1024;
			}
		}
		if (furniture == PohFurniture.ENTRANCE_PORTAL
			&& (canonicalId == 40460 || canonicalId == 40476 || canonicalId == 56074))
		{
			correction += 1024;
		}
		return (correction + sourceOrientationCorrection(sourceObjectId)) & 2047;
	}

	static int sourceOrientationCorrection(int sourceObjectId)
	{
		Definition definition = get(sourceObjectId);
		if (definition != null && definition.rotation != null)
		{
			return definition.rotation & 2047;
		}
		Recipe recipe = recipeIndex.get(sourceObjectId);
		return recipe == null ? 0 : recipe.orientation;
	}

	private static void indexCatalogue(Collection<Recipe> catalogue)
	{
		Map<Integer, Recipe> indexed = new HashMap<>();
		Map<Integer, Definition> models = new LinkedHashMap<>();
		Map<Integer, Definition> mappedTargets = new HashMap<>();
		for (Recipe recipe : catalogue)
		{
			indexed.put(recipe.source.objectId, recipe);
			models.put(recipe.source.objectId, recipe.source);
			models.put(recipe.open.objectId, recipe.open);
		}
		for (Recipe recipe : catalogue)
		{
			indexed.putIfAbsent(recipe.open.objectId, recipe);
			for (int alias : recipe.aliases)
			{
				indexed.put(alias, recipe);
			}
		}
		for (Definition definition : models.values())
		{
			for (int targetId : definition.targetObjectIds)
			{
				mappedTargets.put(targetId, definition);
			}
		}
		recipeIndex = Collections.unmodifiableMap(indexed);
		definitions = Collections.unmodifiableMap(models);
		targets = Collections.unmodifiableMap(mappedTargets);
	}

	/**
	 * One selectable appearance. The source retains its saved selection ID; the
	 * rendered states can use different models or share IDs with another recipe.
	 */
	static final class Recipe
	{
		String name;
		Definition source;
		Definition closed;
		Definition open;
		int[] aliases = NO_IDS;
		int orientation;
		boolean bobbing;
		int transitionModelId = -1;
		int transitionAnimationId = -1;
		int transitionHandoff;
		final Map<PohFurniture, Calibration> placements = new EnumMap<>(PohFurniture.class);

		private Recipe()
		{
		}

		Definition state(boolean opened)
		{
			return opened ? open : closed;
		}


	}

	@Getter
	@EqualsAndHashCode
	static class Calibration
	{
		int rotation;
		int scaleX = NATIVE_MODEL_SCALE;
		int scaleHeight = NATIVE_MODEL_SCALE;
		int scaleY = NATIVE_MODEL_SCALE;
		// Horizontal offsets are relative to the normalized furniture orientation.
		int offsetX;
		int offsetHeight;
		int offsetY;
		boolean flipX;

		Calibration()
		{
		}

		Calibration(int rotation, int scaleX, int scaleHeight, int scaleY,
			int offsetX, int offsetHeight, int offsetY)
		{
			this.rotation = rotation;
			this.scaleX = scaleX;
			this.scaleHeight = scaleHeight;
			this.scaleY = scaleY;
			this.offsetX = offsetX;
			this.offsetHeight = offsetHeight;
			this.offsetY = offsetY;
		}

		int signedScaleX()
		{
			return flipX ? -scaleX : scaleX;
		}

	}

	@Getter
	static class Definition
	{
		int objectId;
		int sizeX;
		int sizeY;
		int[] modelIds = NO_IDS;
		int animationId = -1;
		int spawnAnimationId = -1;
		boolean spawnOnce;
		int offsetX;
		int offsetHeight;
		int offsetY;
		short[] recolorFrom = NO_COLOURS;
		short[] recolorTo = NO_COLOURS;
		// Exact portal-energy face colours; recolouring preserves each shade's luminance.
		short[] portalColours = NO_COLOURS;
		int portalSaturation = -1;
		short[] crystalColours = NO_COLOURS;
		short[] tobColours = NO_COLOURS;
		short[] gauntletColours = NO_COLOURS;
		short[] deadmanColours = NO_COLOURS;
		short[] toaColours = NO_COLOURS;
		short[] nodeColours = NO_COLOURS;
		short[] nodeGreyColours = NO_COLOURS;
		int modelScaleX;
		int modelScaleHeight;
		int modelScaleY;
		int[] targetObjectIds = NO_IDS;
		Integer rotation;

		private Definition()
		{
		}

		private void normalize(@Nullable ObjectComposition source)
		{
			int nativeSizeX = source == null ? Math.max(1, sizeX) : source.getSizeX();
			int nativeSizeY = source == null ? Math.max(1, sizeY) : source.getSizeY();
			if (sizeX == 0)
			{
				sizeX = nativeSizeX;
			}
			if (sizeY == 0)
			{
				sizeY = nativeSizeY;
			}
			if (modelScaleX == 0)
			{
				modelScaleX = ModelFactory.footprintScale(NATIVE_MODEL_SCALE, sizeX, nativeSizeX);
			}
			if (modelScaleY == 0)
			{
				modelScaleY = ModelFactory.footprintScale(NATIVE_MODEL_SCALE, sizeY, nativeSizeY);
			}
			if (modelScaleHeight == 0)
			{
				modelScaleHeight = Math.min(modelScaleX, modelScaleY);
			}
		}

		Definition(int objectId, int sizeX, int sizeY, int[] modelIds, int animationId)
		{
			this.objectId = objectId;
			this.sizeX = sizeX;
			this.sizeY = sizeY;
			this.modelIds = modelIds;
			this.animationId = animationId;
			modelScaleX = NATIVE_MODEL_SCALE;
			modelScaleHeight = NATIVE_MODEL_SCALE;
			modelScaleY = NATIVE_MODEL_SCALE;
		}
	}

	static final class ModelFactory
	{
		private final Client client;
		private final PohCosmeticTransmogsConfig config;

		ModelFactory(Client client, PohCosmeticTransmogsConfig config)
		{
			this.client = client;
			this.config = config;
		}

		static Calibration calibration(PohFurniture furniture, Definition definition)
		{
			return calibration(furniture, furniture.getSizeX(), furniture.getSizeY(), definition);
		}

		static Calibration calibration(@Nullable PohFurniture furniture,
			int targetSizeX, int targetSizeY, Definition definition)
		{
			Calibration tuned = furniture == null ? null
				: placement(furniture, definition.getObjectId());
			if (tuned != null)
			{
				return tuned;
			}
			int scaleX = definition.getModelScaleX();
			int scaleHeight = definition.getModelScaleHeight();
			int scaleY = definition.getModelScaleY();
			if (isTobChest(definition))
			{
				int sourceSpan = Math.max(definition.getSizeX(), definition.getSizeY());
				int targetSpan = Math.min(2, Math.max(targetSizeX, targetSizeY));
				scaleX = footprintScale(scaleX, targetSpan, sourceSpan);
				scaleHeight = footprintScale(scaleHeight, targetSpan, sourceSpan);
				scaleY = footprintScale(scaleY, targetSpan, sourceSpan);
			}
			else if (shouldFitTargetFootprint(furniture, definition))
			{
				if (isQuarterTurn(sourceOrientationCorrection(definition.getObjectId())))
				{
					int swap = targetSizeX;
					targetSizeX = targetSizeY;
					targetSizeY = swap;
				}
				scaleX = footprintScale(scaleX, targetSizeX, definition.getSizeX());
				scaleY = footprintScale(scaleY, targetSizeY, definition.getSizeY());
				if (isCompactDecoration(definition) && targetSizeX != targetSizeY)
				{
					scaleX = limitedDecorationAxisScale(
						definition.getModelScaleX(), definition.getSizeX(), scaleX);
					scaleY = limitedDecorationAxisScale(
						definition.getModelScaleY(), definition.getSizeY(), scaleY);
				}
			}
			int rotation = orientationCorrection(furniture, definition.getObjectId());
			int canonicalId = canonicalSelectionId(definition.getObjectId());
			int offsetY = isCoxChest(definition) || isCoxStorage(definition)
				|| canonicalId == 44788 ? 16 : 0;
			return new Calibration(rotation, scaleX, scaleHeight, scaleY,
				definition.getOffsetX(), definition.getOffsetHeight(), definition.getOffsetY() + offsetY);
		}

		boolean recoloursPortal(@Nullable PohFurniture furniture, Definition definition)
		{
			return config.portalColour().getHue() >= 0
				&& definition.getPortalColours().length > 0
				&& (furniture == PohFurniture.ENTRANCE_PORTAL
					|| config.recolourPortalsInAllPositions());
		}

		@Nullable
		Model load(@Nullable PohFurniture furniture, Definition definition, Calibration calibration)
		{
			ModelData[] parts = new ModelData[definition.getModelIds().length];
			for (int i = 0; i < parts.length; i++)
			{
				parts[i] = client.loadModelData(definition.getModelIds()[i]);
				if (parts[i] == null)
				{
					return null;
				}
			}
			// Merging also gives mirrored recipes their own face-index arrays; a
			// shallow copy would reverse RuneLite's shared cached model in place.
			ModelData model = client.mergeModels(parts, parts.length);
			if (model == null)
			{
				return null;
			}
			model.cloneVertices();
			model.cloneColors();
			for (int i = 0; i < definition.getRecolorFrom().length; i++)
			{
				model.recolor(definition.getRecolorFrom()[i], definition.getRecolorTo()[i]);
			}
			if (recoloursPortal(furniture, definition))
			{
				recolorHue(model, definition.getPortalColours(), config.portalColour().getHue(),
					definition.getPortalSaturation());
			}
			int hue = config.recolourColour().getHue();
			if (hue >= 0)
			{
				if (config.recolourCrystals())
				{
					recolorHue(model, definition.getCrystalColours(), hue, -1);
				}
				if (config.recolourTobChest())
				{
					recolorHue(model, definition.getTobColours(), hue, -1);
				}
				if (config.recolourGauntletChest())
				{
					recolorHue(model, definition.getGauntletColours(), hue, -1);
				}
				if (config.recolourDeadmanChest())
				{
					recolorHue(model, definition.getDeadmanColours(), hue, -1);
				}
				if (config.recolourToaContainers())
				{
					recolorHue(model, definition.getToaColours(), hue, -1);
				}
			}
			if (config.recolourNodePortal() && definition.getObjectId() == 42819)
			{
				recolorNodePortal(model, definition);
			}

			// Animation transforms use the cache model's original coordinate system.
			// Animated recipes are therefore resized after each posed frame by the
			// manager; preprocessing their vertices makes moving parts drift away.
			if (definition.getAnimationId() < 0)
			{
				model.scale(calibration.getScaleX(), calibration.getScaleHeight(), calibration.getScaleY());
				model.translate(0, calibration.getOffsetHeight(), 0);
			}
			Model lit = model.light();
			if (calibration.isFlipX())
			{
				if (definition.getAnimationId() < 0)
				{
					// Static geometry already has its magnitude; reflect it after
					// lighting so its normals can be mirrored deliberately below.
					lit.scale(-128, 128, 128);
				}
				correctMirroredWinding(lit);
			}
			return lit;
		}

		private static void correctMirroredWinding(Model model)
		{
			swap(model.getFaceIndices1(), model.getFaceIndices3());
			swap(model.getFaceColors1(), model.getFaceColors3());
			swap(model.getTexIndices1(), model.getTexIndices3());
			negate(model.getVertexNormalsX());
		}

		private static void swap(@Nullable int[] first, @Nullable int[] third)
		{
			if (first == null || third == null)
			{
				return;
			}
			for (int i = 0; i < Math.min(first.length, third.length); i++)
			{
				int value = first[i];
				first[i] = third[i];
				third[i] = value;
			}
		}

		private static void negate(@Nullable int[] values)
		{
			if (values != null)
			{
				for (int i = 0; i < values.length; i++)
				{
					values[i] = -values[i];
				}
			}
		}

		private static void recolorHue(ModelData model, short[] sources, int hue, int saturationOverride)
		{
			recolorHue(model, sources, hue, saturationOverride, 0);
		}

		private void recolorNodePortal(ModelData model, Definition definition)
		{
			int accountType = client.getVarbitValue(VarbitID.IRONMAN);
			switch (accountType)
			{
				case 1: // Ironman - grey
					recolorHue(model, definition.getNodeColours(), 0, 0);
					break;
				case 2: // Ultimate ironman - light grey
					recolorHue(model, definition.getNodeColours(), 0, 0, 20);
					break;
				case 3: // Hardcore ironman - red
					recolorHue(model, definition.getNodeColours(), 0, 6);
					recolorHue(model, definition.getNodeGreyColours(), 0, 6);
					break;
				case 4: // Ranked group ironman native
					break;
				case 5: // Hardcore group ironman - red trim
					recolorHue(model, definition.getNodeColours(), 0, 6);
					break;
				case 6: // Unranked group ironman - green trim
					recolorHue(model, definition.getNodeColours(), 14, 6);
					break;
				default: // Main - bronze
					recolorHue(model, definition.getNodeColours(), 5, 4);
					recolorHue(model, definition.getNodeGreyColours(), 5, 4);
					break;
			}
		}

		private static void recolorHue(ModelData model, short[] sources,
			int hue, int saturationOverride, int luminanceOffset)
		{
			for (short source : sources)
			{
				int saturation = saturationOverride >= 0
					? saturationOverride : JagexColor.unpackSaturation(source);
				int luminance = Math.max(0, Math.min(127,
					JagexColor.unpackLuminance(source) + luminanceOffset));
				model.recolor(source, JagexColor.packHSL(hue, saturation, luminance));
			}
		}

		static boolean shouldFitTargetFootprint(@Nullable PohFurniture furniture, Definition definition)
		{
			return furniture != null
				&& furniture != PohFurniture.ENTRANCE_PORTAL
				&& furniture != PohFurniture.MAGIC_WARDROBE
				&& !isTobChest(definition);
		}

		private static boolean isTobChest(Definition definition)
		{
			return definition.getObjectId() == 32991 || definition.getObjectId() == 41746;
		}

		static boolean isCoxChest(Definition definition)
		{
			int id = canonicalSelectionId(definition.getObjectId());
			return id == 30027 || id == 30028 || id == 47419 || id == 29742;
		}

		static boolean isCoxStorage(Definition definition)
		{
			int id = canonicalSelectionId(definition.getObjectId());
			return id == 29770 || id == 29779 || id == 29780 || id == 37978;
		}

		private static boolean isCompactDecoration(Definition definition)
		{
			int id = canonicalSelectionId(definition.getObjectId());
			return isCoxStorage(definition) || id == 29757 || id == 29794 || id == 29766
				|| id == 4928 || id == 32996;
		}

		static boolean isQuarterTurn(int orientation)
		{
			return (orientation & 1023) == 512;
		}

		static int footprintScale(int nativeScale, int targetTiles, int sourceTiles)
		{
			return Math.max(1, Math.round((float) nativeScale * targetTiles / Math.max(1, sourceTiles)));
		}

		static int limitedDecorationAxisScale(int nativeScale, int sourceTiles, int fittedScale)
		{
			int oneTileScale = footprintScale(nativeScale, 1, sourceTiles);
			return Math.min(fittedScale, Math.round(oneTileScale * 1.25f));
		}
	}
}
