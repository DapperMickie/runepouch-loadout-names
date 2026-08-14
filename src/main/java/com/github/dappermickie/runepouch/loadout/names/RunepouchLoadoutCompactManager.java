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

// Optional alternate rendering for the rune pouch loadout list, enabled via
// RunepouchLoadoutNamesConfig#enableCompactLayout(). Reflows
// RUNEPOUCH_LOADOUT_A..J from vanilla's single-column list into a 2-column
// grid, renders per-loadout custom names and theme icons on top of it, and
// reverts everything back to vanilla's own positioning when the toggle is
// turned off (or the panel closes). Reads/writes the exact same RS-profile
// config keys RunepouchLoadoutNamesPlugin already uses for the classic
// single-column view, so switching the toggle never loses data.
@Slf4j
@Singleton
class RunepouchLoadoutCompactManager
{
	private static final String ICON_CHILD_NAME = "rlg-theme-icon";
	private static final String LAYER_CHILD_NAME = "rlg-theme-icon-layer";
	private static final String ARROW_CHILD_NAME = "rlg-load-arrow";
	private static final String RUNE_ICON_CHILD_PREFIX = "rlg-rune-icon-";
	// Vanilla's own "no rune assigned" item, used elsewhere in this same
	// interface for empty rune slots — reused here so our placeholder icon
	// matches natively instead of using an unrelated sprite.
	private static final int PLACEHOLDER_ITEM_ID = 11526;

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

	// Per-loadout, per-rune-position (1-4) saved quantity cap. Confirmed via
	// logging: e.g. RUNE_POUCH_LOADOUT_A_CAP1 read 1000 after saving a
	// loadout with Cosmic rune capped at 1000, and 0 for a rune saved as
	// unlimited ("All"). Position 3 (index 2) for loadouts D and H is -1
	// (unavailable) because that one cap is split across extra bits
	// (_BITSA/_BITSB/_BITSC) whose combination formula isn't derivable from
	// the gameval constants alone — quantity is skipped just for that slot
	// rather than risk showing a wrong number.
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
	// Widgets we've created ourselves (theme icons, rune icons), keyed by
	// "parentWidgetId:tag" so we can find-and-reuse them across refreshes
	// without relying on the Name field — which turned out to double as
	// (part of) the hover/menu text for dynamically-created children, unlike
	// native widgets where TargetVerb handles that separately. Reference
	// identity (not Name) is also how we tell "ours" apart from vanilla's
	// own children when hiding/restoring.
	private final Map<String, Widget> ownedWidgets = new HashMap<>();
	// Exactly the vanilla (not-ours) widgets compact mode has explicitly
	// hidden — populated at every setHidden(true) call site below that
	// targets a vanilla child (compactRuneIcons(), suppressVanillaInterference()).
	// restoreNativeLayout() un-hides only what's tracked here, rather than
	// every non-owned child indiscriminately: some vanilla children (a red
	// "insufficient" indicator overlay, confirmed by user report) are
	// unnamed just like our own widgets, so isVanillaRendered()'s name check
	// can't tell them apart, but they were never touched by compact mode in
	// the first place — a blanket "not ours, so show it" rule force-shows
	// them incorrectly. Reference-tracking exactly what we hid avoids that.
	private final Set<Widget> hiddenVanillaWidgets = new HashSet<>();
	// Vanilla's own Load-button decorations (a hover-glow border, confirmed
	// via debug logging) grow a couple pixels on mouseover and never shrink
	// back, which reads as the whole button growing in our tightly-spaced
	// grid. Caches each decoration's first-seen (un-hovered) geometry, keyed
	// by "loadWidgetId:childIndex", so it can be pinned back every tick the
	// same way applyLoadoutIcon already pins the button itself.
	private final Map<String, int[]> loadButtonChildGeometry = new HashMap<>();
	// Vanilla's own value before we ever touch it — restoreNativeLayout()
	// puts this back so the classic layout's scrollbar/content still fit
	// its actual (taller, single-column) height after leaving compact mode.
	// Not covered by cacheOriginalGeometry()/restoreGeometry(), which only
	// track position/size widget properties, not this separate scroll state.
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

