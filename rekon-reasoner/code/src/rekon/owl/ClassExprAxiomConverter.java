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
import rekon.build.input.*;
import rekon.util.*;

/**
 * @author Colin Puleston
 */
class ClassExprAxiomConverter extends CategoryAxiomConverter {

	private OWLClass owlNothing;

	private class ConvertedClassEquivalence
						extends ConvertedNameEquivalence<ClassNode>
						implements InputClassEquivalence {

		ConvertedClassEquivalence(OWLAxiom source, ClassNode first, ClassNode second) {

			super(source, first, second);
		}
	}

	private class ConvertedClassComplexEquivalence
						extends ConvertedEquivalence<ClassNode, InputComplexNode>
						implements InputClassComplexEquivalence {

		ConvertedClassComplexEquivalence(
			OWLAxiom source,
			ClassNode first,
			InputComplexNode second) {

			super(source, first, second);
		}
	}

	private class ConvertedComplexEquivalence
						extends ConvertedEquivalence<InputComplexNode, InputComplexNode>
						implements InputComplexEquivalence {

		ConvertedComplexEquivalence(
			OWLAxiom source,
			InputComplexNode first,
			InputComplexNode second) {

			super(source, first, second);
		}
	}

	private class ConvertedClassSubSuper
						extends ConvertedNameSubSuper<ClassNode>
						implements InputClassSubSuper {

		ConvertedClassSubSuper(OWLAxiom source, ClassNode sub, ClassNode sup) {

			super(source, sub, sup);
		}
	}

	private class ConvertedClassSubComplexSuper
						extends ConvertedSubSuper<ClassNode, InputComplexSuper>
						implements InputClassSubComplexSuper {

		ConvertedClassSubComplexSuper(OWLAxiom source, ClassNode sub, InputComplexSuper sup) {

			super(source, sub, sup);
		}
	}

	private class ConvertedComplexSubClassSuper
						extends ConvertedSubSuper<InputComplexNode, ClassNode>
						implements InputComplexSubClassSuper {

		ConvertedComplexSubClassSuper(OWLAxiom source, InputComplexNode sub, ClassNode sup) {

			super(source, sub, sup);
		}
	}

	private class ConvertedComplexSubSuper
						extends ConvertedSubSuper<InputComplexNode, InputComplexSuper>
						implements InputComplexSubSuper {

		ConvertedComplexSubSuper(OWLAxiom source, InputComplexNode sub, InputComplexSuper sup) {

			super(source, sub, sup);
		}
	}

	private class OwlClassExprLink extends OwlLink<OWLClassExpression, ClassNode> {

		OwlClassExprLink(OWLEquivalentClassesAxiom source) {

			super(source, source.getClassExpressions());
		}

		OwlClassExprLink(OWLSubClassOfAxiom source) {

			super(source, source.getSubClass(), source.getSuperClass());
		}

		InputComplexNode firstOrSubAsComplex() {

			return expressions.toComplexNode(getSourceAxiom(), firstOrSub);
		}

		InputComplexNode secondAsComplex() {

			return expressions.toComplexNode(getSourceAxiom(), secondOrSup);
		}

		InputComplexSuper supAsComplexSuper() {

			return expressions.toComplexSuper(getSourceAxiom(), secondOrSup);
		}

		OwlLinkStatus checkMatch(OWLClassExpression expr, boolean isName) {

			if (expr instanceof OWLClass == isName) {

				if (isName && expr.equals(owlNothing)) {

					return OwlLinkStatus.INVALID_MATCH;
				}

				return OwlLinkStatus.VALID_MATCH;
			}

			return OwlLinkStatus.NON_MATCH;
		}

		ClassNode asName(OWLClassExpression expr) {

			return names.resolve((OWLClass)expr);
		}
	}

	private class ClassExprEquivalenceSplitter
						extends
							TypeAxiomResolver<OWLEquivalentClassesAxiom> {

		Class<OWLEquivalentClassesAxiom> getAxiomType() {

			return OWLEquivalentClassesAxiom.class;
		}

		Collection<? extends OWLAxiom> resolveAxiomOfType(OWLEquivalentClassesAxiom axiom) {

			return axiom.asPairwiseAxioms();
		}
	}

	private class ClassExprSubSuperSplitter
					extends
						SubSuperSplitter<OWLSubClassOfAxiom, OWLClassExpression> {

		Class<OWLSubClassOfAxiom> getAxiomType() {

			return OWLSubClassOfAxiom.class;
		}

		OWLClassExpression getSuper(OWLSubClassOfAxiom axiom) {

			return axiom.getSuperClass();
		}

		OWLClassExpression getSub(OWLSubClassOfAxiom axiom) {

			return axiom.getSubClass();
		}

		OWLAxiom createComponentAxiom(OWLClassExpression sup, OWLClassExpression sub) {

			return factory.getOWLSubClassOfAxiom(sub, sup);
		}
	}

	private abstract class ClassExprLinkConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends TypeAxiomConverter<S, I> {

		boolean convertAxiomOfType(S source) {

			OwlClassExprLink owlLink = createOwlLink(source);

			switch (owlLink.checkMatch(firstOrSubIsName(), secondOrSupIsName())) {

				case VALID_MATCH:

					inputAxioms.add(createInputAxiom(owlLink));
					break;

				case NON_MATCH:

					return false;
			}

			return true;
		}

		abstract OwlClassExprLink createOwlLink(S source);

		abstract boolean firstOrSubIsName();

		abstract boolean secondOrSupIsName();

		abstract I createInputAxiom(OwlClassExprLink owlLink);
	}

