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
class GCIsCreator {

	private Assertions assertions;

	private MappedNames mappedNames;
	private PatternComponents patternComponents;

	private GCIClasses gciClasses;

	GCIsCreator(
		Assertions assertions,
		MappedNames mappedNames,
		PatternComponents patternComponents,
		GCIClasses gciClasses) {

		this.assertions = assertions;
		this.mappedNames = mappedNames;
		this.patternComponents = patternComponents;
		this.gciClasses = gciClasses;

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

		Set<NodePattern> patterns = new HashSet<NodePattern>();

		for (OWLClassExpression e : ax.getClassExpressions()) {

			if (e instanceof OWLClass) {

				return true;
			}

			NodePattern p = patternComponents.toNodePattern(e);

			if (p == null) {

				return false;
			}

			patterns.add(p);
		}

		gciClasses.create(patterns);

		return true;
	}

	private boolean createForSubClass(OWLSubClassOfAxiom ax) {

		OWLClassExpression sub = ax.getSubClass();

		if (sub instanceof OWLClass) {

			return true;
		}

		NodePattern pSub = patternComponents.toNodePattern(sub);

		if (pSub == null) {

			return false;
		}

		OWLClassExpression sup = ax.getSuperClass();

		if (sup instanceof OWLClass) {

			gciClasses.create(mappedNames.get((OWLClass)sup), pSub);
		}
		else {

			NodePattern pSup = patternComponents.toNodePattern(sup);

			if (pSup == null) {

				return false;
			}

			gciClasses.create(gciClasses.create(pSup), pSub);
		}

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
