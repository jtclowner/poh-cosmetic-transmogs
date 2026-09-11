package com.pohcosmetictransmogs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.Point;
import net.runelite.api.Perspective;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.api.widgets.Widget;

/**
 * Manages cosmetic replacements while retaining the original scene objects.
 * The original GameObject continues to own actions and collision.
 */
@Singleton
@Slf4j
class PohCosmeticTransmogsManager
{
	private static final int SCALE_TRANSITION_DURATION = 30;
	private final Client client;
	private final Catalogue.ModelFactory modelFactory;
	private final Map<String, String> selections = new HashMap<>();
	private final Map<ModelCacheKey, Model> modelCache = new HashMap<>();
	private final Map<Integer, TargetBinding> targetsById = new HashMap<>();
	private final Set<String> reportedModelFailures = new HashSet<>();
	private final Set<TileObject> sceneObjects = identitySet();
	private final Set<TileObject> retiredObjects = identitySet();
	private final Set<TileObject> suppressedObjects = identitySet();
	private final Map<TileObject, RuneLiteObject> activeReplacements = new IdentityHashMap<>();
	private final Map<RuneLiteObject, RuneLiteObject> transitionEffects = new IdentityHashMap<>();
	private final Map<PlacementKey, TileObject> scenePlacements = new HashMap<>();
	private final Map<PlacementKey, Integer> recentDespawnedStates = new HashMap<>();
	private final Set<WorldView> worldViews = identitySet();
	private final Set<ZoneKey> pendingZoneInvalidations = new HashSet<>();
	private final Map<Integer, Integer> visiblePlanes = new HashMap<>();
	private final Set<String> playedSpawnAnimations = new HashSet<>();
	private volatile Set<TileObject> suppressedSnapshot = Collections.emptySet();
	private volatile Set<TileObject> activeSnapshot = Collections.emptySet();
	private volatile boolean running;
	private volatile boolean hidden;
	private final Set<TileObject> pendingModels = identitySet();
	private Catalogue catalogue = Catalogue.current;
	private int zoneInvalidationBatchDepth;
	private boolean snapshotsDirty;

	@Inject
	PohCosmeticTransmogsManager(Client client, PohCosmeticTransmogsConfig config)
	{
		this.client = client;
		modelFactory = new Catalogue.ModelFactory(client, config);
	}

	void start(Map<String, String> initialSelections, boolean hidden)
	{
		DrawCallbacks callbacks = client.getDrawCallbacks();
		log.debug("Starting PoH furniture transmogs with renderer {}",
			callbacks == null ? "software" : callbacks.getClass().getName());
		this.hidden = hidden;
		running = true;
		catalogue = Catalogue.current;
		rebuildTargets();
		setSelections(initialSelections);
		rescanLoadedWorldViews();
		log.debug("PoH furniture scan found {} targets and activated {} replacements",
			sceneObjects.size(), activeReplacements.size());
	}

	void stop()
	{
		running = false;
		deactivateAll();
		// GPU/117 HD cache scenery by zone. Shutdown unregisters the render
		// callback before this client-thread cleanup, so rebuilding these zones
		// restores every original object as the RuneLiteObjects are removed.
		invalidateAllKnownZones();
		sceneObjects.clear();
		scenePlacements.clear();
		recentDespawnedStates.clear();
		worldViews.clear();
		retiredObjects.clear();
		modelCache.clear();
		targetsById.clear();
		reportedModelFailures.clear();
		visiblePlanes.clear();
		playedSpawnAnimations.clear();
		selections.clear();
		pendingZoneInvalidations.clear();
		zoneInvalidationBatchDepth = 0;
		snapshotsDirty = false;
		pendingModels.clear();
	}

	void setSelections(Map<String, String> updated)
	{
		if (!running || selections.equals(updated))
		{
			return;
		}
		Set<String> changed = new HashSet<>(selections.keySet());
		changed.addAll(updated.keySet());
		changed.removeIf(key -> java.util.Objects.equals(selections.get(key), updated.get(key)));
		selections.clear();
		selections.putAll(updated);
		reportedModelFailures.clear();
		rebuildTargets();
		refreshObjects(changed);
		rescanLoadedWorldViews();
	}

	void setCatalogue(Catalogue catalogue)
	{
		this.catalogue = catalogue;
		if (!running)
		{
			return;
		}
		deactivateAll();
		invalidateAllKnownZones();
		clearSceneState();
		modelCache.clear();
		reportedModelFailures.clear();
		pendingModels.clear();
		rebuildTargets();
		rescanLoadedWorldViews();
	}

