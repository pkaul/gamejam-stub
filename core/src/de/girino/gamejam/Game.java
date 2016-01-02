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
    private Vector2 worldSize;
    // size of a world's tile in pixels
    private Vector2 worldTileSize;
    private TiledMapRenderer tileMapRenderer;

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
        // set hero to tile 1/1
        this.hero.setX(worldTileSize.x*1);
        this.hero.setY(worldSize.y- worldTileSize.y*(1+1));
        moveHero(0,0); // init position in the world

        // enemy is composed from sprite 0/11
        this.enemy = new Sprite(sprites[0][11]);
        // set enemy to tile 2/2
        this.enemy.setX(worldTileSize.x*2);
        this.enemy.setY(worldSize.y- worldTileSize.y*(2+1));
    }

    private void initWorld() {

        // Load tile map
        // Edit the tilemap using "Tiled Map" http://www.mapeditor.org/download.html
        worldTilemap = new TmxMapLoader().load("world.tmx");
        worldSize = getWorldSize(worldTilemap);
        worldTileSize = getTileSize(worldTilemap);
        log("Tilemap size: " + worldSize + ", tile size: " + worldTileSize);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        setViewportWorldPosition(0, 0);

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
            moveHero(heroXOffset, heroYOffset);
        }


        debugOutput.setText("Hero: " + getWorldPosition(hero));
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
    private void moveHero(int xoffset, int yoffset) {

        // check if hero is allowed to move to this position (e.g. something in his way?)
        Vector2 heroWorldPosition = getWorldPosition(hero);
        getTileAt(heroWorldPosition.x+xoffset, heroWorldPosition.y+yoffset);


        // move hero
        hero.translate(xoffset, yoffset);

        // check if hero has reached viewport's border. if so: scroll background
        int viewPortXOffset = 0;
        int viewPortYOffset = 0;
        final int VIEWPORT_MIN_DISTANCE = 64;

        Vector2 heroViewPortPosition = getViewportPosition(hero);
        if( heroViewPortPosition.x < VIEWPORT_MIN_DISTANCE ) {
            viewPortXOffset -= (VIEWPORT_MIN_DISTANCE-heroViewPortPosition.x);
        }
        else if( heroViewPortPosition.x > this.viewportWidth-VIEWPORT_MIN_DISTANCE) {
            viewPortXOffset += (heroViewPortPosition.x-this.viewportWidth+VIEWPORT_MIN_DISTANCE);
        }

        if( heroViewPortPosition.y < VIEWPORT_MIN_DISTANCE ) {
            viewPortYOffset += (VIEWPORT_MIN_DISTANCE-heroViewPortPosition.y);
        }
        else if( heroViewPortPosition.y > this.viewportHeight-VIEWPORT_MIN_DISTANCE) {
            viewPortYOffset -= (heroViewPortPosition.y-this.viewportHeight+VIEWPORT_MIN_DISTANCE);
        }

        if (viewPortXOffset != 0 || viewPortYOffset != 0 ) {
            // scroll background
            camera.translate(viewPortXOffset, viewPortYOffset);
        }
    }

    /**
     * Sets position of the viewport in relation to upper/left corner of screen/world
     */
    private void setViewportWorldPosition(int x, int y) {

        // set camera to upper/left corner of tilemap.
        // Note: Camera's position is the _center_ of the viewport
        this.camera.position.x = x+this.viewportWidth/2;
        this.camera.position.y = this.worldSize.y-y-this.viewportHeight/2;
    }

    /**
     * @return Position of the viewport in relation to upper/left corner of screen/world
     */
    private Vector2 getViewportWorldPosition() {
        return new Vector2(this.camera.position.x - this.viewportWidth / 2, this.worldSize.y - this.viewportHeight / 2 - this.camera.position.y);
    }

    /**
     * Sprite's position on the viewport
     */
    private Vector2 getViewportPosition(Sprite sprite) {
        Vector2 viewPortPosition = getViewportWorldPosition();
        return new Vector2(sprite.getX() - viewPortPosition.x, worldSize.y - sprite.getY() - sprite.getHeight() - viewPortPosition.y );
    }

    /**
     * Sprite's position in relation to world's upper/left corner
     */
    private Vector2 getWorldPosition(Sprite sprite) {
       return new Vector2(sprite.getX(), this.worldSize.y-sprite.getY());
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

    private static Vector2 getWorldSize(TiledMap tiledMap) {

        MapProperties properties = tiledMap.getProperties();

        int mapWidth = properties.get("width", Integer.class);
        int mapHeight = properties.get("height", Integer.class);
        int tilePixelWidth = properties.get("tilewidth", Integer.class);
        int tilePixelHeight = properties.get("tileheight", Integer.class);

        return new Vector2(mapWidth*tilePixelWidth, mapHeight*tilePixelHeight);
    }

    private static Vector2 getTileSize(TiledMap tiledMap) {

        MapProperties properties = tiledMap.getProperties();

        int tilePixelWidth = properties.get("tilewidth", Integer.class);
        int tilePixelHeight = properties.get("tileheight", Integer.class);

        return new Vector2(tilePixelWidth, tilePixelHeight);
    }

    /**
     * The tile at a given position
     * TODO finish implementation
     */
    private Object getTileAt(float x, float y) {


        TiledMapTileLayer roadblocks = (TiledMapTileLayer) worldTilemap.getLayers().get(0); // assumption: tilemap's first layer are the roadblocks
        int tileXpos = Math.round(x / roadblocks.getTileWidth());
        int tileYpos = Math.round(y / roadblocks.getHeight());

//        if (tileXpos < 0 || tileXpos >= roadblocks.getWidth()) {
//            // out of bounds
//            return null;
//        }
//        if (tileYpos < 0 || tileYpos >= roadblocks.getHeight()) {
//            // out of bounds
//            return null;
//        }

        TiledMapTileLayer.Cell cell = roadblocks.getCell(tileXpos, tileYpos);
        if( cell != null ) {

            log("Cell "+cell);
        }

        return cell;
    }

    private void log(String message) {
        Gdx.app.log(LOGTAG, message);
    }

}
