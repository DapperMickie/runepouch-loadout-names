package com.github.dappermickie.runepouch.loadout.names;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
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
	name = "Runepouch Loadouts"
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

		if (widget == null || widget.getParent() == null)
		{
			return;
		}

		int parentId = widget.getParentId();

		switch (parentId)
		{
			case 983068:
				List<MenuEntry> leftClickMenus = new ArrayList<>(actions.length + 1);
				firstEntry.setOption("Load " + getLoadoutName(1));
				leftClickMenus.add(client.createMenuEntry(1)
					.setOption("Rename " + getLoadoutName(1))
					.setType(MenuAction.RUNELITE)
					.onClick((MenuEntry e) -> renameLoadout(1)));
				break;
			case 983070:
				leftClickMenus = new ArrayList<>(actions.length + 1);
				firstEntry.setOption("Load " + getLoadoutName(2));
				leftClickMenus.add(client.createMenuEntry(1)
					.setOption("Rename " + getLoadoutName(2))
					.setType(MenuAction.RUNELITE)
					.onClick((MenuEntry e) -> renameLoadout(2)));
				break;
			case 983072:
				leftClickMenus = new ArrayList<>(actions.length + 1);
				firstEntry.setOption("Load " + getLoadoutName(3));
				leftClickMenus.add(client.createMenuEntry(1)
					.setOption("Rename " + getLoadoutName(3))
					.setType(MenuAction.RUNELITE)
					.onClick((MenuEntry e) -> renameLoadout(3)));
				break;
			case 983074:
				leftClickMenus = new ArrayList<>(actions.length + 1);
				firstEntry.setOption("Load " + getLoadoutName(4));
				leftClickMenus.add(client.createMenuEntry(1)
					.setOption("Rename " + getLoadoutName(4))
					.setType(MenuAction.RUNELITE)
					.onClick((MenuEntry e) -> renameLoadout(4)));
				break;
		}
	}

	private String getLoadoutName(int id)
	{
		String loadoutName = configManager.getRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout." + lastRunepouchVarbitValue + "." + id);

		if (loadoutName == null || loadoutName.isEmpty())
		{
			loadoutName = "Loadout " + id;
			configManager.setRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout."+ lastRunepouchVarbitValue + "."  + id, loadoutName);
		}

		return loadoutName;
	}

	private void renameLoadout(int id)
	{
		String oldLoadoutName = getLoadoutName(id);
		chatboxPanelManager.openTextInput(String.format(LOADOUT_PROMPT_FORMAT, "Loadout:", 40))
			.value(Strings.nullToEmpty(oldLoadoutName))
			.onDone((newLoadoutName) ->
			{
				if (newLoadoutName == null)
				{
					return;
				}

				newLoadoutName = Text.removeTags(newLoadoutName).trim();
				configManager.setRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, "runepouch.loadout."+ lastRunepouchVarbitValue + "." +  id, newLoadoutName);
				clientThread.invokeLater(this::reloadRunepouchLoadout);
			}).build();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == 9727)
		{
			final int varbitValue = event.getValue();
			if (varbitValue == 3 || varbitValue == 4)
			{
				lastRunepouchVarbitValue = varbitValue;
				clientThread.invokeLater(this::reloadRunepouchLoadout);
			}
		}
	}

	private void resetRunepouchWidget()
	{
		final Set<Integer> resizableWidgets = Set.of(983068, 983070, 983072, 983074);
		Widget runepouchWidget = client.getWidget(983065);
		Widget runepouchLoadoutTextWidget = client.getWidget(983067);

		runepouchLoadoutTextWidget.setHidden(false);

		for (Widget row : runepouchWidget.getStaticChildren())
		{

			if (resizableWidgets.contains(row.getId()))
			{
				Widget child = null;

				for (Widget column : row.getDynamicChildren())
				{
					if (column.getName().equals("Loadout"))
					{
						column.setHidden(true);
					}
					var itemId = column.getItemId();
					if (itemId > 0)
					{
						column.setOriginalWidth(32);
						column.setOriginalHeight(28);
						column.setForcedPosition(-1, -1);
						column.revalidate();
					}
				}
			}
		}
	}

	private void reloadRunepouchLoadout()
	{
		final int spaces = lastRunepouchVarbitValue;
		final Set<Integer> resizableWidgets = Set.of(983068, 983070, 983072, 983074);
		Widget runepouchWidget = client.getWidget(983065);
		Widget runepouchLoadoutTextWidget = client.getWidget(983067);

		runepouchLoadoutTextWidget.setHidden(true);

		for (Widget row : runepouchWidget.getStaticChildren())
		{

			if (resizableWidgets.contains(row.getId()))
			{
				Widget child = null;

				for (Widget column : row.getDynamicChildren())
				{
					if (column.getName().equals("Loadout"))
					{
						child = column;
					}
					var itemId = column.getItemId();
					if (itemId > 0)
					{
						if (spaces == 3)
						{
							column.setOriginalWidth(26);
							column.setOriginalHeight(20);
							column.revalidate();

							final int relativeX = column.getRelativeX();
							int x = relativeX == 45 ? 55 :
								relativeX == 55 ? 55 :
									relativeX == 83 ? 103 :
										relativeX == 103 ? 103 :
											relativeX == 121 ? 151 :
												relativeX == 151 ? 151 : 0;

							column.setForcedPosition(x, 16);
						}
						else if (spaces == 4)
						{
							column.setOriginalWidth(26);
							column.setOriginalHeight(20);
							column.revalidate();

							column.setForcedPosition(column.getRelativeX(), 16);
						}
					}
				}

				if (child == null)
				{
					child = row.createChild(4);
					child.setName("Loadout");
				}

				switch (row.getId())
				{
					case 983068:
						LoadLoadout(1, child);
						break;
					case 983070:
						LoadLoadout(2, child);
						break;
					case 983072:
						LoadLoadout(3, child);
						break;
					case 983074:
						LoadLoadout(4, child);
						break;
				}

			}
		}
	}

	private void LoadLoadout(int id, Widget loadoutWidget)
	{
		String loadoutName = getLoadoutName(id);

		loadoutWidget.setText(loadoutName);
		loadoutWidget.setOriginalHeight(20);
		loadoutWidget.setOriginalWidth(150);
		loadoutWidget.setPos(0, 0);
		loadoutWidget.setFontId(495);
		loadoutWidget.setXPositionMode(2);
		loadoutWidget.setYPositionMode(0);
		loadoutWidget.setXTextAlignment(1);
		loadoutWidget.setYTextAlignment(1);
		loadoutWidget.setTextShadowed(true);
		loadoutWidget.setTextColor(client.getWidget(983067).getTextColor());
		loadoutWidget.setHidden(false);
		loadoutWidget.revalidate();
	}

	@Provides
	RunepouchLoadoutNamesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RunepouchLoadoutNamesConfig.class);
	}
}
