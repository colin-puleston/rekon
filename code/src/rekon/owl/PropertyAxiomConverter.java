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
class PropertyAxiomConverter extends CategoryAxiomConverter {

	private OWLObjectProperty owlBottomObjectProperty;
	private OWLDataProperty owlBottomDataProperty;

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

	private class ConvertedNodePropertyTransitive
						extends ConvertedNodePropertyAttribute
						implements InputNodePropertyTransitive {

		ConvertedNodePropertyTransitive(OWLAxiom source, NodeProperty property) {

			super(source, property);
		}
	}

	private class ConvertedDataPropertyEquivalence
						extends ConvertedNameEquivalence<DataProperty>
						implements InputDataPropertyEquivalence {

		ConvertedDataPropertyEquivalence(
			OWLAxiom source,
			DataProperty first,
			DataProperty second) {

			super(source, first, second);
		}
	}

	private class ConvertedDataPropertySubSuper
						extends ConvertedNameSubSuper<DataProperty>
						implements InputDataPropertySubSuper {

		ConvertedDataPropertySubSuper(OWLAxiom source, DataProperty sub, DataProperty sup) {

			super(source, sub, sup);
		}
	}

	private class OwlObjectPropertyLink extends OwlLink<OWLObjectPropertyExpression, NodeProperty> {

		OwlObjectPropertyLink(OWLEquivalentObjectPropertiesAxiom source) {

			super(source, source.getProperties());
		}

		OwlObjectPropertyLink(OWLSubObjectPropertyOfAxiom source) {

			super(source, source.getSubProperty(), source.getSuperProperty());
		}

		OwlObjectPropertyLink(OWLInverseObjectPropertiesAxiom source) {

			super(source, source.getFirstProperty(), source.getSecondProperty());
		}

		OwlLinkStatus checkMatch(OWLObjectPropertyExpression expr, boolean isName) {

			if (expr instanceof OWLObjectProperty && !expr.equals(owlBottomObjectProperty)) {

				return OwlLinkStatus.VALID_MATCH;
			}

			return OwlLinkStatus.INVALID_MATCH;
		}

		NodeProperty asName(OWLObjectPropertyExpression expr) {

			return names.resolve((OWLObjectProperty)expr);
		}
	}

	private class OwlDataPropertyLink extends OwlLink<OWLDataPropertyExpression, DataProperty> {

		OwlDataPropertyLink(OWLEquivalentDataPropertiesAxiom source) {

			super(source, source.getProperties());
		}

		OwlDataPropertyLink(OWLSubDataPropertyOfAxiom source) {

			super(source, source.getSubProperty(), source.getSuperProperty());
		}

		OwlLinkStatus checkMatch(OWLDataPropertyExpression expr, boolean isName) {

			if (expr instanceof OWLDataProperty && !expr.equals(owlBottomDataProperty)) {

				return OwlLinkStatus.VALID_MATCH;
			}

			return OwlLinkStatus.INVALID_MATCH;
		}

