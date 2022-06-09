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

	private Assertions assertions;

	private MappedNames mappedNames;
	private MatchComponents matchComponents;
	private MatchStructures matchStructures;

	ClassDefinitionsCreator(
		Assertions assertions,
		MappedNames mappedNames,
		MatchComponents matchComponents,
		MatchStructures matchStructures) {

		this.assertions = assertions;
		this.mappedNames = mappedNames;
		this.matchComponents = matchComponents;
		this.matchStructures = matchStructures;

		for (OWLEquivalentClassesAxiom ax : getEquivalenceAxioms()) {

			if (!createForEquivalents(ax)) {

				logOutOfScopeAxiom("EQUIVALENT-CLASSES", ax);
			}
		}

		for (OWLSubClassOfAxiom ax : getSubClassAxioms()) {

			if (!createForSubClass(ax)) {

				logOutOfScopeAxiom("SUB-CLASS", ax);
			}
		}
	}

	private boolean createForEquivalents(OWLEquivalentClassesAxiom ax) {

		Set<NodePattern> defns = new HashSet<NodePattern>();
		Set<NodePattern> subs = new HashSet<NodePattern>();

		ClassName equivCls = null;

		for (OWLClassExpression e : ax.getClassExpressions()) {

			if (e instanceof OWLClass) {

				equivCls = mappedNames.get((OWLClass)e);
			}
			else {

				List<NodePattern> djs = matchComponents.toNodePatternDisjunction(e);

				if (djs == null) {

					return false;
				}

				if (djs.size() == 1) {

					defns.add(djs.get(0));
				}
				else {

					subs.addAll(djs);
				}
			}
		}

		if (equivCls != null) {

			matchStructures.addClassDefinitions(equivCls, defns);
		}
		else {

			if (defns.isEmpty()) {

				return false;
			}

			equivCls = matchStructures.addImpliedClass(defns);
		}

		if (!subs.isEmpty()) {

			matchStructures.addImpliedClasses(equivCls, subs);
		}

		return true;
	}

	private boolean createForSubClass(OWLSubClassOfAxiom ax) {

		OWLClassExpression sub = ax.getSubClass();

		if (sub instanceof OWLClass) {

			return true;
		}

		List<NodePattern> pSubs = matchComponents.toNodePatternDisjunction(sub);

		if (pSubs == null) {

			return false;
		}

		OWLClassExpression sup = ax.getSuperClass();
		ClassName supCls = null;

		if (sup instanceof OWLClass) {

			supCls = mappedNames.get((OWLClass)sup);
		}
		else {

			NodePattern pSup = matchComponents.toNodePattern(sup);

			if (pSup == null) {

				return false;
			}

			supCls = matchStructures.addImpliedClass(pSup);
		}

		matchStructures.addImpliedClasses(supCls, pSubs);

		return true;
	}

	private Collection<OWLEquivalentClassesAxiom> getEquivalenceAxioms() {

		return assertions.getAxioms(AxiomType.EQUIVALENT_CLASSES);
	}

	private Collection<OWLSubClassOfAxiom> getSubClassAxioms() {

		return assertions.getAxioms(AxiomType.SUBCLASS_OF);
	}

	private void logOutOfScopeAxiom(String axiomDesc, OWLAxiom axiom) {

		Logger logger = Logger.SINGLETON;

		logger.logOutOfScopeWarningLine(axiomDesc);
		logger.logLine("AXIOM: " + axiom);
	}
}