	private abstract class ClassExprEquivalenceConverter
								<I extends InputAxiom>
								extends
									ClassExprLinkConverter<OWLEquivalentClassesAxiom, I> {

		Class<OWLEquivalentClassesAxiom> getSourceAxiomType() {

			return OWLEquivalentClassesAxiom.class;
		}

		OwlClassExprLink createOwlLink(OWLEquivalentClassesAxiom source) {

			 return new OwlClassExprLink(source);
		}
	}

	private class ClassEquivalenceConverter
					extends
						ClassExprEquivalenceConverter<InputClassEquivalence> {

		boolean firstOrSubIsName() {

			return true;
		}

		boolean secondOrSupIsName() {

			return true;
		}

		InputClassEquivalence createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedClassEquivalence(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private class ClassComplexEquivalenceConverter
					extends
						ClassExprEquivalenceConverter<InputClassComplexEquivalence> {

		boolean firstOrSubIsName() {

			return true;
		}

		boolean secondOrSupIsName() {

			return false;
		}

		InputClassComplexEquivalence createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedClassComplexEquivalence(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondAsComplex());
		}
	}

	private class ComplexClassEquivalenceConverter
					extends
						ClassExprEquivalenceConverter<InputClassComplexEquivalence> {

		boolean firstOrSubIsName() {

			return false;
		}

		boolean secondOrSupIsName() {

			return true;
		}

		InputClassComplexEquivalence createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedClassComplexEquivalence(
							owlLink.source,
							owlLink.secondOrSupAsName(),
							owlLink.firstOrSubAsComplex());
		}
	}

	private class ComplexEquivalenceConverter
					extends
						ClassExprEquivalenceConverter<InputComplexEquivalence> {

		boolean firstOrSubIsName() {

			return false;
		}

		boolean secondOrSupIsName() {

			return false;
		}

		InputComplexEquivalence createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedComplexEquivalence(
							owlLink.source,
							owlLink.firstOrSubAsComplex(),
							owlLink.secondAsComplex());
		}
	}

	private abstract class ClassExprSubSuperConverter
								<I extends InputAxiom>
								extends
									ClassExprLinkConverter<OWLSubClassOfAxiom, I> {

		Class<OWLSubClassOfAxiom> getSourceAxiomType() {

			return OWLSubClassOfAxiom.class;
		}

		OwlClassExprLink createOwlLink(OWLSubClassOfAxiom source) {

			 return new OwlClassExprLink(source);
		}
	}

	private class ClassSubSuperConverter
					extends
						ClassExprSubSuperConverter<InputClassSubSuper> {

		boolean firstOrSubIsName() {

			return true;
		}

		boolean secondOrSupIsName() {

			return true;
		}

		InputClassSubSuper createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedClassSubSuper(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private class ClassSubComplexSuperConverter
					extends
						ClassExprSubSuperConverter<InputClassSubComplexSuper> {

		boolean firstOrSubIsName() {

			return true;
		}

		boolean secondOrSupIsName() {

			return false;
		}

		InputClassSubComplexSuper createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedClassSubComplexSuper(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.supAsComplexSuper());
		}
	}

	private class ComplexSubClassSuperConverter
					extends
						ClassExprSubSuperConverter<InputComplexSubClassSuper> {

		boolean firstOrSubIsName() {

			return false;
		}

		boolean secondOrSupIsName() {

			return true;
		}

		InputComplexSubClassSuper createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedComplexSubClassSuper(
							owlLink.source,
							owlLink.firstOrSubAsComplex(),
							owlLink.secondOrSupAsName());
		}
	}

	private class ComplexSubSuperConverter
					extends
						ClassExprSubSuperConverter<InputComplexSubSuper> {

		boolean firstOrSubIsName() {

			return false;
		}

		boolean secondOrSupIsName() {

			return false;
		}

		InputComplexSubSuper createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedComplexSubSuper(
							owlLink.source,
							owlLink.firstOrSubAsComplex(),
							owlLink.supAsComplexSuper());
		}
	}

	ClassExprAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);

		owlNothing = factory.getOWLNothing();

		new ClassExprEquivalenceSplitter();
		new ClassExprSubSuperSplitter();

		new ClassEquivalenceConverter();
		new ClassComplexEquivalenceConverter();
		new ComplexClassEquivalenceConverter();
		new ComplexEquivalenceConverter();
		new ClassSubSuperConverter();
		new ClassSubComplexSuperConverter();
		new ComplexSubClassSuperConverter();
		new ComplexSubSuperConverter();
	}

	Iterable<InputClassEquivalence> getClassEquivalences() {

		return getInputAxioms(ClassEquivalenceConverter.class);
	}

	Iterable<InputClassComplexEquivalence> getClassComplexEquivalences() {

		CompoundIterable<InputClassComplexEquivalence> all
			= new CompoundIterable<InputClassComplexEquivalence>();

		all.addComponent(getInputAxioms(ClassComplexEquivalenceConverter.class));
		all.addComponent(getInputAxioms(ComplexClassEquivalenceConverter.class));

		return all;
	}

	Iterable<InputComplexEquivalence> getComplexEquivalences() {

		return getInputAxioms(ComplexEquivalenceConverter.class);
	}

	Iterable<InputClassSubSuper> getClassSubSupers() {

		return getInputAxioms(ClassSubSuperConverter.class);
	}

	Iterable<InputClassSubComplexSuper> getClassSubComplexSupers() {

		return getInputAxioms(ClassSubComplexSuperConverter.class);
	}

	Iterable<InputComplexSubClassSuper> getComplexSubClassSupers() {

		return getInputAxioms(ComplexSubClassSuperConverter.class);
	}

	Iterable<InputComplexSubSuper> getComplexSubSupers() {

		return getInputAxioms(ComplexSubSuperConverter.class);
	}
}
