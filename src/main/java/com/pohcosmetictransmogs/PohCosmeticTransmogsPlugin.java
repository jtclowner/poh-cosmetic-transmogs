package com.pohcosmetictransmogs;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "PoH Cosmetic Transmogs",
	description = "Transmog specific PoH slot and props into curated, whitelisted cosmetic objects",
	tags = {"poh", "house", "slot", "cosmetic", "transmog"}
)
@Slf4j
public class PohCosmeticTransmogsPlugin extends Plugin
{
	private static final int RENDERER_STARTUP_GRACE_TICKS = 5;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private PohCosmeticTransmogsConfig config;

	@Inject
	private PohCosmeticTransmogsManager manager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PohCosmeticTransmogsOverlay overlay;

	private final RenderCallback renderCallback = new RenderCallback()
	{
		@Override
		public boolean drawObject(Scene scene, TileObject object)
		{
			return manager.shouldDrawObject(object);
		}
	};

	private int sceneScanTicksRemaining;
	private volatile boolean enabled;
	private boolean managerStarted;
	private int rendererWaitTicks;
	private boolean overlayAdded;

	@Provides
	PohCosmeticTransmogsConfig provideConfig(ConfigManager manager, Gson gson)
	{
		Catalogue.current = Catalogue.loadCatalogue(gson, null, Catalogue.openCatalogueReader());
		return manager.getConfig(PohCosmeticTransmogsConfig.class);
	}

	@Override
	protected void startUp()
	{
		enabled = true;
		managerStarted = false;
		rendererWaitTicks = 0;
		renderCallbackManager.register(renderCallback);
		updateShapeOverlay();
		clientThread.invokeLater(() ->
		{
			if (!enabled)
			{
				return;
			}
			reloadCatalogue();
			Map<String, String> selections = readSelections();
			if (manager.isSupportedRenderer())
			{
				startManager(selections);
			}
		});
	}

	@Override
	protected void shutDown()
	{
		enabled = false;
		renderCallbackManager.unregister(renderCallback);
		updateShapeOverlay();
		clientThread.invoke(manager::stop);
		sceneScanTicksRemaining = 0;
		managerStarted = false;
		rendererWaitTicks = 0;
	}

