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

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.net.*;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.RaycastResult;
import com.almasb.fxgl.ui.UI;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * A simple clone of Pong.
 * Sounds from https://freesound.org/people/NoiseCollector/sounds/4391/ under CC BY 3.0.
 *
 * @author Almas Baimagambetov (AlmasB) (almaslvl@gmail.com)
 */
public class MainApp extends GameApplication implements MessageHandler<String> {

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Fighting game");
        settings.setVersion("1.0");
        settings.setFontUI("pong.ttf");
        settings.setApplicationMode(ApplicationMode.DEBUG);
    }

    private Entity player1;
    private Entity player2;
    private PlayerComponent player1Bat;
    private PlayerComponent player2Bat;

    private Server<String> server;

    public boolean playerConnected1 = false;
    public boolean playerConnected2 = false;

    public  double attackCooldown1 = 0;

    public  double attackCooldown2 = 0;

    public double throwWindup1 = 0;

    public boolean throwcheck1 = false;

    public double throwWindup2 = 0;

    public boolean throwcheck2 = false;

    public int spectators = 0;

    public boolean player1Dead = false;

    public boolean player2Dead = false;

    public double respawnTimer = 0;




    @Override
    protected void initInput() {
        getInput().addAction(new UserAction("Right1"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead) {
                if (!player1Bat.block1 && !player1Bat.block2 && attackCooldown1 < System.currentTimeMillis()) {
                    player1Bat.right();
                    server.broadcast("Anim1,1");
                }
            }
            }

            @Override
            protected void onActionEnd() {
                player1Bat.stop();
                server.broadcast("Anim1,0");
            }
        }, KeyCode.A);


        getInput().addAction(new UserAction("Left1"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead) {
                if (!player1Bat.block1 && !player1Bat.block2 && attackCooldown1 < System.currentTimeMillis()) {
                    player1Bat.left();
                    server.broadcast("Anim1,1");
                }
            }
            }

          @Override
            protected void onActionEnd() {
                player1Bat.stop();
                server.broadcast("Anim1,0");
           }
        }, KeyCode.D);

        getInput().addAction(new UserAction("Block1"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead){player1Bat.stop(); player1Bat.block(); server.broadcast("Anim1,2");}}

            @Override
            protected void onActionEnd() {
                player1Bat.unblock();
                server.broadcast("Anim1,0");
            }
        }, KeyCode.W);


        getInput().addAction(new UserAction("Right2"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead) {
                if (!player2Bat.block1 && !player2Bat.block2 && attackCooldown2 < System.currentTimeMillis()) {
                    player2Bat.right();
                    server.broadcast("Anim2,1");
                }
            }
            }

            @Override
            protected void onActionEnd() {
                player2Bat.stop();
                server.broadcast("Anim2,0");
            }
        }, KeyCode.J);


        getInput().addAction(new UserAction("Left2"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead) {
                if (!player2Bat.block1 && !player2Bat.block2 && attackCooldown2 < System.currentTimeMillis()) {
                    player2Bat.left();
                    server.broadcast("Anim2,1");
                }
            }
            }

            @Override
            protected void onActionEnd() {
                player2Bat.stop();
                server.broadcast("Anim2,0");
            }
        }, KeyCode.L);

        getInput().addAction(new UserAction("Block2"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead){player2Bat.stop(); player2Bat.block(); server.broadcast("Anim2,2");}}

            @Override
            protected void onActionEnd() {
                player2Bat.unblock();
                server.broadcast("Anim2,0");
            }
        }, KeyCode.I);

        getInput().addAction(new UserAction("BlockKick1"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead){player1Bat.stop(); player1Bat.blockKick(); server.broadcast("Anim1,3");}}

            @Override
            protected void onActionEnd() {
                player1Bat.unblock();
                server.broadcast("Anim1,0");
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("BlockKick2"){
            @Override
            protected void onAction() {if (!player1Dead &&!player2Dead){player2Bat.stop(); player2Bat.blockKick(); server.broadcast("Anim2,3");}}

            @Override
            protected void onActionEnd() {
                player2Bat.unblock();
                server.broadcast("Anim2,0");
            }
        }, KeyCode.K);

        //player 1 punch
        onKeyDown(KeyCode.E, ()-> {if (!player1Dead &&!player2Dead){if(!player2Bat.block1){
            if(!player1Bat.block1 && !player1Bat.block2){
                if (attackCooldown1 <= System.currentTimeMillis()){
                    player1Bat.stop();
                    RaycastResult Punch1 = getPhysicsWorld().raycast(player1.getCenter(), new Point2D(player1.getCenter().getX() + 70, player1.getCenter().getY()));
                    if (Punch1.getEntity().isPresent()){
                        if(Punch1.getEntity().get().getType() == EntityType.PLAYER_2){
                            System.out.println("entity hit");
                            inc("player2health", -5);
                            server.broadcast("SCORES," + geti("player1health") + "," + geti("player2health"));
                            server.broadcast("Anim2,6" + "|");
                            attackCooldown2 = System.currentTimeMillis() + 400;
                        }
                    }
                    else {
                        System.out.println("entity not hit");
                    }
                    attackCooldown1 = System.currentTimeMillis() + 500;
                    server.broadcast("Anim1,4");
                }
            }
        }
        }});

        //player 2 punch
        onKeyDown(KeyCode.O, ()-> {if (!player1Dead &&!player2Dead){if(!player1Bat.block1){
            if(!player2Bat.block1 && !player2Bat.block2){
                if (attackCooldown2 <= System.currentTimeMillis()){
                    player2Bat.stop();
                    RaycastResult Punch2 = getPhysicsWorld().raycast(player2.getCenter(), new Point2D(player2.getCenter().getX() - 70, player2.getCenter().getY()));
                    if (Punch2.getEntity().isPresent()) {
                        if (Punch2.getEntity().get().getType() == EntityType.PLAYER_1) {
                            inc("player1health", -5);
                            server.broadcast("SCORES," + geti("player1health") + "," + geti("player2health"));
                            server.broadcast("Anim1,6" + "|");
                            attackCooldown1 = System.currentTimeMillis() + 400;
                        }
                    }
                    else {
                        System.out.println("entity not hit");
                    }
                    attackCooldown2 = System.currentTimeMillis() + 500;
                    server.broadcast("Anim2,4");
                }
            }
        }
        }});

        //player 1 kick
        onKeyDown(KeyCode.Q, ()-> {if (!player1Dead &&!player2Dead){if(!player2Bat.block2){
            if(!player1Bat.block1 && !player1Bat.block2){
                if (attackCooldown1 <= System.currentTimeMillis()){
                    player1Bat.stop();
                    RaycastResult Kick1 = getPhysicsWorld().raycast(player1.getCenter(), new Point2D(player1.getCenter().getX() + 70, player1.getCenter().getY()));
                    if (Kick1.getEntity().isPresent()){
                        if(Kick1.getEntity().get().getType() == EntityType.PLAYER_2){
                            inc("player2health", -7);
                            server.broadcast("SCORES," + geti("player1health") + "," + geti("player2health"));
                            server.broadcast("Anim2,6" + "|");
                            attackCooldown2 = System.currentTimeMillis() + 400;
                        }
                    }
                    else {
                        System.out.println("entity not hit|");
                    }
                    attackCooldown1 = System.currentTimeMillis() + 500;
                    server.broadcast("Anim1,5");
                }
            }
        }
        }});

        //player 2 kick
        onKeyDown(KeyCode.U, ()-> {if (!player1Dead &&!player2Dead){if(!player1Bat.block2){
            if(!player2Bat.block1 && !player2Bat.block2) {
                if (attackCooldown2 <= System.currentTimeMillis()) {
                    player2Bat.stop();
                    RaycastResult Kick2 = getPhysicsWorld().raycast(player2.getCenter(), new Point2D(player2.getCenter().getX() - 70, player2.getCenter().getY()));
                    if (Kick2.getEntity().isPresent()) {
                        if (Kick2.getEntity().get().getType() == EntityType.PLAYER_1) {
                            inc("player1health", -7);
                            server.broadcast("SCORES," + geti("player1health") + "," + geti("player2health"));
                            server.broadcast("Anim1,6" + "|");
                            attackCooldown1 = System.currentTimeMillis() + 400;
                        }
                    } else {
                        System.out.println("entity not hit");
                    }
                    attackCooldown2 = System.currentTimeMillis() + 500;
                    server.broadcast("Anim2,5");
                }
            }
        }
        }});

        onKeyDown(KeyCode.R, ()-> {if (!player1Dead &&!player2Dead){if(attackCooldown1 <= System.currentTimeMillis()){
            player1Bat.stop();
            server.broadcast("Anim1,7");
            RaycastResult Grab1 = getPhysicsWorld().raycast(player1.getCenter(), new Point2D(player1.getCenter().getX() + 70, player1.getCenter().getY()));
            if (Grab1.getEntity().isPresent()){
                if(Grab1.getEntity().get().getType() == EntityType.PLAYER_2) {
                    player2Bat.stop();
                    throwWindup1 = System.currentTimeMillis() + 500;
                    throwcheck1 = true;
                }
            }
            else {
                attackCooldown1 = System.currentTimeMillis() + 500;
            }
        }
        }});

        onKeyDown(KeyCode.P, ()-> {if (!player1Dead &&!player2Dead){if(attackCooldown2 <= System.currentTimeMillis()){
            player2Bat.stop();
            server.broadcast("Anim2,7");
            RaycastResult Grab2 = getPhysicsWorld().raycast(player2.getCenter(), new Point2D(player2.getCenter().getX() + -70, player2.getCenter().getY()));
            if (Grab2.getEntity().isPresent()){
                if(Grab2.getEntity().get().getType() == EntityType.PLAYER_1) {
                    player1Bat.stop();
                    throwWindup2 = System.currentTimeMillis() + 500;
                    throwcheck2 = true;
                }
            }
            else {
                attackCooldown2 = System.currentTimeMillis() + 500;
            }
        }
        }});

    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("player1health", 100);
        vars.put("player2health", 100);
        vars.put("player1Score", 0);
        vars.put("player2Score", 0);
    }

    @Override
    protected void initGame() {
        Writers.INSTANCE.addTCPWriter(String.class, outputStream -> new MessageWriterS(outputStream));
        Readers.INSTANCE.addTCPReader(String.class, in -> new MessageReaderS(in));

        server = getNetService().newTCPServer(55555, new ServerConfig<>(String.class));


        server.setOnConnected(connection -> {

            if (!playerConnected1){
                connection.getLocalSessionData().setValue("player", 1);
                playerConnected1 = true;
                System.out.print("player 1 connected");
            } else if (!playerConnected2) {
                connection.getLocalSessionData().setValue("player", 2);
                playerConnected2 = true;
                System.out.print("player 2 connected");
            }
            else {
                connection.getLocalSessionData().setValue("player", -1);
                spectators++;
                System.out.print("spectator added");
            }


           //Thread clientThread = new Thread(connection.getLocalSessionData().getValue("player", 1));
           //clientThread.start();
            //connection.send("ID,"+Id); if player needs to know
            connection.addMessageHandlerFX(this);
        });


        getGameWorld().addEntityFactory(new FightClub());
        getGameScene().setBackgroundColor(Color.rgb(0, 0, 5));

        initScreenBounds();
        initGameObjects();

        var t = new Thread(server.startTask()::run);
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void initPhysics() {getPhysicsWorld().setGravity(0, 100);}

    @Override
    protected void initUI() {
        MainUIController controller = new MainUIController();
        UI ui = getAssetLoader().loadUI("main.fxml", controller);

        controller.getLabelScorePlayer().textProperty().bind(getip("player1health").asString());
        controller.getLabelScoreEnemy().textProperty().bind(getip("player2health").asString());

        getGameScene().addUI(ui);
    }

    @Override
    protected void onUpdate(double tpf) {
        if (!server.getConnections().isEmpty()) {
            var message = "GAME_DATA," + player1.getY() + "," + player1.getX() + "," + player2.getY() + "," + player2.getX();
            server.broadcast(message);
        }
        if (throwcheck1){
            if (throwWindup1 <= System.currentTimeMillis()){
                if (attackCooldown1 < System.currentTimeMillis()){
                    inc("player2health", -10);
                    player2Bat.thrown(2);
                    server.broadcast("Anim1,8" + "|" + "Anim2,9" + "|" + "SCORES," + geti("player1health") + "," + geti("player2health"));
                    attackCooldown1 = System.currentTimeMillis() + 600;
                    attackCooldown2 = System.currentTimeMillis() + 600;
                }
                throwcheck1 = false;
            }
        }
        if (throwcheck2){
            if (throwWindup2 <= System.currentTimeMillis()){
                if (attackCooldown2 < System.currentTimeMillis()){
                    inc("player1health", -10);
                    player1Bat.thrown(1);
                    server.broadcast("Anim2,8" + "|" + "Anim1,9" + "|" + "SCORES," + geti("player1health") + "," + geti("player2health"));
                    attackCooldown1 = System.currentTimeMillis() + 600;
                    attackCooldown2 = System.currentTimeMillis() + 600;
                }
                throwcheck2 = false;
            }
        }
        if (geti("player1health") <= 0) {
            if (!player1Dead && !player2Dead) {
                player1Dead = true;
                player1Bat.death(1);
                inc("player1Score",+1);
                server.broadcast("Anim2,0" + "|" + "Anim1,10" + "|" + "Win," + geti("player1Score") + "," + geti("player2Score"));
                respawnTimer = System.currentTimeMillis() + 5000;
            }
        }
        if (geti("player2health") <= 0) {
            if (!player1Dead && !player2Dead) {
                player2Dead = true;
                player2Bat.death(2);
                inc("player2Score",+1);
                server.broadcast("Anim1,0" + "|" + "Anim2,10" + "|" + "Win," + geti("player1Score") + "," + geti("player2Score"));
                respawnTimer = System.currentTimeMillis() + 5000;
            }
        }
        if (player1Dead || player2Dead) {
            if (respawnTimer < System.currentTimeMillis()) {
                player1Dead = false;
                player2Dead = false;
                player1Bat.reset(1);
                player2Bat.reset(2);
                set("player1health", 100);
                set("player2health", 100);
                server.broadcast("Anim1,0" + "|" + "Anim2,0" + "|" + "SCORES," + geti("player1health") + "," + geti("player2health"));
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
        player1 = spawn("bat", new SpawnData(getAppWidth() / 4, getAppHeight() / 2 - 30).put("isPlayer", true));
        player2 = spawn("bat", new SpawnData(3 * getAppWidth() / 4 - 20, getAppHeight() / 2 - 30).put("isPlayer", false));

        player1Bat = player1.getComponent(PlayerComponent.class);
        player2Bat = player2.getComponent(PlayerComponent.class);
    }


    @Override
    public void onReceive(Connection<String> connection, String message) {
        var tokens = message.split(",");
        var splits = message.split("|");

        System.out.println(Optional.ofNullable(connection.getLocalSessionData().getInt("player")));
        int id=connection.getLocalSessionData().getInt("player");
        if(message.equals("player disconnected")){
            if(id == 1){
                playerConnected1 = false;
                System.out.println("player 1 died");
                return;}
            else if(id == 2){
                playerConnected2 = false;
                System.out.println("player 2 died");
                return;}}

        if (id == -1){
            if (!playerConnected1){
                connection.getLocalSessionData().setValue("player", 1);
                playerConnected1 = true;
            }
            else if(!playerConnected2){
                connection.getLocalSessionData().setValue("player", 2);
                playerConnected2 = true;
            }
        }
        System.out.print(System.currentTimeMillis() + "\n");

        if (id == 1) {
            Arrays.stream(tokens).skip(1).forEach(key -> {
                if (key.endsWith("_DOWN")) {
                    getInput().mockKeyPress(KeyCode.valueOf(key.substring(0, 1)));
                } else if (key.endsWith("_UP")) {
                    getInput().mockKeyRelease(KeyCode.valueOf(key.substring(0, 1)));
                }

            });
        } else if (id == 2) {
            Arrays.stream(tokens).skip(1).forEach(key -> {
                var code = KeyCode.getKeyCode(key.substring(0, 1)).name();
                var newKey = "";
                switch (code) {
                    case "A":
                        newKey = "J";
                        break;
                    case "W":
                        newKey = "I";
                        break;
                    case "S":
                        newKey = "K";
                        break;
                    case "D":
                        newKey = "L";
                        break;
                    case "E":
                        newKey = "O";
                        break;
                    case "Q":
                        newKey = "U";
                        break;
                    case "R":
                        newKey = "P";
                        break;
                }
                if (key.endsWith("_DOWN")) {
                    getInput().mockKeyPress(KeyCode.valueOf(newKey));
                } else if (key.endsWith("_UP")) {
                    getInput().mockKeyRelease(KeyCode.valueOf(newKey));
                }
            });
        }
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
