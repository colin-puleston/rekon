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

	private OntologyNames names;
	private InputAxioms axioms;

	private abstract class TypeNameHandler<N extends Name> {

		TypeNameHandler() {

			addInterNameLinks();
			addOrphansAsRootSubs();
		}

		abstract Name getHierarchyRootName();

		abstract Iterable<N> getAllTypeNames();

		abstract Iterable<? extends InputNameEquivalence<N>> getTypeEquivalences();

		void addInterNameLinks() {

			for (InputNameEquivalence<N> ax : getTypeEquivalences()) {

				ax.getFirst().addEquivalent(ax.getSecond());
			}
		}

		private void addOrphansAsRootSubs() {

			Name root = getHierarchyRootName();

			for (N n : getAllTypeNames()) {

				if (n != root && n.getSubsumers().isEmpty()) {

					n.addSubsumer(root);
				}
			}
		}
	}

	private abstract class HierarchyNameHandler
								<N extends Name>
								extends TypeNameHandler<N> {

		HierarchyNameHandler() {

			getHierarchyRootName().setAsRoot();
		}

		void addInterNameLinks() {

			super.addInterNameLinks();

			for (InputNameSubSuper<N> ax : getTypeSubSupers()) {

				ax.getSub().addSubsumer(ax.getSuper());
			}
		}

		abstract Iterable<? extends InputNameSubSuper<N>> getTypeSubSupers();
	}

	private class ClassNodeHandler extends HierarchyNameHandler<ClassNode> {

		ClassNode getHierarchyRootName() {

			return names.getRootClassNode();
		}

		Iterable<ClassNode> getAllTypeNames() {

			return names.getClassNodes();
		}

		Iterable<InputClassEquivalence> getTypeEquivalences() {

			return axioms.getClassEquivalences();
		}

		Iterable<InputClassSubSuper> getTypeSubSupers() {

			return axioms.getClassSubSupers();
		}
	}

	private class IndividualNodeHandler extends TypeNameHandler<IndividualNode> {

		IndividualNodeHandler() {

			for (InputIndividualClassType ax : axioms.getIndividualClassTypes()) {

				ax.getIndividual().addSubsumer(ax.getClassType());
			}
		}

		ClassNode getHierarchyRootName() {

			return names.getRootClassNode();
		}

		Iterable<IndividualNode> getAllTypeNames() {

			return names.getIndividualNodes();
		}

		Iterable<InputIndividualEquivalence> getTypeEquivalences() {

			return axioms.getIndividualEquivalences();
		}
	}

	private class NodePropertyHandler extends HierarchyNameHandler<NodeProperty> {

		NodePropertyHandler() {

			for (InputNodePropertyDomain ax : axioms.getNodePropertyDomains()) {

				ax.getProperty().setDomain(ax.getDomain());
			}

			for (InputNodePropertyInverse ax : axioms.getNodePropertyInverses()) {

				ax.getProperty().addInverse(ax.getInverse());
			}

			for (InputNodePropertyChain ax : axioms.getNodePropertyChains()) {

				new PropertyChain(ax.getProperty(), ax.getChain());
			}

			for (InputNodePropertySymmetric ax : axioms.getNodePropertySymmetrics()) {

				ax.getProperty().setSymmetric();
			}

			for (InputNodePropertyTransitive ax : axioms.getNodePropertyTransitives()) {

				new PropertyChain(ax.getProperty());
			}
		}

		NodeProperty getHierarchyRootName() {

			return names.getRootNodeProperty();
		}

		Iterable<NodeProperty> getAllTypeNames() {

			return names.getNodeProperties();
		}

		Iterable<InputNodePropertyEquivalence> getTypeEquivalences() {

			return axioms.getNodePropertyEquivalences();
		}

		Iterable<InputNodePropertySubSuper> getTypeSubSupers() {

			return axioms.getNodePropertySubSupers();
		}
	}

	private class DataPropertyHandler extends HierarchyNameHandler<DataProperty> {

		DataProperty getHierarchyRootName() {

			return names.getRootDataProperty();
		}

		Iterable<DataProperty> getAllTypeNames() {

			return names.getDataProperties();
		}

		Iterable<InputDataPropertyEquivalence> getTypeEquivalences() {

			return axioms.getDataPropertyEquivalences();
		}

		Iterable<InputDataPropertySubSuper> getTypeSubSupers() {

			return axioms.getDataPropertySubSupers();
		}
	}

	BasicStructureBuilder(OntologyNames names, InputAxioms axioms) {

		this.names = names;
		this.axioms = axioms;

		new ClassNodeHandler();
		new IndividualNodeHandler();
		new NodePropertyHandler();
		new DataPropertyHandler();
	}
}
