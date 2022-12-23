package org.lexize.ulyanovsk.models;

import org.lexize.ulyanovsk.Ulyanovsk;

import java.util.List;

public class JailedPlayerSavedData {
    public double posX,posY,posZ;
    public float rotYaw,rotPitch;
    public String world_uuid;
    public String[] encodedInventoryData;
    public List<String> encodedPotionEffects;
    public double health;

    @Override
    public String toString() {
        return Ulyanovsk.getInstance().getJson().toJson(this);
    }
}
