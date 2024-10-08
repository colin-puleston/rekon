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
class IndividualAxiomConverter extends CategoryAxiomConverter {

	private class ConvertedIndividualEquivalence
						extends ConvertedNameEquivalence<IndividualNode>
						implements InputIndividualEquivalence {

		ConvertedIndividualEquivalence(
			OWLAxiom source,
			IndividualNode first,
			IndividualNode second) {

			super(source, first, second);
		}
	}

	private abstract class ConvertedIndividualAttribute
								extends ConvertedAxiom
								implements InputIndividualAttribute {

		private IndividualNode individual;

		public IndividualNode getIndividual() {

			return individual;
		}

		ConvertedIndividualAttribute(OWLAxiom source, IndividualNode individual) {

			super(source);

			this.individual = individual;
		}
	}

	private class ConvertedIndividualClassType
						extends ConvertedIndividualAttribute
						implements InputIndividualClassType {

		private ClassNode type;

		public ClassNode getClassType() {

			return type;
		}

		ConvertedIndividualClassType(
			OWLAxiom source,
			IndividualNode individual,
			ClassNode type) {

			super(source, individual);

			this.type = type;
		}
	}

	private class ConvertedIndividualComplexType
						extends ConvertedIndividualAttribute
						implements InputIndividualComplexType {

		private InputComplexSuper type;

		public InputComplexSuper getType() {

			return type;
		}

		ConvertedIndividualComplexType(
			OWLAxiom source,
			IndividualNode individual,
			InputComplexSuper type) {

			super(source, individual);

			this.type = type;
		}
	}

	private class ConvertedIndividualRelation
						extends ConvertedIndividualAttribute
						implements InputIndividualRelation {

		private InputRelation relation;

		public InputRelation getRelation() {

			return relation;
		}

		ConvertedIndividualRelation(
			OWLAxiom source,
			IndividualNode individual,
			InputRelation relation) {

			super(source, individual);

			this.relation = relation;
		}
	}

	private class OwlIndividualLink extends OwlLink<OWLIndividual, IndividualNode> {

		OwlIndividualLink(OWLSameIndividualAxiom source) {

			super(source, source.getIndividuals());
		}

		OwlLinkStatus checkMatch(OWLIndividual expr, boolean isName) {

			if (expr instanceof OWLNamedIndividual) {

				return OwlLinkStatus.VALID_MATCH;
			}

			return OwlLinkStatus.INVALID_MATCH;
		}

		IndividualNode asName(OWLIndividual expr) {

			return names.resolve((OWLNamedIndividual)expr);
		}
	}

	private class IndividualEquivalenceSplitter
						extends
							TypeAxiomResolver<OWLSameIndividualAxiom> {

		Class<OWLSameIndividualAxiom> getAxiomType() {

			return OWLSameIndividualAxiom.class;
		}

		Collection<? extends OWLAxiom> resolveAxiomOfType(OWLSameIndividualAxiom axiom) {

			return axiom.asPairwiseAxioms();
		}
	}

	private class IndividualTypeSplitter
					extends
						SubSuperSplitter<OWLClassAssertionAxiom, OWLIndividual> {

		Class<OWLClassAssertionAxiom> getAxiomType() {

			return OWLClassAssertionAxiom.class;
		}

		OWLClassExpression getSuper(OWLClassAssertionAxiom axiom) {

			return axiom.getClassExpression();
		}

		OWLIndividual getSub(OWLClassAssertionAxiom axiom) {

			return axiom.getIndividual();
		}

		OWLAxiom createComponentAxiom(OWLClassExpression sup, OWLIndividual sub) {

			return factory.getOWLClassAssertionAxiom(sup, sub);
		}
	}

