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
public class InstanceName extends NodeName {

	private Instance instance;

	private abstract class TypeLinksHandler {

		final NameClassification classification;

		TypeLinksHandler(Names directs) {

			classification = getClassification();

			for (Name type : directs.getNames()) {

				processLinkToType(type);
				processLinkFromType(type.getClassification());
			}
		}

		abstract void processLinkToType(Name type);

		abstract void processLinkFromType(NameClassification typeClassification);
	}

	private class TypeLinksAdder extends TypeLinksHandler {

		TypeLinksAdder(Names directs) {

			super(directs);
		}

		void processLinkToType(Name type) {

			classification.addTransientDirectSuper(type);
		}

		void processLinkFromType(NameClassification typeClassification) {

			typeClassification.addTransientDirectSub(InstanceName.this);
		}
	}

	private class TypeLinksRemover extends TypeLinksHandler {

		TypeLinksRemover() {

			super(getSupers(true));
		}

		void processLinkToType(Name type) {

			classification.removeTransientDirectSuper(type);
		}

		void processLinkFromType(NameClassification typeClassification) {

			typeClassification.removeTransientDirectSub(InstanceName.this);
		}
	}

	private class ReferencedsFinder {

		private Set<InstanceName> found = new HashSet<InstanceName>();

		boolean anyPresent() {

			return !findAll().isEmpty();
		}

		Set<InstanceName> findAll() {

			PatternNode pn = getPatternNode();

			if (pn == null) {

				return Collections.emptySet();
			}

			findFrom(pn.getProfile());

			return found;
		}

		private void findFrom(NodePattern p) {

			for (Relation r : p.getDirectRelations()) {

				Value t = r.getTarget();

				if (t instanceof NodeValue) {

					findFrom(t.asNodeValue().getValueNode());
				}
			}
		}

		private void findFrom(NodeName n) {

			if (n instanceof InstanceName) {

				found.add((InstanceName)n);
			}
			else if (n instanceof FreeClassName) {

				findFrom((FreeClassName)n);
			}
		}

		private void findFrom(FreeClassName c) {

			PatternNode pn = c.getPatternNode();

			if (pn != null) {

				findFrom(pn.getProfile());
			}

			for (DisjunctionNode dn : c.getDisjunctionNodes()) {

				for (Name d : dn.getDisjuncts().getNames()) {

					findFrom((NodeName)d);
				}
			}
		}
	}

	public String getLabel() {

		return instance.getLabel();
	}

	InstanceName(Instance instance) {

		this.instance = instance;
	}

	void completeClassification() {

		Names directs = findDirectTypes();

		setClassification();

		new TypeLinksAdder(directs);

		getClassification().optimiseEmptyLinks();
	}

	void removeFromClassification() {

		new TypeLinksRemover();
	}

	Instance getInstance() {

		return instance;
	}

	boolean anyReferenceds() {

		return new ReferencedsFinder().anyPresent();
	}

	Set<InstanceName> getReferenceds() {

		return new ReferencedsFinder().findAll();
	}

	private Names findDirectTypes() {

		Names all = getClassifier().getSubsumers();
		NameSet directs = new NameSet();

		directs.addAll(all);

		for (Name s : all.getNames()) {

			directs.removeAll(s.getSubsumers());
		}

		return directs;
	}
}