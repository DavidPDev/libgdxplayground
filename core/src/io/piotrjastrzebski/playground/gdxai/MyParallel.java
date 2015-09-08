package io.piotrjastrzebski.playground.gdxai;

/**
 * Created by PiotrJ on 01/09/15.
 *//*******************************************************************************
 * Copyright 2014 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import com.badlogic.gdx.ai.btree.BranchTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.utils.Array;


/** A {@code Parallel} is a special branch task that starts or resumes all children every single time, parallel task will succeed
 * if all the children succeed, fail if one of the children fail. The typical use case: make the game entity react on event while
 * sleeping or wandering.
 *
 * @param <E> type of the blackboard object that tasks use to read or modify game state
 *
 * @author implicit-invocation
 * @author davebaol */
public class MyParallel<E> extends BranchTask<E> {

	private boolean[] runningTasks;
	private boolean success;
	private boolean noRunningTasks;
	private int currentChildIndex;

	/** Creates a parallel task with no children */
	public MyParallel () {
		this(new Array<Task<E>>());
	}

	/** Creates a parallel task with the given children
	 * @param tasks the children */
	public MyParallel (Task<E>... tasks) {
		this(new Array<Task<E>>(tasks));
	}

	/** Creates a parallel task with the given children
	 * @param tasks the children */
	public MyParallel (Array<Task<E>> tasks) {
		super(tasks);
		this.success = true;
		this.noRunningTasks = true;
	}

	@Override
	public void start () {
		if (runningTasks == null)
			runningTasks = new boolean[children.size];
		else {
			for (int i = 0; i < runningTasks.length; i++)
				runningTasks[i] = false;
		}
		success = true;
	}

	@Override
	public void run () {
		noRunningTasks = true;
		for (currentChildIndex = 0; currentChildIndex < children.size; currentChildIndex++) {
			Task<E> child = children.get(currentChildIndex);
			if (runningTasks[currentChildIndex]) {
				child.run();
			} else {
				child.setControl(this);
				child.start();
				child.run();
			}
			if (noRunningTasks) break;
		}
	}

	@Override
	public void childRunning (Task<E> task, Task<E> reporter) {
		runningTasks[currentChildIndex] = true;
		noRunningTasks = false;
		control.childRunning(this, this);
	}

	@Override
	public void childSuccess (Task<E> runningTask) {
		runningTasks[currentChildIndex] = false;
		if (noRunningTasks && currentChildIndex == children.size - 1) {
			if (success) {
				success();
			} else {
				fail();
			}
		} else if (currentChildIndex < children.size - 1) {
			noRunningTasks = false;
		}
	}

	@Override
	public void childFail (Task<E> runningTask) {
		runningTasks[currentChildIndex] = false;
		success = false;
		noRunningTasks = true;
		for (int i = 0; i < children.size; i++) {
			Task<E> current = children.get(i);
			if (!runningTasks[i]) continue;
			if (current == runningTask) continue;
			current.fail();
		}
		fail();
	}

	@Override
	public void reset () {
		super.reset();
		if (runningTasks != null) {
			for (int i = 0; i < runningTasks.length; i++)
				runningTasks[i] = false;
		}
		success = true;
	}

}

