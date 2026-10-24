package rip.ysm.api.network.fabric.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.Packet;
import rip.ysm.api.network.fabric.YSMChannelImpl;

public final class YSMChannelClientImpl {

    private YSMChannelClientImpl() {
    }

    public static void init(CustomPacketPayload.Type<YSMChannelImpl.Payload> type) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> YSMChannelImpl.dispatch(payload, new ClientPacketContext(context.client(), context.client().getConnection().getConnection())));
    }

    public static void sendToServer(YSMChannelImpl.Payload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static Packet<?> toServerboundPacket(YSMChannelImpl.Payload payload) {
        return ClientPlayNetworking.createServerboundPacket(payload);
    }
}
