package io.piotrjastrzebski.playground.box2dtest;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.Array;
import io.piotrjastrzebski.playground.BaseScreen;
import io.piotrjastrzebski.playground.PlaygroundGame;

/**
 * Created by PiotrJ on 31/07/15.
 */
public class Box2dInterpolationTest extends BaseScreen {
	public final static float STEP_TIME = 1.0f / 15.0f;
	private final static int MAX_STEPS = 3;

	World box2dWorld;
	Array<Box> boxes = new Array<>();
	Texture largeBox;
	Texture smallBox;
	Box2DDebugRenderer debugRenderer;
	boolean debugDraw = true;

	public Box2dInterpolationTest (PlaygroundGame game) {
		super(game);
		debugRenderer = new Box2DDebugRenderer();
		box2dWorld = new World(new Vector2(0, -10), true);
		largeBox = new Texture("box2d/box64.png");
		smallBox = new Texture("box2d/box32.png");
		createBounds();
		reset();
	}

	Body groundBody;

	private void createBounds () {
		float halfWidth = VP_WIDTH / 2f - 0.5f;
		float halfHeight = VP_HEIGHT / 2f - 0.5f;
		ChainShape chainShape = new ChainShape();
		chainShape.createLoop(new Vector2[] {new Vector2(-halfWidth, -halfHeight), new Vector2(halfWidth, -halfHeight),
			new Vector2(halfWidth, halfHeight), new Vector2(-halfWidth, halfHeight)});
		BodyDef chainBodyDef = new BodyDef();
		chainBodyDef.type = BodyDef.BodyType.StaticBody;
		groundBody = box2dWorld.createBody(chainBodyDef);
		groundBody.createFixture(chainShape, 0);
		chainShape.dispose();
	}

	private void reset () {
		if (mouseJoint != null) {
			box2dWorld.destroyJoint(mouseJoint);
			mouseJoint = null;
		}
		for (Box box : boxes) {
			box2dWorld.destroyBody(box.body);
		}
		boxes.clear();

		for (int i = 0; i < 30; i++) {
			float x = MathUtils.random(-15, 15);
			float y = MathUtils.random(-8, 8);
			float rotation = MathUtils.random(90);
			if (MathUtils.randomBoolean()) {
				createBox(x, y, rotation, largeBox);
			} else {
				createBox(x, y, rotation, smallBox);
			}
		}
	}

	private void createBox (float x, float y, float rotation, Texture texture) {
		Box box = new Box(x, y, rotation, texture);

		BodyDef def = new BodyDef();
		def.position.set(x, y);
		def.angle = rotation * MathUtils.degreesToRadians;
		def.type = BodyDef.BodyType.DynamicBody;
		box.body = box2dWorld.createBody(def);
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(box.width / 2, box.height / 2);
		box.body.createFixture(shape, 1);
		shape.dispose();

		boxes.add(box);
	}

	float accumulator;

	@Override public void render (float delta) {
		super.render(delta);
		accumulator += delta;
		int steps = 0;
		while (STEP_TIME < accumulator && MAX_STEPS > steps) {
			box2dWorld.clearForces();
			box2dWorld.step(STEP_TIME, 6, 2);
			accumulator -= STEP_TIME;
			steps++;
			fixedUpdate();
		}

		variableUpdate(delta, accumulator / STEP_TIME);

		draw();
	}

	private void fixedUpdate () {
		for (Box box : boxes) {
			box.fixedUpdate();
		}
	}

	private void variableUpdate (float delta, float alpha) {
		for (Box box : boxes) {
			box.variableUpdate(delta, alpha);
		}
	}

	private void draw () {
		if (debugDraw) {
			debugRenderer.render(box2dWorld, gameCamera.combined);
		}
		batch.setProjectionMatrix(gameCamera.combined);
		batch.begin();
		for (Box box : boxes) {
			box.draw(batch);
		}
		batch.end();
	}

	private class Box {
		public Body body;
		public Texture texture;
		private Transform start = new Transform();
		private Transform current = new Transform();
		private Transform target = new Transform();
		private float width;
		private float height;
		private int srcWidth;
		private int srcHeight;

