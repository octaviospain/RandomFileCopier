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

import static org.docopt.Python.*;

final class Required extends BranchPattern {

	public Required(final List<? extends Pattern> children) {
		super(children);
	}

	@Override
	protected MatchResult match(final List<LeafPattern> left,
			List<LeafPattern> collected) {
		if (collected == null) {
			collected = list();
		}

		List<LeafPattern> l = left;
		List<LeafPattern> c = collected;

		for (final Pattern pattern : getChildren()) {
			final MatchResult m = pattern.match(l, c);

			l = m.getLeft();
			c = m.getCollected();

			if (!m.matched()) {
				return new MatchResult(false, left, collected);
			}
		}

		return new MatchResult(true, l, c);
	}
}
