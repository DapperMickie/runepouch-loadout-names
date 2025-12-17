package com.github.dappermickie.runepouch.loadout.names;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
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
	@Inject private Client client;
	@Inject private RunepouchLoadoutNamesConfig config;
	@Inject private ClientThread clientThread;
	@Inject private ConfigManager configManager;
	@Inject private ChatboxPanelManager chatboxPanelManager;

	private static final int DEFAULT_LOADOUT_ICON = SpriteID.AccManIcons._6;
	private static final String LOADOUT_PROMPT_FORMAT = "%s<br>" +
		ColorUtil.prependColorTag("(Limit %s Characters)", new Color(0, 0, 170));
	private static final int RUNEPOUCH_LOADOUT_ICON_BG_SPRITE_ID_START = SpriteID.V2StoneButton.TOP_LEFT -1;
	private static final int RUNEPOUCH_LOADOUT_ICON_BG_SPRITE_ID_END = SpriteID.V2StoneButton.BOTTOM +1;

	private static final List<Integer> NAME_INTERFACE_IDS = new ArrayList<Integer>() {{
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_A);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_B);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_C);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_D);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_E);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_F);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_G);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_H);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_I);
		add(InterfaceID.Bankside.RUNEPOUCH_NAME_J);
	}};

	private static final List<Integer> LOAD_INTERFACE_IDS = new ArrayList<Integer>() {{
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_A);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_B);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_C);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_D);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_E);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_F);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_G);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_H);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_I);
		add(InterfaceID.Bankside.RUNEPOUCH_LOAD_J);
	}};

	private int lastRunepouchVarbitValue = 0;

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
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN || event.getGroupId() == InterfaceID.BANKSIDE)
		{
			chatboxPanelManager.close();
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		MenuEntry[] actions = event.getMenuEntries();
		MenuEntry firstEntry = event.getFirstEntry();

		Widget widget = firstEntry.getWidget();
		if (widget == null) return;

		var widgetId = widget.getId();

		var loadoutIndex = LOAD_INTERFACE_IDS.indexOf(widgetId);
		if (loadoutIndex == -1) return;

		setLoadMenuActions(loadoutIndex + 1, actions);
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		var menuEntry = event.getMenuEntry();
		var widget = menuEntry.getWidget();
		if (widget == null) return;

		var widgetId = widget.getId();

		var loadoutIndex = LOAD_INTERFACE_IDS.indexOf(widgetId);
		if (loadoutIndex != -1) {
			setLoadMenuEntry(loadoutIndex + 1, menuEntry);
			return;
		}

		loadoutIndex = NAME_INTERFACE_IDS.indexOf(widgetId);
		if (loadoutIndex != -1) {
			setRenameMenuEntry(loadoutIndex + 1, menuEntry);
			return;
		}
	}

	private void setLoadMenuActions(int loadoutId, MenuEntry[] actions)
	{
		var leftClickMenus = new ArrayList<>(actions.length + 1);
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

	private void setLoadMenuEntry(int loadoutId, MenuEntry menuEntry)
	{
		menuEntry
			.setOption("Load")
			.setTarget(getLoadoutName(loadoutId));
	}

	private void setRenameMenuEntry(int loadoutId, MenuEntry menuEntry)
	{
		menuEntry
			.setOption("Rename")
			.setTarget(getLoadoutName(loadoutId));
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
				if (newLoadoutName == null) {
					return;
				}

				newLoadoutName = Text.removeTags(newLoadoutName).trim();
				configManager.setRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id, newLoadoutName);
				clientThread.invokeLater(this::reloadRunepouchLoadout);
			}).build();
	}

	private int getLoadoutIcon(int id)
	{
		if (!config.enableRunePouchIcons())
		{
			return DEFAULT_LOADOUT_ICON;
		}

		String loadoutIcon = configManager.getRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id + ".icon");

		if (loadoutIcon == null || loadoutIcon.isEmpty())
		{
			loadoutIcon = String.valueOf(DEFAULT_LOADOUT_ICON);
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
		chatboxPanelManager.close();

		setLoadoutIcon(id, DEFAULT_LOADOUT_ICON);
	}

	private void changeLoadoutIcon(int id)
	{
		new RunepouchLoadoutIconChatbox(chatboxPanelManager, clientThread, client)
			.currentSpriteID(getLoadoutIcon(id))
			.onDone((spriteId) -> {
				setLoadoutIcon(id, spriteId);
			})
			.build();
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
			} else if (varbitValue == 0) {
				// 0 = bank container closed, so hide the icon chatbox
				chatboxPanelManager.close();
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
		if (config.enableRunePouchNames()) {
			for (int i = 0; i < NAME_INTERFACE_IDS.size(); i++) {
				final int nameWidgetIndex = i + 1;
				var nameWidgetID = NAME_INTERFACE_IDS.get(i);
				var nameWidget = client.getWidget(nameWidgetID);

				if (nameWidget == null) continue;

				nameWidget.setHidden(false);
				nameWidget.setType(WidgetType.TEXT);
				nameWidget.setFontId(FontID.PLAIN_12);
				nameWidget.setTextColor(0xFF981F);
				nameWidget.setTextShadowed(true);
				nameWidget.setText(getLoadoutName(nameWidgetIndex));
				nameWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
				nameWidget.setOriginalY(0);
				nameWidget.setYTextAlignment(WidgetTextAlignment.TOP);
				nameWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
				nameWidget.setHidden(false);
				nameWidget.setHasListener(true);
				nameWidget.clearActions();
				nameWidget.setAction(0, "Rename");
				nameWidget.setTargetVerb(getLoadoutName(nameWidgetIndex));
				nameWidget.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> {
					if (event.getOp() != 1) return;
					renameLoadout(nameWidgetIndex);
				});
				nameWidget.revalidate();
			}
		}

		for (int i = 0; i < LOAD_INTERFACE_IDS.size(); i++) {
			final int loadWidgetIndex = i + 1;
			final int loadWidgetID = LOAD_INTERFACE_IDS.get(i);

			// All of this is to handle the icon changing when hovering
			Widget loadButton = client.getWidget(loadWidgetID);
			if (loadButton != null) {
				var loadoutIcon = getLoadoutIcon(loadWidgetIndex);
				var isCustomLoadoutIcon = loadoutIcon != DEFAULT_LOADOUT_ICON;

				var loadButtonChildren = loadButton.getDynamicChildren();
				if (loadButtonChildren.length > 0) {
					final Widget loadButtonSprite = loadButtonChildren[loadButtonChildren.length - 1];
					if (loadButtonSprite != null) {
						loadButtonSprite.setSpriteId(loadoutIcon);
						loadButtonSprite.setOriginalWidth(22);
						loadButtonSprite.setOriginalHeight(22);
						loadButtonSprite.setOpacity(50);
						if (isCustomLoadoutIcon) {
							loadButtonSprite.setOriginalWidth(28);
							loadButtonSprite.setOriginalHeight(28);
							loadButtonSprite.setOpacity(0);
						}
						loadButtonSprite.revalidate();
					}

					var buttonElementOffset = 8;
					
					loadButton.setOnMouseLeaveListener((JavaScriptCallback) (ScriptEvent event) -> {
						if (loadButtonSprite != null) {
							loadButtonSprite.setSpriteId(loadoutIcon);
							loadButtonSprite.setOpacity(50);
							if (isCustomLoadoutIcon) {
								loadButtonSprite.setOpacity(0);
							}
							loadButtonSprite.revalidate();

							var buttonElements = event.getSource().getDynamicChildren();
							for (var buttonElement : buttonElements) {
								if (buttonElement.getType() != WidgetType.GRAPHIC) continue;
								if (buttonElement.getSpriteId() >= (RUNEPOUCH_LOADOUT_ICON_BG_SPRITE_ID_START + buttonElementOffset) && buttonElement.getSpriteId() < (RUNEPOUCH_LOADOUT_ICON_BG_SPRITE_ID_END + buttonElementOffset)) {
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
								if (buttonElement.getSpriteId() >= RUNEPOUCH_LOADOUT_ICON_BG_SPRITE_ID_START && buttonElement.getSpriteId() < RUNEPOUCH_LOADOUT_ICON_BG_SPRITE_ID_END) {
									buttonElement.setSpriteId(buttonElement.getSpriteId() + buttonElementOffset);
									buttonElement.setOpacity(50);
									buttonElement.revalidate();
								}
							}
						}
					});
				}
			}
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
