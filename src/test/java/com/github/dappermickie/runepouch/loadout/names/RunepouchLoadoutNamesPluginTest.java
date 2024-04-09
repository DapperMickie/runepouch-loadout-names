package com.github.dappermickie.runepouch.loadout.names;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RunepouchLoadoutNamesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RunepouchLoadoutNamesPlugin.class);
		RuneLite.main(args);
	}
}