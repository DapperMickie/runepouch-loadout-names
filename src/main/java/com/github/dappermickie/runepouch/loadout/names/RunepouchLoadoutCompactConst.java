package com.github.dappermickie.runepouch.loadout.names;

final class RunepouchLoadoutCompactConst
{
	private RunepouchLoadoutCompactConst()
	{
	}

	static final int GRID_COLUMNS = 2;
	static final int SLOT_COUNT = 10;
	static final int GRID_ROWS = (SLOT_COUNT + GRID_COLUMNS - 1) / GRID_COLUMNS;

	static final int CELL_HEIGHT = 78;
	static final int CELL_GUTTER_X = 3;
	static final int CELL_GUTTER_Y = 3;
	static final int CONTAINER_PADDING_X = 3;
	// Scrollbar is hidden outright rather than reserved for (see RunepouchLoadoutCompactManager).
	static final int SCROLLBAR_RESERVE = 0;

	static final int NAME_HEIGHT = 16;
	static final int ROW_TOP_GAP = 2;

	// Vanilla's own un-hovered Load button size.
	static final int LOAD_BUTTON_WIDTH = 30;
	static final int LOAD_BUTTON_HEIGHT = 32;

	static final int ARROW_ICON_SIZE = 22;

	static final int CUSTOM_ICON_SIZE = 22;
	static final int CUSTOM_ICON_GUTTER = 6;
	static final int ICON_BORDER_PADDING = 2;
	static final int BUTTON_ICON_GAP = 4;

	static final int RUNE_ICON_SIZE = 17;
	static final int RUNE_ICON_GUTTER = 3;
	static final int RUNE_ICON_MAX_SLOTS = 6;
	static final int RUNE_ROW_GAP = 4;
}
