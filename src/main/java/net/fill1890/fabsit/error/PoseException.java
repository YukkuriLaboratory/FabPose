package net.fill1890.fabsit.error;

public class PoseException extends Exception {
    public PoseException(String message) {
        super(message);
    }

    // trying to pose in midair
    public static class MidairException extends PoseException {
        public MidairException() {
            super("Cannot pose in midair");
        }
    }
    // trying to pose as a spectator
    public static class SpectatorException extends PoseException {
        public SpectatorException() {
            super("Cannot pose as a spectator");
        }
    }
    // trying to pose when underwater/flying/etc
    public static class StateException extends PoseException {
        public StateException(String message) {
            super(message);
        }
    }
    // pose disabled
    public static class PoseDisabled extends PoseException {
        public PoseDisabled() {
            super("Pose is disabled");
        }
    }
    // block already occupied
    public static class BlockOccupied extends PoseException {
        public BlockOccupied() {
            super("Block is already occupied");
        }
    }

    public static class TooQuickly extends PoseException {
        public TooQuickly() {
            super("Too quickly");
        }
    }

    public static class PermissionException extends PoseException {
        public PermissionException() {
            super("You do not have permission to pose");
        }
    }
}
