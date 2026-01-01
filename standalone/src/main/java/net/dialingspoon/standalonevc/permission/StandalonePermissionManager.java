package net.dialingspoon.standalonevc.permission;

import de.maxhenkel.voicechat.permission.Permission;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.permission.PermissionType;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;

import java.lang.reflect.InvocationTargetException;

public class StandalonePermissionManager extends PermissionManager {

    @Override
    public Permission createPermissionInternal(String modId, String node, PermissionType type) {
        return new Permission() {
            @Override
            public boolean hasPermission(de.maxhenkel.voicechat.api.ServerPlayer player) {
                try {
                    return (boolean) StandaloneVoiceChat.getHasPermissionMethod().invoke(type, player); //TODO pull in forge/fabric managers
                } catch (IllegalAccessException | InvocationTargetException e) {
                    StandaloneVoiceChat.LOGGER.error("Permission check failed, defaulting", e);
                    return false;
                }
            }

            @Override
            public PermissionType getPermissionType() {
                return type;
            }
        };
    }
}
