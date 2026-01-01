package net.dialingspoon.standalonevc.plugins.impl;

import de.maxhenkel.voicechat.api.Position;

public class PositionImpl implements Position {

    private final double x,y,z;

    public PositionImpl(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object instanceof PositionImpl other) {
            return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0 && Double.compare(z, other.z) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(z);
        return result;
    }
}
