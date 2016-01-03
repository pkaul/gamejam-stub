package de.girino.gamejam;

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
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
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
    private TextureRegion[][] sprites;

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

    // The obstacles in the world
    private TiledMapTileLayer worldObstacleLayer;


    // --- text
    private Skin skin;
    private Label message;
    private TextArea debugOutput;

    // --- entities
    private Sprite hero;
    private Sprite enemy;

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
        this.sprites = loadSprites();

        // hero is composed from sprite 0/10
        this.hero = new Sprite(sprites[0][10]);
        // set hero to world's upper/left corner
        this.hero.setX(this.tileWidth*1);
        this.hero.setY(this.worldHeight - this.tileHeight*(1+1));
        moveHero(this.hero, 0,0); // init position in the world

        // enemy is composed from sprite 0/11
        this.enemy = new Sprite(sprites[0][11]);
        // set enemy to tile 2/2
        this.enemy.setX(this.tileWidth*2);
        this.enemy.setY(this.worldHeight - this.tileHeight*(2+1));
    }

    private void initWorld() {

        // Load tile map
        // Edit the tilemap using "Tiled Map" http://www.mapeditor.org/download.html
        worldTilemap = new TmxMapLoader().load("world.tmx");

        MapProperties tileMapProperties = worldTilemap.getProperties();
        this.tileWidth = tileMapProperties.get("tilewidth", Integer.class);
        this.tileHeight = tileMapProperties.get("tileheight", Integer.class);
        this.worldWidth = this.tileWidth * tileMapProperties.get("width", Integer.class);
        this.worldHeight = this.tileHeight * tileMapProperties.get("height", Integer.class);

        worldObstacleLayer = (TiledMapTileLayer) worldTilemap.getLayers().get(0); // assumption: tilemap's first layer contains the obstacles

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

        // --- move hero
        final int HERO_SPEED = 2;
        int heroXOffset = 0;
        int heroYOffset = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            heroXOffset = -HERO_SPEED;
            hero.translateX(-HERO_SPEED);
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            heroXOffset = HERO_SPEED;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            heroYOffset = -HERO_SPEED;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            heroYOffset = HERO_SPEED;
        }

        if( heroXOffset != 0 || heroYOffset != 0 ) {
            moveHero(hero, heroXOffset, heroYOffset);
        }


        debugOutput.setText("Hero: " + hero.getX()+"/"+hero.getY());
        debugOutput.invalidate();
    }

    private void handleAI() {

        if (enemy.getBoundingRectangle().overlaps(hero.getBoundingRectangle())) {
            this.message.setText("Ooops!");
        } else {
            this.message.setText("");
        }
    }


    // -----------------------


    /**
     * Moves the hero relative to the current position
     */
    private void moveHero(Sprite hero, int xoffset, int yoffset) {

        // check if hero is allowed to move to this position (e.g. something in his way?)
        float targetPosX = hero.getX() + xoffset + (xoffset > 0 ? hero.getWidth() : 0);
        float targetPosY = hero.getY() + yoffset + (yoffset > 0 ? hero.getHeight() : 0);
        TiledMapTileLayer.Cell targetPosObstacle = getObstacleElementAt(targetPosX, targetPosY);
        if( targetPosObstacle != null ) {
            // there is an obstacle at the target position. don't move



            log("Obstacle at " + targetPosX + "/" + targetPosY + " is: " + targetPosObstacle.getTile().getId());
            //return;
        }

        // do hero movement
        hero.translate(xoffset, yoffset);

        // check if hero has reached viewport's border. if so: scroll background
        int viewPortXOffset = 0;
        int viewPortYOffset = 0;
        final int VIEWPORT_MIN_DISTANCE = 64;

        // position of the viewport in the world
        int viewportXPos = Math.round(this.camera.position.x - this.viewportWidth / 2);
        int viewportYPos = Math.round(this.camera.position.y - this.viewportHeight / 2);

        // position of hero in the viewport
        int heroViewportXPos = Math.round(hero.getX() - viewportXPos);
        int heroViewportYPos = Math.round(hero.getY() - viewportYPos);

//        Vector2 viewPortPosition = getViewportWorldPosition();
//        return new Vector2(sprite.getX() - viewPortPosition.x, this.worldHeight - sprite.getY() - sprite.getHeight() - viewPortPosition.y );
//


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

        if (viewPortXOffset != 0 || viewPortYOffset != 0 ) {
            // scroll background
            camera.translate(viewPortXOffset, viewPortYOffset);
        }
    }


    private void drawGround(OrthographicCamera camera) {
        tileMapRenderer.setView(camera);
        tileMapRenderer.render();
    }

    private void drawActors(SpriteBatch batch) {

        hero.draw(batch);
        enemy.draw(batch);
    }

    private void drawForeground(SpriteBatch batch) {
        message.draw(batch, 0.5f);
        debugOutput.draw(batch, 0.5f);
    }

    private TextureRegion[][] loadSprites() {
        Texture spriteSheet = new Texture(Gdx.files.internal("spritesheet_example.png"));
        return TextureRegion.split(spriteSheet, spriteSheet.getWidth() / SPRITESHEET_COLUMNS, spriteSheet.getHeight() / SPRITESHEET_ROWS);
    }
    /**
     * obstacle tile at a given position or null if no roadblock
     */
    private TiledMapTileLayer.Cell getObstacleElementAt(float x, float y) {

        int tileXpos = Math.round(x / worldObstacleLayer.getTileWidth()) - 1;
        int tileYpos = worldObstacleLayer.getWidth() - Math.round(y / worldObstacleLayer.getTileHeight()) - 1;

        return worldObstacleLayer.getCell(tileXpos, tileYpos);
    }

    private void log(String message) {
        Gdx.app.log(LOGTAG, message);
    }

}
