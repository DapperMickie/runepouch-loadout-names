package com.github.dappermickie.runepouch.loadout.names;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;

// Alternate 2-column grid rendering, enabled via enableCompactLayout(); shares config keys with the classic view.
@Slf4j
@Singleton
class RunepouchLoadoutCompactManager
{
	private static final String ICON_CHILD_NAME = "rlg-theme-icon";
	private static final String LAYER_CHILD_NAME = "rlg-theme-icon-layer";
	private static final String ARROW_CHILD_NAME = "rlg-load-arrow";
	private static final String RUNE_ICON_CHILD_PREFIX = "rlg-rune-icon-";
	private static final int PLACEHOLDER_ITEM_ID = 11526; // vanilla's own "no rune assigned" icon

	private static final int[] LOADOUT_WIDGET_IDS = {
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_A,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_B,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_C,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_D,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_E,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_F,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_G,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_H,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_I,
		InterfaceID.Bankside.RUNEPOUCH_LOADOUT_J,
	};

	private static final int[] LOAD_WIDGET_IDS = {
		InterfaceID.Bankside.RUNEPOUCH_LOAD_A,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_B,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_C,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_D,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_E,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_F,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_G,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_H,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_I,
		InterfaceID.Bankside.RUNEPOUCH_LOAD_J,
	};

	private static final int[] NAME_WIDGET_IDS = {
		InterfaceID.Bankside.RUNEPOUCH_NAME_A,
		InterfaceID.Bankside.RUNEPOUCH_NAME_B,
		InterfaceID.Bankside.RUNEPOUCH_NAME_C,
		InterfaceID.Bankside.RUNEPOUCH_NAME_D,
		InterfaceID.Bankside.RUNEPOUCH_NAME_E,
		InterfaceID.Bankside.RUNEPOUCH_NAME_F,
		InterfaceID.Bankside.RUNEPOUCH_NAME_G,
		InterfaceID.Bankside.RUNEPOUCH_NAME_H,
		InterfaceID.Bankside.RUNEPOUCH_NAME_I,
		InterfaceID.Bankside.RUNEPOUCH_NAME_J,
	};

	// Per-loadout, per-rune-position quantity cap varbit; -1 (D/H pos 3) can't be read (split-bit encoding), so skipped.
	private static final int[][] RUNE_CAP_VARBIT_IDS = {
		{VarbitID.RUNE_POUCH_LOADOUT_A_CAP1, VarbitID.RUNE_POUCH_LOADOUT_A_CAP2, VarbitID.RUNE_POUCH_LOADOUT_A_CAP3, VarbitID.RUNE_POUCH_LOADOUT_A_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_B_CAP1, VarbitID.RUNE_POUCH_LOADOUT_B_CAP2, VarbitID.RUNE_POUCH_LOADOUT_B_CAP3, VarbitID.RUNE_POUCH_LOADOUT_B_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_C_CAP1, VarbitID.RUNE_POUCH_LOADOUT_C_CAP2, VarbitID.RUNE_POUCH_LOADOUT_C_CAP3, VarbitID.RUNE_POUCH_LOADOUT_C_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_D_CAP1, VarbitID.RUNE_POUCH_LOADOUT_D_CAP2, -1, VarbitID.RUNE_POUCH_LOADOUT_D_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_E_CAP1, VarbitID.RUNE_POUCH_LOADOUT_E_CAP2, VarbitID.RUNE_POUCH_LOADOUT_E_CAP3, VarbitID.RUNE_POUCH_LOADOUT_E_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_F_CAP1, VarbitID.RUNE_POUCH_LOADOUT_F_CAP2, VarbitID.RUNE_POUCH_LOADOUT_F_CAP3, VarbitID.RUNE_POUCH_LOADOUT_F_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_G_CAP1, VarbitID.RUNE_POUCH_LOADOUT_G_CAP2, VarbitID.RUNE_POUCH_LOADOUT_G_CAP3, VarbitID.RUNE_POUCH_LOADOUT_G_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_H_CAP1, VarbitID.RUNE_POUCH_LOADOUT_H_CAP2, -1, VarbitID.RUNE_POUCH_LOADOUT_H_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_I_CAP1, VarbitID.RUNE_POUCH_LOADOUT_I_CAP2, VarbitID.RUNE_POUCH_LOADOUT_I_CAP3, VarbitID.RUNE_POUCH_LOADOUT_I_CAP4},
		{VarbitID.RUNE_POUCH_LOADOUT_J_CAP1, VarbitID.RUNE_POUCH_LOADOUT_J_CAP2, VarbitID.RUNE_POUCH_LOADOUT_J_CAP3, VarbitID.RUNE_POUCH_LOADOUT_J_CAP4},
	};

