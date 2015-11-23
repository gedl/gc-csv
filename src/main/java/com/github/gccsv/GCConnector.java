package com.github.gccsv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gdata.client.Query;
import com.google.gdata.client.Query.CustomParameter;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.util.ServiceException;

public class GCConnector {

	private static String STORE_DIR = "gc-csv/";
	private static String REFRESH_TOKEN_FILE = "rt";

	private boolean verbose;

	private final static String groupIdTemplate = "http://www.google.com/m8/feeds/groups/%s/base/%s";

	private String tokenStore;
	private String groupId;
	private String email;
	private String clientSecret;
	private String clientId;
	private int pageSize;
	private int maxResults;

	public GCConnector(String clientId, String clientSecret, int pageSize, int maxResults, String tokenStorageDir) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.pageSize = pageSize;
		this.maxResults = maxResults;
		this.tokenStore = tokenStorageDir;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	private void ensureAccessKey() throws GoogleConnectorException {
		File refreshTokenStore = new File(tokenStore + STORE_DIR + REFRESH_TOKEN_FILE);
		if (verbose) {
			System.out.println("Using the following path as the refresh token: " + refreshTokenStore);
		}
		if (!refreshTokenStore.exists()) {
			if (verbose) {
				System.out.println("Refresh token not found. Requesting one. User action and input is necessary.");
			}
			GoogleAuthorizationCodeRequestUrl authorizationCodeURL = new GoogleAuthorizationCodeRequestUrl(clientId, "urn:ietf:wg:oauth:2.0:oob",
					Collections.singleton("https://www.googleapis.com/auth/contacts.readonly"));
			String authorizationURL = authorizationCodeURL.build();
			authorizationCodeURL.setAccessType("offline");

			System.out.println("Please allow GC to CSV to access your google contacts on the URL below");
			System.out.println("Then copy the auth code and paste on the prompt");
			System.out.println("Authorization URL: " + authorizationURL);
			System.out.println();
			System.out.print(">");
			Scanner scan = new Scanner(System.in);
			String authorizationToken = scan.nextLine();
			scan.close();
			if (verbose) {
				System.out.println(String.format("Read %d characters from keyboard", authorizationToken.length()));
			}

			HttpTransport transport = new NetHttpTransport();

			JsonFactory jsonFactory = new JacksonFactory();
			GoogleAuthorizationCodeTokenRequest authorizationTokenRequest = new GoogleAuthorizationCodeTokenRequest(transport, jsonFactory,
					clientId, clientSecret, authorizationToken, "urn:ietf:wg:oauth:2.0:oob");

			GoogleTokenResponse tokenResponse;
			try {
				tokenResponse = authorizationTokenRequest.execute();
			} catch (IOException e) {
				if (verbose) {
					System.err.println("Could not acquire a refresh token. Cause: " + e.getMessage());
				}
				throw new GoogleConnectorException(
						"Could not acquired a refresh token from google. The most likely cause for this error is a wrong authorization token being provided by the user. Also, check your internet connectivity.",
						e);
			}

			String refreshToken = tokenResponse.getRefreshToken();
			if (verbose) {
				System.out.println("Got a refresh token. Will write it to " + refreshTokenStore.getParent());
			}

			refreshTokenStore.getParentFile().mkdirs();
			PrintWriter fileWriter = null;
			try {
				refreshTokenStore.createNewFile();
				refreshTokenStore.setExecutable(false, false);
				refreshTokenStore.setWritable(true, true);
				refreshTokenStore.setReadable(true, true);
				fileWriter = new PrintWriter(refreshTokenStore);
				fileWriter.print(refreshToken);
			} catch (IOException e) {
				if (verbose) {
					System.err.println("Could write refresh token to store. Cause: " + e.getMessage());
				}
				throw new GoogleConnectorException("An error occurred writing a file.", e);
			} finally {
				if (fileWriter != null) {
					fileWriter.flush();
					fileWriter.close();
				}
			}

			if (verbose) {
				System.out.println("Stored refresh token");
			}
		}

	}

