package net.dialingspoon.standalonevc.fabric;

import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.fabricmc.api.ModInitializer;

public final class StandaloneVCFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        StandaloneVCCommon.init();
    }
}
