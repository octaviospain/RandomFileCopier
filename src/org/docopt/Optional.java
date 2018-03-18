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

class Optional extends BranchPattern {

	public Optional(final List<? extends Pattern> children) {
		super(children);
	}

	@Override
	protected MatchResult match(List<LeafPattern> left,
			List<LeafPattern> collected) {
		if (collected == null) {
			collected = list();
		}

		for (final Pattern pattern : getChildren()) {
			final MatchResult u = pattern.match(left, collected);
			left = u.getLeft();
			collected = u.getCollected();
		}

		return new MatchResult(true, left, collected);
	}
}
