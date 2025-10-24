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
class NodePropertyAxiomConverter
		extends
			PropertyAxiomConverter<OWLObjectPropertyExpression, NodeProperty> {

	private OWLObjectProperty owlBottomObjectProperty;

	private NodePropertyEquivalenceConverter equivalenceConverter = new NodePropertyEquivalenceConverter();
	private NodePropertySubSuperConverter subSuperConverter = new NodePropertySubSuperConverter();
	private NodePropertyInverseConverter inverseConverter = new NodePropertyInverseConverter();
	private NodePropertyChainConverter chainConverter = new NodePropertyChainConverter();
	private NodePropertySymmetricConverter symmetricConverter = new NodePropertySymmetricConverter();
	private NodePropertyTransitiveConverter transitiveConverter = new NodePropertyTransitiveConverter();

	private class ConvertedNodePropertyEquivalence
						extends ConvertedEquivalence<NodeProperty>
						implements InputNodePropertyEquivalence {

		ConvertedNodePropertyEquivalence(
			OWLAxiom source,
			NodeProperty first,
			NodeProperty second) {

			super(source, first, second);
		}
	}

	private class ConvertedNodePropertySubSuper
						extends ConvertedSubSuper<NodeProperty>
						implements InputNodePropertySubSuper {

		ConvertedNodePropertySubSuper(
			OWLAxiom source,
			NodeProperty sub,
			NodeProperty sup) {

			super(source, sub, sup);
		}
	}

	private class ConvertedNodePropertyInverse
						extends ConvertedPropertyAttribute<NodeProperty>
						implements InputNodePropertyInverse {

		private NodeProperty inverse;

		public NodeProperty getInverse() {

			return inverse;
		}

		ConvertedNodePropertyInverse(
			OWLAxiom source,
			NodeProperty property,
			NodeProperty inverse) {

			super(source, property);

			this.inverse = inverse;
		}
	}

	private class ConvertedNodePropertyChain
						extends ConvertedPropertyAttribute<NodeProperty>
						implements InputNodePropertyChain {

		private List<NodeProperty> chain;

		public List<NodeProperty> getChain() {

			return chain;
		}

		ConvertedNodePropertyChain(
			OWLAxiom source,
			NodeProperty property,
			List<NodeProperty> chain) {

			super(source, property);

			this.chain = chain;

			expressions.addChainedProperties(chain);
		}
	}

	private class ConvertedNodePropertySymmetric
						extends ConvertedPropertyAttribute<NodeProperty>
						implements InputNodePropertySymmetric {

		ConvertedNodePropertySymmetric(OWLAxiom source, NodeProperty property) {

			super(source, property);
		}
	}

	private class ConvertedNodePropertyTransitive
						extends ConvertedPropertyAttribute<NodeProperty>
						implements InputNodePropertyTransitive {

		ConvertedNodePropertyTransitive(OWLAxiom source, NodeProperty property) {

			super(source, property);

			expressions.addChainedProperty(property);
		}
	}

	private class NodePropertyEquivalenceSplitter
						extends
							EquivalenceSplitter<OWLEquivalentObjectPropertiesAxiom> {

		Class<OWLEquivalentObjectPropertiesAxiom> getAxiomType() {

			return OWLEquivalentObjectPropertiesAxiom.class;
		}

		Collection<? extends OWLAxiom> asPairwiseAxioms(OWLEquivalentObjectPropertiesAxiom axiom) {

			return axiom.asPairwiseAxioms();
		}
	}

	private class NodePropertyEquivalenceConverter
					extends
						PropertyLinkConverter
							<OWLEquivalentObjectPropertiesAxiom,
							InputNodePropertyEquivalence> {

		Class<OWLEquivalentObjectPropertiesAxiom> getSourceAxiomType() {

			return OWLEquivalentObjectPropertiesAxiom.class;
		}

		Collection<OWLEquivalentObjectPropertiesAxiom> resolveSourceAxiom(
														OWLEquivalentObjectPropertiesAxiom ax) {

			return new NodePropertyEquivalenceSplitter().resolve(ax);
		}

		OwlPropertyLink createOwlLink(OWLEquivalentObjectPropertiesAxiom source) {

			return new OwlPropertyLink(source, source.getProperties());
		}

		InputNodePropertyEquivalence createInputAxiom(OwlPropertyLink owlLink) {

			return new ConvertedNodePropertyEquivalence(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private class NodePropertySubSuperConverter
					extends
						PropertyLinkConverter
							<OWLSubObjectPropertyOfAxiom,
							InputNodePropertySubSuper> {

		Class<OWLSubObjectPropertyOfAxiom> getSourceAxiomType() {

			return OWLSubObjectPropertyOfAxiom.class;
		}

		OwlPropertyLink createOwlLink(OWLSubObjectPropertyOfAxiom source) {

			return new OwlPropertyLink(
							source,
							source.getSubProperty(),
							source.getSuperProperty());
		}

		InputNodePropertySubSuper createInputAxiom(OwlPropertyLink owlLink) {

			return new ConvertedNodePropertySubSuper(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private class NodePropertyInverseConverter
					extends
						PropertyLinkConverter
							<OWLInverseObjectPropertiesAxiom,
							InputNodePropertyInverse> {

		Class<OWLInverseObjectPropertiesAxiom> getSourceAxiomType() {

			return OWLInverseObjectPropertiesAxiom.class;
		}

		OwlPropertyLink createOwlLink(OWLInverseObjectPropertiesAxiom source) {

			return new OwlPropertyLink(
							source,
							source.getFirstProperty(),
							source.getSecondProperty());
		}

		InputNodePropertyInverse createInputAxiom(OwlPropertyLink owlLink) {

			return new ConvertedNodePropertyInverse(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private class NodePropertyChainConverter
					extends
						PropertyAttributeConverter
							<OWLSubPropertyChainOfAxiom,
							InputNodePropertyChain> {

		Class<OWLSubPropertyChainOfAxiom> getSourceAxiomType() {

			return OWLSubPropertyChainOfAxiom.class;
		}

		InputNodePropertyChain createInputAxiom(
									NodeProperty prop,
									OWLSubPropertyChainOfAxiom source) {

			List<NodeProperty> c = toPropertiesOrNull(source.getPropertyChain());

			return c != null ? new ConvertedNodePropertyChain(source, prop, c) : null;
		}

		OWLObjectPropertyExpression getPropertyExpr(OWLSubPropertyChainOfAxiom source) {

			return source.getSuperProperty();
		}
	}

	private class NodePropertySymmetricConverter
					extends
						PropertyAttributeConverter
							<OWLSymmetricObjectPropertyAxiom,
							InputNodePropertySymmetric> {

		Class<OWLSymmetricObjectPropertyAxiom> getSourceAxiomType() {

			return OWLSymmetricObjectPropertyAxiom.class;
		}

		OWLObjectPropertyExpression getPropertyExpr(OWLSymmetricObjectPropertyAxiom source) {

			return source.getProperty();
		}

		InputNodePropertySymmetric createInputAxiom(
										NodeProperty prop,
										OWLSymmetricObjectPropertyAxiom source) {

			return new ConvertedNodePropertySymmetric(source, prop);
		}
	}

	private class NodePropertyTransitiveConverter
					extends
						PropertyAttributeConverter
							<OWLTransitiveObjectPropertyAxiom,
							InputNodePropertyTransitive> {

		Class<OWLTransitiveObjectPropertyAxiom> getSourceAxiomType() {

			return OWLTransitiveObjectPropertyAxiom.class;
		}

		OWLObjectPropertyExpression getPropertyExpr(OWLTransitiveObjectPropertyAxiom source) {

			return source.getProperty();
		}

		InputNodePropertyTransitive createInputAxiom(
										NodeProperty prop,
										OWLTransitiveObjectPropertyAxiom source) {

			return new ConvertedNodePropertyTransitive(source, prop);
		}
	}

	NodePropertyAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);

		owlBottomObjectProperty = factory.getOWLBottomObjectProperty();
	}

	Iterable<InputNodePropertyEquivalence> getEquivalences() {

		return equivalenceConverter;
	}

	Iterable<InputNodePropertySubSuper> getSubSupers() {

		return subSuperConverter;
	}

	Iterable<InputNodePropertyInverse> getInverses() {

		return inverseConverter;
	}

	Iterable<InputNodePropertyChain> getChains() {

		return chainConverter;
	}

	Iterable<InputNodePropertySymmetric> getSymmetrics() {

		return symmetricConverter;
	}

	Iterable<InputNodePropertyTransitive> getTransitives() {

		return transitiveConverter;
	}

	OWLObjectProperty getOWLBottomProperty() {

		return owlBottomObjectProperty;
	}

	NodeProperty toPropertyOrNull(OWLObjectPropertyExpression expr) {

		return expr instanceof OWLObjectProperty ? names.resolve((OWLObjectProperty)expr) : null;
	}
}
