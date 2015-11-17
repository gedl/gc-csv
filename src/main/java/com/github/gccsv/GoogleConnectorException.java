package com.github.gccsv;

public class GoogleConnectorException extends Exception {
	private static final long serialVersionUID = 8241096918069761302L;
	
	public GoogleConnectorException(String message, Exception e) {
		super(message, e);
	}

}
