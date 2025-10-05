package com.github.dappermickie.runepouch.loadout.names;

import javax.inject.Inject;
import net.runelite.client.game.chatbox.ChatboxInput;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.ScriptID;
import net.runelite.client.util.ColorUtil;
import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.ScriptEvent;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.FontID;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Predicate;
import java.awt.Color;
import java.util.function.Consumer;

@Slf4j
public class RunepouchLoadoutIconChatbox extends ChatboxInput {
  private final ChatboxPanelManager chatboxPanelManager;
  private final Client client;

  @Inject
	protected RunepouchLoadoutIconChatbox(ChatboxPanelManager chatboxPanelManager, ClientThread clientThread, Client client)
	{
		this.chatboxPanelManager = chatboxPanelManager;
    this.client = client;
	}

  private Predicate<Integer> onDone;
	private Runnable onClose;
	private int currentSpriteID;
	private int scrollY = 0;

  private final int iconsPerRow = 12;
  private final int iconSize = 28;
  private final int iconSpacing = 11;

	public RunepouchLoadoutIconChatbox onDone(Consumer<Integer> onDone)
	{
		this.onDone = (s) ->
		{
			onDone.accept(s);
			return true;
		};
		return this;
	}

	public RunepouchLoadoutIconChatbox onClose(Runnable onClose)
	{
		this.onClose = onClose;
		return this;
	}

	public RunepouchLoadoutIconChatbox currentSpriteID(int spriteId)
	{
		this.currentSpriteID = spriteId;
		return this;
	}

	public RunepouchLoadoutIconChatbox build()
	{
		chatboxPanelManager.openInput(this);

		return this;
	}

	@Override
	protected void close()
	{
		if (this.onClose != null) {
			onClose.run();
		}
	}

	@Override
  protected void open()
  {
		var container = chatboxPanelManager.getContainerWidget();
		container.deleteAllChildren();

		var prompt = ColorUtil.wrapWithColorTag("Search:", Color.BLACK);
		
		client.setVarcIntValue(VarClientID.MESLAYERMODE, 14);
		client.runScript(ScriptID.CHAT_TEXT_INPUT_REBUILD, prompt);
		
		var text = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		text.setFontId(FontID.BOLD_12);
		text.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		text.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		text.setOriginalX(0);
		text.setOriginalY(0);
		text.setWidthMode(WidgetSizeMode.MINUS);
		text.setOriginalWidth(20);
		text.setHidden(false);
		text.setHasListener(true);
		text.setOnKeyListener((JavaScriptCallback) (ScriptEvent event) -> {
			client.runScript(112, event.getTypedKeyCode(), event.getTypedKeyChar(), prompt);

			update(client.getVarcStrValue(359));
		});
		text.revalidate();

		var closeButton = client.getWidget(InterfaceID.Chatbox.MES_LAYER_CLOSE);
		closeButton.setHidden(false);
		closeButton.setWidthMode(WidgetSizeMode.ABSOLUTE);
		closeButton.setHeightMode(WidgetSizeMode.ABSOLUTE);
		closeButton.setOriginalWidth(15);
		closeButton.setOriginalHeight(15);
		closeButton.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		closeButton.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		closeButton.setOriginalX(2);
		closeButton.setOriginalY(2);
		closeButton.revalidate();

		var closeIcon = client.getWidget(InterfaceID.Chatbox.CLOSE_ICON);
		closeIcon.setSpriteId(SpriteID.CloseButtonsV2.BUTTON);
		closeIcon.setWidthMode(WidgetSizeMode.MINUS);
		closeIcon.setHeightMode(WidgetSizeMode.MINUS);
		closeIcon.setOriginalWidth(0);
		closeIcon.setOriginalHeight(0);
		closeIcon.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		closeIcon.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		closeIcon.setOriginalX(0);
		closeIcon.setOriginalY(0);
		closeIcon.setHasListener(true);
		closeIcon.setOnMouseOverListener((JavaScriptCallback) (ScriptEvent event) -> {
			closeIcon.setSpriteId(SpriteID.CloseButtonsV2.HOVERED);
			closeIcon.revalidate();
		});
		closeIcon.setOnMouseLeaveListener((JavaScriptCallback) (ScriptEvent event) -> {
			closeIcon.setSpriteId(SpriteID.CloseButtonsV2.BUTTON);
			closeIcon.revalidate();
		});
		closeIcon.revalidate();

		update("");
  }