	// Called when the user clicks a loadout's name text. Wired by the plugin
	// since opening the rename chatbox lives there, not here.
	void setRenameRequestHandler(IntConsumer handler)
	{
		this.renameRequestHandler = handler;
	}

	// Called when the user clicks a loadout's theme icon slot (slotIndex,
	// layer). Wired by the plugin since opening the icon picker lives there.
	void setIconChangeRequestHandler(BiConsumer<Integer, Integer> handler)
	{
		this.iconChangeRequestHandler = handler;
	}

	void applyGrid(int viewValue)
	{
		this.currentViewValue = viewValue;

		// ownedWidgets is intentionally NOT cleared here on every entry into
		// compact mode — only resetTrackedState() (fresh panel open) clears
		// it, so a toggle back to compact reuses the same widget objects via
		// getOrCreateOwned() below instead of leaking a new generation.

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

		// Confirmed via logging: the container itself (170px) sits inside a
		// wider frame (190px) — vanilla's own static reservation for the
		// scrollbar track, independent of the scrollbar widget's visibility.
		// Since we hide that scrollbar entirely (below), widen the container
		// to match the frame and reclaim that space instead of leaving it as
		// dead margin down the right edge.
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

		// Hidden rather than repositioned/resized — reclaims the width it
		// occupied for content instead of just reserving space around it.
		// Scroll wheel/drag still works since it's driven by the container's
		// own scroll state, not by this widget's visibility.
		Widget scrollbar = client.getWidget(InterfaceID.Bankside.RUNEPOUCH_LOADOUT_SCROLLBAR);
		if (scrollbar != null)
		{
			scrollbar.setHidden(true);
			scrollbar.revalidate();
		}

		gridApplied = true;
	}

	// Subtle fill behind the whole cell so adjacent loadouts read as
	// distinct blocks instead of blending into the container's own
	// background. Created first so it renders behind the name/button/icon
	// children added below — dynamic children draw in creation order.
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

	// Cheap per-tick correction for the two things vanilla actively keeps
	// fighting us on — re-showing its own rune-icon widgets and enlarging
	// the Load button's decorative children on hover. Called every
	// PostClientTick while compact mode is on instead of the full
	// applyGrid(), since cell position/name/icon slots are static once
	// applied and only write when something's actually drifted.
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

	// Re-reads each loadout's actual rune contents and rebuilds the compact
	// rune-icon row/hover text from them — picks up edits the player just
	// made through vanilla's own rune picker (we forward clicks to it, so
	// there's no direct save callback). Called once per game tick (600ms),
	// not every client tick like suppressVanillaInterference() — rebuilding
	// per loadout is real work, not a near-free geometry check.
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

	// Re-renders names/icons from config without touching layout — used
	// after the user renames a loadout or changes its icon.
	void refresh()
	{
		if (!gridApplied)
		{
			return;
		}

		applyGrid(currentViewValue);
	}

	// Reverts everything applyGrid() touched: geometry of the native
	// loadout/load/name widgets, our own created child widgets (hidden,
	// since there's no per-child removal API — only deleteAllChildren(),
	// which would also wipe vanilla's own children of the same parent), and
	// visibility of vanilla's rune-icon children compactRuneIcons() hid.
	// Idempotent — safe even if compact mode was never turned on.
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
				// Two disjoint, precisely-tracked sets rather than a
				// name-based heuristic or a blanket "not ours, so show it"
				// rule — both mishandle vanilla children compact mode never
				// actually touched (a row-background piece, a red
				// "insufficient" indicator). A child in neither set was
				// never touched by compact mode — leave its state as-is.
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

