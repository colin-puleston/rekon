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

package rekon.owl.convert;

import java.util.*;

import org.semanticweb.owlapi.model.*;

import rekon.core.*;
import rekon.build.input.*;
import rekon.util.*;

/**
 * @author Colin Puleston
 */
class ClassAxiomConverter extends CategoryAxiomConverter {

	private OWLClass owlNothing;

	private ClassEquivalenceConverter equivalenceConverter = new ClassEquivalenceConverter();
	private ClassSubSuperConverter subSuperConverter = new ClassSubSuperConverter();

	private class ConvertedClassEquivalence
						extends ConvertedEquivalence<InputNode>
						implements InputClassEquivalence {

		ConvertedClassEquivalence(OWLAxiom source, InputNode first, InputNode second) {

			super(source, first, second);
		}
	}

	private class ConvertedClassSubSuper
						extends ConvertedSubSuper<InputNode>
						implements InputClassSubSuper {

		ConvertedClassSubSuper(OWLAxiom source, InputNode sub, InputNode sup) {

			super(source, sub, sup);
		}
	}

	private class OwlClassLink extends OwlLink<OWLClassExpression, ClassNode> {

		OwlClassLink(OWLEquivalentClassesAxiom source) {

			super(source, source.getClassExpressions());
		}

		OwlClassLink(OWLSubClassOfAxiom source) {

			super(source, source.getSubClass(), source.getSuperClass());
		}

		InputNode firstOrSubAsInputNode() {

			return toInputNode(firstOrSub);
		}

		InputNode secondOrSupAsInputNode() {

			return toInputNode(secondOrSup);
		}

		boolean invalidEndPointExprType(OWLClassExpression expr) {

			return false;
		}

		boolean invalidEndPointInbuiltEntity(OWLClassExpression expr) {

			return expr.equals(owlNothing);
		}

		ClassNode asName(OWLClassExpression expr) {

			return names.resolve((OWLClass)expr);
		}

		private InputNode toInputNode(OWLClassExpression expr) {

			return expressions.toAxiomNode(getSourceAxiom(), expr);
		}
	}

	private class ClassEquivalenceSplitter
						extends
							EquivalenceSplitter<OWLEquivalentClassesAxiom> {

		Class<OWLEquivalentClassesAxiom> getAxiomType() {

			return OWLEquivalentClassesAxiom.class;
		}

		Collection<? extends OWLAxiom> asPairwiseAxioms(OWLEquivalentClassesAxiom axiom) {

			return axiom.asPairwiseAxioms();
		}
	}

	private class ClassSubSuperSplitter
					extends
						SubSuperSplitter<OWLSubClassOfAxiom, OWLClassExpression> {

		OWLClassExpression getSuper(OWLSubClassOfAxiom axiom) {

			return axiom.getSuperClass();
		}

		OWLClassExpression getSub(OWLSubClassOfAxiom axiom) {

			return axiom.getSubClass();
		}

		OWLSubClassOfAxiom createComponentAxiom(OWLClassExpression sup, OWLClassExpression sub) {

			return factory.getOWLSubClassOfAxiom(sub, sup);
		}
	}

	private abstract class ClassLinkConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends TypeAxiomConverter<S, I> {

		ClassLinkConverter() {

			super(parentConverter);
		}

		I checkConvert(S source) {

			OwlClassLink owlLink = createOwlLink(source);

			return owlLink.checkValidEndPoints() ? createInputAxiom(owlLink) : null;
		}

		abstract OwlClassLink createOwlLink(S source);

		abstract I createInputAxiom(OwlClassLink owlLink);
	}

	private class ClassEquivalenceConverter
						extends
							ClassLinkConverter
								<OWLEquivalentClassesAxiom, InputClassEquivalence> {

		Class<OWLEquivalentClassesAxiom> getSourceAxiomType() {

			return OWLEquivalentClassesAxiom.class;
		}

		Collection<OWLEquivalentClassesAxiom> resolveSourceAxiom(OWLEquivalentClassesAxiom ax) {

			return new ClassEquivalenceSplitter().resolve(ax);
		}

		OwlClassLink createOwlLink(OWLEquivalentClassesAxiom source) {

			 return new OwlClassLink(source);
		}

		InputClassEquivalence createInputAxiom(OwlClassLink owlLink) {

			return new ConvertedClassEquivalence(
							owlLink.source,
							owlLink.firstOrSubAsInputNode(),
							owlLink.secondOrSupAsInputNode());
		}
	}

	private class ClassSubSuperConverter
						extends
							ClassLinkConverter
								<OWLSubClassOfAxiom, InputClassSubSuper> {

		Class<OWLSubClassOfAxiom> getSourceAxiomType() {

			return OWLSubClassOfAxiom.class;
		}

		Collection<OWLSubClassOfAxiom> resolveSourceAxiom(OWLSubClassOfAxiom ax) {

			return new ClassSubSuperSplitter().resolve(ax);
		}

		OwlClassLink createOwlLink(OWLSubClassOfAxiom source) {

			 return new OwlClassLink(source);
		}

		InputClassSubSuper createInputAxiom(OwlClassLink owlLink) {

			return new ConvertedClassSubSuper(
							owlLink.source,
							owlLink.firstOrSubAsInputNode(),
							owlLink.secondOrSupAsInputNode());
		}
	}

	ClassAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);

		owlNothing = factory.getOWLNothing();
	}

	Iterable<InputClassEquivalence> getClassEquivalences() {

		return equivalenceConverter;
	}

	Iterable<InputClassSubSuper> getClassSubSupers() {

		return subSuperConverter;
	}
}