	void setHidden(boolean hidden)
	{
		if (!running)
		{
			return;
		}
		this.hidden = hidden;
		if (hidden)
		{
			deactivateAll();
			invalidateAllKnownZones();
		}
		else
		{
			refreshAllObjects();
		}
	}

	void refreshColours()
	{
		if (!running)
		{
			return;
		}
		beginZoneInvalidationBatch();
		try
		{
			modelCache.clear();
			refreshAllObjects();
		}
		finally
		{
			endZoneInvalidationBatch();
		}
	}

	void loadMissingModels()
	{
		if (running && !hidden)
		{
			for (TileObject object : new ArrayList<>(pendingModels))
			{
				refreshObject(object);
			}
		}
	}

	void addObject(@Nullable TileObject object)
	{
		if (!running || !(object instanceof GameObject))
		{
			return;
		}
		GameObject gameObject = (GameObject) object;
		TargetBinding target = targetsById.get(object.getId());
		if (target == null)
		{
			return;
		}
		if (sceneObjects.contains(object))
		{
			return;
		}

		// Construction state changes may spawn the new GameObject before the old
		// one despawns. Retire the indexed prior state for this exact placement.
		PlacementKey placement = new PlacementKey(gameObject, target);
		TileObject candidate = scenePlacements.get(placement);
		Integer recentState = recentDespawnedStates.remove(placement);
		int previousId = candidate != null && candidate != object
			? candidate.getId() : recentState == null ? -1 : recentState;
		boolean stateTransition = previousId >= 0
			&& target == targetsById.get(previousId)
			&& target.target.isOpen(previousId) != target.target.isOpen(object.getId());
		boolean opening = target.target.isOpen(object.getId());
		if (candidate != null && candidate != object)
		{
			// Suppress the retired state until its despawn event so it cannot flash.
			retiredObjects.add(candidate);
			suppressedObjects.add(candidate);
			deactivate(candidate);
			sceneObjects.remove(candidate);
			publishSnapshots();
		}
		if (sceneObjects.add(object))
		{
			scenePlacements.put(placement, object);
			worldViews.add(object.getWorldView());
			log.debug("Matched target object {} in world view {}",
				object.getId(), object.getWorldView());
			refreshObject(object);
			if (stateTransition)
			{
				transition(object, target, opening);
			}
		}
	}

	void removeObject(@Nullable TileObject object)
	{
		if (object != null)
		{
			deactivate(object);
			pendingModels.remove(object);
			boolean tracked = sceneObjects.remove(object);
			if (object instanceof GameObject)
			{
				TargetBinding target = targetsById.get(object.getId());
				PlacementKey placement = new PlacementKey((GameObject) object, target);
				// Late despawns from a retired state must not replace the current state.
				if (tracked && target != null && target.target.isStateful())
				{
					recentDespawnedStates.put(placement, object.getId());
				}
				scenePlacements.remove(placement, object);
			}
			retiredObjects.remove(object);
			suppressedObjects.remove(object);
			publishSnapshots();
		}
	}

	void removeWorldView(WorldView worldView)
	{
		visiblePlanes.remove(worldView.getId());
		worldViews.remove(worldView);
		beginZoneInvalidationBatch();
		try
		{
			for (TileObject object : new ArrayList<>(sceneObjects))
			{
				if (object.getWorldView() == worldView)
				{
					removeObject(object);
				}
			}
			recentDespawnedStates.keySet().removeIf(key -> key.worldView == worldView);
			retiredObjects.removeIf(object -> object.getWorldView() == worldView);
			suppressedObjects.removeIf(object -> object.getWorldView() == worldView);
			publishSnapshots();
		}
		finally
		{
			endZoneInvalidationBatch();
		}
	}

	void clearSceneState()
	{
		pendingModels.clear();
		deactivateAll();
		sceneObjects.clear();
		scenePlacements.clear();
		recentDespawnedStates.clear();
		worldViews.clear();
		retiredObjects.clear();
		playedSpawnAnimations.clear();
		visiblePlanes.clear();
	}

	void reconcileLoadedWorldViews()
	{
		beginZoneInvalidationBatch();
		try
		{
			rescanLoadedWorldViews();
			for (TileObject object : sceneObjects)
			{
				if (!activeReplacements.containsKey(object))
				{
					refreshObject(object);
				}
			}
		}
		finally
		{
			endZoneInvalidationBatch();
		}
	}

