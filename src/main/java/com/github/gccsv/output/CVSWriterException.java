package com.github.gccsv.output;

import java.io.IOException;

public class CVSWriterException extends Exception {
	private static final long serialVersionUID = -391208342179864889L;

	public CVSWriterException(String message, IOException e) {
		super(message, e);
	}
}
