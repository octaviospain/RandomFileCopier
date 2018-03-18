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

final class Either extends BranchPattern {

	public Either(final List<? extends Pattern> children) {
		super(children);
	}

	@Override
	protected MatchResult match(final List<LeafPattern> left,
			List<LeafPattern> collected) {
		if (collected == null) {
			collected = list();
		}

		final List<MatchResult> outcomes = list();

		for (final Pattern pattern : getChildren()) {
			final MatchResult m = pattern.match(left, collected);
			if (m.matched()) {
				outcomes.add(m);
			}
		}

		if (!outcomes.isEmpty()) {
			// >>> return min(outcomes, key=lambda outcome: len(outcome[1]))
			{
				return Collections.min(outcomes, new Comparator<MatchResult>() {

					@Override
					public int compare(final MatchResult o1,
							final MatchResult o2) {
						final Integer s1 = Integer.valueOf(o1.getLeft().size());
						final Integer s2 = Integer.valueOf(o2.getLeft().size());
						return s1.compareTo(s2);
					}
				});
			}
		}

		return new MatchResult(false, left, collected);
	}
}
