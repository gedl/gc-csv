package com.github.gccsv;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class GCToCSVParser extends DefaultParser {
	@Override
	public CommandLine parse(Options options, String[] arguments, Properties properties, boolean stopAtNonOption) throws ParseException {
		CommandLine commandLine = super.parse(options, arguments, properties, stopAtNonOption);
		this.checkCombinations(commandLine);

		return commandLine;
	}

	private void checkCombinations(CommandLine commandLine) throws ParseException {
		this.checkGroupOptions(commandLine);
		this.checkNumericOption(commandLine, "page-size");
		this.checkNumericOption(commandLine, "max-results");
		this.checkHeaderAndFields(commandLine);

	}

	private void checkHeaderAndFields(CommandLine commandLine) throws ParseException {
		if (commandLine.hasOption("output-header") || commandLine.hasOption("output-mapping")) {
			if (!commandLine.hasOption("output-header")) {
				throw new ParseException("Missing mandatory option: output-header. This option is mandatory because output-mapping was specified");
			}
			if (!commandLine.hasOption("output-mapping")) {
				throw new ParseException("Missing mandatory option: output-mapping. This option is mandatory because output-header was specified");
			}
			if (commandLine.hasOption("output-header") && commandLine.hasOption("output-mapping")) {
				int headerColumns = commandLine.getOptionValues("output-header").length;
				int mappingFields = commandLine.getOptionValues("output-mapping").length;
				if (headerColumns != mappingFields) {
					throw new ParseException(String.format(
							"Specified % header columns and %d fields. The number of fields has to match the number of header columns.",
							headerColumns, mappingFields));
				}
			}
			if (commandLine.hasOption("output-mapping")) {
				String[] mappingFields = commandLine.getOptionValues("output-mapping");
				for (String mappingField : mappingFields) {
					try {
						GC_FIELD.valueOf(mappingField);
					} catch (IllegalArgumentException e) {
						throw new ParseException(mappingField + " is not a valid google contacts field. Check help for valid options.");
					}

				}
			}
		}

	}

	private void checkNumericOption(CommandLine commandLine, String optionName) throws ParseException {
		if (commandLine.hasOption(optionName)) {
			try {
				Integer optionValue = new Integer(commandLine.getOptionValue(optionName));
				if (optionValue <= 0) {
					throw new ParseException(String.format("%s has to be a positive integer.", optionName));
				}
			} catch (NumberFormatException e) {
				throw new ParseException(String.format("%s has to be a valid integer.", optionName));
			}
		}
	}

	private void checkGroupOptions(CommandLine commandLine) throws ParseException {
		if (commandLine.hasOption("group-id") || commandLine.hasOption("email")) {
			if (!commandLine.hasOption("group-id")) {
				throw new ParseException("Missing mandatory option: group-id. This option is mandatory because email was specified");
			}
			if (!commandLine.hasOption("email")) {
				throw new ParseException("Missing mandatory option: email. This option is mandatory because group-id was specified");
			}

		}
	}

}
