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

/**
 * @author Colin Puleston
 */
class BasicStructureBuilder {

	private abstract class TypeNameHandler<N extends Name, IN extends InputName<N>> {

		TypeNameHandler(Collection<IN> inputNames) {

			for (IN ae : inputNames) {

				configureName(ae);
			}
		}

		void configureName(IN inputName) {

			N n = inputName.getName();

			addInputEquivalents(inputName, n);
			addInputSupers(inputName, n);
		}

		abstract void addInputSupers(IN inputName, N name);

		private void addInputEquivalents(IN inputName, N name) {

			for (N e : inputName.getEquivs()) {

				name.addEquivalent(e);
			}
		}
	}

	private abstract class HierarchyNameHandler
								<N extends Name,
								IN extends InputHierarchyName<N>>
								extends TypeNameHandler<N, IN> {

		HierarchyNameHandler(Collection<IN> inputNames) {

			super(inputNames);
		}

		void addInputSupers(IN inputName, N name) {

			for (N s : inputName.getSupers()) {

				name.addSubsumer(s);
			}
		}
	}

	private class ClassNodeHandler extends HierarchyNameHandler<ClassNode, InputClass> {

		ClassNodeHandler(InputAssertions assertions) {

			super(assertions.getClasses());
		}
	}

	private class IndividualNodeHandler extends TypeNameHandler<IndividualNode, InputIndividual> {

		IndividualNodeHandler(InputAssertions assertions) {

			super(assertions.getIndividuals());
		}

		void addInputSupers(InputIndividual inputName, IndividualNode node) {

			for (ClassNode t : inputName.getTypes()) {

				node.addSubsumer(t);
			}
		}
	}

	private class NodePropertyHandler extends HierarchyNameHandler<NodeProperty, InputObjectProperty> {

		NodePropertyHandler(InputAssertions assertions) {

			super(assertions.getObjectProperties());
		}

		void configureName(InputObjectProperty inputName) {

			super.configureName(inputName);

			NodeProperty p = inputName.getName();

			p.addInverses(inputName.getInverses());

			if (inputName.transitive()) {

				new PropertyChain(p);
			}

			for (List<NodeProperty> chain : inputName.getChains()) {

				new PropertyChain(p, chain);
			}

			if (inputName.symmetric()) {

				p.setSymmetric();
			}
		}
	}

	private class DataPropertyHandler extends HierarchyNameHandler<DataProperty, InputDataProperty> {

		DataPropertyHandler(InputAssertions assertions) {

			super(assertions.getDataProperties());
		}
	}

	BasicStructureBuilder(InputAssertions assertions) {

		new ClassNodeHandler(assertions);
		new IndividualNodeHandler(assertions);
		new NodePropertyHandler(assertions);
		new DataPropertyHandler(assertions);
	}
}