		public Box (float x, float y, float rotation, Texture texture) {
			current.x = start.x = target.x = x;
			current.y = start.y = target.y = y;
			current.rot = start.rot = target.rot = rotation;

			this.texture = texture;
			srcWidth = texture.getWidth();
			width = srcWidth * INV_SCALE;
			srcHeight = texture.getHeight();
			height = srcHeight * INV_SCALE;
		}

		public void fixedUpdate () {
			Vector2 position = body.getPosition();
			target.x = position.x;
			target.y = position.y;
			target.rot = body.getAngle() * MathUtils.radiansToDegrees;
			start.set(current);
		}

		public void variableUpdate (float delta, float alpha) {
			current.interpolate(start, target, alpha);
		}

		public void draw (Batch batch) {
			batch
				.draw(texture, current.x - width / 2, current.y - height / 2, width / 2, height / 2, width, height, 1, 1, current.rot,
					0, 0, srcWidth, srcHeight, false, false);
		}

		private class Transform {
			public float x;
			public float y;
			public float rot;

			public void set (Transform other) {
				x = other.x;
				y = other.y;
				rot = other.rot;
			}

			public void interpolate (Transform src, Transform dst, float alpha) {
				x = Interpolation.linear.apply(src.x, dst.x, alpha);
				y = Interpolation.linear.apply(src.y, dst.y, alpha);
				rot = Interpolation.linear.apply(src.rot, dst.rot, alpha);
			}
		}
	}
	Body hitBody;
	Vector3 testPoint = new Vector3();
	QueryCallback callback = new QueryCallback() {
		@Override
		public boolean reportFixture(Fixture fixture) {
			if (fixture.getBody() == groundBody)
				return true;

			if (fixture.testPoint(testPoint.x, testPoint.y)) {
				hitBody = fixture.getBody();
				return false;
			} else
				return true;
		}
	};

	private MouseJoint mouseJoint;
	@Override public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		gameCamera.unproject(testPoint.set(screenX, screenY, 0));

		// ask the world which bodies are within the given
		// bounding box around the mouse pointer
		hitBody = null;
		box2dWorld.QueryAABB(callback, testPoint.x - 0.1f, testPoint.y - 0.1f,
			testPoint.x + 0.1f, testPoint.y + 0.1f);

		// if we hit something we create a new mouse joint
		// and attach it to the hit body.
		if (hitBody != null) {
			MouseJointDef def = new MouseJointDef();
			def.bodyA = groundBody;
			def.bodyB = hitBody;
			def.collideConnected = true;
			def.target.set(testPoint.x, testPoint.y);
			def.maxForce = 1000.0f * hitBody.getMass();

			mouseJoint = (MouseJoint) box2dWorld.createJoint(def);
			hitBody.setAwake(true);
		}

		return super.touchDown(screenX, screenY, pointer, button);
	}
	Vector2 target = new Vector2();

	@Override
	public boolean touchDragged(int x, int y, int pointer) {
		gameCamera.unproject(testPoint.set(x, y, 0));
		target.set(testPoint.x, testPoint.y);
		// if a mouse joint exists we simply update
		// the target of the joint based on the new
		// mouse coordinates
		if (mouseJoint != null) {
			mouseJoint.setTarget(target);
		}
		return false;
	}

	@Override
	public boolean touchUp(int x, int y, int pointer, int button) {
		// if a mouse joint exists we simply destroy it
		if (mouseJoint != null) {
			box2dWorld.destroyJoint(mouseJoint);
			mouseJoint = null;
		}
		return false;
	}

	@Override public boolean keyDown (int keycode) {
		if (keycode == Input.Keys.F5) {
			reset();
		}
		if (keycode == Input.Keys.Z) {
			debugDraw = !debugDraw;
		}
		return super.keyDown(keycode);
	}

	@Override public void dispose () {
		super.dispose();
		largeBox.dispose();
		smallBox.dispose();
	}
}