	private class IndividualEquivalenceConverter
					extends
						TypeAxiomConverter
							<OWLSameIndividualAxiom,
							InputIndividualEquivalence> {

		Class<OWLSameIndividualAxiom> getSourceAxiomType() {

			return OWLSameIndividualAxiom.class;
		}

		boolean convertAxiomOfType(OWLSameIndividualAxiom source) {

			OwlIndividualLink owlLink = new OwlIndividualLink(source);

			if (owlLink.checkMatch(true, true) == OwlLinkStatus.VALID_MATCH) {

				inputAxioms.add(createInputAxiom(owlLink));
			}

			return true;
		}

		private InputIndividualEquivalence createInputAxiom(OwlIndividualLink owlLink) {

			return new ConvertedIndividualEquivalence(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private abstract class IndividualAttributeConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends TypeAxiomConverter<S, I> {

		boolean convertAxiomOfType(S source) {

			if (convertsSourceAxiom(source)) {

				OWLIndividual expr = getIndividualExpr(source);
				IndividualNode n = toIndividualNode(expr);

				if (n != null) {

					inputAxioms.add(createInputAxiom(n, source));
				}
				else {

					logOutOfScopeAxiom(source, expr);
				}

				return true;
			}

			return false;
		}

		abstract boolean convertsSourceAxiom(S source);

		abstract OWLIndividual getIndividualExpr(S source);

		abstract I createInputAxiom(IndividualNode node, S source);
	}

	private abstract class IndividualTypeConverter
								<I extends InputAxiom>
								extends
									IndividualAttributeConverter<OWLClassAssertionAxiom, I> {

		Class<OWLClassAssertionAxiom> getSourceAxiomType() {

			return OWLClassAssertionAxiom.class;
		}

		boolean convertsSourceAxiom(OWLClassAssertionAxiom source) {

			return namedType(source) == convertsNamedTypes();
		}

		OWLIndividual getIndividualExpr(OWLClassAssertionAxiom source) {

			return source.getIndividual();
		}

		I createInputAxiom(IndividualNode node, OWLClassAssertionAxiom source) {

			return createInputAxiom(node, source.getClassExpression(), source);
		}

		abstract boolean convertsNamedTypes();

		abstract I createInputAxiom(
						IndividualNode node,
						OWLClassExpression typeExpr,
						OWLClassAssertionAxiom source);

		private boolean namedType(OWLClassAssertionAxiom source) {

			return source.getClassExpression() instanceof OWLClass;
		}
	}

	private class IndividualClassTypeConverter
						extends
							IndividualTypeConverter<InputIndividualClassType> {

		boolean convertsNamedTypes() {

			return true;
		}

		InputIndividualClassType createInputAxiom(
										IndividualNode node,
										OWLClassExpression typeExpr,
										OWLClassAssertionAxiom source) {

			ClassNode type = names.resolve((OWLClass)typeExpr);

			return new ConvertedIndividualClassType(source, node, type);
		}
	}

	private class IndividualComplexTypeConverter
						extends
							IndividualTypeConverter<InputIndividualComplexType> {

		boolean convertsNamedTypes() {

			return false;
		}

		InputIndividualComplexType createInputAxiom(
										IndividualNode node,
										OWLClassExpression typeExpr,
										OWLClassAssertionAxiom source) {

			InputComplexSuper type = expressions.toComplexSuper(source, typeExpr);

			return new ConvertedIndividualComplexType(source, node, type);
		}
	}

	private abstract class IndividualRelationConverter
								<S extends OWLPropertyAssertionAxiom<?, ?>>
								extends
									IndividualAttributeConverter<S, InputIndividualRelation> {

		boolean convertsSourceAxiom(S source) {

			return true;
		}

		OWLIndividual getIndividualExpr(S source) {

			return source.getSubject();
		}

		InputIndividualRelation createInputAxiom(IndividualNode node, S source) {

			return new ConvertedIndividualRelation(source, node, toRelation(source));
		}

		abstract OWLRestriction toOwlRestriction(S source);

		private InputRelation toRelation(S source) {

			return expressions.toRelation(source, toOwlRestriction(source));
		}
	}

	private class IndividualObjectRelationConverter
						extends
							IndividualRelationConverter<OWLObjectPropertyAssertionAxiom> {

		Class<OWLObjectPropertyAssertionAxiom> getSourceAxiomType() {

			return OWLObjectPropertyAssertionAxiom.class;
		}

		OWLRestriction toOwlRestriction(OWLObjectPropertyAssertionAxiom source) {

			return factory.getOWLObjectHasValue(source.getProperty(), source.getObject());
		}
	}

	private class IndividualDataRelationConverter
						extends
							IndividualRelationConverter<OWLDataPropertyAssertionAxiom> {

		Class<OWLDataPropertyAssertionAxiom> getSourceAxiomType() {

			return OWLDataPropertyAssertionAxiom.class;
		}

		OWLRestriction toOwlRestriction(OWLDataPropertyAssertionAxiom source) {

			return factory.getOWLDataHasValue(source.getProperty(), source.getObject());
		}
	}

	IndividualAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);

		new IndividualEquivalenceSplitter();
		new IndividualTypeSplitter();

		new IndividualEquivalenceConverter();
		new IndividualClassTypeConverter();
		new IndividualComplexTypeConverter();
		new IndividualObjectRelationConverter();
		new IndividualDataRelationConverter();
	}

	Iterable<InputIndividualEquivalence> getIndividualEquivalences() {

		return getInputAxioms(IndividualEquivalenceConverter.class);
	}

	Iterable<InputIndividualClassType> getIndividualClassTypes() {

		return getInputAxioms(IndividualClassTypeConverter.class);
	}

	Iterable<InputIndividualComplexType> getIndividualComplexTypes() {

		return getInputAxioms(IndividualComplexTypeConverter.class);
	}

	Iterable<InputIndividualRelation> getIndividualRelations() {

		CompoundIterable<InputIndividualRelation> all
			= new CompoundIterable<InputIndividualRelation>();

		all.addComponent(getInputAxioms(IndividualObjectRelationConverter.class));
		all.addComponent(getInputAxioms(IndividualDataRelationConverter.class));

		return all;
	}

	private IndividualNode toIndividualNode(OWLIndividual e) {

		return e instanceof OWLNamedIndividual ? names.resolve((OWLNamedIndividual)e) : null;
	}
}