	void scanWorldView(@Nullable WorldView worldView)
	{
		if (worldView == null || worldView.getScene() == null)
		{
			return;
		}
		worldViews.add(worldView);
		Tile[][][] tiles = worldView.getScene().getTiles();
		if (tiles == null)
		{
			return;
		}
		beginZoneInvalidationBatch();
		try
		{
			for (Tile[][] plane : tiles)
			{
				if (plane == null)
				{
					continue;
				}
				for (Tile[] column : plane)
				{
					if (column == null)
					{
						continue;
					}
					for (Tile tile : column)
					{
						if (tile != null)
						{
							GameObject[] objects = tile.getGameObjects();
							if (objects != null)
							{
								for (GameObject object : objects)
								{
									addObject(object);
								}
							}
						}
					}
				}
			}
		}
		finally
		{
			endZoneInvalidationBatch();
		}
	}

	void syncVisibleLevels()
	{
		boolean changed = false;
		for (WorldView worldView : worldViews)
		{
			Integer previous = visiblePlanes.put(worldView.getId(), worldView.getPlane());
			changed |= previous == null || previous != worldView.getPlane();
		}
		if (changed)
		{
			refreshAllObjects();
		}
	}

	boolean shouldDrawObject(TileObject object)
	{
		// Suppress drawing, not scene membership: the original supplies menu actions and picking.
		Set<TileObject> snapshot = suppressedSnapshot;
		return !running || hidden || snapshot.isEmpty() || !snapshot.contains(object);
	}

	Set<TileObject> getActiveObjects()
	{
		return activeSnapshot;
	}

	boolean isSupportedRenderer()
	{
		return isSupportedRenderer(client.getDrawCallbacks());
	}

	private boolean isSupportedRenderer(@Nullable DrawCallbacks callbacks)
	{
		return client.isGpu() && callbacks != null;
	}

	private void rebuildTargets()
	{
		targetsById.clear();
		targetsById.putAll(catalogue.bind(selections));
	}

	@Nullable
	private Model loadState(TargetBinding binding, GameObject object, boolean open)
	{
		Catalogue.Definition state = binding.appearance.state(open);
		return loadModel(binding, state, binding.appearance.stateKey(state),
			binding.calibration(object.sizeX(), object.sizeY()));
	}

	@Nullable
	private Model loadModel(TargetBinding binding, Catalogue.Definition definition, String stateKey,
		Catalogue.Calibration calibration)
	{
		ModelCacheKey key = new ModelCacheKey(binding.appearance.key, stateKey, definition.getModelIds(),
			modelFactory.recoloursPortal(binding.target, definition), calibration);
		Model model = modelCache.get(key);
		if (model == null)
		{
			model = modelFactory.load(binding.target, definition, calibration);
			if (model != null)
			{
				modelCache.put(key, model);
			}
		}
		return model;
	}

	private void refreshAllObjects()
	{
		refreshObjects(null);
	}

	private void refreshObjects(@Nullable Set<String> targetKeys)
	{
		beginZoneInvalidationBatch();
		try
		{
			for (TileObject object : sceneObjects)
			{
				if (targetKeys == null || changedTarget(object.getId(), targetKeys))
				{
					refreshObject(object);
				}
			}
		}
		finally
		{
			endZoneInvalidationBatch();
		}
	}

