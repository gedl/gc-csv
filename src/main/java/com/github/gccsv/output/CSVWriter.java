package com.github.gccsv.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.gccsv.GC_FIELD;
import com.google.gdata.data.contacts.ContactEntry;

public class CSVWriter {

	private boolean deaccent = true;
	private Map<String, GC_FIELD> mapping;
	private String fieldSepparator = ",";
	private String newLine = System.getProperty("line.separator");

	private StringBuffer csv = new StringBuffer();
	private int numberIndex;
	private int emailIndex;
	private boolean verbose;

	public CSVWriter(Map<String, GC_FIELD> mapping, boolean deaccent) {
		this.deaccent = deaccent;
		this.mapping = mapping;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public void write(List<ContactEntry> entries, OutputStream outputStream) throws CVSWriterException {
		if (verbose) {
			System.out.printf("Got %d entries to write\n", entries.size());
		}
		if (csv == null || csv.length() == 0) {
			csv = this.generateCSV(entries);
		}
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
			bufferedWriter.write(csv.toString());
			bufferedWriter.flush();
		} catch (IOException e) {
			if (verbose) {
				System.err.println("Could not write to file. Cause: " + e.getMessage());
			}
			throw new CVSWriterException(e.getMessage(), e);
		}

	}

	public void write(List<ContactEntry> entries, String destination) throws CVSWriterException {
		if (verbose) {
			System.out.println("Will write CSV file to " + destination);
		}
		try {
			FileOutputStream fos = new FileOutputStream(new File((destination)));
			this.write(entries, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			if (verbose) {
				System.err.println("Could not write to file. Cause: " + e.getMessage());
			}
			throw new CVSWriterException(e.getMessage(), e);
		} catch (IOException e) {
			// nothing else we can do
		}
	}

	private void appendEnclosed(StringBuffer buffer, String text) {
		buffer.append("\"").append(text).append("\"");
	}

	private String deAccent(String str) {
		String result = str;
		String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
		Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
		result = pattern.matcher(nfdNormalizedString).replaceAll("");
		if (verbose && (!result.equals(str))) {
			System.out.printf("De-accented  %s to %s\n", str, result);
		}
		return result;
	}

	private void appendHeader(StringBuffer buffer) {
		for (String field : mapping.keySet()) {
			appendEnclosed(buffer, field);
			buffer.append(fieldSepparator);
		}
		buffer.setLength(buffer.length() - 1);
	}

	private StringBuffer generateCSV(List<ContactEntry> entries) {
		StringBuffer buffer = new StringBuffer();

		appendHeader(buffer);
		buffer.append(newLine);

		for (ContactEntry entry : entries) {
			numberIndex = 0;
			emailIndex = 0;
			for (String field : mapping.keySet()) {
				GC_FIELD source = mapping.get(field);
				appendEnclosed(buffer, readField(entry, source));
				buffer.append(fieldSepparator);
			}
			buffer.setLength(buffer.length() - 1);
			buffer.append(newLine);
		}

		return buffer;
	}

	private String readField(ContactEntry entry, GC_FIELD source) {
		String result = "";

		switch (source) {
		case NAME:
			String title = entry.getTitle().getPlainText();
			result = deaccent ? deAccent(title) : title;
			break;
		case PHONE_NUMBER:
			if (numberIndex < entry.getPhoneNumbers().size()) {
				result = entry.getPhoneNumbers().get(numberIndex).getPhoneNumber();
			}
			numberIndex++;
			break;
		case EMAIL:
			if (emailIndex < entry.getEmailAddresses().size()) {
				result = entry.getEmailAddresses().get(emailIndex).getAddress();
			}
			emailIndex++;
			break;
		case NONE:
			break;
		}

		return result;
	}
}
