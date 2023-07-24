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
	static private final String ABSENT_CLASS_NAME_LABEL = "NOTHING";

	private AbsentClassValue absentClassValue = new AbsentClassValue();

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

	private class AbsentClassNode extends ClassNode {

		public String getLabel() {

			return ABSENT_CLASS_NAME_LABEL;
		}
	}

	private class AbsentClassValue extends NodeValue {

		boolean subsumesOther(Value v) {

			return v == this;
		}

		private AbsentClassValue() {

			super(new AbsentClassNode());
		}
	}

	public AbsentClassValue getAbsentClassValue() {

		return absentClassValue;
	}

	protected ClassNode createRootClassNode(Collection<ClassNode> allSubs) {

		return new RootClassNode(allSubs);
	}

	protected NodeProperty createRootNodeProperty(Collection<NodeProperty> allSubs) {

		return new RootNodeProperty(allSubs);
	}

	protected DataProperty createRootDataProperty(Collection<DataProperty> allSubs) {

		return new RootDataProperty(allSubs);
	}

	protected abstract Collection<ClassNode> getClassNodes();

	protected abstract Collection<IndividualNode> getIndividualNodes();

	protected abstract Collection<NodeProperty> getNodeProperties();

	protected abstract Collection<DataProperty> getDataProperties();
}
