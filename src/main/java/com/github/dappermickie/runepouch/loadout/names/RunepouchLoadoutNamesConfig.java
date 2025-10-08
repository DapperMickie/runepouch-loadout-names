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
		keyName = "hideRunePouchNames",
		name = "Hide Loadout Names",
		description = "",
		section = sectionSettings
	)
	default boolean hideRunePouchNames() {
			return false;
	}
	
	@ConfigItem(
		position = 1,
		keyName = "hideRunePouchLoadoutHeader",
		name = "Hide Load-outs Header Text",
		description = "",
		section = sectionSettings
	)
	default boolean hideRunePouchLoadoutHeader() {
			return true;
	}
}
