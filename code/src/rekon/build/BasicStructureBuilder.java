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

package rekon.build;

import java.util.*;

import rekon.core.*;
import rekon.build.input.*;

/**
 * @author Colin Puleston
 */
class BasicStructureBuilder {

	private InputAxioms axioms;

	private abstract class TypeNameConverter<N extends Name> {

		TypeNameConverter() {

			for (InputNameEquivalence<N> ax : getEquivalences()) {

				ax.getFirst().addEquivalent(ax.getSecond());
			}
		}

		abstract Iterable<? extends InputNameEquivalence<N>> getEquivalences();
	}

	private abstract class HierarchyNameConverter
								<N extends Name>
								extends TypeNameConverter<N> {

		HierarchyNameConverter() {

			for (InputNameSubSuper<N> ax : getSubSupers()) {

				ax.getSub().addSubsumer(ax.getSuper());
			}
		}

		abstract Iterable<? extends InputNameSubSuper<N>> getSubSupers();
	}

	private class ClassNodeConverter extends HierarchyNameConverter<ClassNode> {

		Iterable<InputClassEquivalence> getEquivalences() {

			return axioms.getClassEquivalences();
		}

		Iterable<InputClassSubSuper> getSubSupers() {

			return axioms.getClassSubSupers();
		}
	}

	private class IndividualNodeConverter extends TypeNameConverter<IndividualNode> {

		IndividualNodeConverter() {

			for (InputIndividualClassType ax : axioms.getIndividualClassTypes()) {

				ax.getIndividual().addSubsumer(ax.getClassType());
			}
		}

		Iterable<InputIndividualEquivalence> getEquivalences() {

			return axioms.getIndividualEquivalences();
		}
	}

	private class NodePropertyConverter extends HierarchyNameConverter<NodeProperty> {

		NodePropertyConverter() {

			for (InputNodePropertyChain ax : axioms.getNodePropertyChains()) {

				new PropertyChain(ax.getProperty(), ax.getChain());
			}

			for (InputNodePropertyTransitive ax : axioms.getNodePropertyTransitives()) {

				new PropertyChain(ax.getProperty());
			}
		}

		Iterable<InputNodePropertyEquivalence> getEquivalences() {

			return axioms.getNodePropertyEquivalences();
		}

		Iterable<InputNodePropertySubSuper> getSubSupers() {

			return axioms.getNodePropertySubSupers();
		}
	}

	private class DataPropertyConverter extends HierarchyNameConverter<DataProperty> {

		Iterable<InputDataPropertyEquivalence> getEquivalences() {

			return axioms.getDataPropertyEquivalences();
		}

		Iterable<InputDataPropertySubSuper> getSubSupers() {

			return axioms.getDataPropertySubSupers();
		}
	}

	BasicStructureBuilder(InputAxioms axioms) {

		this.axioms = axioms;

		new ClassNodeConverter();
		new IndividualNodeConverter();
		new NodePropertyConverter();
		new DataPropertyConverter();
	}
}
