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
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
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

	private final int viewportWidth;
	private final int viewportHeight;

	private SpriteBatch batch;
	private TextureRegion[][] sprites;

	private OrthographicCamera camera;

    // --- background
    private TiledMap tileMap;
	private TiledMapRenderer tileMapRenderer;

    // --- text
    private Skin skin;
    private Label message;
    private TextArea debugOutput;

	// --- entities
	private Sprite hero;
	private Sprite enemy;
	private Label message;
	private TiledMap tileMap;

	public Game(int viewportWidth, int viewportHeight) {

		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
	}

	
	@Override
	public void create () {

		this.batch = new SpriteBatch();
		this.sprites = getSprites();

		this.hero = new Sprite(sprites[0][10]);
		this.hero.setX(100);
		this.hero.setY(100);

		this.enemy = new Sprite(sprites[0][11]);
		this.enemy.setX(50);
		this.enemy.setY(50);

		initWorld();
		initUi();

		Gdx.app.log(LOGTAG, "Created");
	}

	@Override
	public void render () {

		// reset last frame
		Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        //Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		handleInput();
		handleAI();

        camera.update();
        batch.setProjectionMatrix(camera.combined);
		drawGround(camera);

        batch.begin();
		drawSprites(batch);
		drawForeground(batch);
		batch.end();

	}

	@Override
	public void resize(int width, int height) {
		Gdx.app.log(LOGTAG, "Resized: "+width+"/"+height);
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

	private void initWorld() {

		camera = new OrthographicCamera();
		camera.setToOrtho(false, viewportWidth, viewportHeight);

		// Edit the world with "Tiled Map" http://www.mapeditor.org/download.html
		tileMap = new TmxMapLoader().load("world.tmx");
		tileMapRenderer = new OrthogonalTiledMapRenderer(tileMap);
		tileMapRenderer.setView(camera);
	}

	private void initUi() {
		this.skin = new Skin(Gdx.files.internal("uiskin.json"));
		this.message = new Label("", this.skin);
        this.message.setX(0);
        this.message.setY(viewportHeight-20);

        this.debugOutput = new TextArea("", this.skin);
        this.debugOutput.setX(viewportWidth-this.debugOutput.getWidth());
        this.debugOutput.setY(50);
	}

    // --------------------

	private void handleInput() {

        // movement of hero
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			hero.translateX(-1);
		}
		else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			hero.translateX(1);
		}

		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			hero.translateY(-1);
		}
		else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			hero.translateY(1);
		}

        // movement of background
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-1, 0);
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(1, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -1);
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, 1);
        }

       debugOutput.setText("Camera:"+camera.position.x+"/"+camera.position.y);

    }

	private void handleAI() {

		if( enemy.getBoundingRectangle().overlaps(hero.getBoundingRectangle()) ) {
			this.message.setText("Ooops!");
		}
		else {
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
		Texture spriteSheet =  new Texture(Gdx.files.internal("spritesheet_example.png"));
  		return  TextureRegion.split(spriteSheet, spriteSheet.getWidth()/SPRITESHEET_COLUMNS, spriteSheet.getHeight()/SPRITESHEET_ROWS);
	}
}