	private ContactFeed produceFeed(int startIndex, int querySize) throws GoogleConnectorException {
		File refreshTokenStore = new File(tokenStore + STORE_DIR + REFRESH_TOKEN_FILE);
		BufferedReader fileReader = null;
		String refreshToken;

		try {
			fileReader = new BufferedReader(new FileReader(refreshTokenStore));
			refreshToken = fileReader.readLine();
		} catch (IOException e) {
			if (verbose) {
				System.err.println("Could not read refresh token from store. Cause: " + e.getMessage());
			}
			throw new GoogleConnectorException("An error occurred reading a file", e);
		} finally {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					// nothing else we can do
				}
			}
		}

		HttpTransport transport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();

		GoogleRefreshTokenRequest refreshTokenRequest = new GoogleRefreshTokenRequest(transport, jsonFactory, refreshToken, clientId, clientSecret);
		GoogleTokenResponse tokenResponse;

		try {
			tokenResponse = refreshTokenRequest.execute();
		} catch (IOException e) {
			if (verbose) {
				System.err.println("Could not read refresh token from store. Cause: " + e.getMessage());
			}
			throw new GoogleConnectorException("An error occurred reading a file", e);
		}
		String at = tokenResponse.getAccessToken();
		if (verbose) {
			System.out.println("Got access token from google.");
		}

		GoogleCredential gc = new GoogleCredential();
		gc.setAccessToken(at);

		ContactsService contactsService = new ContactsService("gc-csv");
		contactsService.setOAuth2Credentials(gc);

		URL feedUrl;
		try {
			feedUrl = new URL("http://www.google.com/m8/feeds/contacts/default/full");
		} catch (MalformedURLException e) {
			if (verbose) {
				System.err.println(String
						.format("Could not create a new URL. This should never happen. Maybe this code got too obsolete and an URL is something different in %s ",
								Calendar.getInstance().get(Calendar.YEAR) + e.getMessage()));
			}
			throw new GoogleConnectorException("Could not read the feed", e);
		}

		String version = "3.0";

		Query myQuery = new Query(feedUrl);
		myQuery.setMaxResults(querySize);
		myQuery.setStartIndex(startIndex);
		myQuery.addCustomParameter(new CustomParameter("v", version));
		 myQuery.addCustomParameter(new CustomParameter("group",
				 "http://www.google.com/m8/feeds/groups/" + email + "/base/" +
				 groupId));
		if ((email != null && email.length() != 0) && (groupId != null && groupId.length() != 0)) {
			myQuery.addCustomParameter(new CustomParameter("group", String.format(groupIdTemplate, email, groupId)));
		}

		ContactFeed queryFeed;
		try {
			queryFeed = contactsService.query(myQuery, ContactFeed.class);
		} catch (IOException | ServiceException e) {
			if (verbose) {
				System.err.println("Could not query google. Cause: " + e.getMessage());
			}
			throw new GoogleConnectorException("Could not query google", e);
		}

		return queryFeed;

	}

	public List<ContactEntry> readContacts() throws GoogleConnectorException {
		this.ensureAccessKey();
		List<ContactEntry> entries = new ArrayList<>();
		int contactsSoFar = 0;
		int totalContacts = 0;

		if (verbose) {
			System.out.println("Will query google with the following parameters:");
			System.out.printf("\tMax Results: %d\n", maxResults);
			System.out.printf("\tPage size: %d\n", pageSize);
			System.out.printf("\tGroup id: %s\n", groupId);
			System.out.printf("\tEmail: %s\n", email);

		}

		do {
			int startIndex = contactsSoFar + 1;
			int querySize = pageSize > (maxResults - contactsSoFar) ? (maxResults - contactsSoFar) : pageSize;
			if (verbose) {
				System.out.printf("Initiating query. Start index: %d, query size: %d\n", startIndex, querySize);
			}
			ContactFeed feed = this.produceFeed(startIndex, querySize);
			entries.addAll(feed.getEntries());
			totalContacts = feed.getTotalResults();
			contactsSoFar = entries.size();
			if (verbose) {
				System.out.println(String.format(
						"Got %d contacts in this page, and %d so far. There is a total of %d contacts to retreive, %s will retreive %d", feed
								.getEntries().size(), contactsSoFar, totalContacts, totalContacts > maxResults ? "but" : "and",
						maxResults > totalContacts ? totalContacts : maxResults));
			}
		} while ((totalContacts > contactsSoFar) && (maxResults > contactsSoFar));

		if (verbose) {
			System.out.printf("Querying finished. Retreived %d contacts\n", contactsSoFar);
		}
		return entries;
	}
}