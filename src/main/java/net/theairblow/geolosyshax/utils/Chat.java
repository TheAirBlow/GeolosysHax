package net.theairblow.geolosyshax.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;

public class Chat {
    public static void send(String str, Object... objs) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.ingameGUI.getChatGUI()
                .printChatMessage(new TextComponentString(String.format(str, objs))));
    }

    public static void sendPrefix(String str, Object... objs) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.ingameGUI.getChatGUI().printChatMessage(
                new TextComponentString("§6[§cGeolosysHax§6] " + String.format(str, objs))));
    }

    public static void send(Style style, String str, Object... objs) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.ingameGUI.getChatGUI()
                .printChatMessage(new TextComponentString(String.format(str, objs)).setStyle(style)));
    }

    public static void sendPrefix(Style style, String str, Object... objs) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.ingameGUI.getChatGUI().printChatMessage(
                new TextComponentString("§6[§cGeolosysHax§6] " + String.format(str, objs)).setStyle(style)));
    }
}
