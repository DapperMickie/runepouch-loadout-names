package com.github.dappermickie.runepouch.loadout.names;

import javax.inject.Inject;
import net.runelite.client.game.chatbox.ChatboxInput;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.Client;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.ScriptEvent;
import net.runelite.api.widgets.JavaScriptCallback;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import lombok.extern.slf4j.Slf4j;
import java.util.function.Predicate;
import java.util.function.Consumer;

@Slf4j
public class RunepouchLoadoutIconChatbox extends ChatboxInput {
  private final ChatboxPanelManager chatboxPanelManager;
  private final Client client;

  @Inject
	protected RunepouchLoadoutIconChatbox(ChatboxPanelManager chatboxPanelManager, Client client)
	{
		this.chatboxPanelManager = chatboxPanelManager;
    this.client = client;
    this.availableSpriteIds = getAllSpriteIds();
	}

  private int[] availableSpriteIds;
  private Predicate<Integer> onDone;
	private Runnable onClose;

  private final int iconsPerRow = 12;
  private final int iconSize = 27;
  private final int iconSpacing = 12;

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

	public RunepouchLoadoutIconChatbox build()
	{
		chatboxPanelManager.openInput(this);

		return this;
	}

	@Override
	public void close()
	{
		if (onClose != null) {
			onClose.run();
		}
	}

	@Override
  protected void open()
  {
		var scrollArea = client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLAREA);
    scrollArea.setHidden(false);
		scrollArea.setOriginalY(0);
		scrollArea.setOriginalHeight(0);
		scrollArea.setHeightMode(WidgetSizeMode.MINUS);
		scrollArea.revalidate();

    int totalRows = (Math.round(availableSpriteIds.length / iconsPerRow));
		int scrollHeight = Math.max(0, (totalRows * (iconSize + iconSpacing)) + iconSpacing);

		var scrollContents = client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS);
		scrollContents.deleteAllChildren();
		scrollContents.setOriginalHeight(0);
		scrollContents.setHeightMode(WidgetSizeMode.MINUS);
		scrollContents.setScrollHeight(scrollHeight);
		scrollContents.setScrollY(0);
		scrollContents.revalidate();

		client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLAREA_RECT0).setHidden(true);
		client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLAREA_RECT1).setHidden(true);
		client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLAREA_RECT2).setHidden(true);

		var scrollBar = client.getWidget(InterfaceID.Chatbox.MES_LAYER_SCROLLBAR);
		scrollBar.setOriginalHeight(0);
		scrollBar.setHeightMode(WidgetSizeMode.MINUS);
		scrollBar.setScrollHeight(scrollHeight);
		scrollBar.setScrollY(0);
		scrollBar.revalidate();

		/**
		 * Create vertical scrollbar
		 * @see https://github.com/runelite/cs2-scripts/blob/4f2c51ea8837d1bda36b17efc6913a8f2fe4c808/scripts/%5Bproc%2Cscript7605%5D.cs2#L2
		 */
		client.runScript(7605,
      InterfaceID.Chatbox.MES_LAYER_SCROLLBAR,
      InterfaceID.Chatbox.MES_LAYER_SCROLLCONTENTS
    );
    
    for (int i = 0; i < availableSpriteIds.length; i++) {
			int spriteId = availableSpriteIds[i];
			int row = i / iconsPerRow;
			int col = i % iconsPerRow;
			
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
			iconButton.setAction(0, "Set Icon (" + spriteId + ")");
			iconButton.setTargetVerb(String.valueOf(spriteId));
			iconButton.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> {
				if (onDone != null) {
					onDone.test(spriteId);
				}
				chatboxPanelManager.close();
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

	private int[] getAllSpriteIds()
	{
		List<Integer> spriteIds = new ArrayList<>();
		spriteIds.add(SpriteID.AccManIcons._6);
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
		addStaticIntFields(SpriteID.PvpwIcons.class, spriteIds);
		addStaticIntFields(SpriteID.PvpaRankicons.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilIcons.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilSkulls.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilCombatIconsSmall.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilSkillingIconsSmall.class, spriteIds);
		addStaticIntFields(SpriteID.DeadmanSigilUtilityIconsSmall.class, spriteIds);
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
				} catch (Exception e) { }
			}
		}
	}
}
