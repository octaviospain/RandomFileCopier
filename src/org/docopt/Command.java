/******************************************************************************
 * Copyright 2016-2018 Octavio Calleya                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * http://www.apache.org/licenses/LICENSE-2.0                                 *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package org.docopt;

import java.util.*;

final class Command extends Argument {

	public Command(final String name, final Object value) {
		super(name, value);
	}

	public Command(final String name) {
		this(name, false);
	}

	@Override
	protected SingleMatchResult singleMatch(final List<LeafPattern> left) {
		for (int n = 0; n < left.size(); n++) {
			final LeafPattern pattern = left.get(n);

			if (pattern.getClass() == Argument.class) {
				if (getName().equals(pattern.getValue())) {
					return new SingleMatchResult(n,
							new Command(getName(), true));
				}
				break;
			}
		}

		return new SingleMatchResult(null, null);
	}
}
