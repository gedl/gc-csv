package com.github.gccsv;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable.PrintMode;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.gccsv.output.CSVWriter;
import com.github.gccsv.output.CVSWriterException;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.util.NotImplementedException;

public class Main {
	private CommandLine commandLine;

	public Main(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	private static Map<String, GC_FIELD> DEFAULT_MAPPING;
	private static int DEFAULT_PAGE_SIZE = 50;
	private static int DEFAULT_MAX_RESULTS = Integer.MAX_VALUE;

	static {
		DEFAULT_MAPPING = new LinkedHashMap<>();
		DEFAULT_MAPPING.put("Name", GC_FIELD.NAME);
		DEFAULT_MAPPING.put("Number 1", GC_FIELD.PHONE_NUMBER);
		DEFAULT_MAPPING.put("Number 2", GC_FIELD.PHONE_NUMBER);
		DEFAULT_MAPPING.put("Email", GC_FIELD.EMAIL);
	}

	private static String defaultMappingAsString() {
		StringBuffer buffer = new StringBuffer();
		for (String key : DEFAULT_MAPPING.keySet()) {
			buffer.append(DEFAULT_MAPPING.get(key)).append("=>").append(key).append(", ");
		}
		buffer.setLength(buffer.length() - 2);

		return buffer.toString();
	}

	private static String defaultHeaderAsString() {
		StringBuffer buffer = new StringBuffer();
		for (String key : DEFAULT_MAPPING.keySet()) {
			buffer.append(key).append(", ");
		}
		buffer.setLength(buffer.length() - 2);

		return buffer.toString();
	}

	private static String gcFieldsAsString() {
		StringBuffer buffer = new StringBuffer();
		for (GC_FIELD value : GC_FIELD.values()) {
			buffer.append(value).append(", ");
		}

		buffer.setLength(buffer.length() - 2);

		return buffer.toString();
	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.setWidth(100);
		formatter.printHelp(" ", options, true);
	}

	private static CommandLine parseOptions(String[] args) {
		Options options = new Options();

		options.addOption(Option.builder("h").optionalArg(true).longOpt("help").desc("Prints this message and exits").build());

		OptionGroup clientId = new OptionGroup();
		clientId.setRequired(true);
		clientId.addOption(Option.builder("C").longOpt("client-id").desc("The OAuth v2 client id").hasArg().argName("id").build());
		clientId.addOption(Option.builder("c").longOpt("client-id-file").desc("The file containing the OAuth v2 client id").hasArg()
				.argName("path").build());
		options.addOptionGroup(clientId);

		OptionGroup clientSecret = new OptionGroup();
		clientSecret.setRequired(true);
		clientSecret.addOption(Option.builder("s").longOpt("client-secret").desc("The OAuth v2 client secret").hasArg().argName("secret").build());
		clientSecret.addOption(Option.builder("S").longOpt("client-secret-file").desc("The file containing the OAuth v2 client secret").hasArg()
				.argName("path").build());
		options.addOptionGroup(clientSecret);

		options.addOption(Option.builder().optionalArg(true).longOpt("page-size")
				.desc(String.format("The page size for the Google Contacts request (default is %d)", DEFAULT_PAGE_SIZE)).hasArg().argName("size")
				.type(Integer.class).build());
		options.addOption(Option.builder().optionalArg(true).longOpt("max-results")
				.desc(String.format("The maximum number of contacts to fetch from Google Contacts (default is %d)",

				DEFAULT_MAX_RESULTS)).hasArg().argName("max").type(Integer.class).build());

		options.addOption(Option.builder().longOpt("group-id").desc("The group id to filter the contacts with (requires email)").hasArg()
				.argName("id").build());
		options.addOption(Option.builder().longOpt("email").desc("The email to filter the contacts with (requires group-id)").hasArg()
				.argName("email").build());

		options.addOption(Option.builder("f").longOpt("output-file")
				.desc("The file to write the CSV output to. If not set, no file will be written.").hasArg().argName("path").build());
		options.addOption(Option.builder("p").longOpt("print").desc("Whether to print the output").build());
		options.addOption(Option.builder().longOpt("deaccent").desc("Replace diacritics into plain ASCII counterparts").build());

		options.addOption(Option
				.builder()
				.longOpt("output-header")
				.desc("The header of the CSV file. Arity has to match the mapping. Do not use commas, enclose names with spaces in quotes. Default is "
						+ defaultHeaderAsString()).hasArgs().argName("header").build());

		options.addOption(Option
				.builder()
				.longOpt("output-mapping")
				.desc(String
						.format("The ordered google contacts field name to populate the columns. Arity has to match number of fields specified in the header. Repeat google contact fields to iterate lists. Default is %s. Valid fields are: %s (case sensitive).",
								defaultMappingAsString(), gcFieldsAsString())).hasArgs().argName("mapping").build());

		options.addOption(Option.builder("d").optionalArg(true).longOpt("storage-dir")
				.desc("The directory to keep the OAuth V2refresh token in. It is recommended to use a full path with leading and trailing slash")
				.required().hasArg().argName("path").build());

		options.addOption(Option.builder("v").optionalArg(true).longOpt("verbose")
				.desc("Verbose mode. Includes java stack traces if an exception occurs").build());

		CommandLine commandLine = null;
		try {
			CommandLineParser parser = new GCToCSVParser();
			commandLine = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			printHelp(options);
		}

		if (commandLine.hasOption("h")) {
			printHelp(options);
			System.exit(0);
		}
		return commandLine;
	}

	public static void main(String[] args) throws NotImplementedException {
		CommandLine commandLine = Main.parseOptions(args);
		if (commandLine == null) {
			System.exit(1);
		}

		Main main = new Main(commandLine);
		try {
			main.execute();
		} catch (GoogleConnectorException | CVSWriterException e) {
			if (commandLine.hasOption("v")) {
				e.printStackTrace();
			}
			System.err.println(String.format("Finished unsucessfully. Cause: %s. Use -v for more details.", e.getMessage()));
			System.exit(2);
		}
		System.out.println("\n\nDone.");
		System.exit(0);
	}

	private void execute() throws GoogleConnectorException, CVSWriterException, NotImplementedException {
		boolean verbose = false;
		if (commandLine.hasOption("v")) {
			verbose = true;
		}
		String clientId = null;
		String clientSecret = null;
		List<ContactEntry> contacts = null;

		if (commandLine.hasOption("C")) {
			clientId = commandLine.getOptionValue("C");
		}
		if (commandLine.hasOption("S")) {
			clientSecret = commandLine.getOptionValue("S");
		}

		if (commandLine.hasOption("c") || commandLine.hasOption("s")) {
			throw new NotImplementedException(
					"Reading client id and secret from file is not implemented. Use 'C' and 'S' to read it from the command line instead");
		}

		if (clientId != null && clientSecret != null) {
			int pageSize = commandLine.hasOption("page-size") ? new Integer(commandLine.getOptionValue("page-size")) : DEFAULT_PAGE_SIZE;
			int maxResults = commandLine.hasOption("max-results") ? new Integer(commandLine.getOptionValue("max-results")) : DEFAULT_MAX_RESULTS;

			GCConnector connector = new GCConnector(clientId, clientSecret, pageSize, maxResults, commandLine.getOptionValue("d"));
			connector.setVerbose(verbose);
			if (commandLine.hasOption("group-id") && commandLine.hasOption("email")) {
				connector.setGroupId(commandLine.getOptionValue("group-id"));
				connector.setEmail(commandLine.getOptionValue("email"));
			}
			if (verbose) {
				System.out.println("#Stage 1 - reading the google contacts feed");
			}
			contacts = connector.readContacts();
		}
		if (contacts != null) {
			Map<String, GC_FIELD> mapping = DEFAULT_MAPPING;
			if (commandLine.hasOption("output-header") && commandLine.hasOption("output-mapping")) {
				String[] columns = commandLine.getOptionValues("output-header");
				String[] fields = commandLine.getOptionValues("output-mapping");
				if (columns.length == fields.length) {
					mapping.clear();
					for (int i = 0; i < columns.length; i++) {
						mapping.put(columns[i], GC_FIELD.valueOf(fields[i]));
					}
				}
			}

			CSVWriter writer = new CSVWriter(mapping, commandLine.hasOption("deaccent"));
			writer.setVerbose(verbose);
			if (verbose) {
				System.out.printf("\n\n\n");
				System.out.println("#Stage 2 - writing the results");
			}
			if (commandLine.hasOption("f")) {
				writer.write(contacts, commandLine.getOptionValue("f"));
			}
			if (commandLine.hasOption("p")) {
				System.out.printf("\n-----\nResult:\n");
				writer.write(contacts, System.out);
			}
		}
	}
}
