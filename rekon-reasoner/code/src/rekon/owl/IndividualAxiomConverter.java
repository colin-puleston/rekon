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
						extends ConvertedEquivalence<IndividualNode>
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

	private class ConvertedIndividualType
						extends ConvertedIndividualAttribute
						implements InputIndividualType {

		private InputNode type;

		public InputNode getType() {

			return type;
		}

		ConvertedIndividualType(
			OWLAxiom source,
			IndividualNode individual,
			InputNode type) {

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

		boolean validEndPoint(OWLIndividual expr) {

			return expr instanceof OWLNamedIndividual;
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

		boolean convert(OWLSameIndividualAxiom source) {

			OwlIndividualLink owlLink = new OwlIndividualLink(source);

			if (owlLink.checkValidEndPoints()) {

				inputAxioms.add(createInputAxiom(owlLink));

				return true;
			}

			return false;
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

		boolean convert(S source) {

			OWLIndividual expr = getIndividualExpr(source);
			IndividualNode n = toIndividualNode(expr);

			if (n != null) {

				inputAxioms.add(createInputAxiom(n, source));

				return true;
			}

			return false;
		}

		abstract OWLIndividual getIndividualExpr(S source);

		abstract I createInputAxiom(IndividualNode node, S source);
	}

	private class IndividualTypeConverter
						extends
							IndividualAttributeConverter
								<OWLClassAssertionAxiom, InputIndividualType> {

		Class<OWLClassAssertionAxiom> getSourceAxiomType() {

			return OWLClassAssertionAxiom.class;
		}

		OWLIndividual getIndividualExpr(OWLClassAssertionAxiom source) {

			return source.getIndividual();
		}

		InputIndividualType createInputAxiom(
								IndividualNode node,
								OWLClassAssertionAxiom source) {

			OWLClassExpression typeExpr = source.getClassExpression();
			InputNode type = expressions.toAxiomNode(source, typeExpr);

			return new ConvertedIndividualType(source, node, type);
		}
	}

	private abstract class IndividualRelationConverter
								<S extends OWLPropertyAssertionAxiom<?, ?>>
								extends
									IndividualAttributeConverter<S, InputIndividualRelation> {

		OWLIndividual getIndividualExpr(S source) {

			return source.getSubject();
		}

		InputIndividualRelation createInputAxiom(IndividualNode node, S source) {

			return new ConvertedIndividualRelation(source, node, toRelation(source));
		}

		abstract OWLRestriction toOwlRestriction(S source);

		private InputRelation toRelation(S source) {

			return expressions.toAxiomRelation(source, toOwlRestriction(source));
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
		new IndividualTypeConverter();
		new IndividualObjectRelationConverter();
		new IndividualDataRelationConverter();
	}

	Iterable<InputIndividualEquivalence> getIndividualEquivalences() {

		return getInputAxioms(IndividualEquivalenceConverter.class);
	}

	Iterable<InputIndividualType> getIndividualTypes() {

		return getInputAxioms(IndividualTypeConverter.class);
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
