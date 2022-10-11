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

package rekon.core;

import java.util.*;

/**
 * @author Colin Puleston
 */
class SignatureRelationCollector {

	private Set<Relation> collected = new HashSet<Relation>();
	private NameSet visitedNodes;

	SignatureRelationCollector(NameSet visitedNodes) {

		this.visitedNodes = visitedNodes != null ? visitedNodes : new NameSet();
	}

	Set<Relation> collectFromProfile(NodePattern profile) {

		collectFromSubsumers(profile.getSingleName());
		collectFromExpandedRelations(profile);

		return collected;
	}

	Set<Relation> collectFromName(NodeName name) {

		checkAddFromName(name);
		collectFromSubsumers(name);

		return collected;
	}

	private SignatureRelationCollector(Name name, NameSet visitedNodes) {

		this.collected = collected;
		this.visitedNodes = visitedNodes;
	}

	private void collectFromSubsumers(NodeName name) {

		for (Name n : name.getSubsumers().getNames()) {

			checkAddFromName((NodeName)n);
		}
	}

	private void collectFromExpandedRelations(NodePattern root) {

		for (Relation r : root.getDirectRelations()) {

			for (Relation sr : r.expandForSignature(visitedNodes)) {

				checkAdd(sr);
			}
		}
	}

	private void checkAddFromName(NodeName name) {

		NodePattern p = name.getProfile();

		if (p != null) {

			for (Relation r : p.getSignatureRelations()) {

				checkAdd(r);
			}
		}
	}

	private void checkAdd(Relation r) {

		for (Relation sr : new HashSet<Relation>(collected)) {

			if (r.subsumes(sr)) {

				return;
			}

			if (sr.subsumes(r)) {

				collected.remove(sr);
			}
		}

		collected.add(r);
	}
}