	private boolean changedTarget(int objectId, Set<String> keys)
	{
		for (String key : keys)
		{
			TargetSpec target = catalogue.targets.get(key);
			if (target != null)
			{
				for (int id : target.objectIds)
				{
					if (id == objectId)
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	private void refreshObject(TileObject object)
	{
		if (!(object instanceof GameObject))
		{
			deactivate(object);
			return;
		}
		GameObject gameObject = (GameObject) object;
		ResolvedReplacement resolved = resolve(gameObject);
		if (hidden || resolved == null)
		{
			deactivate(object);
			if (!retiredObjects.contains(object))
			{
				suppressedObjects.remove(object);
				publishSnapshots();
			}
			invalidateZone(object);
			return;
		}
		Catalogue.Definition definition = resolved.definition;
		Catalogue.Calibration calibration = resolved.calibration;
		Model model = resolved.model;
		WorldView worldView = object.getWorldView();
		if (worldView == null || !isVisibleLevel(object.getPlane(), worldView.getPlane()))
		{
			deactivate(object);
			suppressedObjects.add(object);
			publishSnapshots();
			invalidateZone(object);
			return;
		}

		String appearanceKey = resolved.appearance.key;
		RuneLiteObject replacement = resolved.appearance.bobbing
			? new BobbingRuneLiteObject(client) : client.createRuneLiteObject();
		replacement.setModel(model);
		int baseOrientation = (gameObject.getOrientation()
			+ targetsById.get(gameObject.getId()).target.orientationOffset) & 2047;
		int correction = calibration.getRotation();
		int defaultOrientation = (baseOrientation + correction) & 2047;
		LocalPoint anchor = occupiedTileCentre(gameObject);
		if (calibration.getOffsetX() != 0 || calibration.getOffsetY() != 0)
		{
			anchor = offsetAnchor(anchor, baseOrientation,
				calibration.getOffsetX(), calibration.getOffsetY());
		}
		replacement.setLocation(anchor, object.getPlane());
		replacement.setZ(object.getZ());
		if (replacement instanceof BobbingRuneLiteObject)
		{
			((BobbingRuneLiteObject) replacement).setBaseZ(object.getZ());
		}
		replacement.setOrientation(defaultOrientation);
		int occupiedSpan = Math.max(definition.getSizeX(), definition.getSizeY());
		replacement.setRadius(Math.max(60, 64 * occupiedSpan - 4));

		if (definition.getAnimationId() >= 0)
		{
			Animation animation = client.loadAnimation(definition.getAnimationId());
			if (animation != null)
			{
				PostTransformAnimationController controller = new PostTransformAnimationController(
					client, animation, calibration);
				replacement.setAnimationController(controller);
				Animation spawn = definition.getSpawnAnimationId() < 0 ? null
					: client.loadAnimation(definition.getSpawnAnimationId());
				String spawnKey = spawnAnimationKey(gameObject);
				if (spawn != null && (!definition.isSpawnOnce()
					|| !playedSpawnAnimations.contains(spawnKey)))
				{
					controller.setAnimation(spawn);
					if (definition.isSpawnOnce())
					{
						controller.waitUntil = () -> spawnVisible(gameObject);
					}
					controller.setOnFinished(c ->
					{
						if (definition.isSpawnOnce())
						{
							playedSpawnAnimations.add(spawnKey);
						}
						c.setAnimation(animation);
						c.setOnFinished(AnimationController::loop);
					});
				}
				else
				{
					controller.setOnFinished(AnimationController::loop);
				}
			}
		}

		// Activate first, then atomically make this the replacement used to suppress
		// the original. This avoids a frame where the original reappears between two
		// replacement models during state, config, and renderer changes.
		replacement.setActive(true);
		boolean newlySuppressed = suppressedObjects.add(object);
		RuneLiteObject previous = activeReplacements.put(object, replacement);
		deactivateReplacement(previous);
		if (previous == null || newlySuppressed)
		{
			publishSnapshots();
		}
		log.debug("Activated appearance {} for target object {}",
			appearanceKey, object.getId());
		invalidateZone(object);
	}

	@Nullable
	private ResolvedReplacement resolve(GameObject object)
	{
		TargetBinding target = targetsById.get(object.getId());
		pendingModels.remove(object);
		if (target == null)
		{
			return null;
		}
		Catalogue.Definition definition = target.state(object.getId());
		Catalogue.Calibration calibration = target.calibration(object.sizeX(), object.sizeY());
		Model model = loadModel(target, definition, target.appearance.stateKey(definition), calibration);
		if (model == null)
		{
			pendingModels.add(object);
			if (reportedModelFailures.add(target.appearance.key))
			{
				log.warn("Unable to load appearance {}", target.appearance.key);
			}
			return null;
		}
		return new ResolvedReplacement(definition, model, target.appearance, calibration);
	}

	private void rescanLoadedWorldViews()
	{
		WorldView top = client.getTopLevelWorldView();
		if (top == null)
		{
			return;
		}
		beginZoneInvalidationBatch();
		try
		{
			scanWorldView(top);
			for (WorldView worldView : top.worldViews())
			{
				scanWorldView(worldView);
			}
		}
		finally
		{
			endZoneInvalidationBatch();
		}
	}

	private void startModelTransition(
		TileObject object, TargetBinding binding, boolean opening)
	{
		Catalogue.Recipe appearance = binding.appearance;
		Catalogue.Definition source = appearance;
		Animation animation = client.loadAnimation(appearance.transitionAnimationId);
		RuneLiteObject replacement = activeReplacements.get(object);
		if (animation == null || replacement == null)
		{
			return;
		}
		Model open = loadState(binding, (GameObject) object, true);
		Model closed = loadState(binding, (GameObject) object, false);
		Catalogue.Calibration calibration =
			binding.calibration(((GameObject) object).sizeX(), ((GameObject) object).sizeY());
		Model transition = loadTransitionModel(
			binding, source, appearance.transitionModelId, appearance.transitionAnimationId, calibration);
		if (open == null || closed == null || transition == null)
		{
			return;
		}

		// Hand the chest over before the animated model removes its copy. Closing
		// swaps the stable open chest directly to the closed model at the same point.
		replacement.setModel(open);
		replacement.setAnimationController(null);
		replacement.setActive(!opening);
		RuneLiteObject effect = client.createRuneLiteObject();
		effect.setModel(transition);
		effect.setLocation(replacement.getLocation(), replacement.getLevel());
		effect.setZ(replacement.getZ());
		effect.setOrientation(replacement.getOrientation());
		effect.setRadius(replacement.getRadius());
		PostTransformAnimationController controller =
			new PostTransformAnimationController(client, animation, calibration, !opening);
		controller.setTransitionPoint(appearance.transitionHandoff, () ->
		{
			replacement.setModel(opening ? open : closed);
			replacement.setActive(true);
		});
		Runnable finished = () ->
		{
			effect.setActive(false);
			transitionEffects.remove(replacement);
			replacement.setModel(opening ? open : closed);
			replacement.setActive(true);
		};
		if (opening)
		{
			controller.setOnFinished(ignored -> finished.run());
		}
		else
		{
			controller.setReverseFinished(finished);
		}
		effect.setAnimationController(controller);
		effect.setActive(true);
		transitionEffects.put(replacement, effect);
	}

	private void reverseOpenTransition(TileObject object, TargetBinding binding)
	{
		RuneLiteObject replacement = activeReplacements.get(object);
		Animation transition = client.loadAnimation(binding.appearance.open.getSpawnAnimationId());
		if (replacement == null || transition == null)
		{
			return;
		}
		Catalogue.Calibration calibration =
			binding.calibration(((GameObject) object).sizeX(), ((GameObject) object).sizeY());
		PostTransformAnimationController controller =
			new PostTransformAnimationController(client, transition, calibration, true);
		controller.setReverseFinished(() ->
		{
			if (activeReplacements.get(object) != replacement)
			{
				return;
			}
			Animation idle = client.loadAnimation(binding.appearance.closed.getAnimationId());
			replacement.setAnimationController(idle == null ? null
				: new PostTransformAnimationController(client, idle, calibration));
		});
		replacement.setAnimationController(controller);
	}

	private void startScaleTransition(TileObject object, TargetBinding binding, boolean opening)
	{
		Catalogue.Definition source = binding.appearance;
		RuneLiteObject replacement = activeReplacements.get(object);
		if (source == null || replacement == null)
		{
			return;
		}
		Animation idle = source.getAnimationId() < 0
			? null : client.loadAnimation(source.getAnimationId());
		Catalogue.Calibration calibration =
			binding.calibration(((GameObject) object).sizeX(), ((GameObject) object).sizeY());
		replacement.setAnimationController(new ScaleTransitionController(
			client, idle, calibration, opening, SCALE_TRANSITION_DURATION));
	}

	@Nullable
	private Model loadTransitionModel(TargetBinding binding,
		Catalogue.Definition source, int modelId, int animationId,
		Catalogue.Calibration calibration)
	{
		Catalogue.Definition transition = new Catalogue.Definition(
			source.getSizeX(), source.getSizeY(),
			new int[] {modelId}, animationId);
		transition.recolorFrom = source.getRecolorFrom();
		transition.recolorTo = source.getRecolorTo();
		transition.crystalColours = source.getCrystalColours();
		return loadModel(binding, transition, "transition", calibration);
	}

	private boolean spawnVisible(GameObject object)
	{
		Widget loading = client.getWidget(InterfaceID.PohLoading.UNIVERSE);
		WorldView world = object.getWorldView();
		if (client.getGameState() != GameState.LOGGED_IN
			|| loading != null && !loading.isHidden()
			|| world == null || !isVisibleLevel(object.getPlane(), world.getPlane()))
		{
			return false;
		}
		Point point = Perspective.localToCanvas(client, occupiedTileCentre(object), object.getPlane());
		return point != null && point.getX() >= client.getViewportXOffset()
			&& point.getX() < client.getViewportXOffset() + client.getViewportWidth()
			&& point.getY() >= client.getViewportYOffset()
			&& point.getY() < client.getViewportYOffset() + client.getViewportHeight();
	}

	private static void applyCalibration(Model model,
		Catalogue.Calibration calibration)
	{
		int scaleX = calibration.signedScaleX();
		if (scaleX != 128 || calibration.getScaleHeight() != 128 || calibration.getScaleY() != 128)
		{
			model.scale(scaleX, calibration.getScaleHeight(), calibration.getScaleY());
		}
		if (calibration.getOffsetHeight() != 0)
		{
			model.translate(0, calibration.getOffsetHeight(), 0);
		}
	}

	private static LocalPoint occupiedTileCentre(GameObject object)
	{
		int x = (object.getSceneMinLocation().getX() + object.getSceneMaxLocation().getX() + 1) * 64;
		int y = (object.getSceneMinLocation().getY() + object.getSceneMaxLocation().getY() + 1) * 64;
		return new LocalPoint(x, y, object.getWorldView().getId());
	}

	private static LocalPoint offsetAnchor(LocalPoint point, int orientation, int offsetX, int offsetY)
	{
		double radians = orientation * Math.PI / 1024.0;
		double sin = Math.sin(radians);
		double cos = Math.cos(radians);
		int x = point.getX() + (int) Math.round(cos * offsetX + sin * offsetY);
		int y = point.getY() + (int) Math.round(cos * offsetY - sin * offsetX);
		return new LocalPoint(x, y, point.getWorldView());
	}

	private String spawnAnimationKey(GameObject object)
	{
		TargetBinding target = targetsById.get(object.getId());
		return object.getWorldView().getId() + ":" + object.getPlane() + ":"
			+ object.getSceneMinLocation().getX() + ":"
			+ object.getSceneMinLocation().getY() + ":"
			+ (target == null ? object.getId() : target.target.key);
	}

	static boolean isVisibleLevel(int objectPlane, int activePlane)
	{
		return objectPlane == activePlane || activePlane == 2 && objectPlane == 1;
	}

	private void deactivate(TileObject object)
	{
		RuneLiteObject replacement = activeReplacements.remove(object);
		deactivateReplacement(replacement);
		if (replacement != null)
		{
			publishSnapshots();
		}
	}

	private void deactivateReplacement(@Nullable RuneLiteObject replacement)
	{
		if (replacement != null)
		{
			replacement.setActive(false);
			RuneLiteObject effect = transitionEffects.remove(replacement);
			if (effect != null)
			{
				effect.setActive(false);
			}
		}
	}

	private void deactivateAll()
	{
		for (RuneLiteObject replacement : activeReplacements.values())
		{
			deactivateReplacement(replacement);
		}
		activeReplacements.clear();
		suppressedObjects.clear();
		publishSnapshots();
	}

	private void publishSnapshots()
	{
		if (zoneInvalidationBatchDepth > 0)
		{
			snapshotsDirty = true;
			return;
		}
		suppressedSnapshot = immutableIdentitySnapshot(suppressedObjects);
		activeSnapshot = immutableIdentitySnapshot(activeReplacements.keySet());
		snapshotsDirty = false;
	}

	private static <T> Set<T> identitySet()
	{
		return Collections.newSetFromMap(new IdentityHashMap<>());
	}

	private static <T> Set<T> immutableIdentitySnapshot(Collection<T> values)
	{
		if (values.isEmpty())
		{
			return Collections.emptySet();
		}
		Set<T> snapshot = identitySet();
		snapshot.addAll(values);
		return Collections.unmodifiableSet(snapshot);
	}

	private void invalidateAllKnownZones()
	{
		beginZoneInvalidationBatch();
		try
		{
			for (TileObject object : sceneObjects)
			{
				invalidateZone(object);
			}
		}
		finally
		{
			endZoneInvalidationBatch();
		}
	}

	private void invalidateZone(TileObject object)
	{
		DrawCallbacks callbacks = client.getDrawCallbacks();
		WorldView worldView = object.getWorldView();
		if (!isSupportedRenderer(callbacks) || worldView == null || worldView.getScene() == null)
		{
			return;
		}

		GameObject gameObject = (GameObject) object;
		int zoneX = gameObject.getSceneMinLocation().getX() >> 3;
		int zoneY = gameObject.getSceneMinLocation().getY() >> 3;
		if (worldView.getId() == WorldView.TOPLEVEL)
		{
			int extendedOffset = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;
			zoneX += extendedOffset >> 3;
			zoneY += extendedOffset >> 3;
		}
		int size = worldView.getId() == WorldView.TOPLEVEL
			? Constants.EXTENDED_SCENE_SIZE : Math.max(worldView.getSizeX(), worldView.getSizeY());
		int zoneCount = (size + 7) >> 3;
		if (zoneX >= 0 && zoneY >= 0 && zoneX < zoneCount && zoneY < zoneCount)
		{
			ZoneKey zone = new ZoneKey(worldView.getScene(), zoneX, zoneY);
			if (zoneInvalidationBatchDepth > 0)
			{
				pendingZoneInvalidations.add(zone);
			}
			else
			{
				callbacks.invalidateZone(zone.scene, zone.x, zone.y);
			}
		}
	}

	private void beginZoneInvalidationBatch()
	{
		zoneInvalidationBatchDepth++;
	}

	private void endZoneInvalidationBatch()
	{
		if (--zoneInvalidationBatchDepth != 0)
		{
			return;
		}
		if (snapshotsDirty)
		{
			publishSnapshots();
		}
		DrawCallbacks callbacks = client.getDrawCallbacks();
		try
		{
			if (isSupportedRenderer(callbacks))
			{
				for (ZoneKey zone : pendingZoneInvalidations)
				{
					callbacks.invalidateZone(zone.scene, zone.x, zone.y);
				}
			}
		}
		finally
		{
			pendingZoneInvalidations.clear();
		}
	}

	private void transition(TileObject object, TargetBinding binding, boolean opening)
	{
		Catalogue.Recipe appearance = binding.appearance;
		if (!binding.target.isStateful())
		{
			return;
		}
		if (appearance.transitionModelId >= 0)
		{
			startModelTransition(object, binding, opening);
		}
		else if (!opening && appearance.open.getSpawnAnimationId() >= 0)
		{
			reverseOpenTransition(object, binding);
		}
		else if (binding.target.scaleTransition && appearance.closed == appearance.open)
		{
			startScaleTransition(object, binding, opening);
		}
	}
	@Value
	private static class ResolvedReplacement
	{
		Catalogue.Definition definition;
		Model model;
		Catalogue.Recipe appearance;
		Catalogue.Calibration calibration;
	}

	@Value
	private static class ModelCacheKey
	{
		String appearanceKey;
		String stateKey;
		int[] modelIds;
		boolean portalRecolour;
		int scaleX;
		int scaleHeight;
		int scaleY;
		int offsetHeight;
		boolean flipX;
		@EqualsAndHashCode.Exclude
		Catalogue.Calibration calibration;

		private ModelCacheKey(String appearanceKey, String stateKey, int[] modelIds, boolean portalRecolour,
			Catalogue.Calibration calibration)
		{
			this.appearanceKey = appearanceKey;
			this.stateKey = stateKey;
			this.modelIds = modelIds;
			this.portalRecolour = portalRecolour;
			scaleX = calibration.getScaleX();
			scaleHeight = calibration.getScaleHeight();
			scaleY = calibration.getScaleY();
			offsetHeight = calibration.getOffsetHeight();
			flipX = calibration.isFlipX();
			this.calibration = calibration;
		}
	}

	private static final class PlacementKey
	{
		private final WorldView worldView;
		private final int plane;
		private final int x;
		private final int y;
		private final String target;

		private PlacementKey(GameObject object, @Nullable TargetBinding target)
		{
			worldView = object.getWorldView();
			plane = object.getPlane();
			x = object.getSceneMinLocation().getX();
			y = object.getSceneMinLocation().getY();
			this.target = target == null ? Integer.toString(object.getId()) : target.target.key;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof PlacementKey))
			{
				return false;
			}
			PlacementKey key = (PlacementKey) other;
			return worldView == key.worldView && plane == key.plane
				&& x == key.x && y == key.y && target.equals(key.target);
		}

		@Override
		public int hashCode()
		{
			int hash = System.identityHashCode(worldView);
			hash = 31 * hash + plane;
			hash = 31 * hash + x;
			hash = 31 * hash + y;
			return 31 * hash + target.hashCode();
		}
	}

	private static final class ZoneKey
	{
		private final Scene scene;
		private final int x;
		private final int y;

		private ZoneKey(Scene scene, int x, int y)
		{
			this.scene = scene;
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof ZoneKey))
			{
				return false;
			}
			ZoneKey zone = (ZoneKey) other;
			return scene == zone.scene && x == zone.x && y == zone.y;
		}

		@Override
		public int hashCode()
		{
			int hash = 31 * System.identityHashCode(scene) + x;
			return 31 * hash + y;
		}
	}

