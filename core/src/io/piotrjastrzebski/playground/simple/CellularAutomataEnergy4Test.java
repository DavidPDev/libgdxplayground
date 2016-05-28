package io.piotrjastrzebski.playground.simple;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.kotcrab.vis.ui.VisUI;
import io.piotrjastrzebski.playground.BaseScreen;
import io.piotrjastrzebski.playground.GameReset;
import io.piotrjastrzebski.playground.PlaygroundGame;

/**
 * Created by EvilEntity on 25/01/2016.
 */
@SuppressWarnings("Duplicates")
public class CellularAutomataEnergy4Test extends BaseScreen {
	BitmapFont font;
	GlyphLayout glyphs;

	public final static int WIDTH = (int)VP_WIDTH;
	public final static int HEIGHT = (int)VP_HEIGHT;
	public final static int EMPTY = 0;
	public final static int BLOCK = 1;
	public final static int SOURCE = 2;
	public final static int DRAIN = 3;
	public final static int NO_GROUP = -1;
	public final static float MIN_VALUE = 0.001f;
	public final static float MAX_VALUE = 1;
	public final static float MIN_FLOW = 0.01f;
	public final static float MAX_SPEED = 1;
	public final static float MAX_COMPRESS = 0.02f;
	public final static float MIN_DRAW_VALUE = 0.01f;
	public final static float MAX_DRAW_VALUE = 1.1f;
	public final static float TYPE_TO_VALUE[] = {0, 0, MAX_VALUE, 0};
//	public final static float TYPE_TO_EXTRA[] = {0, -.01f, MAX_VALUE, -MAX_VALUE/3f};
	public final static float TYPE_TO_EXTRA[] = {0, 0, MAX_VALUE, -MAX_VALUE/3};
//	public final static float TYPE_TO_EXTRA[] = {0, 0, MAX_VALUE, -MAX_VALUE/3f};

	public static int[] types;
	public static int[] ticks;
	public static int[] groups;
	public static float[] values;
	public static float[] nextValues;
	public static float[] extraValues;
	private boolean drawText;
	private boolean drawGroupId;
	private boolean simEnabled = true;

	public CellularAutomataEnergy4Test (GameReset game) {
		super(game);
		BitmapFont visFont = VisUI.getSkin().getFont("small-font");
		font = new BitmapFont(new BitmapFont.BitmapFontData(visFont.getData().fontFile, false), visFont.getRegions(), false);
		font.setUseIntegerPositions(false);
		font.getData().setScale(INV_SCALE);
		glyphs = new GlyphLayout();
		clear.set(Color.GRAY);
		types = new int[WIDTH * HEIGHT];
		ticks = new int[WIDTH * HEIGHT];
		groups = new int[WIDTH * HEIGHT];
		values = new float[WIDTH * HEIGHT];
		nextValues = new float[WIDTH * HEIGHT];
		extraValues = new float[WIDTH * HEIGHT];
		for (int x = 10; x <= 20; x++) {
			setTile(x + 5 * WIDTH, BLOCK);
			setTile(x + 10 * WIDTH, BLOCK);
			setTile(x + 15 * WIDTH, BLOCK);
		}
		for (int y = 5; y <= 15; y++) {
			setTile(10 + y * WIDTH, BLOCK);
			setTile(15 + y * WIDTH, BLOCK);
			setTile(20 + y * WIDTH, BLOCK);
		}

		for (int x = 3; x <= 37; x++) {
			if (x % 2 == 0) {
				setTile(x + 18 * WIDTH, BLOCK);
				setTile(x + 20 * WIDTH, BLOCK);
			}
			setTile(x + 19 * WIDTH, BLOCK);
		}

//		for (int i = 0; i < WIDTH * HEIGHT / 2; i++) {
//			setTile(MathUtils.random(WIDTH -1) + MathUtils.random(HEIGHT -1) * WIDTH, BLOCK);
//		}
	}

	void setTile (int index, int type) {
		types[index] = type;
		values[index] = TYPE_TO_VALUE[type];
		nextValues[index] = TYPE_TO_VALUE[type];
		extraValues[index] = TYPE_TO_EXTRA[type];
	}