		// Deliberately not clearing the caches here — see applyGrid(). The
		// panel may still be open (a mode toggle, not a real close), so they
		// stay valid for reuse; only resetTrackedState() clears them.
		gridApplied = false;
	}

	// Clears all cached widget references/geometry and resets applied
	// state. Called by the plugin only on a genuine fresh panel open (the
	// interface rebuilt from scratch, so cached widget references are
	// stale) — not on every compact/classic toggle, which reuses the same
	// widget objects instead (see applyGrid()).
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

	// Deliberately its own dedicated toggle (hideCompactLoadoutNames), not
	// enableRunePouchNames() (classic mode's own check — that one only gates
	// whether typing a custom name is allowed, not whether name text shows)
	// and not hideRunePouchNames() (the legacy toggle kept in sync with
	// vanilla's own runepouch settings varbit — reusing it here would let a
	// classic-mode-oriented setting silently affect compact mode too).
	private boolean namesEnabled()
	{
		return !config.hideCompactLoadoutNames();
	}

	// The name strip's height is only reserved when names are actually
	// shown — otherwise every cell would carry a blank gap above the button
	// row for a widget that's hidden.
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

	// Only reads — the write side (opening the rename chatbox, persisting
	// the new value) reuses RunepouchLoadoutNamesPlugin's existing
	// renameLoadout(id), rather than duplicating it here, since it already
	// writes to this exact key.
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

	// Like getLoadoutName(), setting a new icon (the picker UI, then
	// persisting it) reuses the Plugin's existing changeLoadoutIcon(id,
	// layer)/setLoadoutIcon(id, icon, layer) — only the in-place right-click
	// "Reset icon" action on a grid icon slot needs a direct write here,
	// since that's a widget click within this class, not routed through the
	// Plugin's own menu-entry handling.
	private void resetLoadoutIcon(int slotIndex, int layer)
	{
		configManager.unsetRSProfileConfiguration(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP, iconKey(slotIndex + 1, layer));
	}

	// Matches RunepouchLoadoutNamesPlugin's own key format exactly —
	// "runepouch.loadout.<viewValue>.<id>[.icon[_layer]]" — the viewValue
	// segment scopes names/icons per pouch tier (a regular and a divine
	// pouch don't share the same 10 loadouts) and is easy to drop by
	// accident since it's not in the "id" naming; both layouts must agree
	// on this key or renames/icon changes in one won't show in the other.
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

		// Vanilla sized this widget to fill the remaining row (MINUS mode)
		// beside the load button in its single-column layout. The compact
		// cell is taller, and without pinning this widget's own height it
		// silently grew to cover most of the cell — swallowing clicks meant
		// for the load button and the rune icons below it. Pin it to a thin
		// strip along the top instead.
		nameWidget.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		nameWidget.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		nameWidget.setWidthMode(WidgetSizeMode.MINUS);
		nameWidget.setHeightMode(WidgetSizeMode.ABSOLUTE);
		nameWidget.setOriginalX(0);
		nameWidget.setOriginalY(0);
		nameWidget.setOriginalWidth(0);
		nameWidget.setOriginalHeight(RunepouchLoadoutCompactConst.NAME_HEIGHT);

		// Vanilla's own click action on this widget opens its native preset
		// boss-name picker; override it so clicking the name opens our rename
		// chatbox instead. Setting text/target alone (without an OnOpListener)
		// would leave vanilla's original action still wired underneath.
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

		// Button + both theme icons together, centered as a group in the
		// cell rather than left-aligned — row 1 of 2, with the rune icons
		// in a full-width row beneath both (see compactRuneIcons()). The
		// trailing ICON_BORDER_PADDING accounts for the second icon's
		// border, which extends past its own right edge — without it here,
		// centering only accounts for the icon graphics and the border
		// pokes out past the intended margin on the right.
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

		// Deliberately not touching this button's own mouse listeners to stop
		// the hover-grow (see pinLoadButtonChildren): Widget has no getter for
		// OnMouseOver/Repeat/Leave, so overwriting them with no-ops is a
		// one-way door — vanilla's native hover feedback (button background)
		// never comes back even after switching back to classic mode. Rely on
		// pinLoadButtonChildren's per-tick correction only, which is a little
		// laggier but non-destructive.

		applyLoadArrow(loadoutWidget, buttonX, buttonY);

		int primarySprite = getLoadoutIcon(slotIndex, 0);
		int layerSprite = getLoadoutIcon(slotIndex, 1);
		boolean isCustomPrimary = primarySprite != RunepouchLoadoutNamesPlugin.DEFAULT_LOADOUT_ICON;
		boolean hasLayer = layerSprite != RunepouchLoadoutNamesPlugin.DEFAULT_LOADOUT_ICON;

		// The load button has ~12 of its own dynamic children (vanilla's
		// hover/pressed frame decorations), and attaching our icon as a
		// child of it put our icon in that same stack — the button's own
		// hover script swaps those decorations without knowing about ours,
		// making our icon disappear or get buried on hover. Attaching to
		// the loadout cell instead (a sibling, not nested in the button)
		// avoids that entirely — this widget has no competing hover behavior.
		// Same row as the button, beside it — vertically centered against
		// the button's height rather than sharing its top edge, since the
		// icons are shorter than the button.
		int iconY = buttonY + (RunepouchLoadoutCompactConst.LOAD_BUTTON_HEIGHT - RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE) / 2;
		int iconX = buttonX + RunepouchLoadoutCompactConst.LOAD_BUTTON_WIDTH + RunepouchLoadoutCompactConst.BUTTON_ICON_GAP;

		applyIconSlot(loadoutWidget, ICON_CHILD_NAME, iconX, iconY, RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE, primarySprite, isCustomPrimary, slotIndex, 0);

		// Same size, side by side — not stacked on the primary icon.
		int layerX = iconX + RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE + RunepouchLoadoutCompactConst.CUSTOM_ICON_GUTTER;
		applyIconSlot(loadoutWidget, LAYER_CHILD_NAME, layerX, iconY, RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE, layerSprite, hasLayer, slotIndex, 1);
	}

	// Classic mode's createChild(9, ...) icon overlay always replaces
	// vanilla's own native arrow graphic at that same child index (confirmed
	// via logging), and classic mode runs before compact mode regardless of
	// which layout is selected — so the native arrow is already gone by the
	// time this runs. Draw a replacement in its place rather than depending
	// on it surviving. Attached to loadoutWidget (a sibling of the button,
	// same as the theme icons) rather than as a child of the button itself,
	// for the same hover-interference reasons as applyIconSlot().
	private void applyLoadArrow(Widget loadoutWidget, int buttonX, int buttonY)
	{
		int size = RunepouchLoadoutCompactConst.ARROW_ICON_SIZE;
		int x = buttonX + (RunepouchLoadoutCompactConst.LOAD_BUTTON_WIDTH - size) / 2;
		int y = buttonY + (RunepouchLoadoutCompactConst.LOAD_BUTTON_HEIGHT - size) / 2;

		Widget arrow = getOrCreateOwned(loadoutWidget, ARROW_CHILD_NAME, WidgetType.GRAPHIC);
		// Confirmed via Widget Inspector: vanilla's real arrow is sprite
		// 2151 (SpriteID.AccManIcons._6) — same sprite as DEFAULT_LOADOUT_ICON.
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

	// Vanilla's Load button has a hover-glow border among its dynamic
	// children that a native script enlarges on mouseover and never
	// shrinks back, which reads as the whole button growing in our
	// tightly-spaced grid. Cache each child's first-seen (un-hovered)
	// geometry and pin it back every tick.
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

			// Called every client tick (see RunepouchLoadoutCompactManager's
			// tick-vs-open split) to catch vanilla's hover-grow the moment it
			// happens, so skip the write+revalidate entirely when nothing's
			// actually drifted — keeps the common case (every tick where
			// hover hasn't touched this button) essentially free.
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

	// Renders one custom icon slot — the real sprite when set, or vanilla's
	// own "no rune assigned" item icon as a placeholder when not. Clicking
	// opens the icon picker for this slot/layer.
	private void applyIconSlot(Widget parent, String tag, int x, int y, int size, int spriteId, boolean isSet, int slotIndex, int layer)
	{
		// Black outline (not a filled backdrop — the whole cell has its own
		// backdrop now, see applyCellBackdrop()) so the slot still reads as
		// a distinct target even before an icon is set.
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

		// TargetVerb doesn't concatenate onto Action for dynamically-created
		// children the way it does for native widgets — so put the full text
		// in Action directly. Two actions on one widget show as two
		// right-click menu rows, with op 1 (the first) also firing on a
		// plain left-click.
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

	// Vanilla's own rune-type icon children are item-rendered widgets a
	// vanilla script continuously re-anchors to the right/bottom edge of
	// the old full-width row — any reposition we apply gets silently
	// overwritten. So instead: read which item each shows, then draw our
	// own replacement icons in a full-width row beneath the load
	// button/theme icon row, forwarding clicks to the original (hidden)
	// widget so the vanilla rune picker still opens.
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

		// SpriteId is always -1 on these (they're item-rendered, not sprite
		// graphics) — ItemId is what actually distinguishes a populated slot
		// (e.g. 565 = Blood rune) from an empty one (-1).
		List<Widget> originals = new ArrayList<>();
		for (Widget child : children)
		{
			if (isVanillaRendered(child) && child.getItemId() >= 0)
			{
				originals.add(child);
			}
		}

		// Regular (non-divine) pouches only support 3 rune types — a loadout
		// saved back when a divine pouch was equipped can still have a real
		// 4th rune recorded, but it can't be loaded into the current pouch.
		// Force position 4 to a disabled placeholder rather than showing
		// real-looking data that doesn't work.
		boolean regularPouch = currentViewValue == 3;
		int maxRealSlots = Math.min(originals.size(), regularPouch ? 3 : RunepouchLoadoutCompactConst.RUNE_ICON_MAX_SLOTS);
		int totalSlots = regularPouch ? 4 : maxRealSlots;

		// Vanilla's own regular-pouch view already keeps this widget
		// hidden on its own — leave it untouched rather than hiding +
		// auto-restoring it like the other rune widgets, or restoring it
		// jumbles it on top of classic mode after leaving compact mode.
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

		// Row 1 (button + theme icons) is as tall as the taller of the two;
		// the rune row sits beneath both, spanning and centered within the
		// full cell width — its width varies with how many runes this
		// loadout actually has saved, so the centering offset is computed
		// per-loadout rather than being a constant.
		int row1Top = row1Top();
		int row1Height = Math.max(RunepouchLoadoutCompactConst.LOAD_BUTTON_HEIGHT, RunepouchLoadoutCompactConst.CUSTOM_ICON_SIZE);
		int runeRowY = row1Top + row1Height + RunepouchLoadoutCompactConst.RUNE_ROW_GAP;

		int runeRowWidth = totalSlots > 0
			? totalSlots * RunepouchLoadoutCompactConst.RUNE_ICON_SIZE + (totalSlots - 1) * RunepouchLoadoutCompactConst.RUNE_ICON_GUTTER
			: 0;
		// runeRowWidth is always odd (totalSlots * 20 - 3), so plain "/2"
		// floors the remainder and leans the row 1px left. "+1" rounds that
		// pixel to the left margin instead, centering it correctly.
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
					// A real 4th rune is still recorded (e.g. saved back when
					// a divine pouch was equipped) but can't actually be
					// loaded with the current (regular, 3-slot) pouch — show
					// it greyed out rather than a generic placeholder, so the
					// player can still see what it *was* set to.
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

			// Vanilla's own click action opens its native rune picker on the
			// original widget; forward clicks on our replacement there
			// instead of trying to reimplement or guess at that behavior.
			// TargetVerb doesn't concatenate onto Action for dynamically
			// created children the way it does for native widgets, so the
			// full label goes directly into Action instead.
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

	// Shows a rune saved to a regular pouch's unusable 4th slot faded and
	// non-interactive — visible, but can't be clicked since it can't
	// actually be loaded.
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

	// " (1,000)" when this rune position has a specific saved quantity cap,
	// or "" when it's unlimited ("All", cap 0) or unreadable (see
	// RUNE_CAP_VARBIT_IDS's comment).
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

	// True for vanilla's own rendered widgets (rune-icon children always
	// have a real item name, e.g. "Cosmic rune"), false for ours (never
	// named). Used here instead of reference identity since a rune-picker
	// interaction can make vanilla rebuild a loadout's dynamic children
	// with fresh Widget objects, breaking reference-based matching.
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
