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

package rekon.owl;

import java.util.*;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;

/**
 * @author Colin Puleston
 */
class ClassDefinitionsCreator {

	static private final String EQUIV_AXIOM_DESCRIPTION = "EQUIVALENT-CLASSES";
	static private final String SUBCLASS_AXIOM_DESCRIPTION = "SUB-CLASS-OF";

	private MappedNames mappedNames;
	private MatchComponents matchComponents;
	private MatchStructures matchStructures;

	private abstract class EquivsBasedCreator {

		private Set<NodePattern> defns = new HashSet<NodePattern>();
		private Set<NodePattern> subs = new HashSet<NodePattern>();

		boolean create(Collection<OWLClassExpression> equivs) {

			for (OWLClassExpression e : equivs) {

				if (!absorbEquiv(e)) {

					return false;
				}
			}

			ClassName c = resolveDefinedClass(defns);

			if (c != null) {

				if (!subs.isEmpty()) {

					matchStructures.addImpliedClasses(c, subs);
				}

				return true;
			}

			return false;
		}

		abstract boolean handleOutOfScopeEquiv(OWLClassExpression equiv);

		abstract ClassName resolveDefinedClass(Set<NodePattern> defns);

		private boolean absorbEquiv(OWLClassExpression equiv) {

			List<NodePattern> djs = matchComponents.toNodePatternDisjunction(equiv);

			if (djs == null) {

				if (!handleOutOfScopeEquiv(equiv)) {

					return false;
				}
			}
			else {

				if (djs.size() == 1) {

					defns.add(djs.get(0));
				}
				else {

					subs.addAll(djs);
				}
			}

			return true;
		}
	}

	private class ClassEquivsBasedCreator extends EquivsBasedCreator {

		private AssertedClass assertCls;

		ClassEquivsBasedCreator(AssertedClass assertCls) {

			this.assertCls = assertCls;

			create(assertCls.getEquivExprs());
		}

		boolean handleOutOfScopeEquiv(OWLClassExpression equiv) {

			logOutOfScopeAxiom(EQUIV_AXIOM_DESCRIPTION, assertCls.equivExprToAxiom(equiv));

			return true;
		}

		ClassName resolveDefinedClass(Set<NodePattern> defns) {

			ClassName cls = mappedNames.get(assertCls.getEntity());

			if (!defns.isEmpty()) {

				matchStructures.addClassDefinitions(cls, defns);
			}

			return cls;
		}
	}

	private class GCIEquivsBasedCreator extends EquivsBasedCreator {

		GCIEquivsBasedCreator(OWLEquivalentClassesAxiom ax) {

			if (!create(ax.getClassExpressions())) {

				logOutOfScopeAxiom(SUBCLASS_AXIOM_DESCRIPTION, ax);
			}
		}

		boolean handleOutOfScopeEquiv(OWLClassExpression equiv) {

			return false;
		}

		ClassName resolveDefinedClass(Set<NodePattern> defns) {

			return defns.isEmpty() ? null : matchStructures.addImpliedClass(defns);
		}
	}

	private class GCISuperBasedCreator {

		private OWLClassExpression sup;
		private OWLClassExpression sub;

		GCISuperBasedCreator(OWLSubClassOfAxiom ax) {

			sup = ax.getSuperClass();
			sub = ax.getSubClass();

			if (!create()) {

				logOutOfScopeAxiom(EQUIV_AXIOM_DESCRIPTION, ax);
			}
		}

		private boolean create() {

			List<NodePattern> pSubs = matchComponents.toNodePatternDisjunction(sub);

			if (pSubs != null) {

				ClassName supCls = resolveSuperClass();

				if (supCls != null) {

					matchStructures.addImpliedClasses(supCls, pSubs);

					return true;
				}
			}

			return false;
		}

		private ClassName resolveSuperClass() {

			if (sup instanceof OWLClass) {

				return mappedNames.get((OWLClass)sup);
			}

			NodePattern p = matchComponents.toNodePattern(sup);

			return p != null ? matchStructures.addImpliedClass(p) : null;
		}
	}

	ClassDefinitionsCreator(
		Assertions assertions,
		MappedNames mappedNames,
		MatchComponents matchComponents,
		MatchStructures matchStructures) {

		this.mappedNames = mappedNames;
		this.matchComponents = matchComponents;
		this.matchStructures = matchStructures;

		for (AssertedClass c : assertions.getClasses()) {

			new ClassEquivsBasedCreator(c);
		}

		for (OWLEquivalentClassesAxiom ax : assertions.getEquivGCIs()) {

			new GCIEquivsBasedCreator(ax);
		}

		for (OWLSubClassOfAxiom ax :  assertions.getSuperGCIs()) {

			new GCISuperBasedCreator(ax);
		}
	}

	private void logOutOfScopeAxiom(String axiomDesc, OWLAxiom axiom) {

		Logger logger = Logger.SINGLETON;

		logger.logOutOfScopeWarningLine(axiomDesc);
		logger.logLine("AXIOM: " + axiom);
	}
}
