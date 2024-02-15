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

package rekon.core;

import java.util.*;

/**
 * @author Colin Puleston
 */
public abstract class OntologyNames {

	static private final String ROOT_NAMES_LABEL = "ROOT";

	private class RootClassNode extends ClassNode {

		public String getLabel() {

			return ROOT_NAMES_LABEL;
		}

		public boolean rootName() {

			return true;
		}

		RootClassNode(Collection<ClassNode> allSubs) {

			configureAsRootName(allSubs);
		}
	}

	private class RootNodeProperty extends NodeProperty {

		public String getLabel() {

			return ROOT_NAMES_LABEL;
		}

		public boolean rootName() {

			return true;
		}

		RootNodeProperty(Collection<NodeProperty> allSubs) {

			configureAsRootName(allSubs);
		}
	}

	private class RootDataProperty extends DataProperty {

		public String getLabel() {

			return ROOT_NAMES_LABEL;
		}

		public boolean rootName() {

			return true;
		}

		RootDataProperty(Collection<DataProperty> allSubs) {

			configureAsRootName(allSubs);
		}
	}

	public abstract ClassNode getRootClassNode();

	public abstract Collection<ClassNode> getClassNodes();

	public abstract Collection<IndividualNode> getIndividualNodes();

	public abstract Collection<NodeProperty> getNodeProperties();

	public abstract Collection<DataProperty> getDataProperties();

	protected ClassNode createRootClassNode(Collection<ClassNode> allSubs) {

		return new RootClassNode(allSubs);
	}

	protected NodeProperty createRootNodeProperty(Collection<NodeProperty> allSubs) {

		return new RootNodeProperty(allSubs);
	}

	protected DataProperty createRootDataProperty(Collection<DataProperty> allSubs) {

		return new RootDataProperty(allSubs);
	}
}
