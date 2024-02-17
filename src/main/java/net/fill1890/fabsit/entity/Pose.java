package net.fill1890.fabsit.entity;

import net.fill1890.fabsit.config.Config;
import net.fill1890.fabsit.config.ConfigManager;
import net.fill1890.fabsit.error.PoseException;

/*
Possible poses
 */
public enum Pose {
    SITTING("sitting"),
    LAYING("laying"),
    SPINNING("spinning"),

    SWIMMING("swimming");

    public final String pose;

    Pose(String pose) {
        this.pose = pose;
    }

    public void confirmEnabled() throws PoseException {
        Config.Poses poses = ConfigManager.getConfig().allow_poses;
        boolean allowed = switch (this) {
            case LAYING -> poses.lay;
            case SPINNING -> poses.spin;
            case SITTING -> poses.sit;
            case SWIMMING -> poses.swim;
        };

        if (!allowed) throw new PoseException.PoseDisabled();
    }

    @Override
    public String toString() {
        return this.pose;
    }
}