	Color water = new Color(Color.WHITE);
	Color tmpColor1 = new Color(Color.WHITE);
	Color tmpColor2 = new Color(Color.WHITE);
	Color tmpColor3 = new Color(Color.WHITE);
	int tick;
	float tickTime;
	float tickDelta = 1/60f;
//	float tickDelta = 1/10f;
	@Override public void render (float delta) {
		super.render(delta);
		// handle input
		int mx = (int)cs.x;
		int my = (int)cs.y;
		if (mx >= 0 && mx < WIDTH && my >= 0 && my < HEIGHT) {
			int index = mx + my * WIDTH;
			if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
				setTile(index, BLOCK);
			} else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
				setTile(index, SOURCE);
			} else if (Gdx.input.isKeyPressed(Input.Keys.E)) {
				setTile(index, DRAIN);
			} else if (Gdx.input.isKeyPressed(Input.Keys.R)) {
				setTile(index, EMPTY);
			}
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
			drawText = !drawText;
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
			drawGroupId = !drawGroupId;
		}
		// simulate
		tickTime += delta;
		if (tickTime >= tickDelta && simEnabled) {
			tickTime -= tickDelta;
			tick();
		}
		// draw stuff
		renderer.setProjectionMatrix(gameCamera.combined);
		renderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int index = x + y * WIDTH;
				switch (types[index]) {
				case SOURCE:
				case DRAIN:
				case BLOCK: {

					for (int ox = -1; ox <= 1; ox++) {
						for (int oy = -1; oy <= 1; oy++) {
							if (Math.abs(ox) == Math.abs(oy))
								continue;
							if (x + ox < 0 || x + ox >= WIDTH || y + oy < 0 || y + oy >= HEIGHT)
								continue;
							int otherId = (x + ox) + (y + oy) * WIDTH;
							if (types[otherId] != EMPTY) {
								rect(x + .5f, y + .5f, x + .5f + ox / 2f, y + .5f + oy / 2f, .35f, Color.DARK_GRAY,
									Color.DARK_GRAY);
							}
						}
					}
					tmpColor3.set(Color.DARK_GRAY);
					if (types[index] == SOURCE) {
						tmpColor3.set(Color.GREEN);
						renderer.setColor(tmpColor3);
						renderer.circle(x + .5f, y + .5f, .5f, 8);
					} else if (types[index] == DRAIN) {
						tmpColor3.set(Color.MAGENTA);
						renderer.setColor(tmpColor3);
						renderer.circle(x + .5f, y + .5f, .5f, 8);
					} else {
						renderer.setColor(tmpColor3);
						renderer.circle(x + .5f, y + .5f, .25f, 16);
					}
					float value = values[index];
					if (value > MIN_DRAW_VALUE) {
						getWaterColor(value, water);
						for (int ox = -1; ox <= 1; ox++) {
							for (int oy = -1; oy <= 1; oy++) {
								if (Math.abs(ox) == Math.abs(oy))
									continue;
								if (x + ox < 0 || x + ox >= WIDTH || y + oy < 0 || y + oy >= HEIGHT)
									continue;
								int otherId = (x + ox) + (y + oy) * WIDTH;
								if (types[otherId] != EMPTY) {

									float otherValue = values[otherId];
									getWaterColor(otherValue, tmpColor1);
									tmpColor2.set(water);
									tmpColor2.lerp(tmpColor1, .5f);
									rect(x + .5f, y + .5f, x + .5f + ox / 2f, y + .5f + oy / 2f, .16f, water, tmpColor2);
								}
							}
						}
						renderer.setColor(getWaterColor(value, water));
//						renderer.rect(x + .35f, y + .35f, .3f, .3f);
						renderer.circle(x + .5f, y + .5f, .15f, 16);
					}
					if (types[index] == DRAIN) {
						value = -value * 1/extraValues[index];
						renderer.setColor(1-value, value, 0, 1);
						renderer.rect(x, y, 1, .15f);
					}
				} break;
				}

			}
		}
		renderer.end();
		renderer.begin(ShapeRenderer.ShapeType.Line);
		renderer.setColor(Color.BLACK);
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				renderer.rect(x, y, 1, 1);
			}
		}
		renderer.end();
		if (drawText) {
			batch.setProjectionMatrix(gameCamera.combined);
			batch.begin();
			for (int y = 0; y < HEIGHT; y++) {
				for (int x = 0; x < WIDTH; x++) {
					int index = x + y * WIDTH;
					switch (types[index]) {
					case SOURCE:
					case DRAIN:
					case BLOCK: {
						float value = values[index];
						if (value > MIN_DRAW_VALUE) {
							font.setColor(Color.RED);
							if (value > 1) {
								glyphs.setText(font, String.format("%.0f", value));
							} else {
								glyphs.setText(font, String.format("%.2f", value));
							}
							font.draw(batch, glyphs, x + .5f - glyphs.width / 2, y + .5f + glyphs.height / 2);
						}
					}
					break;
					}

				}
			}
			batch.end();
		}
		if (drawGroupId) {
			batch.setProjectionMatrix(gameCamera.combined);
			batch.begin();
			for (int y = 0; y < HEIGHT; y++) {
				for (int x = 0; x < WIDTH; x++) {
					int index = x + y * WIDTH;
					if (types[index] == EMPTY) continue;
					glyphs.setText(font, String.format("%d", groups[index]));
					font.draw(batch, glyphs, x + .5f - glyphs.width / 2, y + .5f + glyphs.height / 2);
				}
			}
			batch.end();
		}
	}

	public int groupCount = 0;
	public IntMap<IntArray> groupToSources = new IntMap<>();
	public IntMap<IntArray> groupToDrains = new IntMap<>();
	public IntMap<IntArray> groupToBlocks = new IntMap<>();
	private void tick () {
		groupCount = 0;
		groupToSources.clear();
		groupToDrains.clear();
		groupToBlocks.clear();
		for (int i = 0; i < WIDTH * HEIGHT; i++) {
			groups[i] = NO_GROUP;
		}

		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int index = x + y * WIDTH;
				if (types[index] == SOURCE && groups[index] == NO_GROUP) {
					groupToSources.put(groupCount, new IntArray());
					groupToDrains.put(groupCount, new IntArray());
					groupToBlocks.put(groupCount, new IntArray());
					buildGroup(x, y, groupCount);
					groupCount++;
				}
			}
		}

		for (int gId = 0; gId < groupCount; gId++) {
			IntArray sources = groupToSources.get(gId);
			IntArray drains = groupToDrains.get(gId);
			IntArray blocks = groupToBlocks.get(gId);
			float totalSource = 0;
			for (int i = 0; i < sources.size; i++) {
				totalSource += extraValues[sources.get(i)];
			}

			float extra = totalSource/drains.size;
//			for (int i = 0; i < drains.size; i++) {
//				values[drains.get(i)] += extraValues[drains.get(i)] + extra + .01f;
//			}
			for (int i = 0; i < drains.size; i++) {
				values[drains.get(i)] = extra;
			}

		}

		// tick down not grouped
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				int index = x + y * WIDTH;
				if ((types[index] == BLOCK || types[index] == DRAIN) && groups[index] == NO_GROUP) {
					values[index] -= 0.01f;
				}
				values[index] = MathUtils.clamp(values[index], 0, 1);
			}
		}
	}

	private void buildGroup (int x, int y, int groupId) {
		int index = x + y * WIDTH;
		if (groups[index] != NO_GROUP) return;
		groups[index] = groupId;
		switch (types[index]) {
		case SOURCE: {
			groupToSources.get(groupId).add(index);
		}break;
		case DRAIN: {
			groupToDrains.get(groupId).add(index);
		}break;
		case BLOCK: {
			groupToBlocks.get(groupId).add(index);
		}break;
		}
		for (int ox = -1; ox <= 1; ox++) {
			for (int oy = -1; oy <= 1; oy++) {
				if (Math.abs(ox) == Math.abs(oy))
					continue;
				if (x + ox < 0 || x + ox >= WIDTH || y + oy < 0 || y + oy >= HEIGHT)
					continue;
				int otherId = (x + ox) + (y + oy) * WIDTH;
				if (types[otherId] == EMPTY)
					continue;
				buildGroup(x + ox, y + oy, groupId);
			}
		}
	}

	private Color getWaterColor(float value, Color out){
		value = MathUtils.clamp(value, MIN_DRAW_VALUE, MAX_DRAW_VALUE);
		out.set(.247f + value * value, .247f + value * value, .247f + value/5, 1);
		return out;
	}

	private float map (float value, float start1, float end1, float start2, float end2) {
		return start2 + ((end2 - start2) / (end1 - start1)) * (value - start1);
	}

	Vector2 tmp = new Vector2();
	public void rect (float x1, float y1, float x2, float y2, float width, Color color1, Color color2) {
		Vector2 t = tmp.set(y2 - y1, x1 - x2).nor();
		width *= 0.5f;
		float tx = t.x * width;
		float ty = t.y * width;
		renderer.triangle(
			x1 + tx, y1 + ty,
			x1 - tx, y1 - ty,
			x2 + tx, y2 + ty,
			color1, color1, color2
		);
		renderer.triangle(
			x2 - tx, y2 - ty,
			x2 + tx, y2 + ty,
			x1 - tx, y1 - ty,
			color2, color2, color1
		);
	}

	@Override public boolean keyDown (int keycode) {
		switch(keycode) {
		case Input.Keys.SPACE: {
			simEnabled = !simEnabled;
		} break;
		case Input.Keys.Z: {
			for (int i = 0; i < WIDTH * HEIGHT; i++) {
				values[i] = nextValues[i] = 0;
			}
		} break;
		}
		return true;
	}

	@Override public void resize (int width, int height) {
		gameViewport.update(width, height, true);
		guiViewport.update(width, height, true);
	}
	
	// allow us to start this test directly
	public static void main (String[] args) {
		PlaygroundGame.start(args, CellularAutomataEnergy4Test.class);
	}
}