	/** Applies visual fitting to the completed pose, including its animated translations. */
	private static final class PostTransformAnimationController extends AnimationController
	{
		private final Catalogue.Calibration calibration;
		private final boolean reverse;
		private int reverseElapsedTicks;
		private Runnable reverseFinished = () -> { };
		private Runnable transitionPoint;
		private int transitionPercent = 50;
		private boolean reverseFinishedRun;
		private BooleanSupplier waitUntil;

		private PostTransformAnimationController(Client client, Animation animation,
			Catalogue.Calibration calibration)
		{
			this(client, animation, calibration, false);
		}

		private PostTransformAnimationController(Client client, Animation animation,
			Catalogue.Calibration calibration, boolean reverse)
		{
			super(client, animation);
			this.calibration = calibration;
			this.reverse = reverse;
			if (reverse)
			{
				setFrame(Math.max(0, animation.getDuration() - 1));
			}
		}

		@Override
		public void tick(int ticks)
		{
			if (waitUntil != null)
			{
				if (!waitUntil.getAsBoolean())
				{
					return;
				}
				waitUntil = null;
				// Do not consume time accumulated while the scene was hidden.
				return;
			}
			Animation animation = getAnimation();
			if (animation == null)
			{
				return;
			}
			if (!reverse)
			{
				super.tick(ticks);
				runTransitionPoint(animation);
				return;
			}
			if (animation.isMayaAnim())
			{
				setFrame(Math.max(0, getFrame() - ticks));
				runTransitionPoint(animation);
				finishReverseAtFirstFrame();
				return;
			}
			reverseElapsedTicks += ticks;
			int[] frameLengths = animation.getFrameLengths();
			while (getFrame() > 0 && reverseElapsedTicks > frameLengths[getFrame()])
			{
				reverseElapsedTicks -= frameLengths[getFrame()];
				setFrame(getFrame() - 1);
			}
			runTransitionPoint(animation);
			finishReverseAtFirstFrame();
		}

