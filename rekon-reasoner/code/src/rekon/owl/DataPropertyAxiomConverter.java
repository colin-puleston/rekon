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
class DataPropertyAxiomConverter
		extends
			PropertyAxiomConverter<OWLDataPropertyExpression, DataProperty> {

	private OWLDataProperty owlBottomDataProperty;

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

		ConvertedDataPropertySubSuper(
			OWLAxiom source,
			DataProperty sub,
			DataProperty sup) {

			super(source, sub, sup);
		}
	}

	private abstract class ConvertedDataPropertyAttribute
								extends ConvertedAxiom
								implements InputDataPropertyAttribute {

		private DataProperty property;

		public DataProperty getProperty() {

			return property;
		}

		ConvertedDataPropertyAttribute(OWLAxiom source, DataProperty property) {

			super(source);

			this.property = property;
		}
	}

	private class ConvertedDataPropertyDomain
						extends ConvertedDataPropertyAttribute
						implements InputDataPropertyDomain {

		private ClassNode domain;

		public ClassNode getDomain() {

			return domain;
		}

		ConvertedDataPropertyDomain(
			OWLAxiom source,
			DataProperty property,
			ClassNode domain) {

			super(source, property);

			this.domain = domain;
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

	private abstract class DataPropertyLinkConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends PropertyLinkConverter<S, I> {
	}

	private abstract class DataPropertyAttributeConverter
								<S extends OWLAxiom, I extends InputAxiom>
								extends PropertyAttributeConverter<S, I> {
	}

	private class DataPropertyEquivalenceConverter
					extends
						DataPropertyLinkConverter
							<OWLEquivalentDataPropertiesAxiom,
							InputDataPropertyEquivalence> {

		Class<OWLEquivalentDataPropertiesAxiom> getSourceAxiomType() {

			return OWLEquivalentDataPropertiesAxiom.class;
		}

		OwlPropertyLink createOwlLink(OWLEquivalentDataPropertiesAxiom source) {

			return new OwlPropertyLink(source, source.getProperties());
		}

		InputDataPropertyEquivalence createInputAxiom(OwlPropertyLink owlLink) {

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

		OwlPropertyLink createOwlLink(OWLSubDataPropertyOfAxiom source) {

			return new OwlPropertyLink(
							source,
							source.getSubProperty(),
							source.getSuperProperty());
		}

		InputDataPropertySubSuper createInputAxiom(OwlPropertyLink owlLink) {

			return new ConvertedDataPropertySubSuper(
							owlLink.source,
							owlLink.firstOrSubAsName(),
							owlLink.secondOrSupAsName());
		}
	}

	private class DataPropertyDomainConverter
					extends
						DataPropertyAttributeConverter
							<OWLDataPropertyDomainAxiom,
							InputDataPropertyDomain> {

		Class<OWLDataPropertyDomainAxiom> getSourceAxiomType() {

			return OWLDataPropertyDomainAxiom.class;
		}

		InputDataPropertyDomain createInputAxiom(
									DataProperty prop,
									OWLDataPropertyDomainAxiom source) {

			ClassNode d = toClassNode(source.getDomain());

			return d != null ? new ConvertedDataPropertyDomain(source, prop, d) : null;
		}

		OWLDataPropertyExpression getPropertyExpr(OWLDataPropertyDomainAxiom source) {

			return source.getProperty();
		}
	}

	DataPropertyAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);

		owlBottomDataProperty = factory.getOWLBottomDataProperty();

		new DataPropertyEquivalenceSplitter();

		new DataPropertyEquivalenceConverter();
		new DataPropertySubSuperConverter();
		new DataPropertyDomainConverter();
	}

	Iterable<InputDataPropertyEquivalence> getEquivalences() {

		return getInputAxioms(DataPropertyEquivalenceConverter.class);
	}

	Iterable<InputDataPropertySubSuper> getSubSupers() {

		return getInputAxioms(DataPropertySubSuperConverter.class);
	}

	Iterable<InputDataPropertyDomain> getDomains() {

		return getInputAxioms(DataPropertyDomainConverter.class);
	}

	OWLDataProperty getOWLBottomProperty() {

		return owlBottomDataProperty;
	}

	DataProperty toPropertyOrNull(OWLDataPropertyExpression expr) {

		return expr instanceof OWLDataProperty ? names.resolve((OWLDataProperty)expr) : null;
	}
}