	@Override
	public void resetConfiguration()
	{
		Map<String, String> selections = new LinkedHashMap<>();
		for (PohTargetSlot slot : PohTargetSlot.values())
		{
			String key = slot.getConfigKey();
			Enum<?> original = slot.option("");
			if (!original.name().equals(configManager.getConfiguration(PohCosmeticTransmogsConfig.GROUP, key)))
			{
				configManager.setConfiguration(PohCosmeticTransmogsConfig.GROUP, key, original);
			}
		}
		clientThread.invokeLater(() -> manager.setSelections(selections));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!PohCosmeticTransmogsConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (PohTargetSlot.isConfigKey(event.getKey()))
		{
			clientThread.invokeLater(() -> manager.setSelections(readSelections()));
			return;
		}

		switch (event.getKey())
		{
			case "pohtransmogs":
				clientThread.invokeLater(this::reloadCatalogue);
				break;
			case "hideFurnitureTransmogs":
				clientThread.invokeLater(() -> manager.setHidden(config.hideFurnitureTransmogs()));
				break;
			case "outlineOriginalClickbox":
				updateShapeOverlay();
				break;
			case "portalColour":
			case "recolourPortalsInAllPositions":
			case "recolourColour":
			case "recolourCoxCrystals":
			case "recolourTobChest":
			case "recolourGauntletChest":
			case "recolourDeadmanChest":
			case "recolourToaSarcophagus":
			case "recolourNodePortal":
				clientThread.invokeLater(manager::refreshColours);
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		clientThread.invokeLater(() ->
		{
			reloadCatalogue();
			manager.setSelections(readSelections());
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!manager.isSupportedRenderer())
		{
			if (managerStarted || ++rendererWaitTicks >= RENDERER_STARTUP_GRACE_TICKS)
			{
				disableForUnsupportedRenderer();
			}
			return;
		}
		rendererWaitTicks = 0;
		if (!managerStarted)
		{
			startManager(readSelections());
		}
		manager.syncVisibleLevels();
		manager.loadMissingModels();
		if (sceneScanTicksRemaining > 0)
		{
			manager.reconcileLoadedWorldViews();
			sceneScanTicksRemaining--;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.HOPPING || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			sceneScanTicksRemaining = 0;
			manager.clearSceneState();
		}
		else if (event.getGameState() == GameState.LOADING)
		{
			manager.clearSceneState();
			sceneScanTicksRemaining = 3;
		}
		else if (event.getGameState() == GameState.LOGGED_IN)
		{
			manager.refreshColours();
			manager.reconcileLoadedWorldViews();
			sceneScanTicksRemaining = Math.max(sceneScanTicksRemaining, 2);
		}
	}

	@Subscribe
	public void onWorldViewLoaded(WorldViewLoaded event)
	{
		manager.scanWorldView(event.getWorldView());
		sceneScanTicksRemaining = Math.max(sceneScanTicksRemaining, 2);
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		manager.removeWorldView(event.getWorldView());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		manager.addObject(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		manager.removeObject(event.getGameObject());
	}

	private void startManager(Map<String, String> selections)
	{
		manager.start(selections, config.hideFurnitureTransmogs());
		managerStarted = true;
	}

	private void disableForUnsupportedRenderer()
	{
		if (!enabled)
		{
			return;
		}
		enabled = false;
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
			"GPU or 117 HD must be enabled to use PoH Cosmetic Transmogs.", null);
		log.warn("PoH Cosmetic Transmogs requires GPU or 117 HD; disabling plugin");
		SwingUtilities.invokeLater(() ->
		{
			pluginManager.setPluginEnabled(this, false);
			try
			{
				pluginManager.stopPlugin(this);
			}
			catch (PluginInstantiationException ex)
			{
				log.error("Unable to stop PoH Cosmetic Transmogs", ex);
			}
		});
	}

	private void updateShapeOverlay()
	{
		boolean shouldAdd = enabled && config.outlineOriginalShape();
		if (shouldAdd == overlayAdded)
		{
			return;
		}
		overlayAdded = shouldAdd;
		if (shouldAdd)
		{
			overlayManager.add(overlay);
		}
		else
		{
			overlayManager.remove(overlay);
			overlay.reset();
		}
	}

	private void reloadCatalogue()
	{
		Catalogue.current = Catalogue.loadCatalogue(gson, client, Catalogue.openCatalogueReader(),
			new StringReader(java.util.Objects.toString(configManager.getConfiguration(
				PohCosmeticTransmogsConfig.GROUP, "pohtransmogs"), "{}")));
		manager.setCatalogue(Catalogue.current);
	}

	private Map<String, String> readSelections()
	{
		Map<String, String> selections = new LinkedHashMap<>();
		for (PohTargetSlot slot : PohTargetSlot.values())
		{
			selections.put(slot.getTargetKey(), readAppearanceKey(slot));
		}
		return selections;
	}

	private String readAppearanceKey(PohTargetSlot slot)
	{
		switch (slot)
		{
			case ENTRANCE_PORTAL: return config.entrancePortal().getAppearanceKey();
			case FANCY_DRESS_BOX: return config.fancyDressBox().getAppearanceKey();
			case MAGIC_WARDROBE: return config.magicWardrobe().getAppearanceKey();
			case TREASURE_CHEST: return config.treasureChest().getAppearanceKey();
			case CAPE_RACK: return config.capeRack().getAppearanceKey();
			case ARMOUR_CASE: return config.armourCase().getAppearanceKey();
			case TOY_BOX: return config.toyBox().getAppearanceKey();
			default: throw new IllegalArgumentException(slot.name());
		}
	}

}