		private void setTransitionPoint(int percent, Runnable action)
		{
			transitionPercent = percent;
			transitionPoint = action;
		}

		private void runTransitionPoint(Animation animation)
		{
			if (transitionPoint == null)
			{
				return;
			}
			int duration = animation.getDuration();
			int frame = duration * (reverse ? 100 - transitionPercent : transitionPercent) / 100;
			if (reverse ? getFrame() <= frame : getFrame() >= frame)
			{
				Runnable action = transitionPoint;
				transitionPoint = null;
				action.run();
			}
		}

		private void setReverseFinished(Runnable reverseFinished)
		{
			this.reverseFinished = reverseFinished;
		}

		private void finishReverseAtFirstFrame()
		{
			if (getFrame() == 0 && !reverseFinishedRun)
			{
				reverseFinishedRun = true;
				reverseFinished.run();
			}
		}

		@Override
		public Model animate(Model model, @Nullable AnimationController other)
		{
			Model posed = super.animate(model, other);
			applyCalibration(posed, calibration);
			return posed;
		}
	}

	private static final class ScaleTransitionController extends AnimationController
	{
		private final Client client;
		private static final int OPEN_SCALE = 160;
		private final Catalogue.Calibration calibration;
		private final boolean opening;
		private final int duration;
		private int elapsed;
		private Model finalPose;

