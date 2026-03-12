package com.xinyihl.functionalstoragelegacy.common.network;

import com.xinyihl.functionalstoragelegacy.Tags;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class NetworkHandler {

    public static SimpleNetworkWrapper CHANNEL;
    private static final int packetId = 0;

    public static void init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

        // Register packets here as needed
        // Example: CHANNEL.registerMessage(SomePacket.Handler.class, SomePacket.class, packetId++, Side.SERVER);
    }
}