	private void update(String searchText) {
		var scrollArea = client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLAREA);
    scrollArea.setHidden(false);
		scrollArea.revalidate();

		var icons = RunepouchLoadoutIcon.getIcons(searchText);

    int totalRows = icons.size() / iconsPerRow + 1;
		int scrollHeight = Math.max(0, (totalRows * (iconSize + iconSpacing)) + iconSpacing);

		var scrollContents = client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
		scrollContents.deleteAllChildren();
		scrollContents.setScrollHeight(scrollHeight);
		scrollContents.setScrollY(scrollY);
		scrollContents.revalidate();

		/**
		 * Create vertical scrollbar
		 * @see https://github.com/runelite/cs2-scripts/blob/4f2c51ea8837d1bda36b17efc6913a8f2fe4c808/scripts/%5Bproc%2Cscript7605%5D.cs2
		 */
		client.runScript(7605,
      InterfaceID.Chatbox.MES_LAYER_SCROLLBAR,
      InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS
    );
    
    for (int i = 0; i < icons.size(); i++) {
			var icon = icons.get(i);
			int spriteId = icon.spriteId;
			int row = i / iconsPerRow;
			int col = i % iconsPerRow;

			// Create highlight effect
			var iconButtonHighlight = scrollContents.createChild(-1, WidgetType.RECTANGLE);
			iconButtonHighlight.setTextColor(0xffffff);
			iconButtonHighlight.setFilled(true);
			iconButtonHighlight.setSpriteId(spriteId);
			iconButtonHighlight.setWidthMode(WidgetSizeMode.ABSOLUTE);
			iconButtonHighlight.setHeightMode(WidgetSizeMode.ABSOLUTE);
			iconButtonHighlight.setOriginalWidth(iconSize + 4);
			iconButtonHighlight.setOriginalHeight(iconSize + 4);
			iconButtonHighlight.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
			iconButtonHighlight.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
			iconButtonHighlight.setOriginalX(col * (iconSize + iconSpacing) + iconSpacing - 2);
			iconButtonHighlight.setOriginalY(row * (iconSize + iconSpacing) + iconSpacing - 2);
			iconButtonHighlight.setOpacity(255);
			if (currentSpriteID == spriteId) {
				iconButtonHighlight.setOpacity(150);
			}
			iconButtonHighlight.revalidate();
			
			// Create icon button
			var iconButton = scrollContents.createChild(-1, WidgetType.GRAPHIC);
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
			iconButton.setAction(0, icon.name);
			iconButton.setTargetVerb(String.valueOf(spriteId));
			iconButton.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> {
				if (onDone != null) {
					onDone.test(spriteId);
				}
				// currentSpriteID(spriteId);
				// update(searchText);
				chatboxPanelManager.close();
			});

			// Add hover effect
			iconButton.setOnMouseRepeatListener((JavaScriptCallback) (ScriptEvent event) -> {
				iconButtonHighlight.setOpacity(150);
				iconButtonHighlight.revalidate();
			});
			iconButton.setOnMouseLeaveListener((JavaScriptCallback) (ScriptEvent event) -> {
				iconButtonHighlight.setOpacity(255);
				if (currentSpriteID == spriteId) {
					iconButtonHighlight.setOpacity(150);
				}
				iconButtonHighlight.revalidate();
			});
			
			iconButton.revalidate();
		}
	}
}
