/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxglgames.pong;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.net.*;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.ui.UI;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.almasb.fxglgames.pong.NetworkMessages.*;

/**
 * A simple clone of Pong.
 * Sounds from https://freesound.org/people/NoiseCollector/sounds/4391/ under CC BY 3.0.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class PongApp extends GameApplication implements MessageHandler<String> {
    String key = "AO29HfbMiwneL";

    public static String xorCypher(String text, String key) {
        char[] charText = text.toCharArray();

        for (int i = 0; i < charText.length; i++)
        {
            charText[i] ^= key.charAt(i % key.length());
        }
        return new String(charText);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Pong");
        settings.setVersion("1.0");
        settings.setFontUI("pong.ttf");
        settings.setApplicationMode(ApplicationMode.DEBUG);
    }

    private Entity player1;
    private Entity player2;
    private Entity ball;
    int connectionID;
   // private BatComponent player1Bat;
   // private BatComponent player2Bat;
    private PlayerCharacterComponent player1Character;
    private PlayerCharacterComponent player2Character;

    private Server<String> server;
    long last_time = System.nanoTime();
    private int nextPlayerID = 1;

    int player1Score;
    int player2Score;

    @Override
    protected void initInput() {
        // Player 1 Controls
        getInput().addAction(new UserAction("Up1") {
            @Override
            protected void onAction() {
                player1Character.moveUp();
            }

            @Override
            protected void onActionEnd() {
                player1Character.stop();
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Down1") {
            @Override
            protected void onAction() {
                player1Character.moveDown();
            }

            @Override
            protected void onActionEnd() {
                player1Character.stop();
            }
        }, KeyCode.S);

        // Player 2 Controls
        getInput().addAction(new UserAction("Up2") {
            @Override
            protected void onAction() {
                player2Character.moveUp();
            }

            @Override
            protected void onActionEnd() {
                player2Character.stop();
            }
        }, KeyCode.I);

        getInput().addAction(new UserAction("Down2") {
            @Override
            protected void onAction() {
                player2Character.moveDown();
            }

            @Override
            protected void onActionEnd() {
                player2Character.stop();
            }
        }, KeyCode.K);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("player1score", 0);
        vars.put("player2score", 0);
    }

    @Override
    protected void initGame() {
        Writers.INSTANCE.addTCPWriter(String.class, outputStream -> new MessageWriterS(outputStream));
        Readers.INSTANCE.addTCPReader(String.class, in -> new MessageReaderS(in));

        server = getNetService().newTCPServer(55555, new ServerConfig<>(String.class));

        server.setOnConnected(connection -> {
            int playerID = nextPlayerID++;  // Assign and increment the player ID
            //playerIDs.put(connection, playerID);  // Store the player ID in the map for this connection

            // Send the playerID to the client
            //connection.send("PLAYER_ID," + playerID);

            connection.addMessageHandlerFX(this);
        });

        getGameWorld().addEntityFactory(new PongFactory());
        getGameScene().setBackgroundColor(Color.rgb(0, 0, 5));

        initScreenBounds();
        initGameObjects();

        var t = new Thread(server.startTask()::run);
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 0);

        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BALL, EntityType.WALL) {
            @Override
            protected void onHitBoxTrigger(Entity a, Entity b, HitBox boxA, HitBox boxB) {
                if (boxB.getName().equals("LEFT")) {
                    inc("player2score", +1);
                    player2Score++;
                    server.broadcast(xorCypher("SCORES," + geti("player1score") + "," + geti("player2score"), key));

                    server.broadcast(xorCypher(HIT_WALL_LEFT, key));
                } else if (boxB.getName().equals("RIGHT")) {
                    inc("player1score", +1);
                    player1Score++;
                    server.broadcast(xorCypher("SCORES," + geti("player1score") + "," + geti("player2score"), key));

                    server.broadcast(xorCypher(HIT_WALL_RIGHT, key));
                } else if (boxB.getName().equals("TOP")) {
                    server.broadcast(xorCypher(HIT_WALL_UP, key));
                } else if (boxB.getName().equals("BOT")) {
                    server.broadcast(xorCypher(HIT_WALL_DOWN, key));
                }

                getGameScene().getViewport().shakeTranslational(5);
            }
        });

        CollisionHandler ballBatHandler = new CollisionHandler(EntityType.BALL, EntityType.PLAYER_1) {
            @Override
            protected void onCollisionBegin(Entity a, Entity player) {
                playHitAnimation(player);

                server.broadcast(xorCypher(player == player1 ? BALL_HIT_BAT1 : BALL_HIT_BAT2, key));
                server.broadcast(xorCypher(player == player2 ? BALL_HIT_BAT2 : BALL_HIT_BAT1, key));
            }
        };

        getPhysicsWorld().addCollisionHandler(ballBatHandler);
        getPhysicsWorld().addCollisionHandler(ballBatHandler.copyFor(EntityType.BALL, EntityType.PLAYER_2));
    }

    @Override
    protected void initUI() {
        MainUIController controller = new MainUIController();
        UI ui = getAssetLoader().loadUI("main.fxml", controller);

        controller.getLabelScorePlayer().textProperty().bind(getip("player1score").asString());
        controller.getLabelScoreEnemy().textProperty().bind(getip("player2score").asString());

        getGameScene().addUI(ui);
    }

    @Override
    protected void onUpdate(double tpf) {
        long time = System.nanoTime();
        long deltaTime =  (int) ((time - last_time) / 1000000);
        last_time = time;
        if (deltaTime >= 1) {
            server.broadcast(xorCypher(String.valueOf(deltaTime), key));
            if (!server.getConnections().isEmpty()) {
                var message = "GAME_DATA," + player1.getY() + "," + player2.getY() + "," + ball.getX() + "," + ball.getY() + "," + player1.getX() + "," + player2.getX() + "," + connectionID + "," + player1Score + "," + player2Score;
                message = xorCypher(message, key);
                server.broadcast(message);
            }
        }
    }


    private void initScreenBounds() {
        Entity walls = entityBuilder()
                .type(EntityType.WALL)
                .collidable()
                .buildScreenBounds(150);

        getGameWorld().addEntity(walls);
    }

    private void initGameObjects() {
        ball = spawn("ball", getAppWidth() / 2 - 5, getAppHeight() / 2 - 5);
        player1 = spawn("playerChar", new SpawnData(getAppWidth() / 4, getAppHeight() / 2 - 30).put("isPlayer", true));
        player2 = spawn("playerChar", new SpawnData(3 * getAppWidth() / 4 - 20, getAppHeight() / 2 - 30).put("isPlayer", false));

        player1Character = player1.getComponent(PlayerCharacterComponent.class);
        player2Character = player2.getComponent(PlayerCharacterComponent.class);
    }

    private void playHitAnimation(Entity player) {
        animationBuilder()
                .autoReverse(true)
                .duration(Duration.seconds(0.5))
                .interpolator(Interpolators.BOUNCE.EASE_OUT())
                .rotate(player)
                .from(FXGLMath.random(-25, 25))
                .to(0)
                .buildAndPlay();
    }

    @Override
    public void onReceive(Connection<String> connection, String message) {
        connectionID = connection.getConnectionNum();
        var tokens = message.split(",");
        System.out.println("Processing connection number: " + connectionID);

        Arrays.stream(tokens).skip(1).forEach(key -> {
            if (connectionID == 1) { // CLIENT1's controls
                if (key.endsWith("_DOWN")) {
                    if (key.equals("W_DOWN")) {
                        getInput().mockKeyPress(KeyCode.W); // Maps W to Up1 action for CLIENT1
                    } else if (key.equals("S_DOWN")) {
                        getInput().mockKeyPress(KeyCode.S); // Maps S to Down1 action for CLIENT1
                    }
                } else if (key.endsWith("_UP")) {
                        if (key.equals("W_UP")) {
                            getInput().mockKeyRelease(KeyCode.W);
                        } else if (key.equals("S_UP")) {
                            getInput().mockKeyRelease(KeyCode.S);

                        }
                    }
            } else if (connectionID == 2) { // CLIENT2's controls
                if (key.endsWith("_DOWN")) {
                    if (key.equals("W_DOWN")) {
                        getInput().mockKeyPress(KeyCode.I); // Maps W to Up1 action for CLIENT1
                    } else if (key.equals("S_DOWN")) {
                        getInput().mockKeyPress(KeyCode.K); // Maps S to Down1 action for CLIENT1
                    }
                } else if (key.endsWith("_UP")) {
                    if (key.equals("W_UP")) {
                        getInput().mockKeyRelease(KeyCode.I);
                    } else if (key.equals("S_UP")) {
                        getInput().mockKeyRelease(KeyCode.K);
                    }


                }
            }
        });
    }



    static class MessageWriterS implements TCPMessageWriter<String> {

        private OutputStream os;
        private PrintWriter out;

        MessageWriterS(OutputStream os) {
            this.os = os;
            out = new PrintWriter(os, true);
        }

        @Override
        public void write(String s) throws Exception {
            out.print(s.toCharArray());
            out.flush();
        }
    }

    static class MessageReaderS implements TCPMessageReader<String> {

        private BlockingQueue<String> messages = new ArrayBlockingQueue<>(50);

        private InputStreamReader in;

        MessageReaderS(InputStream is) {
            in =  new InputStreamReader(is);

            var t = new Thread(() -> {
                try {

                    char[] buf = new char[36];

                    int len;

                    while ((len = in.read(buf)) > 0) {
                        var message = new String(Arrays.copyOf(buf, len));

                        System.out.println("Recv message: " + message);

                        messages.put(message);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            t.setDaemon(true);
            t.start();
        }

        @Override
        public String read() throws Exception {
            return messages.take();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
