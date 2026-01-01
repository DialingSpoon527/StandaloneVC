package net.dialingspoon.standalonevc.mixin;

import de.maxhenkel.voicechat.config.ServerConfig;
import de.maxhenkel.voicechat.configbuilder.ConfigBuilder;
import de.maxhenkel.voicechat.configbuilder.entry.IntegerConfigEntry;
import net.dialingspoon.standalonevc.StandaloneVCCommon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(ServerConfig.class)
public class ServerConfigMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lde/maxhenkel/voicechat/configbuilder/ConfigBuilder;integerEntry(Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;[Ljava/lang/String;)Lde/maxhenkel/voicechat/configbuilder/entry/IntegerConfigEntry;", ordinal = 0))
    private IntegerConfigEntry forwardGroup(ConfigBuilder instance, String string, Integer i, Integer j, Integer k, String[] strings) {
        StandaloneVCCommon.proxyPassword = instance.stringEntry("proxy_password", UUID.randomUUID().toString(), new String[]{"The password to ensure that someone else doesn't try to take over your proxy. Copy this into your other config, or replace with any password you like."});
        return instance.integerEntry(string, i, j, k, strings);
    }
}
