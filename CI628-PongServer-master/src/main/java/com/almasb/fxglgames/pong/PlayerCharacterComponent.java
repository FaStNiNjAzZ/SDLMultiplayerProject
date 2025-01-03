package com.almasb.fxglgames.pong;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;

/**
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class PlayerCharacterComponent extends Component {

    private static final double  PLAYER_SPEED = 420;
    private static final double PLAYER_JUMP_FORCE = 100;

    protected PhysicsComponent physics;

    public void moveDown()
    {
        if (entity.getY() <= FXGL.getAppWidth()-25)
            physics.setVelocityY(PLAYER_SPEED);
        else
            stop();
    }

    public void moveUp()
    {
        if (entity.getY() >= PLAYER_SPEED / 60)
            physics.setVelocityY(-PLAYER_SPEED);
        else
            stop();
    }


    public void stop() {
        physics.setLinearVelocity(0, 0);
    }
}
