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

/**
 * @author Colin Puleston
 */
abstract class ValidInputQueryable implements Queryable {

	private SupersResolver supersResolver = new SupersResolver();
	private SubsResolver subsResolver = new SubsResolver();

	private abstract class LinkedClassesResolver {

		Names resolve(Names names, boolean direct) {

			if (anyFreeNames(names)) {

				return direct ? resolveForFreeDirects(names) : purgeAllFreeNames(names);
			}

			return names;
		}

		abstract Names getLinked(Name name, boolean direct);

		private Names resolveForFreeDirects(Names withFreeNames) {

			NameSet resolved = new NameSet();

			for (Name d : withFreeNames) {

				collectClosestLinkedMappeds(resolved, d);
			}

			for (Name d : resolved.copyNames()) {

				resolved.removeAll(getLinked(d, false));
			}

			return resolved;
		}

		private void collectClosestLinkedMappeds(NameSet collected, Name n) {

			if (n.mapped()) {

				collected.add(n);
			}
			else {

				for (Name l : getLinked(n, true)) {

					collectClosestLinkedMappeds(collected, l);
				}
			}
		}
	}

	private class SupersResolver extends LinkedClassesResolver {

		Names getLinked(Name name, boolean direct) {

			return name.getSupers(direct);
		}
	}

	private class SubsResolver extends LinkedClassesResolver {

		Names getLinked(Name name, boolean direct) {

			return name.getSubs(ClassNode.class, direct);
		}
	}

	public Names getEquivalents() {

		return checkPurgeAllFreeNames(getRawEquivalents());
	}

	public Names getSupers(boolean direct) {

		return supersResolver.resolve(getRawSupers(direct), direct);
	}

	public Names getSubs(boolean direct) {

		return subsResolver.resolve(getRawSubs(direct), direct);
	}

	public Names getIndividuals(boolean direct) {

		return checkPurgeAllFreeNames(getRawIndividuals(direct));
	}

	public boolean equivalentTo(Queryable other) {

		if (other instanceof ValidInputQueryable) {

			return equivalentTo((ValidInputQueryable)other);
		}

		return false;
	}

	public boolean subsumes(Queryable other) {

		if (other instanceof ValidInputQueryable) {

			return subsumes((ValidInputQueryable)other);
		}

		return false;
	}

	abstract void configureAsPotentialSubsumed();

	abstract NodeX getNode();

	abstract Names getRawEquivalents();

	abstract Names getRawSupers(boolean direct);

	abstract Names getRawSubs(boolean direct);

	abstract Names getRawIndividuals(boolean direct);

	private boolean equivalentTo(ValidInputQueryable other) {

		return subsumes(other) && other.subsumes(this);
	}

	private boolean subsumes(ValidInputQueryable other) {

		other.configureAsPotentialSubsumed();

		return getNode().subsumes(other.getNode());
	}

	private Names checkPurgeAllFreeNames(Names names) {

		return anyFreeNames(names) ? purgeAllFreeNames(names) : names;
	}

	private boolean anyFreeNames(Names names) {

		for (Name n : names) {

			if (!n.mapped()) {

				return true;
			}
		}

		return false;
	}

	private Names purgeAllFreeNames(Names withFreeNames) {

		NameList purged = new NameList();

		for (Name n : withFreeNames) {

			if (n.mapped()) {

				purged.add(n);
			}
		}

		return purged;
	}
}
