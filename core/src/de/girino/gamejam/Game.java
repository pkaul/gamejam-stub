package de.girino.gamejam;

import apple.laf.JRSUIConstants;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;


/**
 * GameJam Game
 */
public class Game extends ApplicationAdapter {



    private static final String LOGTAG = "GameJam";

    // --- graphics

    private static final int SPRITESHEET_ROWS = 16;
    private static final int SPRITESHEET_COLUMNS = 16;

    // size the entire screen/world in pixels
    private int screenWidth;
    private int screenHeight;

    // size of the viewport (fragment of the screen/world)
    private final int viewportWidth;
    private final int viewportHeight;

    private SpriteBatch foregroundBatch;
    private SpriteBatch worldBatch;

    private OrthographicCamera camera;

    // --- background
    private TiledMap worldTilemap;
    // size of world in pixels
    private int worldWidth;
    private int worldHeight;
    // size of a world's tile in pixels
    private int tileWidth;
    private int tileHeight;
    private TiledMapRenderer tileMapRenderer;

    // --- characters
    private TiledMapTileSet charactersTileSet;

    // The obstacles in the world
    private TiledMapTileLayer worldObstacleLayer;

    private TiledMapTileLayer constructionLayer;
    private TiledMapTileLayer.Cell trackRightBottom;
    private TiledMapTileLayer.Cell  trackLeftBottom;
    private TiledMapTileLayer.Cell  trackRightTop;
    private TiledMapTileLayer.Cell  trackLeftTop;
    private TiledMapTileLayer.Cell  trackLeftRight;
    private TiledMapTileLayer.Cell  trackTopBottom;
    private TiledMapTileLayer.Cell  trackRight;
    private TiledMapTileLayer.Cell  trackLeft;
    private TiledMapTileLayer.Cell  trackBottom;
    private TiledMapTileLayer.Cell  trackTop;



    // --- text
    private Skin skin;
    private Label message;
    private TextArea debugOutput;

    // --- actors
    private Player player1;
    private Player player2;


    private long tickCount = 0;

    public Game(int viewportWidth, int viewportHeight) {

        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }


    @Override
    public void create() {

        this.worldBatch = new SpriteBatch();
        this.foregroundBatch = new SpriteBatch();

        initWorld();
        initActors();
        initUi();

        log("Created");
    }

    @Override
    public void render() {

        tickCount++;

        // reset last frame
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // processes user input
        handleInput();

        // processes actor AI
        handleAI();

        // draw background / world
        camera.update();
        worldBatch.setProjectionMatrix(camera.combined);
        drawGround(camera);

        // draw actors
        worldBatch.begin();
        drawActors(worldBatch);
        worldBatch.end();

        // draw foreground / text
        foregroundBatch.begin();
        drawForeground(foregroundBatch);
        foregroundBatch.end();

    }

    @Override
    public void resize(int width, int height) {
        this.screenHeight = height;
        this.screenWidth = width;
        log("Resized: " + width + "/" + height);
    }

    @Override
    public void pause() {
        log("Paused");
    }

    @Override
    public void resume() {
        log("Resumed");
    }

    // ==============

    private void initActors() {

        int firstGid = charactersTileSet.getProperties().get("firstgid", Integer.class);
        int tilesWidth = charactersTileSet.getProperties().get("imagewidth", Integer.class) / (charactersTileSet.getProperties().get("tilewidth", Integer.class)+charactersTileSet.getProperties().get("spacing", Integer.class));

        // player1
        this.player1 = new Player();
        this.player1.sprite = new Sprite(charactersTileSet.getTile(firstGid+5*tilesWidth+1).getTextureRegion());
        this.player1.sprite.setX(this.tileWidth * 1);
        this.player1.sprite.setY(this.worldHeight - this.tileHeight * (1 + 1));
        this.player1.direction = Player.DIRECTION_RIGHT;

        // player2
        this.player2 = new Player();
        this.player2.sprite = new Sprite(charactersTileSet.getTile(firstGid+6*tilesWidth+1).getTextureRegion());
        this.player2.sprite.setX(this.tileWidth * 2);
        this.player2.sprite.setY(this.worldHeight - this.tileHeight * (2 + 1));
        this.player2.direction = Player.DIRECTION_LEFT;

        movePlayer(this.player1, 0, 0, this.player2); // init position in the world
    }

