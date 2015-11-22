package de.girino.gamejam;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * GameJam Game
 */
public class Game extends ApplicationAdapter {

	private static final String LOGTAG = "GameJam";

	private final int SPRITESHEET_ROWS = 16;
	private final int SPRITESHEET_COLUMNS = 16;

	private SpriteBatch batch;
	private TextureRegion[][] sprites;
	private Sprite hero;
	private Sprite enemy;
	
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

		Gdx.app.log(LOGTAG, "Created");
	}

	@Override
	public void render () {

		// reset last frame
		Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// draw all available sprites
//		for( int y=0; y<sprites.length; y++ ) {
//
//			for( int x=0; x<sprites[y].length; x++ ) {
//
//				TextureRegion sprite = sprites[y][x];
//
//				batch.begin();
//				batch.draw(sprite, x*32, y*32);
//				batch.end();
//			}
//		}

		handleInput();

		batch.begin();
		drawSprites(batch);
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

	private void handleInput() {

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
	}

	private void drawSprites(SpriteBatch batch) {
		hero.draw(batch);
		enemy.draw(batch);
	}


	private TextureRegion[][] getSprites() {
		Texture spriteSheet =  new Texture(Gdx.files.internal("spritesheet_example.png"));
  		return  TextureRegion.split(spriteSheet, spriteSheet.getWidth()/SPRITESHEET_COLUMNS, spriteSheet.getHeight()/SPRITESHEET_ROWS);
	}
}