	private final Client client;
	private final ConfigManager configManager;
	private final RunepouchLoadoutNamesConfig config;

	private final Map<Integer, int[]> originalGeometry = new HashMap<>();
	// Widgets we've created, keyed by "parentWidgetId:tag" — reference identity distinguishes "ours" from vanilla's.
	private final Map<String, Widget> ownedWidgets = new HashMap<>();
	// Vanilla widgets we've hidden (not created) — restoreNativeLayout() un-hides only these.
	private final Set<Widget> hiddenVanillaWidgets = new HashSet<>();
	// First-seen (un-hovered) geometry of the Load button's decorations, which vanilla grows on hover and never shrinks back.
	private final Map<String, int[]> loadButtonChildGeometry = new HashMap<>();
	private int originalScrollHeight = -1;
	private boolean gridApplied;
	private int currentViewValue;
	private IntConsumer renameRequestHandler;
	private BiConsumer<Integer, Integer> iconChangeRequestHandler;

	@Inject
	RunepouchLoadoutCompactManager(Client client, ConfigManager configManager, RunepouchLoadoutNamesConfig config)
	{
		this.client = client;
		this.configManager = configManager;
		this.config = config;
	}

	void setRenameRequestHandler(IntConsumer handler)
	{
		this.renameRequestHandler = handler;
	}

	void setIconChangeRequestHandler(BiConsumer<Integer, Integer> handler)
	{
		this.iconChangeRequestHandler = handler;
	}

	void applyGrid(int viewValue)
	{
		this.currentViewValue = viewValue;

		Widget container = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_CONTAINER);
		if (container == null)
		{
			return;
		}

		int containerWidth = container.getWidth();
		if (containerWidth <= 0)
		{
			log.debug("Skipping rune pouch grid layout, container width not resolved yet: {}", containerWidth);
			return;
		}