    private void initWorld() {

        // Load tile map
        // Edit the tilemap using "Tiled Map" http://www.mapeditor.org/download.html
        worldTilemap = new TmxMapLoader().load("world2.tmx");

        MapProperties tileMapProperties = worldTilemap.getProperties();
        this.tileWidth = tileMapProperties.get("tilewidth", Integer.class);
        this.tileHeight = tileMapProperties.get("tileheight", Integer.class);
        this.worldWidth = this.tileWidth * tileMapProperties.get("width", Integer.class);
        this.worldHeight = this.tileHeight * tileMapProperties.get("height", Integer.class);

        worldObstacleLayer = (TiledMapTileLayer) worldTilemap.getLayers().get("obstacles"); // fetch named layer
        constructionLayer = (TiledMapTileLayer) worldTilemap.getLayers().get("construction");


        TiledMapTileLayer utilLayer = (TiledMapTileLayer) worldTilemap.getLayers().get("util");
        trackRightBottom =  utilLayer.getCell(0, utilLayer.getHeight()-1);
        trackLeftBottom =  utilLayer.getCell(1, utilLayer.getHeight()-1);
        trackRightTop =  utilLayer.getCell(2, utilLayer.getHeight()-1);
        trackLeftTop =  utilLayer.getCell(3, utilLayer.getHeight()-1);
        trackLeftRight =  utilLayer.getCell(4, utilLayer.getHeight() - 1);
        trackTopBottom =  utilLayer.getCell(5, utilLayer.getHeight() - 1);
        trackRight =  utilLayer.getCell(6, utilLayer.getHeight() - 1);
        trackLeft =  utilLayer.getCell(7, utilLayer.getHeight() - 1);
        trackBottom =  utilLayer.getCell(8, utilLayer.getHeight() - 1);
        trackTop =  utilLayer.getCell(9, utilLayer.getHeight() - 1);


        charactersTileSet = worldTilemap.getTileSets().getTileSet("characters");


        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        // set camera to the upper/left corner of the world
        camera.position.x = viewportWidth/2;
        camera.position.y = worldHeight-viewportHeight/2;

        tileMapRenderer = new OrthogonalTiledMapRenderer(worldTilemap);
        tileMapRenderer.setView(camera);
    }

    private void initUi() {
        this.skin = new Skin(Gdx.files.internal("uiskin.json"));
        this.message = new Label("", this.skin);
        this.message.setX(0);
        this.message.setY(viewportHeight - 20);

        this.debugOutput = new TextArea("", this.skin);
        this.debugOutput.setX(0);
        this.debugOutput.setY(50);
    }

    // --------------------

    private void handleInput() {



        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            player1.direction = Player.DIRECTION_LEFT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            player1.direction = Player.DIRECTION_RIGHT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            player1.direction = Player.DIRECTION_DOWN;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            player1.direction = Player.DIRECTION_UP;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            player2.direction = Player.DIRECTION_LEFT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            player2.direction = Player.DIRECTION_RIGHT;
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            player2.direction = Player.DIRECTION_DOWN;
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            player2.direction = Player.DIRECTION_UP;
        }


            /*
            if( Gdx.input.isKeyPressed(Input.Keys.SPACE) ) {

                int tileXpos = (int) Math.floor(player1.getX() / constructionLayer.getTileWidth());
                int tileYpos = (int) Math.floor(player1.getY() / constructionLayer.getTileHeight());



                constructionLayer.setCell(tileXpos, tileYpos, trackLeftRight);
            }
            */



        movePlayer(player1, player2);
        movePlayer(player2, player1);




