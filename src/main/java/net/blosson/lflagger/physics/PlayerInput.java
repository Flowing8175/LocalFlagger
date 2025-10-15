package net.blosson.lflagger.physics;

public class PlayerInput {
    public float forward;
    public float strafe;
    public boolean jumping;
    public boolean sneaking;

    public PlayerInput(float forward, float strafe, boolean jumping, boolean sneaking) {
        this.forward = forward;
        this.strafe = strafe;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }
}