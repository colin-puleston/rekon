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

	private class ConvertedNodePropertyEquivalence
						extends ConvertedNameEquivalence<NodeProperty>
						implements InputNodePropertyEquivalence {

		ConvertedNodePropertyEquivalence(
			OWLAxiom source,
			NodeProperty first,
			NodeProperty second) {

			super(source, first, second);
		}
	}

	private class ConvertedNodePropertySubSuper
						extends ConvertedNameSubSuper<NodeProperty>
						implements InputNodePropertySubSuper {

		ConvertedNodePropertySubSuper(
			OWLAxiom source,
			NodeProperty sub,
			NodeProperty sup) {

			super(source, sub, sup);
		}
	}

	private abstract class ConvertedNodePropertyAttribute
								extends ConvertedAxiom
								implements InputNodePropertyAttribute {

		private NodeProperty property;

		public NodeProperty getProperty() {

			return property;
		}

		ConvertedNodePropertyAttribute(OWLAxiom source, NodeProperty property) {

			super(source);

			this.property = property;
		}
	}

	private class ConvertedNodePropertyDomain
						extends ConvertedNodePropertyAttribute
						implements InputNodePropertyDomain {

		private ClassNode domain;

		public ClassNode getDomain() {

			return domain;
		}

		ConvertedNodePropertyDomain(
			OWLAxiom source,
			NodeProperty property,
			ClassNode domain) {

			super(source, property);

			this.domain = domain;
		}
	}

	private class ConvertedNodePropertyRange
						extends ConvertedNodePropertyAttribute
						implements InputNodePropertyRange {

		private ClassNode range;

		public ClassNode getRange() {

			return range;
		}

		ConvertedNodePropertyRange(
			OWLAxiom source,
			NodeProperty property,
			ClassNode range) {

			super(source, property);

			this.range = range;
		}
	}

	private class ConvertedNodePropertyInverse
						extends ConvertedNodePropertyAttribute
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
						extends ConvertedNodePropertyAttribute
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
		}
	}

	private class ConvertedNodePropertySymmetric
						extends ConvertedNodePropertyAttribute
						implements InputNodePropertySymmetric {

		ConvertedNodePropertySymmetric(OWLAxiom source, NodeProperty property) {

			super(source, property);
		}
	}

	private class ConvertedNodePropertyTransitive
						extends ConvertedNodePropertyAttribute
						implements InputNodePropertyTransitive {

		ConvertedNodePropertyTransitive(OWLAxiom source, NodeProperty property) {

			super(source, property);
		}
	}

	private class NodePropertyEquivalenceSplitter
						extends
							TypeAxiomResolver<OWLEquivalentObjectPropertiesAxiom> {

		Class<OWLEquivalentObjectPropertiesAxiom> getAxiomType() {

			return OWLEquivalentObjectPropertiesAxiom.class;
		}

		Collection<? extends OWLAxiom> resolveAxiomOfType(OWLEquivalentObjectPropertiesAxiom axiom) {

			return axiom.asPairwiseAxioms();
		}
	}

	private abstract class NodePropertyLinkConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends PropertyLinkConverter<S, I> {
	}

	private abstract class NodePropertyAttributeConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends PropertyAttributeConverter<S, I> {
	}

	private class NodePropertyEquivalenceConverter
					extends
						NodePropertyLinkConverter
							<OWLEquivalentObjectPropertiesAxiom,
							InputNodePropertyEquivalence> {

		Class<OWLEquivalentObjectPropertiesAxiom> getSourceAxiomType() {

			return OWLEquivalentObjectPropertiesAxiom.class;
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
						NodePropertyLinkConverter
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

	private class NodePropertyDomainConverter
					extends
						NodePropertyAttributeConverter
							<OWLObjectPropertyDomainAxiom,
							InputNodePropertyDomain> {

		Class<OWLObjectPropertyDomainAxiom> getSourceAxiomType() {

			return OWLObjectPropertyDomainAxiom.class;
		}

		InputNodePropertyDomain createInputAxiom(
									NodeProperty prop,
									OWLObjectPropertyDomainAxiom source) {

			ClassNode d = toClassNode(source.getDomain());

			return d != null ? new ConvertedNodePropertyDomain(source, prop, d) : null;
		}

		OWLObjectPropertyExpression getPropertyExpr(OWLObjectPropertyDomainAxiom source) {

			return source.getProperty();
		}
	}

	private class NodePropertyRangeConverter
					extends
						NodePropertyAttributeConverter
							<OWLObjectPropertyRangeAxiom,
							InputNodePropertyRange> {

		Class<OWLObjectPropertyRangeAxiom> getSourceAxiomType() {

			return OWLObjectPropertyRangeAxiom.class;
		}

		InputNodePropertyRange createInputAxiom(
									NodeProperty prop,
									OWLObjectPropertyRangeAxiom source) {

			ClassNode r = toClassNode(source.getRange());

			return r != null ? new ConvertedNodePropertyRange(source, prop, r) : null;
		}

		OWLObjectPropertyExpression getPropertyExpr(OWLObjectPropertyRangeAxiom source) {

			return source.getProperty();
		}
	}

	private class NodePropertyInverseConverter
					extends
						NodePropertyLinkConverter
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
						NodePropertyAttributeConverter
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
						NodePropertyAttributeConverter
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
						NodePropertyAttributeConverter
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

		new NodePropertyEquivalenceSplitter();

		new NodePropertyEquivalenceConverter();
		new NodePropertySubSuperConverter();
		new NodePropertyDomainConverter();
		new NodePropertyRangeConverter();
		new NodePropertyInverseConverter();
		new NodePropertyChainConverter();
		new NodePropertySymmetricConverter();
		new NodePropertyTransitiveConverter();
	}

	Iterable<InputNodePropertyEquivalence> getEquivalences() {

		return getInputAxioms(NodePropertyEquivalenceConverter.class);
	}

	Iterable<InputNodePropertySubSuper> getSubSupers() {

		return getInputAxioms(NodePropertySubSuperConverter.class);
	}

	Iterable<InputNodePropertyDomain> getDomains() {

		return getInputAxioms(NodePropertyDomainConverter.class);
	}

	Iterable<InputNodePropertyRange> getRanges() {

		return getInputAxioms(NodePropertyRangeConverter.class);
	}

	Iterable<InputNodePropertyInverse> getInverses() {

		return getInputAxioms(NodePropertyInverseConverter.class);
	}

	Iterable<InputNodePropertyChain> getChains() {

		return getInputAxioms(NodePropertyChainConverter.class);
	}

	Iterable<InputNodePropertySymmetric> getSymmetrics() {

		return getInputAxioms(NodePropertySymmetricConverter.class);
	}

	Iterable<InputNodePropertyTransitive> getTransitives() {

		return getInputAxioms(NodePropertyTransitiveConverter.class);
	}

	OWLObjectProperty getOWLBottomProperty() {

		return owlBottomObjectProperty;
	}

	NodeProperty toPropertyOrNull(OWLObjectPropertyExpression expr) {

		return expr instanceof OWLObjectProperty ? names.resolve((OWLObjectProperty)expr) : null;
	}
}
