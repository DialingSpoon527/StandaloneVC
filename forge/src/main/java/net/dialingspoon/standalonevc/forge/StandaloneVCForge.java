package net.dialingspoon.standalonevc.forge;

import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.dialingspoon.standalonevc.net.packets.PlayerLevelPacket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("standalonevc")
public final class StandaloneVCForge {
    public StandaloneVCForge() {
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) ->
                StandaloneVCCommon.send(new PlayerLevelPacket(e.getEntity().getUUID(), e.getEntity().level().dimension().location().toString()))
        );
        StandaloneVCCommon.init();
    }
}
