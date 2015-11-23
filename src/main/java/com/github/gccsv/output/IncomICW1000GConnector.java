package com.github.gccsv.output;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//Tested for firmware v1.0.5
public class IncomICW1000GConnector {
	private MultipartBuilder multipart;
	private int port;
	private String host;
	private String password;
	private boolean verbose;

	public static final String[] REQUIRED_HEADER = { "Name", "Group", "Number", "Number1", "Number2", "Number3", "Number4", "Key Number",
			"Number Type", "Email", "Email1", "Messenger", "Contents" };

	public IncomICW1000GConnector(String connection) throws IncomICW1000GConnectorException {
		if (connection.indexOf(":") == -1 || connection.indexOf("@") == 1) {
			throw new IncomICW1000GConnectorException("Argument has to be in the form of password@ip:port. Received: " + connection);
		}
		String[] addressAndPassword = connection.split("@");
		String[] addressAndPort = addressAndPassword[1].split(":");
		if (addressAndPassword.length != 2 || addressAndPort.length != 2) {
			throw new IncomICW1000GConnectorException("Argument has to be in the form of password@ip:port. Received: " + connection);
		}
		this.host = addressAndPort[0];
		try {
			this.port = new Integer(addressAndPort[1]);
		} catch (NumberFormatException e) {
			throw new IncomICW1000GConnectorException("Port has to be an integer. Received: " + addressAndPort[1]);
		}
		this.password = addressAndPassword[0];
		try {
			if (!this.authenticate()) {
				throw new IncomICW1000GConnectorException(String.format("Connection was made, but could not autenticate against http://%s:%d/",
						host, port));
			}
			multipart = new MultipartBuilder(String.format("http://%s:%d/pbimport.html", host, port), "UTF-8");
		} catch (IOException e) {
			throw new IncomICW1000GConnectorException(String.format("Could not connect to %s:%s", host, port), e);
		}
		multipart.addHeaderField("User-Agent", "GC-TO-CSV");
	}

	public void setVerbose(boolean verbose) {		this.verbose = verbose;
	}

	public OutputStream getOutputStream() throws IncomICW1000GConnectorException {
		try {
			return multipart.beginFilePart("upload", "text/csv");
		} catch (IOException e) {
			throw new IncomICW1000GConnectorException(String.format("Could not build HTTP multipart request"), e);
		}

	}

	public int sendFile() throws IncomICW1000GConnectorException {
		try {
			List<String> response = multipart.finish();
			if (response != null && response.size() == 1) {
				try {
					int result = new Integer(response.get(0));
					if (verbose) {
						System.out.printf("Sent %d entries to ICW 1000G", result);
					}
					return result;
				} catch (NumberFormatException e) {
					throw new IncomICW1000GConnectorException(String.format("Unexpected reply from %s:%s. Was expecting a number but got %s",
							host, port, response.get(0)), e);
				}
			} else {
				StringBuffer responseText = new StringBuffer();
				for (String line : response) {
					responseText.append(line).append("\n");
				}
				throw new IncomICW1000GConnectorException(String.format(
						"Unexpected reply from %s:%s. Was expecting exactline one line but got %d lines: %s ", host, port, response.size(),
						responseText));
			}
		} catch (IOException e) {
			throw new IncomICW1000GConnectorException(String.format("Could not upload file to %s:%s", host, port), e);
		}
	}

	private boolean authenticate() throws IOException {
		boolean result = false;
		URL url = new URL(String.format("http://%s:%d/login.htm", host, port));
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setUseCaches(false);
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		httpConn.setRequestMethod("POST");
		httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		OutputStream outputStream = httpConn.getOutputStream();
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
		writer.append("?id=user&password=" + password);
		writer.flush();
		writer.close();

		List<String> response = new ArrayList<String>();

		// checks server's status code first
		int status = httpConn.getResponseCode();
		if (status == HttpURLConnection.HTTP_OK) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				response.add(line);
			}
			reader.close();
			httpConn.disconnect();
			result = true;
		}

		return result;
	}
}