		// Widen to the frame's width to reclaim the space vanilla reserves for the (now-hidden) scrollbar track.
		Widget frame = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_FRAME);
		if (frame != null && frame.getWidth() > containerWidth)
		{
			cacheOriginalGeometry(container);
			container.setWidthMode(WidgetSizeMode.ABSOLUTE);
			container.setOriginalWidth(frame.getWidth());
			container.revalidate();
			containerWidth = container.getWidth();
		}

		int usableWidth = containerWidth - RunepouchLoadoutCompactConst.SCROLLBAR_RESERVE - RunepouchLoadoutCompactConst.CONTAINER_PADDING_X * 2;
		int cellWidth = (usableWidth - RunepouchLoadoutCompactConst.CELL_GUTTER_X) / RunepouchLoadoutCompactConst.GRID_COLUMNS;

		for (int i = 0; i < LOADOUT_WIDGET_IDS.length; i++)
		{
			Widget loadout = client.getWidget(LOADOUT_WIDGET_IDS[i]);
			if (loadout == null)
			{
				continue;
			}

			cacheOriginalGeometry(loadout);

			int col = i % RunepouchLoadoutCompactConst.GRID_COLUMNS;
			int row = i / RunepouchLoadoutCompactConst.GRID_COLUMNS;

			loadout.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			loadout.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			loadout.setWidthMode(WidgetSizeMode.ABSOLUTE);
			loadout.setHeightMode(WidgetSizeMode.ABSOLUTE);
			loadout.setOriginalX(RunepouchLoadoutCompactConst.CONTAINER_PADDING_X + col * (cellWidth + RunepouchLoadoutCompactConst.CELL_GUTTER_X));
			loadout.setOriginalY(row * (cellHeight() + RunepouchLoadoutCompactConst.CELL_GUTTER_Y));
			loadout.setOriginalWidth(cellWidth);
			loadout.setOriginalHeight(cellHeight());
			loadout.revalidate();

			applyCellBackdrop(loadout, cellWidth);

			applyLoadoutName(i);
			applyLoadoutIcon(i, cellWidth);
			compactRuneIcons(i, cellWidth);
		}

		if (originalScrollHeight < 0)
		{
			originalScrollHeight = container.getScrollHeight();
		}

		int scrollHeight = RunepouchLoadoutCompactConst.GRID_ROWS * (cellHeight() + RunepouchLoadoutCompactConst.CELL_GUTTER_Y);
		container.setScrollHeight(scrollHeight);
		if (container.getScrollY() > scrollHeight)
		{
			container.setScrollY(Math.max(0, scrollHeight - container.getHeight()));
		}
		container.revalidateScroll();

		Widget scrollbar = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_SCROLLBAR);
		if (scrollbar != null)
		{
			scrollbar.setHidden(true);
			scrollbar.revalidate();
		}

		gridApplied = true;
	}

	private void applyCellBackdrop(Widget loadout, int cellWidth)
	{
		Widget backdrop = getOrCreateOwned(loadout, "rlg-cell-backdrop", WidgetType.RECTANGLE);
		backdrop.setFilled(true);
		backdrop.setTextColor(0x000000);
		backdrop.setOpacity(190);
		backdrop.setWidthMode(WidgetSizeMode.ABSOLUTE);
		backdrop.setHeightMode(WidgetSizeMode.ABSOLUTE);
		backdrop.setOriginalWidth(cellWidth);
		backdrop.setOriginalHeight(cellHeight());
		backdrop.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		backdrop.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		backdrop.setOriginalX(0);
		backdrop.setOriginalY(0);
		backdrop.setHidden(false);
		backdrop.revalidate();
	}

	// Per-tick correction for vanilla re-showing rune icons / enlarging the Load button on hover.
	void suppressVanillaInterference()
	{
		if (!gridApplied)
		{
			return;
		}

		for (int widgetId : LOADOUT_WIDGET_IDS)
		{
			Widget loadoutWidget = client.getWidget(widgetId);
			if (loadoutWidget == null)
			{
				continue;
			}

			Widget[] children = loadoutWidget.getDynamicChildren();
			if (children == null)
			{
				continue;
			}

			for (Widget child : children)
			{
				if (!child.isHidden() && child.getItemId() >= 0 && isVanillaRendered(child))
				{
					child.setHidden(true);
					hiddenVanillaWidgets.add(child);
					child.revalidate();
				}
			}
		}

		for (int widgetId : LOAD_WIDGET_IDS)
		{
			Widget loadWidget = client.getWidget(widgetId);
			if (loadWidget != null)
			{
				pinLoadButtonChildren(loadWidget);
			}
		}
	}

	void refreshRuneContents()
	{
		if (!gridApplied)
		{
			return;
		}

		Widget container = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_CONTAINER);
		if (container == null)
		{
			return;
		}

		int usableWidth = container.getWidth() - RunepouchLoadoutCompactConst.SCROLLBAR_RESERVE - RunepouchLoadoutCompactConst.CONTAINER_PADDING_X * 2;
		int cellWidth = (usableWidth - RunepouchLoadoutCompactConst.CELL_GUTTER_X) / RunepouchLoadoutCompactConst.GRID_COLUMNS;

		for (int i = 0; i < LOADOUT_WIDGET_IDS.length; i++)
		{
			compactRuneIcons(i, cellWidth);
		}
	}

	void refresh()
	{
		if (!gridApplied)
		{
			return;
		}

		applyGrid(currentViewValue);
	}

	// Idempotent revert of everything applyGrid() touched.
	void restoreNativeLayout()
	{
		if (!gridApplied)
		{
			return;
		}

		restoreGeometry(LOADOUT_WIDGET_IDS);
		restoreGeometry(LOAD_WIDGET_IDS);
		restoreGeometry(NAME_WIDGET_IDS);
		restoreGeometry(new int[]{InterfaceID.Bankside.RUNEPOUCH_LOADOUT_CONTAINER});

		Widget container = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_CONTAINER);
		if (container != null && originalScrollHeight >= 0)
		{
			container.setScrollHeight(originalScrollHeight);
			container.revalidateScroll();
		}
		originalScrollHeight = -1;

		Widget scrollbar = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_SCROLLBAR);
		if (scrollbar != null)
		{
			scrollbar.setHidden(false);
			scrollbar.revalidate();
		}

		for (int widgetId : LOADOUT_WIDGET_IDS)
		{
			Widget loadoutWidget = client.getWidget(widgetId);
			if (loadoutWidget == null)
			{
				continue;
			}

			Widget[] children = loadoutWidget.getDynamicChildren();
			if (children == null)
			{
				continue;
			}

			for (Widget child : children)
			{
				// Only touch the two tracked sets; anything else was never touched by compact mode.
				if (ownedWidgets.containsValue(child))
				{
					child.setHidden(true);
					child.revalidate();
				}
				else if (hiddenVanillaWidgets.contains(child))
				{
					child.setHidden(false);
					child.revalidate();
				}
			}
		}

		// Caches deliberately not cleared — panel may still be open; only resetTrackedState() clears them.
		gridApplied = false;
	}

	void resetTrackedState()
	{
		originalGeometry.clear();
		ownedWidgets.clear();
		hiddenVanillaWidgets.clear();
		loadButtonChildGeometry.clear();
		gridApplied = false;
	}

	private void restoreGeometry(int[] widgetIds)
	{
		for (int widgetId : widgetIds)
		{
			Widget widget = client.getWidget(widgetId);
			int[] original = originalGeometry.get(widgetId);
			if (widget == null || original == null)
			{
				continue;
			}

			widget.setXPositionMode(original[0]);
			widget.setYPositionMode(original[1]);
			widget.setWidthMode(original[2]);
			widget.setHeightMode(original[3]);
			widget.setOriginalX(original[4]);
			widget.setOriginalY(original[5]);
			widget.setOriginalWidth(original[6]);
			widget.setOriginalHeight(original[7]);
			widget.revalidate();
		}
	}

	// Own toggle — not enableRunePouchNames() or the legacy hideRunePouchNames().
	private boolean namesEnabled()
	{
		return !config.hideCompactLoadoutNames();
	}

	private int cellHeight()
	{
		return namesEnabled()
			? RunepouchLoadoutCompactConst.CELL_HEIGHT
			: RunepouchLoadoutCompactConst.CELL_HEIGHT - RunepouchLoadoutCompactConst.NAME_HEIGHT;
	}

	private int row1Top()
	{
		return namesEnabled()
			? RunepouchLoadoutCompactConst.NAME_HEIGHT + RunepouchLoadoutCompactConst.ROW_TOP_GAP
			: RunepouchLoadoutCompactConst.ROW_TOP_GAP;
	}

	String getLoadoutName(int slotIndex)
	{
		int id = slotIndex + 1;
		String name = configManager.getRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, nameKey(id));
		return name == null || name.isEmpty() ? "Loadout " + id : name;
	}

	private int getLoadoutIcon(int slotIndex, int layer)
	{
		int id = slotIndex + 1;
		String value = configManager.getRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, iconKey(id, layer));
		if (value == null || value.isEmpty())
		{
			return RunepouchLoadoutNamesPlugin.DEFAULT_LOADOUT_ICON;
		}

		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e)
		{
			return RunepouchLoadoutNamesPlugin.DEFAULT_LOADOUT_ICON;
		}
	}

	private void resetLoadoutIcon(int slotIndex, int layer)
	{
		configManager.unsetRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, iconKey(slotIndex + 1, layer));
	}

	// Must match RunepouchLoadoutNamesPlugin's key format exactly.
	private String nameKey(int id)
	{
		return "runepouch.loadout." + currentViewValue + "." + id;
	}

	private String iconKey(int id, int layer)
	{
		return "runepouch.loadout." + currentViewValue + "." + id + (layer == 0 ? ".icon" : ".icon_" + layer);
	}

	int slotIndexForLoadWidget(int widgetId)
	{
		return indexOf(LOAD_WIDGET_IDS, widgetId);
	}

	private void applyLoadoutName(int slotIndex)
	{
		Widget nameWidget = client.getWidget(NAME_WIDGET_IDS[slotIndex]);
		if (nameWidget == null)
		{
			return;
		}

		cacheOriginalGeometry(nameWidget);

		if (!namesEnabled())
		{
			nameWidget.setHidden(true);
			nameWidget.revalidate();
			return;
		}

		String name = getLoadoutName(slotIndex);

		nameWidget.setHidden(false);
		nameWidget.setType(WidgetType.TEXT);
		nameWidget.setFontId(FontID.PLAIN_12);
		nameWidget.setTextColor(0xFF981F);
		nameWidget.setTextShadowed(true);
		nameWidget.setText(name);

		// Pinned to a thin strip along the top; vanilla's MINUS-mode sizing would otherwise swallow clicks below it.
		nameWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		nameWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		nameWidget.setWidthMode(WidgetSizeMode.MINUS);
		nameWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
		nameWidget.setOriginalX(0);
		nameWidget.setOriginalY(0);
		nameWidget.setOriginalWidth(0);
		nameWidget.setOriginalHeight(RunepouchLoadoutCompactConst.NAME_HEIGHT);

		nameWidget.setHasListener(true);
		nameWidget.clearActions();
		nameWidget.setAction(0, "Rename");
		nameWidget.setTargetVerb(name);
		nameWidget.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
		{
			if (event.getOp() != 1)
			{
				return;
			}

			if (renameRequestHandler != null)
			{
				renameRequestHandler.accept(slotIndex);
			}
		});
		nameWidget.revalidate();
	}

	private void applyLoadoutIcon(int slotIndex, int cellWidth)
	{
		Widget loadWidget = client.getWidget(LOAD_WIDGET_IDS[slotIndex]);
		Widget loadoutWidget = client.getWidget(LOADOUT_WIDGET_IDS[slotIndex]);
		if (loadWidget == null || loadoutWidget == null)
		{
			return;
		}

		int row1Width = RunepouchLoadoutCompactConst.LOAD_BUTTON_WIDTH + RunepouchLoadoutCompactConst.BUTTON_ICON_GAP
			+ RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE * 2 + RunepouchLoadoutCompactConst.CUSTOM_ICON_GUTTER
			+ RunepouchLoadoutCompactConst.ICON_BORDER_PADDING;
		int row1X = (cellWidth - row1Width) / 2;
		int buttonX = row1X;
		int buttonY = row1Top();

		cacheOriginalGeometry(loadWidget);
		loadWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		loadWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		loadWidget.setWidthMode(WidgetSizeMode.ABSOLUTE);
		loadWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
		loadWidget.setOriginalX(buttonX);
		loadWidget.setOriginalY(buttonY);
		loadWidget.setOriginalWidth(RunepouchLoadoutCompactConst.LOAD_BUTTON_WIDTH);
		loadWidget.setOriginalHeight(RunepouchLoadoutCompactConst.LOAD_BUTTON_HEIGHT);
		loadWidget.revalidate();
		pinLoadButtonChildren(loadWidget);

		// Not touching this button's own mouse listeners: Widget has no getter for them, so overwriting is a one-way door.

		applyLoadArrow(loadoutWidget, buttonX, buttonY);

		int primarySprite = getLoadoutIcon(slotIndex, 0);
		int layerSprite = getLoadoutIcon(slotIndex, 1);
		boolean isCustomPrimary = primarySprite != RunepouchLoadoutNamesPlugin.DEFAULT_LOADOUT_ICON;
		boolean hasLayer = layerSprite != RunepouchLoadoutNamesPlugin.DEFAULT_LOADOUT_ICON;

		// Attached to the loadout cell, not the button — the button's own hover script would bury/hide our icon.
		int iconY = buttonY + (RunepouchLoadoutCompactConst.LOAD_BUTTON_HEIGHT - RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE) / 2;
		int iconX = buttonX + RunepouchLoadoutCompactConst.LOAD_BUTTON_WIDTH + RunepouchLoadoutCompactConst.BUTTON_ICON_GAP;

		applyIconSlot(loadoutWidget, ICON_CHILD_NAME, iconX, iconY, RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE, primarySprite, isCustomPrimary, slotIndex, 0);

		int layerX = iconX + RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE + RunepouchLoadoutCompactConst.CUSTOM_ICON_GUTTER;
		applyIconSlot(loadoutWidget, LAYER_CHILD_NAME, layerX, iconY, RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE, layerSprite, hasLayer, slotIndex, 1);
	}

	// Classic mode's createChild(9, ...) always replaces vanilla's native arrow, so draw our own replacement.
	private void applyLoadArrow(Widget loadoutWidget, int buttonX, int buttonY)
	{
		int size = RunepouchLoadoutCompactConst.ARROW_ICON_SIZE;
		int x = buttonX + (RunepouchLoadoutCompactConst.LOAD_BUTTON_WIDTH - size) / 2;
		int y = buttonY + (RunepouchLoadoutCompactConst.LOAD_BUTTON_HEIGHT - size) / 2;

		Widget arrow = getOrCreateOwned(loadoutWidget, ARROW_CHILD_NAME, WidgetType.GRAPHIC);
		// Confirmed via Widget Inspector: vanilla's real arrow is sprite 2151
		// (SpriteID.AccManIcons._6) — same sprite as DEFAULT_LOADOUT_ICON.
		arrow.setSpriteId(RunepouchLoadoutNamesPlugin.DEFAULT_LOADOUT_ICON);
		arrow.setItemId(-1);
		arrow.setOpacity(0);
		arrow.setWidthMode(WidgetSizeMode.ABSOLUTE);
		arrow.setHeightMode(WidgetSizeMode.ABSOLUTE);
		arrow.setOriginalWidth(size);
		arrow.setOriginalHeight(size);
		arrow.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		arrow.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		arrow.setOriginalX(x);
		arrow.setOriginalY(y);
		arrow.setHasListener(false);
		arrow.setHidden(false);
		arrow.revalidate();
	}

	private void pinLoadButtonChildren(Widget loadWidget)
	{
		Widget[] children = loadWidget.getDynamicChildren();
		if (children == null)
		{
			return;
		}

		for (int i = 0; i < children.length; i++)
		{
			Widget child = children[i];
			String key = loadWidget.getId() + ":" + i;
			int[] original = loadButtonChildGeometry.computeIfAbsent(key, k -> new int[]{
				child.getOriginalWidth(),
				child.getOriginalHeight(),
				child.getOriginalX(),
				child.getOriginalY(),
			});

			if (child.getOriginalWidth() == original[0] && child.getOriginalHeight() == original[1]
				&& child.getOriginalX() == original[2] && child.getOriginalY() == original[3])
			{
				continue;
			}

			child.setOriginalWidth(original[0]);
			child.setOriginalHeight(original[1]);
			child.setOriginalX(original[2]);
			child.setOriginalY(original[3]);
			child.revalidate();
		}
	}

	private void applyIconSlot(Widget parent, String tag, int x, int y, int size, int spriteId, boolean isSet, int slotIndex, int layer)
	{
		int padding = RunepouchLoadoutCompactConst.ICON_BORDER_PADDING;
		Widget border = getOrCreateOwned(parent, tag + "-border", WidgetType.RECTANGLE);
		border.setFilled(false);
		border.setBorderType(1);
		border.setTextColor(0x000000);
		border.setOpacity(0);
		border.setWidthMode(WidgetSizeMode.ABSOLUTE);
		border.setHeightMode(WidgetSizeMode.ABSOLUTE);
		border.setOriginalWidth(size + padding * 2);
		border.setOriginalHeight(size + padding * 2);
		border.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		border.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		border.setOriginalX(x - padding);
		border.setOriginalY(y - padding);
		border.setHidden(false);
		border.revalidate();

		Widget icon = getOrCreateOwned(parent, tag, WidgetType.GRAPHIC);
		if (isSet)
		{
			icon.setSpriteId(spriteId);
			icon.setItemId(-1);
			icon.setOpacity(0);
		}
		else
		{
			icon.setSpriteId(-1);
			icon.setItemId(PLACEHOLDER_ITEM_ID);
			icon.setItemQuantity(1);
			icon.setItemQuantityMode(ItemQuantityMode.NEVER);
			icon.setOpacity(0);
		}
		icon.setWidthMode(WidgetSizeMode.ABSOLUTE);
		icon.setHeightMode(WidgetSizeMode.ABSOLUTE);
		icon.setOriginalWidth(size);
		icon.setOriginalHeight(size);
		icon.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		icon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		icon.setOriginalX(x);
		icon.setOriginalY(y);
		icon.setHidden(false);

		// TargetVerb doesn't concatenate onto Action for dynamic children like it does natively, so the label goes in Action.
		icon.setHasListener(true);
		icon.clearActions();
		icon.setAction(0, "Change icon");
		icon.setAction(1, "Reset icon");
		icon.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
		{
			int op = event.getOp();
			if (op == 1 && iconChangeRequestHandler != null)
			{
				iconChangeRequestHandler.accept(slotIndex, layer);
			}
			else if (op == 2)
			{
				resetLoadoutIcon(slotIndex, layer);
				refresh();
			}
		});
		icon.revalidate();
	}

	// Vanilla continuously re-anchors its own rune icons, so draw our own replacement row and forward clicks to the original.
	private void compactRuneIcons(int slotIndex, int cellWidth)
	{
		Widget loadoutWidget = client.getWidget(LOADOUT_WIDGET_IDS[slotIndex]);
		if (loadoutWidget == null)
		{
			return;
		}

		Widget[] children = loadoutWidget.getDynamicChildren();
		if (children == null)
		{
			return;
		}

		List<Widget> originals = new ArrayList<>();
		for (Widget child : children)
		{
			if (isVanillaRendered(child) && child.getItemId() >= 0)
			{
				originals.add(child);
			}
		}

		// Regular pouches only support 3 rune types; a loadout saved under a divine pouch may still record a 4th.
		boolean regularPouch = currentViewValue == 3;
		int maxRealSlots = Math.min(originals.size(), regularPouch ? 3 : RunepouchLoadoutCompactConst.RUNE_ICON_MAX_SLOTS);
		int totalSlots = regularPouch ? 4 : maxRealSlots;

		// Vanilla already keeps this slot hidden on its own for regular pouches — leave it untouched.
		Widget unusedFourthSlotOriginal = regularPouch && originals.size() > 3 ? originals.get(3) : null;

		for (Widget child : children)
		{
			if (!isVanillaRendered(child) || child == unusedFourthSlotOriginal)
			{
				continue;
			}

			child.setHidden(true);
			hiddenVanillaWidgets.add(child);
			child.revalidate();
		}

		int row1Top = row1Top();
		int row1Height = Math.max(RunepouchLoadoutCompactConst.LOAD_BUTTON_HEIGHT, RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE);
		int runeRowY = row1Top + row1Height + RunepouchLoadoutCompactConst.RUNE_ROW_GAP;

		int runeRowWidth = totalSlots > 0
			? totalSlots * RunepouchLoadoutCompactConst.RUNE_ICON_SIZE + (totalSlots - 1) * RunepouchLoadoutCompactConst.RUNE_ICON_GUTTER
			: 0;
		// runeRowWidth is always odd; "+1" rounds the leftover pixel left instead of leaning the row 1px off-center.
		int runeRowX = (cellWidth - runeRowWidth + 1) / 2;

		for (int i = 0; i < RunepouchLoadoutCompactConst.RUNE_ICON_MAX_SLOTS; i++)
		{
			Widget runeIcon = getOrCreateOwned(loadoutWidget, RUNE_ICON_CHILD_PREFIX + i, WidgetType.GRAPHIC);
			int runeIconX = runeRowX + i * (RunepouchLoadoutCompactConst.RUNE_ICON_SIZE + RunepouchLoadoutCompactConst.RUNE_ICON_GUTTER);

			boolean disabledFourthSlot = regularPouch && i == 3;

			if (disabledFourthSlot || i >= maxRealSlots)
			{
				if (disabledFourthSlot && i < originals.size())
				{
					renderGreyedRuneSlot(runeIcon, originals.get(i), runeIconX, runeRowY);
				}
				else
				{
					runeIcon.setHasListener(false);
					runeIcon.setHidden(true);
					runeIcon.revalidate();
				}
				continue;
			}

			Widget original = originals.get(i);

			runeIcon.setItemId(original.getItemId());
			runeIcon.setItemQuantity(1);
			runeIcon.setItemQuantityMode(ItemQuantityMode.NEVER);
			runeIcon.setWidthMode(WidgetSizeMode.ABSOLUTE);
			runeIcon.setHeightMode(WidgetSizeMode.ABSOLUTE);
			runeIcon.setOriginalWidth(RunepouchLoadoutCompactConst.RUNE_ICON_SIZE);
			runeIcon.setOriginalHeight(RunepouchLoadoutCompactConst.RUNE_ICON_SIZE);
			runeIcon.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			runeIcon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			runeIcon.setOriginalX(runeIconX);
			runeIcon.setOriginalY(runeRowY);
			runeIcon.setOpacity(0);
			runeIcon.setHidden(false);

			runeIcon.setHasListener(true);
			runeIcon.clearActions();
			runeIcon.setAction(0, "Change " + Text.removeTags(original.getName()) + runeCapSuffix(slotIndex, i));
			runeIcon.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
			{
				if (event.getOp() != 1)
				{
					return;
				}

				client.menuAction(original.getIndex(), original.getId(), MenuAction.CC_OP,
					1, original.getItemId(), "Change", "");
			});
			runeIcon.revalidate();
		}
	}

	private void renderGreyedRuneSlot(Widget runeIcon, Widget original, int x, int y)
	{
		runeIcon.setItemId(original.getItemId());
		runeIcon.setItemQuantity(1);
		runeIcon.setItemQuantityMode(ItemQuantityMode.NEVER);
		runeIcon.setWidthMode(WidgetSizeMode.ABSOLUTE);
		runeIcon.setHeightMode(WidgetSizeMode.ABSOLUTE);
		runeIcon.setOriginalWidth(RunepouchLoadoutCompactConst.RUNE_ICON_SIZE);
		runeIcon.setOriginalHeight(RunepouchLoadoutCompactConst.RUNE_ICON_SIZE);
		runeIcon.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		runeIcon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		runeIcon.setOriginalX(x);
		runeIcon.setOriginalY(y);
		runeIcon.setOpacity(160);
		runeIcon.setHidden(false);
		runeIcon.setHasListener(false);
		runeIcon.clearActions();
		runeIcon.revalidate();
	}

	private String runeCapSuffix(int slotIndex, int position)
	{
		int[] caps = RUNE_CAP_VARBIT_IDS[slotIndex];
		if (position >= caps.length || caps[position] == -1)
		{
			return "";
		}

		int cap = client.getVarbitValue(caps[position]);
		return cap > 0 ? String.format(" (%,d)", cap) : "";
	}

	// Reference identity isn't reliable — a rune-picker interaction can rebuild these children with fresh objects.
	private static boolean isVanillaRendered(Widget child)
	{
		String name = child.getName();
		return name != null && !name.isEmpty();
	}

	private Widget getOrCreateOwned(Widget parent, String tag, int type)
	{
		String key = parent.getId() + ":" + tag;
		Widget cached = ownedWidgets.get(key);
		if (cached != null)
		{
			return cached;
		}

		Widget created = parent.createChild(-1, type);
		ownedWidgets.put(key, created);
		return created;
	}

	private static int indexOf(int[] widgetIds, int widgetId)
	{
		for (int i = 0; i < widgetIds.length; i++)
		{
			if (widgetIds[i] == widgetId)
			{
				return i;
			}
		}

		return -1;
	}

	private void cacheOriginalGeometry(Widget widget)
	{
		originalGeometry.computeIfAbsent(widget.getId(), id -> new int[]{
			widget.getXPositionMode(),
			widget.getYPositionMode(),
			widget.getWidthMode(),
			widget.getHeightMode(),
			widget.getOriginalX(),
			widget.getOriginalY(),
			widget.getOriginalWidth(),
			widget.getOriginalHeight(),
		});
	}
}
