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

    private final int viewportWidth;
    private final int viewportHeight;
    private int screenWidth;
    private int screenHeight;

    private SpriteBatch foregroundBatch;
    private SpriteBatch worldBatch;
    private TextureRegion[][] sprites;

    private OrthographicCamera camera;

    // --- background
    private TiledMap tileMap;
    private Vector2 tileMapWorldSize;
    private Vector2 tileSize;
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

        Gdx.app.log(LOGTAG, "Created");
    }

    @Override
    public void render() {

        // reset last frame
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        handleInput();
        handleAI();

        // draw background / world
        camera.update();
        worldBatch.setProjectionMatrix(camera.combined);
        drawGround(camera);

        worldBatch.begin();
        drawSprites(worldBatch);
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
        Gdx.app.log(LOGTAG, "Resized: " + width + "/" + height);
    }

    @Override
    public void pause() {
        Gdx.app.log(LOGTAG, "Paused");
    }

    @Override
    public void resume() {
        Gdx.app.log(LOGTAG, "Resumed");
    }

    // ==============

    private void initActors() {
        this.sprites = getSprites();

        this.hero = new Sprite(sprites[0][10]);
        // set hero to tile 1/1
        this.hero.setX(tileSize.x*1);
        this.hero.setY(tileMapWorldSize.y-tileSize.y*(1+1));

        this.enemy = new Sprite(sprites[0][11]);
        // set enemy to tile 2/2
        this.enemy.setX(tileSize.x*2);
        this.enemy.setY(tileMapWorldSize.y-tileSize.y*(2+1));
    }

    private void initWorld() {

        // Load tile map
        // Edit the tilemap using "Tiled Map" http://www.mapeditor.org/download.html
        tileMap = new TmxMapLoader().load("world.tmx");
        tileMapWorldSize = getWorldSize(tileMap);
        tileSize = getTileSize(tileMap);
        Gdx.app.log(LOGTAG, "Tilemap size: "+ tileMapWorldSize+", tile size: "+tileSize);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportWidth, viewportHeight);

        // set camera to upper/left corner of tilemap.
        // Note: Camera's position is the _center_ of the viewport
        camera.position.x = this.viewportWidth/2;
        camera.position.y = tileMapWorldSize.y-this.viewportHeight/2;


        tileMapRenderer = new OrthogonalTiledMapRenderer(tileMap);
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

        // movement of hero
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            hero.translateX(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            hero.translateX(1);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            hero.translateY(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            hero.translateY(1);
        }


//        final int VIEWPORT_DISTANCE = 50;
//
//        if( hero.getY() > camera.position.y-VIEWPORT_DISTANCE ) {
//            camera.translate(0, camera.position.y-hero.getY());
//        }


        // check if hero has reached boundaries and whether camera should be moved as well


        // movement of background
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-1, 0);
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(1, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, 1);
        }

        debugOutput.setText("Camera:" + camera.position.x + "/" + camera.position.y + "\nHero: " + hero.getX() + "/" + hero.getY());
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

    private void drawGround(OrthographicCamera camera) {
        tileMapRenderer.setView(camera);
        tileMapRenderer.render();
    }

    private void drawSprites(SpriteBatch batch) {

        hero.draw(batch);
        enemy.draw(batch);
    }

    private void drawForeground(SpriteBatch batch) {
        message.draw(batch, 0.5f);
        debugOutput.draw(batch, 0.5f);
    }

    private TextureRegion[][] getSprites() {
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

}
