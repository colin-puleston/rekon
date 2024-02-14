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

	private class ConvertedNodePropertyTransitive
						extends ConvertedNodePropertyAttribute
						implements InputNodePropertyTransitive {

		ConvertedNodePropertyTransitive(OWLAxiom source, NodeProperty property) {

			super(source, property);
		}
	}

	private class ConvertedNodePropertySymmetric
						extends ConvertedNodePropertyAttribute
						implements InputNodePropertySymmetric {

		ConvertedNodePropertySymmetric(OWLAxiom source, NodeProperty property) {

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

		Class<OWLObjectProperty> getNameExprType() {

			return OWLObjectProperty.class;
		}

		NodeProperty asName(OWLObjectPropertyExpression e) {

			return names.get((OWLObjectProperty)e);
		}
	}

	private class OwlDataPropertyLink extends OwlLink<OWLDataPropertyExpression, DataProperty> {

		OwlDataPropertyLink(OWLEquivalentDataPropertiesAxiom source) {

			super(source, source.getProperties());
		}

		OwlDataPropertyLink(OWLSubDataPropertyOfAxiom source) {

			super(source, source.getSubProperty(), source.getSuperProperty());
		}

		Class<OWLDataProperty> getNameExprType() {

			return OWLDataProperty.class;
		}

		DataProperty asName(OWLDataPropertyExpression e) {

			return names.get((OWLDataProperty)e);
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

			if (owlLink.matches(true, true)) {

				inputAxioms.add(createInputAxiom(owlLink));
			}
			else {

				logOutOfScopeAxiom(source, owlLink.getNonNames());
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

	private class NodePropertyInverseConverter
					extends
						NodePropertyLinkConverter
							<OWLInverseObjectPropertiesAxiom,
							InputNodePropertyInverse> {

		Class<OWLInverseObjectPropertiesAxiom> getSourceAxiomType() {

			return OWLInverseObjectPropertiesAxiom.class;
		}

		OwlObjectPropertyLink createOwlLink(OWLInverseObjectPropertiesAxiom source) {

			return new OwlObjectPropertyLink(source);
		}

		InputNodePropertyInverse createInputAxiom(OwlObjectPropertyLink owlLink) {

			return new ConvertedNodePropertyInverse(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private abstract class NodePropertyAttributeConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends TypeAxiomConverter<S, I> {

		boolean convertAxiomOfType(S source) {

			NodeProperty p = toNodeProperty(getPropertyExpr(source));

			if (p != null) {

				I ax = createInputAxiom(p, source);

				if (ax != null) {

					inputAxioms.add(ax);

					return true;
				}
			}

			logOutOfScopeAxiom(source);

			return true;
		}

		abstract OWLObjectPropertyExpression getPropertyExpr(S source);

		abstract I createInputAxiom(NodeProperty prop, S source);
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

			List<NodeProperty> chain = toNodeProperties(source.getPropertyChain());

			return chain != null ? new ConvertedNodePropertyChain(source, prop, chain) : null;
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

		new NodePropertyEquivalenceSplitter();
		new DataPropertyEquivalenceSplitter();

		new NodePropertyEquivalenceConverter();
		new NodePropertySubSuperConverter();
		new NodePropertyInverseConverter();
		new NodePropertyChainConverter();
		new NodePropertySymmetricConverter();
		new NodePropertyTransitiveConverter();
		new DataPropertyEquivalenceConverter();
		new DataPropertySubSuperConverter();
	}

	Collection<InputNodePropertyEquivalence> getNodePropertyEquivalences() {

		return getInputAxioms(NodePropertyEquivalenceConverter.class);
	}

	Collection<InputNodePropertySubSuper> getNodePropertySubSupers() {

		return getInputAxioms(NodePropertySubSuperConverter.class);
	}

	Collection<InputNodePropertyInverse> getNodePropertyInverses() {

		return getInputAxioms(NodePropertyInverseConverter.class);
	}

	Collection<InputNodePropertyChain> getNodePropertyChains() {

		return getInputAxioms(NodePropertyChainConverter.class);
	}

	Collection<InputNodePropertySymmetric> getNodePropertySymmetrics() {

		return getInputAxioms(NodePropertySymmetricConverter.class);
	}

	Collection<InputNodePropertyTransitive> getNodePropertyTransitives() {

		return getInputAxioms(NodePropertyTransitiveConverter.class);
	}

	Collection<InputDataPropertyEquivalence> getDataPropertyEquivalences() {

		return getInputAxioms(DataPropertyEquivalenceConverter.class);
	}

	Collection<InputDataPropertySubSuper> getDataPropertySubSupers() {

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

		return e instanceof OWLObjectProperty ? names.get((OWLObjectProperty)e) : null;
	}
}
