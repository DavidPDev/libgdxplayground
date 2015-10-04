package io.piotrjastrzebski.playground.uitesting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.*;
import io.piotrjastrzebski.playground.BaseScreen;
import io.piotrjastrzebski.playground.GameReset;

/**
 * We want to figure out a way to drag and drop stuff from one tree to another, to proper places
 * Also change level of a node in a tree and order at same depth
 * Largish actors in nodes
 *
 * https://github.com/libgdx/libgdx/blob/master/tests/gdx-tests/src/com/badlogic/gdx/tests/DragAndDropTest.java
 * Created by PiotrJ on 20/06/15.
 */
public class UIDaDTest extends BaseScreen {
	VisWindow window;
	VisTree tree;

	VisLabel trash;

	VisTextButton rebuild;
	DragAndDrop dadAdd;
	DragAndDrop dadMove;

	public UIDaDTest (GameReset game) {
		super(game);

		window = new VisWindow("BTE");
		window.setSize(700, 700);
		window.centerWindow();
		window.setResizable(true);
		stage.addActor(window);

		rebuild = new VisTextButton("Rebuild");
		rebuild.addListener(new ClickListener(){
			@Override public void clicked (InputEvent event, float x, float y) {
				rebuild();
			}
		});
		window.add(rebuild);
		trash = new VisLabel("Trash");
		window.add(trash);
		window.row();

		tree = new VisTree();
		window.add(tree).expand().fill();

		VisTable nodes = new VisTable();
		VisLabel nodeA = new VisLabel("NodeA");
		nodes.add(nodeA).row();
		VisLabel nodeB = new VisLabel("NodeB");
		nodes.add(nodeB).row();
		VisLabel nodeC = new VisLabel("NodeC");
		nodes.add(nodeC).row();
		VisLabel nodeD = new VisLabel("NodeD");
		nodes.add(nodeD).row();

		dadAdd = new DragAndDrop();
		dadAdd.addSource(source(nodeA));
		dadAdd.addSource(source(nodeB));
		dadAdd.addSource(source(nodeC));
		dadAdd.addSource(source(nodeD));

		dadMove = new DragAndDrop();
		dadMove.addTarget(new Target(trash) {
			@Override public boolean drag (Source source, Payload payload, float x, float y, int pointer) {
				return true;
			}

			@Override public void drop (Source source, Payload payload, float x, float y, int pointer) {
				ViewNode node = (ViewNode)payload.getObject();
				node.remove();
			}
		});



		window.add(nodes);
		rebuild();
	}

	private Source source(final VisLabel label) {
		return source(label, null);
	}
	private Source source(final VisLabel label, final Object extra) {
		Source source = new Source(label) {
			@Override public Payload dragStart (InputEvent event, float x, float y, int pointer) {
				Payload payload = new Payload();
				payload.setObject(extra);

				payload.setDragActor(new VisLabel(label.getText()));

				VisLabel validLabel = new VisLabel(label.getText());
				validLabel.setColor(0, 1, 0, 1);
				payload.setValidDragActor(validLabel);

				VisLabel invalidLabel = new VisLabel(label.getText());
				invalidLabel.setColor(1, 0, 0, 1);
				payload.setInvalidDragActor(invalidLabel);

				return payload;
			}
		};
		return source;
	}

	private void rebuild() {
		tree.clearChildren();
		ViewNode node = new ViewNode("root");
		ViewNode nodeA = new ViewNode("nodeA");
		ViewNode nodeAA = new ViewNode("nodeAA");
		ViewNode nodeAB = new ViewNode("nodeAB");
		ViewNode nodeB = new ViewNode("nodeB");
		ViewNode nodeBA = new ViewNode("nodeBA");
		ViewNode nodeBB = new ViewNode("nodeBB");
		ViewNode nodeBC = new ViewNode("nodeBC");
		ViewNode nodeBCA = new ViewNode("nodeBCA");
		ViewNode nodeBCB = new ViewNode("nodeBCB");
		ViewNode nodeC = new ViewNode("nodeC");
		ViewNode nodeCA = new ViewNode("nodeCA");
		ViewNode nodeD = new ViewNode("nodeD");
		node.add(nodeA);
		nodeA.add(nodeAA);
		nodeA.add(nodeAB);
		node.add(nodeB);
		nodeB.add(nodeBA);
		nodeB.add(nodeBB);
		nodeB.add(nodeBC);
		nodeBC.add(nodeBCA);
		nodeBC.add(nodeBCB);
		node.add(nodeC);
		nodeC.add(nodeCA);
		node.add(nodeD);
		tree.add(node);
		tree.expandAll();
	}

	private class ViewNode extends Tree.Node {
		public ViewNode (String text) {
			super(new VisLabel(text));
			setObject(this);
			dadAdd.addTarget(new Target(getActor()) {
				@Override public boolean drag (Source source, Payload payload, float x, float y, int pointer) {
					// check if we want the payload
					return true;
				}

				@Override public void drop (Source source, Payload payload, float x, float y, int pointer) {
					VisLabel label = (VisLabel)source.getActor();
					add(new ViewNode(label.getText().toString()));
					expandAll();
				}
			});
			dadMove.addSource(source((VisLabel)getActor(), this));
			dadMove.addTarget(new Target(getActor()) {
				@Override public boolean drag (Source source, Payload payload, float x, float y, int pointer) {
					// check if we want the pay
					ViewNode node = (ViewNode)payload.getObject();
//					if (ViewNode.this.findNode(node) != null) {
//						return false;
//					}
					if (node.findNode(ViewNode.this) != null) {
						return false;
					}
					return true;
				}

				@Override public void drop (Source source, Payload payload, float x, float y, int pointer) {
					float height = source.getActor().getHeight();
					float a = y / height;
					// could use a to determine if we want to add the node as child or insert bofore/after this one
					ViewNode node = (ViewNode)payload.getObject();
					Tree.Node parent = ViewNode.this.getParent();
					if (a < 0.3f) {
						// insert below this node
						Gdx.app.log("", "Insert below");
						if (parent != null) {
							node.remove();
							int id = ViewNode.this.getIndex();
							parent.insert(id+1, node);
							return;
						}
					} else if (a > 0.7f) {
						// insert above this node
						Gdx.app.log("", "Insert above");
						if (parent != null) {
							node.remove();
							int id = ViewNode.this.getIndex();
							parent.insert(id, node);
							return;
						}
					}
					// add to this node as default
					Gdx.app.log("", "Add");
					node.remove();
					ViewNode.this.add(node);
					expandAll();
				}
			});
		}

		private int getIndex() {
			Tree.Node parent = getParent();
			if (parent == null) return 0;
			return parent.getChildren().indexOf(this, true);
		}
	}

	@Override public void render (float delta) {
		Gdx.gl.glClearColor(.5f, .5f, .5f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.act(delta);
		stage.draw();
	}
}
