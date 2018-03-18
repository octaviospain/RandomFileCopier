/******************************************************************************
 * Copyright 2016-2018 Octavio Calleya                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * http://www.apache.org/licenses/LICENSE-2.0                                 *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package org.docopt;

/**
 * An exception thrown by {@link Docopt#parse} to indicate that the application
 * should exit. This could be normal (e.g. default {@code --help} behavior) or
 * abnormal (e.g. incorrect arguments).
 */
public final class DocoptExitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int exitCode;

	private final boolean printUsage;

	DocoptExitException(final int exitCode, final String message,
			final boolean printUsage) {
		super(message);
		this.exitCode = exitCode;
		this.printUsage = printUsage;
	}

	DocoptExitException(final int exitCode) {
		this(exitCode, null, false);
	}

	/**
	 * Returns a numeric code indicating the cause of the exit. By convention, a
	 * non-zero code indicates abnormal termination.
	 *
	 * @return the exit code
	 */
	public int getExitCode() {
		return exitCode;
	}

	boolean getPrintUsage() {
		return printUsage;
	}
}
