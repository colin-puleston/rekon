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

/**
 * @author Colin Puleston
 */
class ClassExprAxiomConverter extends CategoryAxiomConverter {

	private class ConvertedClassEquivalence
						extends ConvertedNameEquivalence<ClassNode>
						implements InputClassEquivalence {

		ConvertedClassEquivalence(OWLAxiom source, ClassNode first, ClassNode second) {

			super(source, first, second);
		}
	}

	private class ConvertedClassComplexEquivalence
						extends ConvertedEquivalence<ClassNode, InputComplex>
						implements InputClassComplexEquivalence {

		ConvertedClassComplexEquivalence(
			OWLAxiom source,
			ClassNode first,
			InputComplex second) {

			super(source, first, second);
		}
	}

	private class ConvertedComplexEquivalence
						extends ConvertedEquivalence<InputComplex, InputComplex>
						implements InputComplexEquivalence {

		ConvertedComplexEquivalence(
			OWLAxiom source,
			InputComplex first,
			InputComplex second) {

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
						extends ConvertedSubSuper<InputComplex, ClassNode>
						implements InputComplexSubClassSuper {

		ConvertedComplexSubClassSuper(OWLAxiom source, InputComplex sub, ClassNode sup) {

			super(source, sub, sup);
		}
	}

	private class ConvertedComplexSubSuper
						extends ConvertedSubSuper<InputComplex, InputComplexSuper>
						implements InputComplexSubSuper {

		ConvertedComplexSubSuper(OWLAxiom source, InputComplex sub, InputComplexSuper sup) {

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

		InputComplex firstOrSubAsComplex() {

			return expressions.toComplex(firstOrSub);
		}

		InputComplex secondAsComplex() {

			return expressions.toComplex(secondOrSup);
		}

		InputComplexSuper supAsComplexSuper() {

			return expressions.toComplexSuper(secondOrSup);
		}

		Class<OWLClass> getNameExprType() {

			return OWLClass.class;
		}

		ClassNode asName(OWLClassExpression e) {

			return names.get((OWLClass)e);
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

	private class ClassExprSubSuperSplitter extends TypeAxiomResolver<OWLSubClassOfAxiom> {

		Class<OWLSubClassOfAxiom> getAxiomType() {

			return OWLSubClassOfAxiom.class;
		}

		Collection<? extends OWLAxiom> resolveAxiomOfType(OWLSubClassOfAxiom axiom) {

			OWLClassExpression sup = axiom.getSuperClass();

			if (sup instanceof OWLObjectIntersectionOf) {

				OWLClassExpression sub = axiom.getSubClass();

				return createComponentAxioms((OWLObjectIntersectionOf)sup, sub);
			}

			return Collections.singleton(axiom);
		}

		private Collection<OWLAxiom> createComponentAxioms(
										OWLObjectIntersectionOf sup,
										OWLClassExpression sub) {

			List<OWLAxiom> components = new ArrayList<OWLAxiom>();

			for (OWLClassExpression compSup : sup.getOperands()) {

				components.add(factory.getOWLSubClassOfAxiom(sub, compSup));
			}

			return components;
		}
	}

	private abstract class ClassExprLinkConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends TypeAxiomConverter<S, I> {

		boolean convertAxiomOfType(S source) {

			OwlClassExprLink owlLink = createOwlLink(source);

			if (convertsSourceAxiom(owlLink)) {

				inputAxioms.add(createInputAxiom(owlLink));

				return true;
			}

			return false;
		}

		abstract OwlClassExprLink createOwlLink(S source);

		abstract boolean convertsSourceAxiom(OwlClassExprLink owlLink);

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

		boolean convertsSourceAxiom(OwlClassExprLink owlLink) {

			return owlLink.matches(true, true);
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

		boolean convertsSourceAxiom(OwlClassExprLink owlLink) {

			return owlLink.matches(true, false) || owlLink.matches(true, false);
		}

		InputClassComplexEquivalence createInputAxiom(OwlClassExprLink owlLink) {

			return new ConvertedClassComplexEquivalence(
							owlLink.source,
							getClassNodeEquiv(owlLink),
							getComplexEquiv(owlLink));
		}

		private ClassNode getClassNodeEquiv(OwlClassExprLink owlLink) {

			return owlLink.matches(true, false)
					? owlLink.firstOrSubAsName()
					: owlLink.secondOrSupAsName();
		}

		private InputComplex getComplexEquiv(OwlClassExprLink owlLink) {

			return owlLink.matches(false, true)
					? owlLink.firstOrSubAsComplex()
					: owlLink.secondAsComplex();
		}
	}

	private class ComplexEquivalenceConverter
					extends
						ClassExprEquivalenceConverter<InputComplexEquivalence> {

		boolean convertsSourceAxiom(OwlClassExprLink owlLink) {

			return owlLink.matches(false, false);
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

		boolean convertsSourceAxiom(OwlClassExprLink owlLink) {

			return owlLink.matches(true, true);
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

		boolean convertsSourceAxiom(OwlClassExprLink owlLink) {

			return owlLink.matches(true, false);
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

		boolean convertsSourceAxiom(OwlClassExprLink owlLink) {

			return owlLink.matches(false, true);
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

		boolean convertsSourceAxiom(OwlClassExprLink owlLink) {

			return owlLink.matches(false, false);
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

		new ClassExprEquivalenceSplitter();
		new ClassExprSubSuperSplitter();

		new ClassEquivalenceConverter();
		new ClassComplexEquivalenceConverter();
		new ComplexEquivalenceConverter();
		new ClassSubSuperConverter();
		new ClassSubComplexSuperConverter();
		new ComplexSubClassSuperConverter();
		new ComplexSubSuperConverter();
	}

	Collection<InputClassEquivalence> getClassEquivalences() {

		return getInputAxioms(ClassEquivalenceConverter.class);
	}

	Collection<InputClassComplexEquivalence> getClassComplexEquivalences() {

		return getInputAxioms(ClassComplexEquivalenceConverter.class);
	}

	Collection<InputComplexEquivalence> getComplexEquivalences() {

		return getInputAxioms(ComplexEquivalenceConverter.class);
	}

	Collection<InputClassSubSuper> getClassSubSupers() {

		return getInputAxioms(ClassSubSuperConverter.class);
	}

	Collection<InputClassSubComplexSuper> getClassSubComplexSupers() {

		return getInputAxioms(ClassSubComplexSuperConverter.class);
	}

	Collection<InputComplexSubClassSuper> getComplexSubClassSupers() {

		return getInputAxioms(ComplexSubClassSuperConverter.class);
	}

	Collection<InputComplexSubSuper> getComplexSubSupers() {

		return getInputAxioms(ComplexSubSuperConverter.class);
	}
}
