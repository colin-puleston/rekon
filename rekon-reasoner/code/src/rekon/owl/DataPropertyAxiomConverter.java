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

	private DataPropertyEquivalenceConverter equivalenceConverter = new DataPropertyEquivalenceConverter();
	private DataPropertySubSuperConverter subSuperConverter = new DataPropertySubSuperConverter();

	private class ConvertedDataPropertyEquivalence
						extends ConvertedEquivalence<DataProperty>
						implements InputDataPropertyEquivalence {

		ConvertedDataPropertyEquivalence(
			OWLAxiom source,
			DataProperty first,
			DataProperty second) {

			super(source, first, second);
		}
	}

	private class ConvertedDataPropertySubSuper
						extends ConvertedSubSuper<DataProperty>
						implements InputDataPropertySubSuper {

		ConvertedDataPropertySubSuper(
			OWLAxiom source,
			DataProperty sub,
			DataProperty sup) {

			super(source, sub, sup);
		}
	}

	private class DataPropertyEquivalenceSplitter
						extends
							EquivalenceSplitter<OWLEquivalentDataPropertiesAxiom> {

		Class<OWLEquivalentDataPropertiesAxiom> getAxiomType() {

			return OWLEquivalentDataPropertiesAxiom.class;
		}

		Collection<? extends OWLAxiom> asPairwiseAxioms(OWLEquivalentDataPropertiesAxiom axiom) {

			return axiom.asPairwiseAxioms();
		}
	}

	private class DataPropertyEquivalenceConverter
					extends
						PropertyLinkConverter
							<OWLEquivalentDataPropertiesAxiom,
							InputDataPropertyEquivalence> {

		Class<OWLEquivalentDataPropertiesAxiom> getSourceAxiomType() {

			return OWLEquivalentDataPropertiesAxiom.class;
		}

		Collection<OWLEquivalentDataPropertiesAxiom> resolveSourceAxiom(
														OWLEquivalentDataPropertiesAxiom ax) {

			return new DataPropertyEquivalenceSplitter().resolve(ax);
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
						PropertyLinkConverter
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

	DataPropertyAxiomConverter(AxiomConverter parentConverter) {

		super(parentConverter);

		owlBottomDataProperty = factory.getOWLBottomDataProperty();
	}

	Iterable<InputDataPropertyEquivalence> getEquivalences() {

		return equivalenceConverter;
	}

	Iterable<InputDataPropertySubSuper> getSubSupers() {

		return subSuperConverter;
	}

	OWLDataProperty getOWLBottomProperty() {

		return owlBottomDataProperty;
	}

	DataProperty toPropertyOrNull(OWLDataPropertyExpression expr) {

		return expr instanceof OWLDataProperty ? names.resolve((OWLDataProperty)expr) : null;
	}
}
