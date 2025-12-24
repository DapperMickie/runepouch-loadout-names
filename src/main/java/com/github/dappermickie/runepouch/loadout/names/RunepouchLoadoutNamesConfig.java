package com.github.dappermickie.runepouch.loadout.names;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(RunepouchLoadoutNamesConfig.RUNEPOUCH_LOADOUT_CONFIG_GROUP)
public interface RunepouchLoadoutNamesConfig extends Config
{
	String RUNEPOUCH_LOADOUT_CONFIG_GROUP = "RunepouchLoadoutConfig";

	@ConfigSection(
		position = 0,
		name = "Settings",
		description = ""
	)
	String sectionSettings = "sectionSettings";
	
	@ConfigItem(
		position = 0,
		keyName = "enableRunePouchNames",
		name = "Enable Loadout Names",
		description = "Allow typing custom names for loadouts",
		section = sectionSettings
	)
	default boolean enableRunePouchNames() {
			return true;
	}	

	@ConfigItem(
		position = 1,
		keyName = "enableRunePouchIcons",
		name = "Enable Loadout Icons",
		description = "Show an icon in the load button",
		section = sectionSettings
	)
	default boolean enableRunePouchIcons() {
			return true;
	}

	@ConfigSection(
		position = 1,
		name = "Deprecated",
		description = ""
	)
	String sectionDeprecated = "sectionDeprecated";

	@ConfigItem(
		position = 0,
		keyName = "readme",
		name = "<html>Deprecated configuration options have<br>" +
			"been moved to the in-game settings.</html>",
		description = "",
		section = sectionDeprecated
	)
	void readme();

	@ConfigItem(
		position = 0,
		keyName = "hideRunePouchNames",
		name = "Hide Loadout Names",
		description = "",
		section = sectionDeprecated
	)
	default boolean hideRunePouchNames() {
			return false;
	}
}
