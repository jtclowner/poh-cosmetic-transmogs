package com.pohcosmetictransmogs;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
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

/** Indexed target semantics, appearance recipes and model construction. */
final class Catalogue
{
	private static final int NATIVE_MODEL_SCALE = 128;
	private static final int[] NO_IDS = {};
	private static final short[] NO_COLOURS = {};
	static volatile Catalogue current = new Catalogue(new CatalogueData());
	final Map<String, TargetSpec> targets;
	final Map<String, Recipe> appearances;

	private Catalogue(CatalogueData data)
	{
		targets = Collections.unmodifiableMap(data.targets);
		appearances = Collections.unmodifiableMap(data.appearances);
	}

	static Catalogue loadCatalogue(Gson gson, @Nullable Client client, Reader... readers)
	{
		CatalogueData data = new CatalogueData();
		for (Reader reader : readers)
		{
			if (reader != null)
			{
				try (Reader input = reader)
				{
					readIntoCatalogue(gson, client, input, data);
				}
				catch (IOException ex)
				{
					throw new IllegalStateException("Unable to read catalogue", ex);
				}
			}
		}
		return new Catalogue(data);
	}

	static Reader openCatalogueReader()
	{
		InputStream stream = Catalogue.class.getResourceAsStream("/catalogue.json");
		if (stream == null)
		{
			throw new IllegalStateException("Missing catalogue.json");
		}
		return new InputStreamReader(stream, StandardCharsets.UTF_8);
	}

