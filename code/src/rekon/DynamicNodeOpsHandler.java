/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Colin Puleston
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package rekon;

import java.util.*;

/**
 * @author Colin Puleston
 */
class DynamicNodeOpsHandler extends DynamicOpsHandler {

	private NodeName name;

	DynamicNodeOpsHandler(NodeName name) {

		this.name = name;
	}

	Names getEquivalents() {

		if (name == null) {

			return Names.NO_NAMES;
		}

		NameList equivs = new NameList(name);

		equivs.addAll(name.getEquivalents());

		return equivs;
	}

	Names getSupers(boolean direct) {

		return name != null ? name.getSupers(direct) : Names.NO_NAMES;
	}

	Names getSubs(boolean direct) {

		return getSubs(ClassName.class, direct);
	}

	Names getInstances(boolean direct) {

		return getSubs(IndividualName.class, direct);
	}

	boolean subsumes(DynamicOpsHandler other) {

		if (other instanceof DynamicNodeOpsHandler) {

			return name.subsumes(((DynamicNodeOpsHandler)other).name);
		}

		return super.subsumes(other);
	}

	Collection<NodePattern> getProfiles() {

		List<NodePattern> profs = new ArrayList<NodePattern>();

		for (MatchableNode m : getApplicableMatchables()) {

			profs.add(m.getProfile());
		}

		return profs;
	}

	Collection<NodePattern> getDefinitions() {

		List<NodePattern> defns = new ArrayList<NodePattern>();

		defns.add(new NodePattern(name));

		for (MatchableNode m : getApplicableMatchables()) {

			defns.addAll(m.getDefinitions());
		}

		return defns;
	}

	private Names getSubs(Class<? extends NodeName> type, boolean direct) {

		return name != null ? name.getSubs(type, direct) : Names.NO_NAMES;
	}

	private Collection<MatchableNode> getApplicableMatchables() {

		if (name == null) {

			return Collections.emptyList();
		}

		List<MatchableNode> ms = new ArrayList<MatchableNode>();

		checkAddMatchable(ms, name);

		for (Name en : name.getEquivalents().getNames()) {

			checkAddMatchable(ms, (NodeName)en);
		}

		return ms;
	}

	private void checkAddMatchable(Collection<MatchableNode> ms, NodeName n) {

		MatchableNode m = n.getMatchable();

		if (m != null) {

			ms.add(m);
		}
	}
}
