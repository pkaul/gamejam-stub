package de.girino.gamejam;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * GameJam Game
 */
public class Game extends ApplicationAdapter {

	private final int SPRITESHEET_ROWS = 16;
	private final int SPRITESHEET_COLUMNS = 16;

	private SpriteBatch batch;
	private TextureRegion[][] sprites;
	
	@Override
	public void create () {
		this.batch = new SpriteBatch();
		this.sprites = getSprites();
	}

	@Override
	public void render () {

		// reset last frame
		Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// draw all available sprites
		for( int y=0; y<sprites.length; y++ ) {

			for( int x=0; x<sprites[y].length; x++ ) {

				TextureRegion sprite = sprites[y][x];

				batch.begin();
				batch.draw(sprite, x*32, y*32);
				batch.end();
			}
		}
	}

	// ==============

	private TextureRegion[][] getSprites() {
		Texture spriteSheet =  new Texture(Gdx.files.internal("spritesheet_example.png"));
  		return  TextureRegion.split(spriteSheet, spriteSheet.getWidth()/SPRITESHEET_COLUMNS, spriteSheet.getHeight()/SPRITESHEET_ROWS);
	}
}
