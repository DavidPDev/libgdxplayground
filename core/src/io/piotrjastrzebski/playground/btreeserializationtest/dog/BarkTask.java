/*******************************************************************************
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

package io.piotrjastrzebski.playground.btreeserializationtest.dog;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import com.badlogic.gdx.ai.btree.annotation.TaskAttribute;
import com.badlogic.gdx.ai.utils.random.ConstantIntegerDistribution;
import com.badlogic.gdx.ai.utils.random.IntegerDistribution;

/** @author implicit-invocation
 * @author davebaol */
public class BarkTask extends LeafTask<Dog> {

	@TaskAttribute
	public IntegerDistribution times = ConstantIntegerDistribution.ONE;

	private int t;

	@Override
	public void start (Dog dog) {
		super.start(dog);
		t = times.nextInt();
	}

	@Override
	public void run (Dog dog) {
		for (int i = 0; i < t; i++)
			dog.bark();
		success();
	}

	@Override
	protected Task<Dog> copyTo (Task<Dog> task) {
		BarkTask bark = (BarkTask)task;
		bark.times = times;

		return task;
	}

}
