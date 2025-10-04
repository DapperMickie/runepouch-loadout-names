package com.github.dappermickie.runepouch.loadout.names;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Rune Pouch Loadouts"
)
public class RunepouchLoadoutNamesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private RunepouchLoadoutNamesConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	private static final String LOADOUT_PROMPT_FORMAT = "%s<br>" +
		ColorUtil.prependColorTag("(Limit %s Characters)", new Color(0, 0, 170));
	private int lastRunepouchVarbitValue = 0;

	private int[] getAllSpriteIds()
	{
		List<Integer> spriteIds = new ArrayList<>();
		addStaticIntFields(SpriteID.Magicon2.class, spriteIds);
		addStaticIntFields(SpriteID.LunarMagicOn.class, spriteIds);
		addStaticIntFields(SpriteID.MagicNecroOn.class, spriteIds);
		addStaticIntFields(SpriteID.Prayeron.class, spriteIds);
		addStaticIntFields(SpriteID.IconPrayerZaros01_30x30.class, spriteIds);
		addStaticIntFields(SpriteID.IconBoss25x25.class, spriteIds);
		addStaticIntFields(SpriteID.HiscoresBosses.class, spriteIds);
		addStaticIntFields(SpriteID.FrexRunes.class, spriteIds);
		addStaticIntFields(SpriteID.SideIcons.class, spriteIds);
		addStaticIntFields(SpriteID.Staticons.class, spriteIds);
		addStaticIntFields(SpriteID.Staticons2.class, spriteIds);
		addStaticIntFields(SpriteID.AccountIcons.class, spriteIds);
		addStaticIntFields(SpriteID.OrbIcon.class, spriteIds);
		addStaticIntFields(SpriteID.ToaInvocationIcons.class, spriteIds);
		addStaticIntFields(SpriteID.ToaDifficultyIcons.class, spriteIds);
		addStaticIntFields(SpriteID.HeadiconsPkInterface.class, spriteIds);
		addStaticIntFields(SpriteID.PvpwIcons.class, spriteIds);
		addStaticIntFields(SpriteID.PvpaRankicons.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilIcons.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilSkulls.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilCombatIconsSmall.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilSkillingIconsSmall.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilUtilityIconsSmall.class, spriteIds);
		addStaticIntFields(SpriteID.IconYama34x34.class, spriteIds);
		addStaticIntFields(SpriteID.OptionsIconsSmall.class, spriteIds);
		addStaticIntFields(SpriteID.OptionsIcons.class, spriteIds);
		addStaticIntFields(SpriteID.Emotes.class, spriteIds);
		addStaticIntFields(SpriteID.PengEmotes.class, spriteIds);
		addStaticIntFields(SpriteID.SideiconsInterface.class, spriteIds);
		addStaticIntFields(SpriteID.SideiconsNew.class, spriteIds);
		addStaticIntFields(SpriteID.IiImplingIcons.class, spriteIds);
		addStaticIntFields(SpriteID.Hitmark.class, spriteIds);
		addStaticIntFields(SpriteID.WintIcons.class, spriteIds);
		addStaticIntFields(SpriteID.TinyCombatStaticons.class, spriteIds);
		addStaticIntFields(SpriteID.IconAlchemyChemicals01_27x27.class, spriteIds);
		addStaticIntFields(SpriteID.Dt2Icons.class, spriteIds);
		addStaticIntFields(SpriteID.SoulWarsStaticons.class, spriteIds);
		addStaticIntFields(SpriteID.SoulWarsGameicons.class, spriteIds);
		addStaticIntFields(SpriteID.IronIcons.class, spriteIds);
		addStaticIntFields(SpriteID.ModIconsInterface.class, spriteIds);
		addStaticIntFields(SpriteID.ClanRankIcons.class, spriteIds);
		addStaticIntFields(SpriteID.Mapfunction.class, spriteIds);
		addStaticIntFields(SpriteID.Spectator.class, spriteIds);
		addStaticIntFields(SpriteID.Thumbs.class, spriteIds);
		addStaticIntFields(SpriteID.GodWarsIcons.class, spriteIds);
		addStaticIntFields(SpriteID.OsmStatusIcons.class, spriteIds);
		addStaticIntFields(SpriteID.CaTierSwordsSmall.class, spriteIds);
		addStaticIntFields(SpriteID.LeagueTaskTiers.class, spriteIds);
		addStaticIntFields(SpriteID.LeagueTinyRelic.class, spriteIds);
		addStaticIntFields(SpriteID.LeagueRelics.class, spriteIds);
		addStaticIntFields(SpriteID.TrailblazerRelics.class, spriteIds);
		addStaticIntFields(SpriteID.TrailblazerMapShields.class, spriteIds);
		addStaticIntFields(SpriteID.League3Relics.class, spriteIds);
		addStaticIntFields(SpriteID.League4Relics.class, spriteIds);
		addStaticIntFields(SpriteID.League4MapShields01.class, spriteIds);
		addStaticIntFields(SpriteID.League5Relics.class, spriteIds);
		addStaticIntFields(SpriteID.League5MapShields01.class, spriteIds);
		addStaticIntFields(SpriteID.League5CombatMasterySmall.class, spriteIds);
		addStaticIntFields(SpriteID.League5CombatMasteryTierSmall.class, spriteIds);
		addStaticIntFields(SpriteID.LeagueTrophyIcons.class, spriteIds);
		addStaticIntFields(SpriteID.RomanNumerals.class, spriteIds);
		addStaticIntFields(SpriteID.League3Numerals.class, spriteIds);
		addStaticIntFields(SpriteID.SotnCipher.class, spriteIds);
		addStaticIntFields(SpriteID.MorseCode.class, spriteIds);

		return spriteIds.stream()
			.distinct()
			.mapToInt(Integer::intValue)
			.toArray();
	}

	private void addStaticIntFields(Class<?> clazz, List<Integer> spriteIds)
	{
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			// Check if field is static and int type
			if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
				try {
					field.setAccessible(true);
					int value = field.getInt(null);
					spriteIds.add(value);
				} catch (Exception e) {
					log.warn("Failed to access static int field {} in class {}", field.getName(), clazz.getSimpleName(), e);
				}
			}
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		clientThread.invokeLater(() -> {
			var runepouchWidget = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_CONTAINER);
			if (runepouchWidget != null && !runepouchWidget.isHidden()) {
				reloadRunepouchLoadout();
			}
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientThread.invokeLater(this::resetRunepouchWidget);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		MenuEntry[] actions = event.getMenuEntries();
		MenuEntry firstEntry = event.getFirstEntry();

		Widget widget = firstEntry.getWidget();
		if (widget == null) return;

		var widgetId = widget.getId();

		switch (widgetId)
		{
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_A:
				setLeftClickMenu(1, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_B:
				setLeftClickMenu(2, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_C:
				setLeftClickMenu(3, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_D:
				setLeftClickMenu(4, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_E:
				setLeftClickMenu(5, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_F:
				setLeftClickMenu(6, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_G:
				setLeftClickMenu(7, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_H:
				setLeftClickMenu(8, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_I:
				setLeftClickMenu(9, actions, firstEntry);
				break;
			case InterfaceID.Bankside.RUNEPOUCH_LOAD_J:
				setLeftClickMenu(10, actions, firstEntry);
				break;
		}
	}

	private void setLeftClickMenu(int loadoutId, MenuEntry[] actions, MenuEntry firstEntry)
	{
		var leftClickMenus = new ArrayList<>(actions.length + 1);
		firstEntry
			.setOption("Load")
			.setTarget(getLoadoutName(loadoutId));

		leftClickMenus.add(client.getMenu().createMenuEntry(1)
			.setOption("Rename")
			.setTarget(getLoadoutName(loadoutId))
			.setType(MenuAction.RUNELITE)
			.onClick((MenuEntry e) -> renameLoadout(loadoutId)));

		if (config.enableRunePouchIcons()) {
			leftClickMenus.add(client.getMenu().createMenuEntry(1)
				.setOption("Change")
				.setTarget("Icon")
				.setType(MenuAction.RUNELITE)
				.onClick((MenuEntry e) -> changeLoadoutIcon(loadoutId)));

			leftClickMenus.add(client.getMenu().createMenuEntry(1)
				.setOption("Reset")
				.setTarget("Icon")
				.setType(MenuAction.RUNELITE)
				.onClick((MenuEntry e) -> resetLoadoutIcon(loadoutId)));
		}
	}

	private String getLoadoutName(int id)
	{
		String loadoutName = configManager.getRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id);

		if (loadoutName == null || loadoutName.isEmpty())
		{
			loadoutName = "Loadout " + id;
			configManager.setRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id, loadoutName);
		}

		return loadoutName;
	}

	private void renameLoadout(int id)
	{
		String oldLoadoutName = getLoadoutName(id);
		chatboxPanelManager.openTextInput(String.format(LOADOUT_PROMPT_FORMAT, "Loadout: ", 40))
			.value(Strings.nullToEmpty(oldLoadoutName))
			.onDone((newLoadoutName) ->
			{
				if (newLoadoutName == null) return;

				newLoadoutName = Text.removeTags(newLoadoutName).trim();
				configManager.setRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id, newLoadoutName);
				clientThread.invokeLater(this::reloadRunepouchLoadout);
			}).build();
	}

	private int getLoadoutIcon(int id)
	{
		String loadoutIcon = configManager.getRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id + ".icon");

		if (loadoutIcon == null || loadoutIcon.isEmpty())
		{
			loadoutIcon = String.valueOf(SpriteID.AccManIcons._6);
			configManager.setRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id + ".icon", loadoutIcon);
		}

		return Integer.parseInt(loadoutIcon);
	}

	private void setLoadoutIcon(int id, int icon)
	{
		configManager.setRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id + ".icon", String.valueOf(icon));
		clientThread.invokeLater(this::reloadRunepouchLoadout);
	}

	private void resetLoadoutIcon(int id)
	{
		var chatContainer = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		chatContainer.deleteAllChildren();

		setLoadoutIcon(id, SpriteID.AccManIcons._6);
	}

	private void changeLoadoutIcon(int id)
	{
		final int[] availableSpriteIds = getAllSpriteIds();

		final int iconsPerRow = 12;
		final int iconSize = 28;
		final int iconSpacing = 12;

		var chatContainer = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
		chatContainer.deleteAllChildren();
		
		// Create main layer (overlay)
		var layer = chatContainer.createChild(-1, WidgetType.LAYER);
		layer.setWidthMode(WidgetSizeMode.MINUS);
		layer.setHeightMode(WidgetSizeMode.MINUS);
		layer.setOriginalHeight(23);

		layer.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		layer.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		layer.setNoClickThrough(true);

		layer.revalidate();

		// Create background
		var bg = layer.createChild(-1, WidgetType.GRAPHIC);
		bg.setSpriteId(1017);
		bg.setWidthMode(WidgetSizeMode.MINUS);
		bg.setHeightMode(WidgetSizeMode.MINUS);
		bg.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		bg.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		bg.revalidate();

		// Create scrollable container
		var container = layer.createChild(-1, WidgetType.LAYER);
		container.setWidthMode(WidgetSizeMode.MINUS);
		container.setHeightMode(WidgetSizeMode.MINUS);
		container.setOriginalHeight(12);
		container.setOriginalWidth(12);
		container.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		container.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		container.setOriginalX(6);
		container.setOriginalY(6);
		
		int totalRows = ((availableSpriteIds.length + iconsPerRow + 1) / iconsPerRow) + 1; 
		int scrollHeight = Math.max(0, (totalRows * (iconSize + iconSpacing)) - 200);

		container.setScrollHeight(scrollHeight);
		container.setHasListener(true);
		container.revalidate();

		// Create scrollbar
		var scrollbarContainer = layer.createChild(-1, WidgetType.LAYER);
		scrollbarContainer.setWidthMode(WidgetSizeMode.ABSOLUTE);
		scrollbarContainer.setHeightMode(WidgetSizeMode.MINUS);
		scrollbarContainer.setOriginalWidth(16);
		scrollbarContainer.setOriginalHeight(14);
		scrollbarContainer.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		scrollbarContainer.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		scrollbarContainer.setOriginalY(8);
		scrollbarContainer.setOriginalX(6);
		scrollbarContainer.revalidate();

		var scrollbarBG = scrollbarContainer.createChild(-1, WidgetType.GRAPHIC);
		scrollbarBG.setWidthMode(WidgetSizeMode.ABSOLUTE);
		scrollbarBG.setHeightMode(WidgetSizeMode.MINUS);
		scrollbarBG.setOriginalWidth(16);
		scrollbarBG.setOriginalHeight(32);
		scrollbarBG.setSpriteId(792);
		scrollbarBG.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		scrollbarBG.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		scrollbarBG.setOriginalY(16);
		scrollbarBG.setOriginalX(0);
		scrollbarBG.revalidate();

		var scrollUpButton = scrollbarContainer.createChild(-1, WidgetType.GRAPHIC);
		scrollUpButton.setWidthMode(WidgetSizeMode.ABSOLUTE);
		scrollUpButton.setHeightMode(WidgetSizeMode.ABSOLUTE);
		scrollUpButton.setOriginalWidth(16);
		scrollUpButton.setOriginalHeight(16);
		scrollUpButton.setSpriteId(773);
		scrollUpButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		scrollUpButton.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		scrollUpButton.setOriginalY(0);
		scrollUpButton.setOriginalX(0);
		scrollUpButton.setHasListener(true);
		scrollUpButton.setNoClickThrough(true);
		scrollUpButton.setAction(0, "Up");
		scrollUpButton.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> {
			int currentScrollY = container.getScrollY();
			int newScrollY = Math.max(0, currentScrollY - 30);
			container.setScrollY(newScrollY);
		});
		scrollUpButton.revalidate();

		var scrollDownButton = scrollbarContainer.createChild(-1, WidgetType.GRAPHIC);
		scrollDownButton.setWidthMode(WidgetSizeMode.ABSOLUTE);
		scrollDownButton.setHeightMode(WidgetSizeMode.ABSOLUTE);
		scrollDownButton.setOriginalWidth(16);
		scrollDownButton.setOriginalHeight(16);
		scrollDownButton.setSpriteId(788);
		scrollDownButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		scrollDownButton.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
		scrollDownButton.setOriginalY(0);
		scrollDownButton.setOriginalX(0);
		scrollDownButton.setHasListener(true);
		scrollDownButton.setNoClickThrough(true);
		scrollDownButton.setAction(0, "Down");
		scrollDownButton.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> {
			int maxScrollHeight = container.getScrollHeight();
			int currentScrollY = container.getScrollY();
			int newScrollY = Math.min(currentScrollY + 30, maxScrollHeight);
			container.setScrollY(newScrollY);
		});
		scrollDownButton.revalidate();

		var scrollBarHandle = scrollbarContainer.createChild(-1, WidgetType.GRAPHIC);
		scrollBarHandle.setSpriteId(790);
		scrollBarHandle.setWidthMode(WidgetSizeMode.ABSOLUTE);
		scrollBarHandle.setHeightMode(WidgetSizeMode.ABSOLUTE);
		scrollBarHandle.setOriginalWidth(16);
		scrollBarHandle.setOriginalHeight(16);
		scrollBarHandle.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		scrollBarHandle.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		scrollBarHandle.setOriginalY(16);
		scrollBarHandle.setOriginalX(0);
		scrollBarHandle.setHasListener(true);
		scrollBarHandle.setNoClickThrough(true);
		scrollBarHandle.setDragParent(scrollbarBG);
		scrollBarHandle.setOnDragListener((JavaScriptCallback) (ScriptEvent event) -> {
			var scrollPercentage = (double)event.getMouseY() / 80.0;
			var maxScrollHeight = container.getScrollHeight();
			var newScrollY = (int) (scrollPercentage * maxScrollHeight);
			container.setScrollY(Math.max(0, Math.min(newScrollY, maxScrollHeight)));
		});
		scrollBarHandle.setOnDragCompleteListener((JavaScriptCallback) (ScriptEvent event) -> {
			var scrollPercentage = (double)event.getMouseY() / 80.0;
			var scrollBarHandleY = (int) (scrollPercentage * (scrollbarBG.getHeight() + 32));
			scrollBarHandle.setOriginalY(Math.max(16, Math.min(scrollBarHandleY, scrollbarBG.getHeight())));
			scrollBarHandle.revalidate();
		});
		scrollBarHandle.revalidate();

		container.setOnScrollWheelListener((JavaScriptCallback) (ScriptEvent event) -> {
			int currentScrollY = container.getScrollY();
			int scrollAmount = 30 * event.getMouseY();
			int newScrollY = Math.max(0, Math.min(currentScrollY + scrollAmount, scrollHeight));
			container.setScrollY(newScrollY);

			var scrollBarPercentage = ((double)newScrollY / (double)scrollHeight);
			var scrollBarHandleY = (int) (scrollBarPercentage * scrollbarBG.getHeight()) + 8;
			scrollBarHandle.setOriginalY(Math.max(16, Math.min(scrollBarHandleY, scrollbarBG.getHeight())));
			scrollBarHandle.revalidate();
		});
		container.revalidate();

		for (int i = 0; i < availableSpriteIds.length; i++) {
			int spriteId = availableSpriteIds[i];
			int row = i / iconsPerRow;
			int col = i % iconsPerRow;
			
			// Create icon button
			var iconButton = container.createChild(-1, WidgetType.GRAPHIC);
			iconButton.setSpriteId(spriteId);
			iconButton.setWidthMode(WidgetSizeMode.ABSOLUTE);
			iconButton.setHeightMode(WidgetSizeMode.ABSOLUTE);
			iconButton.setOriginalWidth(iconSize);
			iconButton.setOriginalHeight(iconSize);
			iconButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			iconButton.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			iconButton.setOriginalX(col * (iconSize + iconSpacing) + iconSpacing);
			iconButton.setOriginalY(row * (iconSize + iconSpacing) + iconSpacing);
			iconButton.setHasListener(true);
			iconButton.setNoClickThrough(false);
			iconButton.setNoScrollThrough(false);
			iconButton.setAction(0, "Set Icon (" + spriteId + ")");
			iconButton.setTargetVerb(String.valueOf(spriteId));
			iconButton.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> {
				setLoadoutIcon(id, spriteId);
				chatContainer.deleteAllChildren();
			});
			
			// Add hover effect
			iconButton.setOnMouseRepeatListener((JavaScriptCallback) (ScriptEvent event) -> {
				iconButton.setOpacity(80);
				iconButton.setOriginalWidth(iconSize + 2);
				iconButton.setOriginalHeight(iconSize + 2);
				iconButton.setOriginalX(col * (iconSize + iconSpacing) + iconSpacing - 1);
				iconButton.setOriginalY(row * (iconSize + iconSpacing) + iconSpacing - 1);
				iconButton.revalidate();
			});
			iconButton.setOnMouseLeaveListener((JavaScriptCallback) (ScriptEvent event) -> {
				iconButton.setOpacity(0);
				iconButton.setOriginalWidth(iconSize);
				iconButton.setOriginalHeight(iconSize);
				iconButton.revalidate();
			});
			
			iconButton.revalidate();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.BANK_VIEWCONTAINER)
		{
			final int varbitValue = event.getValue();
			if (varbitValue == 3 || varbitValue == 4)
			{
				lastRunepouchVarbitValue = varbitValue;
				clientThread.invokeLater(this::reloadRunepouchLoadout);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP)) return;

		clientThread.invokeLater(this::reloadRunepouchLoadout);
	}

	private void resetRunepouchWidget()
	{
    // TODO: Implement
		// Closing the bank resets this anyway, so this would be a niece QoL for people toggling this plugin with the rune pouch open
	}

	private void reloadRunepouchLoadout()
	{
		// Hide the header text if configured to do so
		var runepouchLoadoutTextWidget = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_CONTENTS_TEXT1);
		var runepouchLoadoutTextOffset = runepouchLoadoutTextWidget.getHeight();
		if (config.hideRunePouchLoadoutHeader()) {
			runepouchLoadoutTextWidget.setHidden(true);
			runepouchLoadoutTextOffset = 0;
		} else {
			runepouchLoadoutTextWidget.setHidden(false);
		}

		var runepouchTop = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_TOP);
		var runepouchTopOffset = runepouchTop.getRelativeX() + runepouchTop.getHeight() + runepouchLoadoutTextOffset;

		// Move the loadout container up to fill the gap from the header text (if hidden)
		var runepouchLoadoutContainer = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_CONTAINER);
		runepouchLoadoutContainer.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		runepouchLoadoutContainer.setOriginalY(runepouchTopOffset);
		runepouchLoadoutContainer.setHeightMode(WidgetSizeMode.MINUS);
		runepouchLoadoutContainer.setOriginalHeight(runepouchTopOffset);
		runepouchLoadoutContainer.revalidate();

		int loadoutRowHeight = 0;
		int loadoutWidgetIndex = 0;
		for (var loadoutWidget : runepouchLoadoutContainer.getStaticChildren())
		{
			var loadoutNameWidget = client.getWidget(loadoutWidget.getId() + 1);
			var loadoutNameWidgetHeight = loadoutNameWidget.getHeight();

			if (config.hideRunePouchNames()) {
				// Hide the rename button all together
				loadoutNameWidget.setHidden(true);
				for (var loadoutNameWidgetChild : loadoutNameWidget.getDynamicChildren()) {
					loadoutNameWidgetChild.setHidden(false);
					loadoutNameWidgetChild.revalidate();
				}
			} else {
				loadoutNameWidgetHeight = loadoutNameWidgetHeight - 12;

				// Hide the rename button children
				for (var loadoutNameWidgetChild : loadoutNameWidget.getDynamicChildren()) {
					loadoutNameWidgetChild.setHidden(true);
					loadoutNameWidgetChild.revalidate();
				}

				// Replace the rename button with the custom text
				loadoutNameWidget.setType(WidgetType.TEXT);
				loadoutNameWidget.setFontId(FontID.TAHOMA_11);
				loadoutNameWidget.setTextColor(0xFF981F);
				loadoutNameWidget.setTextShadowed(true);
				loadoutNameWidget.setText(getLoadoutName(loadoutWidgetIndex + 1));
				loadoutNameWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
				loadoutNameWidget.setOriginalY(10);
				loadoutNameWidget.setYTextAlignment(WidgetTextAlignment.TOP);
				loadoutNameWidget.setHidden(false);
				loadoutNameWidget.setHasListener(false);
				loadoutNameWidget.clearActions();
			}
			
			loadoutNameWidget.revalidate();

			var newLoadoutHeight = loadoutWidget.getHeight() - loadoutNameWidgetHeight;

		  // Move the loadout widget up to fill the gap
			loadoutWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			loadoutWidget.setOriginalY((newLoadoutHeight * loadoutWidgetIndex - 2 - (2 * loadoutWidgetIndex)) - loadoutNameWidgetHeight);
			loadoutWidget.revalidate();

			for (var loadoutRuneWidget : loadoutWidget.getDynamicChildren())
			{
				if (loadoutRuneWidget.getType() == WidgetType.RECTANGLE) {
					loadoutRuneWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
					loadoutRuneWidget.setOriginalHeight(newLoadoutHeight - 4);
					loadoutRuneWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
					loadoutRuneWidget.setOriginalY(loadoutNameWidgetHeight + 4);
					loadoutRuneWidget.revalidate();

					if (loadoutRowHeight == 0) {
						loadoutRowHeight = loadoutRuneWidget.getHeight() + 2;
					}
					continue;
				}
			}

			// All of this is to handle the icon changing when hovering
			Widget loadButton = null;
			for (var loadoutWidgetChild : loadoutWidget.getStaticChildren()) {
				if (loadoutWidgetChild.getType() == WidgetType.LAYER) {
					loadButton = loadoutWidgetChild;
					break;
				}
			}

			if (loadButton != null) {
				var loadoutIcon = getLoadoutIcon(loadoutWidgetIndex + 1);

				var loadButtonChildren = loadButton.getDynamicChildren();
				final Widget loadButtonSprite = loadButtonChildren[loadButtonChildren.length - 1];
				if (loadButtonSprite != null) {
					loadButtonSprite.setSpriteId(loadoutIcon);

					loadButtonSprite.setOpacity(50);
					loadButtonSprite.revalidate();
				}

				var buttonElementOffset = 8;
				
				loadButton.setOnMouseLeaveListener((JavaScriptCallback) (ScriptEvent event) -> {
					if (loadButtonSprite != null) {
						loadButtonSprite.setSpriteId(loadoutIcon);
						loadButtonSprite.setOpacity(50);
						loadButtonSprite.revalidate();

						var buttonElements = event.getSource().getDynamicChildren();
						for (var buttonElement : buttonElements) {
							if (buttonElement.getType() != WidgetType.GRAPHIC) continue;
							if (buttonElement.getSpriteId() >= (912 + buttonElementOffset) && buttonElement.getSpriteId() <= (920 + buttonElementOffset)) {
								buttonElement.setSpriteId(buttonElement.getSpriteId() - buttonElementOffset);
								buttonElement.setOpacity(0);
								buttonElement.revalidate();
							}
						}
					}
				});
				
				loadButton.setOnMouseRepeatListener((JavaScriptCallback) (ScriptEvent event) -> {
					if (loadButtonSprite != null) {
						loadButtonSprite.setSpriteId(loadoutIcon);
						loadButtonSprite.setOpacity(0);
						loadButtonSprite.revalidate();

						var buttonElements = event.getSource().getDynamicChildren();
						for (var buttonElement : buttonElements) {
							if (buttonElement.getType() != WidgetType.GRAPHIC) continue;
							if (buttonElement.getSpriteId() >= 912 && buttonElement.getSpriteId() <= 920) {
								buttonElement.setSpriteId(buttonElement.getSpriteId() + buttonElementOffset);
								buttonElement.setOpacity(50);
								buttonElement.revalidate();
							}
						}
					}
				});
			}

			loadoutWidgetIndex++;
		}

		// Recalculate how far the container can scroll
		runepouchLoadoutContainer.setScrollHeight(loadoutRowHeight * 10);
		runepouchLoadoutContainer.revalidate();

		// Update the scrollbar
		var runepouchScrollbar = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_SCROLLBAR);
    if (runepouchScrollbar != null) {
      runepouchScrollbar.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			runepouchScrollbar.setOriginalY(runepouchTopOffset);

      runepouchScrollbar.setHeightMode(WidgetSizeMode.MINUS);
      runepouchScrollbar.setOriginalHeight(runepouchTopOffset);
      runepouchScrollbar.revalidate();
    }

    var runepouchScrollbarBG = runepouchScrollbar.getChild(0);
    if (runepouchScrollbarBG != null) {
      runepouchScrollbarBG.setHeightMode(WidgetSizeMode.MINUS);
      runepouchScrollbarBG.setOriginalHeight(32);
      runepouchScrollbarBG.revalidate();
    }
    
    var runepouchScrollbarDown = runepouchScrollbar.getChild(5);
    if (runepouchScrollbarDown != null) {
      runepouchScrollbarDown.setYPositionMode(WidgetPositionMode.ABSOLUTE_BOTTOM);
      runepouchScrollbarDown.setOriginalY(0);
      runepouchScrollbarDown.revalidate();
    }
	}

	@Provides
	RunepouchLoadoutNamesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RunepouchLoadoutNamesConfig.class);
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event) {
		if (event.getCommand().equals("resetrunepouchloadout")) {
			clientThread.invoke(this::resetRunepouchWidget);
		} else if (event.getCommand().equals("reloadrunepouchloadout")) {
			clientThread.invoke(this::reloadRunepouchLoadout);
		}
	}
}
