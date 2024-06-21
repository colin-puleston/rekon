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
public class InstanceNode extends NodeX {

	private Instance instance;

	private abstract class TypeLinksHandler {

		final NameClassification classification;

		TypeLinksHandler(Names directs) {

			classification = getClassification();

			for (Name type : directs) {

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

			typeClassification.addTransientDirectSub(InstanceNode.this);
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

			typeClassification.removeTransientDirectSub(InstanceNode.this);
		}
	}

	private class ReferencedsFinder extends PatternCrawler {

		private Set<InstanceNode> found = new HashSet<InstanceNode>();

		boolean anyPresent() {

			return !findAll().isEmpty();
		}

		Set<InstanceNode> findAll() {

			PatternMatcher p = getProfilePatternMatcher();

			if (p == null) {

				return Collections.emptySet();
			}

			crawlFromRelations(p.getPattern());

			return found;
		}

		boolean visit(NodeX n) {

			if (n instanceof InstanceNode) {

				found.add((InstanceNode)n);

				return false;
			}

			return !n.mapped();
		}

		boolean visit(PatternMatcher m) {

			return true;
		}

		boolean visit(DisjunctionMatcher m) {

			return true;
		}
	}

	public String getLabel() {

		return instance.getLabel();
	}

	InstanceNode(Instance instance, boolean undefinedRef) {

		this.instance = instance;

		if (undefinedRef) {

			completeClassification();
		}
	}

	void checkClassifiable() {

		if (classified()) {

			resetClassifier();
		}
	}

	void completeClassification() {

		Names directs = findDirectTypes();

		setClassification();

		new TypeLinksAdder(directs);

		getClassification().optimiseEmptyLinks();
	}

	void clearLinks() {

		new TypeLinksRemover();

		clearMatchers();
	}

	Instance getInstance() {

		return instance;
	}

	boolean anyReferenceds() {

		return new ReferencedsFinder().anyPresent();
	}

	Set<InstanceNode> getReferenceds() {

		return new ReferencedsFinder().findAll();
	}

	private Names findDirectTypes() {

		Names all = getClassifier().getSubsumers();
		NameSet directs = new NameSet();

		directs.addAll(all);

		for (Name s : all) {

			directs.removeAll(s.getSubsumers());
		}

		return directs;
	}
}