		private ScaleTransitionController(Client client, @Nullable Animation animation,
			Catalogue.Calibration calibration, boolean opening, int duration)
		{
			super(client, animation);
			this.client = client;
			this.calibration = calibration;
			this.opening = opening;
			this.duration = duration;
		}

		@Override
		public void tick(int ticks)
		{
			if (getAnimation() != null)
			{
				super.tick(ticks);
			}
			elapsed = Math.min(duration, elapsed + ticks);
		}

		@Override
		public Model animate(Model model, @Nullable AnimationController other)
		{
			if (finalPose != null)
			{
				return finalPose;
			}
			Model posed = super.animate(model, other);
			if (getAnimation() != null)
			{
				applyCalibration(posed, calibration);
			}
			posed.calculateBoundsCylinder();
			int bottom = posed.getBottomY();
			int delta = (OPEN_SCALE - 128) * elapsed / duration;
			int scale = opening ? 128 + delta : OPEN_SCALE - delta;
			posed.scale(scale, scale, scale);
			posed.translate(0, bottom - bottom * scale / 128, 0);
			if (elapsed == duration && getAnimation() == null && other == null)
			{
				// Animation transforms use scratch models; retain an independent copy.
				finalPose = client.mergeModels(posed);
			}
			return posed;
		}
	}

	private static final class BobbingRuneLiteObject extends RuneLiteObject
	{
		private static final int BOB_CYCLE = 160;
		private static final int BOB_HEIGHT = 4;
		private int baseZ;
		private int phase;

		private BobbingRuneLiteObject(Client client)
		{
			super(client);
		}

		private void setBaseZ(int baseZ)
		{
			this.baseZ = baseZ;
		}

		@Override
		public void tick(int ticks)
		{
			super.tick(ticks);
			phase = (phase + ticks) % BOB_CYCLE;
			setZ(baseZ - Math.round(BOB_HEIGHT *
				(float) Math.sin(phase * Math.PI * 2 / BOB_CYCLE)));
		}
	}
}
