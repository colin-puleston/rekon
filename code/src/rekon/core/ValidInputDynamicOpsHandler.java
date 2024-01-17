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
abstract class ValidInputDynamicOpsHandler implements DynamicOpsHandler {

	public boolean equivalentTo(DynamicOpsHandler other) {

		if (other instanceof ValidInputDynamicOpsHandler) {

			return equivalentTo((ValidInputDynamicOpsHandler)other);
		}

		return false;
	}

	public boolean subsumes(DynamicOpsHandler other) {

		if (other instanceof ValidInputDynamicOpsHandler) {

			return subsumes((ValidInputDynamicOpsHandler)other);
		}

		return false;
	}

	private boolean equivalentTo(ValidInputDynamicOpsHandler other) {

		return subsumes(other) && other.subsumes(this);
	}

	private boolean subsumes(ValidInputDynamicOpsHandler other) {

		return getDefinitionMatchNode().subsumes(other.getProfileMatchNode());
	}

	abstract NodeX getDefinitionMatchNode();

	abstract NodeX getProfileMatchNode();
}
