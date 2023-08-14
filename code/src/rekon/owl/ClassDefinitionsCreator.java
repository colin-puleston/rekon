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

		private Set<Pattern> patternDefns = new HashSet<Pattern>();
		private Set<List<Pattern>> disjunctionDefns = new HashSet<List<Pattern>>();

		boolean create(Collection<OWLClassExpression> equivs) {

			for (OWLClassExpression e : equivs) {

				if (!absorbEquiv(e)) {

					return false;
				}
			}

			ClassNode c = resolveDefinedClass();

			for (Pattern defn : patternDefns) {

				matchStructures.addDefinitionPattern(c, defn);
			}

			for (List<Pattern> disjuncts : disjunctionDefns) {

				List<GNode> nodeDjs = resolveGCIDisjunctionToNodes(disjuncts);

				matchStructures.addDisjunction(c, nodeDjs);
			}

			return true;
		}

		abstract boolean handleOutOfScopeEquiv(OWLClassExpression equiv);

		abstract ClassNode resolveDefinedClass();

		private boolean absorbEquiv(OWLClassExpression equiv) {

			List<Pattern> djs = matchComponents.toPatternDisjunction(equiv);

			if (djs == null) {

				if (!handleOutOfScopeEquiv(equiv)) {

					return false;
				}
			}
			else {

				if (djs.size() == 1) {

					patternDefns.add(djs.get(0));
				}
				else {

					disjunctionDefns.add(djs);
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

		ClassNode resolveDefinedClass() {

			return mappedNames.get(assertCls.getEntity());
		}
	}

	private class GCIEquivsBasedCreator extends EquivsBasedCreator {

		GCIEquivsBasedCreator(OWLEquivalentClassesAxiom ax) {

			if (!create(ax.getClassExpressions())) {

				logOutOfScopeAxiom(EQUIV_AXIOM_DESCRIPTION, ax);
			}
		}

		boolean handleOutOfScopeEquiv(OWLClassExpression equiv) {

			return false;
		}

		ClassNode resolveDefinedClass() {

			return matchStructures.addGCIImpliedClass();
		}
	}

	private class GCISuperBasedCreator {

		private OWLClassExpression sup;
		private OWLClassExpression sub;

		GCISuperBasedCreator(OWLSubClassOfAxiom ax) {

			sup = ax.getSuperClass();
			sub = ax.getSubClass();

			if (!create()) {

				logOutOfScopeAxiom(SUBCLASS_AXIOM_DESCRIPTION, ax);
			}
		}

		private boolean create() {

			List<Pattern> subDjs = matchComponents.toPatternDisjunction(sub);

			if (subDjs != null) {

				ClassNode supCls = resolveSuperClass();

				if (supCls != null) {

					for (GNode subCls : resolveGCIDisjunctionToNodes(subDjs)) {

						subCls.addSubsumer(supCls);
					}

					return true;
				}
			}

			return false;
		}

		private ClassNode resolveSuperClass() {

			if (sup instanceof OWLClass) {

				return mappedNames.get((OWLClass)sup);
			}

			Pattern p = matchComponents.toPattern(sup);

			return p != null ? addGCIImpliedClass(p) : null;
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
		logger.logSeparatorLine();
	}

	private List<GNode> resolveGCIDisjunctionToNodes(List<Pattern> disjuncts) {

		List<GNode> nodeDjs = new ArrayList<GNode>();

		for (Pattern d : disjuncts) {

			nodeDjs.add(resolveGCIDisjunctToNode(d));
		}

		return nodeDjs;
	}

	private GNode resolveGCIDisjunctToNode(Pattern disjunct) {

		GNode n = disjunct.toSingleNode();

		return n != null ? n : addGCIImpliedClass(disjunct);
	}

	private ClassNode addGCIImpliedClass(Pattern defn) {

		ClassNode c = matchStructures.addGCIImpliedClass();

		matchStructures.addDefinitionPattern(c, defn);

		return c;
	}
}