        //debugOutput.setText("Hero: " + player1.getX() + "/" + player1.getY());
        debugOutput.invalidate();
    }

    private void handleAI() {

        /*
        if (player2.getBoundingRectangle().overlaps(player1.getBoundingRectangle())) {
            this.message.setText("Ooops!");
        } else {
            this.message.setText("");
        }
        */
    }


    // -----------------------

    private void movePlayer(Player player, Player other) {

        if( tickCount % 4 == 0 ) {

            final int HERO_SPEED = 16;
            switch (player.direction) {
                case Player.DIRECTION_RIGHT:
                    movePlayer(player, HERO_SPEED, 0, other);
                    break;
                case Player.DIRECTION_LEFT:
                    movePlayer(player, -HERO_SPEED, 0, other);
                    break;
                case Player.DIRECTION_DOWN:
                    movePlayer(player, 0, -HERO_SPEED, other);
                    break;
                case Player.DIRECTION_UP:
                    movePlayer(player, 0, HERO_SPEED, other);
                    break;
            }
        }

    }

    /**
     * Moves the player relative to the current position
     */
    private void movePlayer(Player player, int xoffset, int yoffset, Player other) {

        // check if player1 is allowed to move to this position (e.g. something in his way?)
        float targetPosX = player.sprite.getX() + xoffset + (xoffset > 0 ? player.sprite.getWidth() : 0);
        float targetPosY = player.sprite.getY() + yoffset + (yoffset > 0 ? player.sprite.getHeight() : 0);
        TiledMapTileLayer.Cell targetPosObstacle = getObstacleElementAt(targetPosX, targetPosY);
        if( targetPosObstacle != null ) {
            // there is an obstacle at the target position.
            //log("Obstacle at " + targetPosX + "/" + targetPosY + " is: " + targetPosObstacle.getTile().getId());
            player.reverseDirection();
            return;
        }


        int[] player1ScrollOffset = scrollViewPortForActorOffset(Math.round(player.sprite.getX() + xoffset), Math.round(player.sprite.getY() + yoffset), 0, 0);
        int[] otherPlayerScrollOffset = scrollViewPortForActorOffset(Math.round(other.sprite.getX()), Math.round(other.sprite.getY()), player1ScrollOffset[0], player1ScrollOffset[1]);



        if( otherPlayerScrollOffset[0] != 0 || otherPlayerScrollOffset[1] != 0 ) {
            player.reverseDirection();
            return;
        }




        // do  movement
        player.sprite.translate(xoffset, yoffset);
        if (player1ScrollOffset[0] != 0 || player1ScrollOffset[1] != 0 ) {
            // do background scrolling
            camera.translate(player1ScrollOffset[0], player1ScrollOffset[1]);
        }
    }

    private int[] scrollViewPortForActorOffset(int actorX, int actorY, int cameraOffsetX, int cameraOffsetY) {


        final int VIEWPORT_MIN_DISTANCE = 64;

        // position of the viewport in the world
        float viewportXPos = this.camera.position.x - this.viewportWidth / 2 + cameraOffsetX;
        float viewportYPos = this.camera.position.y - this.viewportHeight / 2 + cameraOffsetY;

        // future position of player in the viewport
        int heroViewportXPos = Math.round(actorX - viewportXPos);
        int heroViewportYPos = Math.round(actorY - viewportYPos);


        int viewPortXOffset = 0;
        int viewPortYOffset = 0;
        if( heroViewportXPos < VIEWPORT_MIN_DISTANCE ) {
            viewPortXOffset -= (VIEWPORT_MIN_DISTANCE-heroViewportXPos);
        }
        else if( heroViewportXPos > this.viewportWidth-VIEWPORT_MIN_DISTANCE) {
            viewPortXOffset += (heroViewportXPos-this.viewportWidth+VIEWPORT_MIN_DISTANCE);
        }

        if( heroViewportYPos < VIEWPORT_MIN_DISTANCE ) {
            viewPortYOffset -= (VIEWPORT_MIN_DISTANCE-heroViewportYPos);
        }
        else if( heroViewportYPos > this.viewportHeight-VIEWPORT_MIN_DISTANCE) {
            viewPortYOffset += (heroViewportYPos-this.viewportHeight+VIEWPORT_MIN_DISTANCE);
        }

        return new int[] {viewPortXOffset, viewPortYOffset};

    }


    private void drawGround(OrthographicCamera camera) {
        tileMapRenderer.setView(camera);
        tileMapRenderer.render();
    }

    private void drawActors(SpriteBatch batch) {

        player1.sprite.draw(batch);
        player2.sprite.draw(batch);
    }

    private void drawForeground(SpriteBatch batch) {
        message.draw(batch, 0.5f);
        debugOutput.draw(batch, 0.5f);
    }

    /**
     * obstacle tile at a given position or null if no roadblock
     */
    private TiledMapTileLayer.Cell getObstacleElementAt(float x, float y) {

        int tileXpos = (int) Math.floor(x / worldObstacleLayer.getTileWidth());
        int tileYpos = (int) Math.floor(y / worldObstacleLayer.getTileHeight());

        return worldObstacleLayer.getCell(tileXpos, tileYpos);
    }

    private void log(String message) {
        Gdx.app.log(LOGTAG, message);
    }

}