	private static void readIntoCatalogue(Gson gson, @Nullable Client client,
		Reader reader, CatalogueData data)
	{
		JsonObject json = gson.fromJson(reader, JsonObject.class);
		if (json == null)
		{
			return;
		}
		if (json.has("targets"))
		{
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("targets").entrySet())
			{
				TargetSpec target = gson.fromJson(entry.getValue(), TargetSpec.class);
				target.key = entry.getKey();
				data.targets.put(target.key, target);
			}
		}
		if (json.has("appearances"))
		{
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("appearances").entrySet())
			{
				Recipe recipe = gson.fromJson(entry.getValue(), Recipe.class);
				recipe.key = entry.getKey();
				for (Definition state : new Definition[] {recipe, recipe.closed, recipe.open})
				{
					if (state != null)
					{
						state.normalize(client == null || state.sourceObjectId < 0 ? null
							: client.getObjectDefinition(state.sourceObjectId));
					}
				}
				if (recipe.closed == null)
				{
					recipe.closed = recipe;
				}
				if (recipe.open == null)
				{
					recipe.open = recipe;
				}
				data.appearances.put(recipe.key, recipe);
			}
		}
	}

	static String name(String appearanceKey)
	{
		Recipe recipe = current.appearances.get(appearanceKey);
		return appearanceKey.isEmpty() ? "Original" : recipe == null ? appearanceKey : recipe.name;
	}

	Map<Integer, TargetBinding> bind(Map<String, String> selections)
	{
		Map<String, TargetBinding> bindings = new LinkedHashMap<>();
		for (Recipe recipe : appearances.values())
		{
			for (String key : recipe.bindTargets)
			{
				bind(bindings, key, recipe);
			}
		}
		selections.forEach((key, appearanceKey) ->
		{
			if (!bindings.containsKey(key))
			{
				bind(bindings, key, appearances.get(appearanceKey));
			}
		});
		Map<Integer, TargetBinding> byId = new LinkedHashMap<>();
		for (TargetBinding binding : bindings.values())
		{
			for (int id : binding.target.objectIds)
			{
				byId.putIfAbsent(id, binding);
			}
		}
		return byId;
	}

	private void bind(Map<String, TargetBinding> bindings, String key, @Nullable Recipe recipe)
	{
		TargetSpec target = targets.get(key);
		if (target != null && recipe != null)
		{
			bindings.put(key, new TargetBinding(target, recipe));
		}
	}

	private static final class CatalogueData
	{
		final Map<String, TargetSpec> targets = new LinkedHashMap<>();
		final Map<String, Recipe> appearances = new LinkedHashMap<>();
	}

	/** Appearance identity and optional container states; inherited fields form its default state. */
	static final class Recipe extends Definition
	{
		String key;
		String name;
		String category;
		Definition closed;
		Definition open;
		String[] bindTargets = {};
		int orientation;
		Alignment alignment = Alignment.NONE;
		boolean bobbing;
		int transitionModelId = -1;
		int transitionAnimationId = -1;
		int transitionHandoff;
		Map<String, Calibration> placements = new LinkedHashMap<>();

		Definition state(boolean opened)
		{
			return opened ? open : closed;
		}

		String stateKey(Definition state)
		{
			return state == this ? "default" : state == closed ? "closed" : "open";
		}
	}

	enum Alignment
	{
		NONE(0, 0),
		SOUTH_WEST(-1, -1),
		SOUTH_EAST(1, -1),
		NORTH_WEST(-1, 1),
		NORTH_EAST(1, 1);

		final int x;
		final int y;

		Alignment(int x, int y)
		{
			this.x = x;
			this.y = y;
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
		// Horizontal offsets are relative to the normalized target orientation.
		int offsetX;
		int offsetHeight;
		int offsetY;
		boolean flipX;
		TargetSpec.FitMode fitMode;

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
		int sourceObjectId = -1;
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

		Integer rotation;

		Definition()
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

		Definition(int sizeX, int sizeY, int[] modelIds, int animationId)
		{
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

		static Calibration calibration(Recipe recipe, TargetSpec target, int targetSizeX, int targetSizeY)
		{
			Calibration tuned = recipe.placements.get(target.key);
			TargetSpec.FitMode fit = tuned != null && tuned.fitMode != null
				? tuned.fitMode : target.defaultFitMode;
			if (tuned != null && fit == TargetSpec.FitMode.NONE)
			{
				return tuned;
			}
			int scaleX = tuned == null ? recipe.getModelScaleX() : tuned.scaleX;
			int scaleHeight = tuned == null ? recipe.getModelScaleHeight() : tuned.scaleHeight;
			int scaleY = tuned == null ? recipe.getModelScaleY() : tuned.scaleY;
			int rotation = tuned != null ? tuned.rotation
				: recipe.rotation == null ? recipe.orientation : recipe.rotation;
			if (fit == TargetSpec.FitMode.FOOTPRINT)
			{
				if (isQuarterTurn(rotation))
				{
					int swap = targetSizeX;
					targetSizeX = targetSizeY;
					targetSizeY = swap;
				}
				scaleX = footprintScale(scaleX, targetSizeX, recipe.sizeX);
				scaleY = footprintScale(scaleY, targetSizeY, recipe.sizeY);
			}
			Calibration result = new Calibration(rotation,
				scaleX, scaleHeight, scaleY,
				tuned == null ? recipe.offsetX : tuned.offsetX,
				tuned == null ? recipe.offsetHeight : tuned.offsetHeight,
				tuned == null ? recipe.offsetY : tuned.offsetY);
			result.flipX = tuned != null && tuned.flipX;
			return result;
		}

		boolean recoloursPortal(TargetSpec target, Definition definition)
		{
			return config.portalColour().getHue() >= 0
				&& definition.getPortalColours().length > 0
				&& (target.portalRecolour
					|| config.recolourPortalsInAllPositions());
		}

		@Nullable
		Model load(TargetSpec target, Definition definition, Calibration calibration)
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
			if (recoloursPortal(target, definition))
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
			if (config.recolourNodePortal() && definition.getNodeColours().length > 0)
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

		static boolean isQuarterTurn(int orientation)
		{
			return (orientation & 1023) == 512;
		}

		static int footprintScale(int nativeScale, int targetTiles, int sourceTiles)
		{
			return Math.max(1, Math.round((float) nativeScale * targetTiles / Math.max(1, sourceTiles)));
		}

	}
}