		DataProperty asName(OWLDataPropertyExpression expr) {

			return names.resolve((OWLDataProperty)expr);
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

	private class DataPropertyEquivalenceSplitter
						extends
							TypeAxiomResolver<OWLEquivalentDataPropertiesAxiom> {

		Class<OWLEquivalentDataPropertiesAxiom> getAxiomType() {

			return OWLEquivalentDataPropertiesAxiom.class;
		}

		Collection<? extends OWLAxiom> resolveAxiomOfType(OWLEquivalentDataPropertiesAxiom axiom) {

			return axiom.asPairwiseAxioms();
		}
	}

	private abstract class PropertyLinkConverter
								<S extends OWLAxiom,
								SA extends OwlLink<?, ?>,
								I extends InputAxiom>
								extends TypeAxiomConverter<S, I> {

		boolean convertAxiomOfType(S source) {

			SA owlLink = createOwlLink(source);

			if (owlLink.checkMatch(true, true) == OwlLinkStatus.VALID_MATCH) {

				inputAxioms.add(createInputAxiom(owlLink));
			}

			return true;
		}

		abstract SA createOwlLink(S source);

		abstract I createInputAxiom(SA owlLink);
	}

	private abstract class NodePropertyLinkConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends PropertyLinkConverter<S, OwlObjectPropertyLink, I> {
	}

	private class NodePropertyEquivalenceConverter
					extends
						NodePropertyLinkConverter
							<OWLEquivalentObjectPropertiesAxiom,
							InputNodePropertyEquivalence> {

		Class<OWLEquivalentObjectPropertiesAxiom> getSourceAxiomType() {

			return OWLEquivalentObjectPropertiesAxiom.class;
		}

		OwlObjectPropertyLink createOwlLink(OWLEquivalentObjectPropertiesAxiom source) {

			return new OwlObjectPropertyLink(source);
		}

		InputNodePropertyEquivalence createInputAxiom(OwlObjectPropertyLink owlLink) {

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

		OwlObjectPropertyLink createOwlLink(OWLSubObjectPropertyOfAxiom source) {

			return new OwlObjectPropertyLink(source);
		}

		InputNodePropertySubSuper createInputAxiom(OwlObjectPropertyLink owlLink) {

			return new ConvertedNodePropertySubSuper(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private abstract class NodePropertyAttributeConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends TypeAxiomConverter<S, I> {

		boolean convertAxiomOfType(S source) {

			OWLObjectPropertyExpression expr = getPropertyExpr(source);
			NodeProperty p = toNodeProperty(expr);

			if (p != null) {

				I ax = createInputAxiom(p, source);

				if (ax != null) {

					inputAxioms.add(ax);

					return true;
				}
			}

			logOutOfScopeAxiom(source, expr);

			return true;
		}

		abstract OWLObjectPropertyExpression getPropertyExpr(S source);

		abstract I createInputAxiom(NodeProperty prop, S source);
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

			List<NodeProperty> c = toNodeProperties(source.getPropertyChain());

			return c != null ? new ConvertedNodePropertyChain(source, prop, c) : null;
		}

		OWLObjectPropertyExpression getPropertyExpr(OWLSubPropertyChainOfAxiom source) {

			return source.getSuperProperty();
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

	private abstract class DataPropertyLinkConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends PropertyLinkConverter<S, OwlDataPropertyLink, I> {
	}

	private class DataPropertyEquivalenceConverter
					extends
						DataPropertyLinkConverter
							<OWLEquivalentDataPropertiesAxiom,
							InputDataPropertyEquivalence> {

		Class<OWLEquivalentDataPropertiesAxiom> getSourceAxiomType() {

			return OWLEquivalentDataPropertiesAxiom.class;
		}

		OwlDataPropertyLink createOwlLink(OWLEquivalentDataPropertiesAxiom source) {

			return new OwlDataPropertyLink(source);
		}

		InputDataPropertyEquivalence createInputAxiom(OwlDataPropertyLink owlLink) {

			return new ConvertedDataPropertyEquivalence(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private class DataPropertySubSuperConverter
					extends
						DataPropertyLinkConverter
							<OWLSubDataPropertyOfAxiom,
							InputDataPropertySubSuper> {

		Class<OWLSubDataPropertyOfAxiom> getSourceAxiomType() {

			return OWLSubDataPropertyOfAxiom.class;
		}

		OwlDataPropertyLink createOwlLink(OWLSubDataPropertyOfAxiom source) {

			return new OwlDataPropertyLink(source);
		}

		InputDataPropertySubSuper createInputAxiom(OwlDataPropertyLink owlLink) {

			return new ConvertedDataPropertySubSuper(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	PropertyAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);

		owlBottomObjectProperty = factory.getOWLBottomObjectProperty();
		owlBottomDataProperty = factory.getOWLBottomDataProperty();

		new NodePropertyEquivalenceSplitter();
		new DataPropertyEquivalenceSplitter();

		new NodePropertyEquivalenceConverter();
		new NodePropertySubSuperConverter();
		new NodePropertyDomainConverter();
		new NodePropertyRangeConverter();
		new NodePropertyChainConverter();
		new NodePropertyTransitiveConverter();
		new DataPropertyEquivalenceConverter();
		new DataPropertySubSuperConverter();
	}

	Iterable<InputNodePropertyEquivalence> getNodePropertyEquivalences() {

		return getInputAxioms(NodePropertyEquivalenceConverter.class);
	}

	Iterable<InputNodePropertySubSuper> getNodePropertySubSupers() {

		return getInputAxioms(NodePropertySubSuperConverter.class);
	}

	Iterable<InputNodePropertyDomain> getNodePropertyDomains() {

		return getInputAxioms(NodePropertyDomainConverter.class);
	}

	Iterable<InputNodePropertyRange> getNodePropertyRanges() {

		return getInputAxioms(NodePropertyRangeConverter.class);
	}

	Iterable<InputNodePropertyChain> getNodePropertyChains() {

		return getInputAxioms(NodePropertyChainConverter.class);
	}

	Iterable<InputNodePropertyTransitive> getNodePropertyTransitives() {

		return getInputAxioms(NodePropertyTransitiveConverter.class);
	}

	Iterable<InputDataPropertyEquivalence> getDataPropertyEquivalences() {

		return getInputAxioms(DataPropertyEquivalenceConverter.class);
	}

	Iterable<InputDataPropertySubSuper> getDataPropertySubSupers() {

		return getInputAxioms(DataPropertySubSuperConverter.class);
	}

	private List<NodeProperty> toNodeProperties(List<OWLObjectPropertyExpression> exprs) {

		List<NodeProperty> nodeProps = new ArrayList<NodeProperty>();

		for (OWLObjectPropertyExpression e : exprs) {

			NodeProperty p = toNodeProperty(e);

			if (p == null) {

				return null;
			}

			nodeProps.add(p);
		}

		return nodeProps;
	}

	private NodeProperty toNodeProperty(OWLObjectPropertyExpression e) {

		return e instanceof OWLObjectProperty ? names.resolve((OWLObjectProperty)e) : null;
	}

	private ClassNode toClassNode(OWLClassExpression e) {

		return e instanceof OWLClass ? names.resolve((OWLClass)e) : null;
	}
}
