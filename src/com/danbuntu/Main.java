package com.danbuntu;

import org.apache.commons.cli.*;
import java.io.File;

public class Main {

    private final static String name = "deduplicator";

    public static void main(String[] args) {
        Options options = new Options();

        Option input = new Option("f", "folder", true, "folder to scan");
        input.setRequired(true);
        options.addOption(input);

        Option recursive = new Option("r", "recursive", false, "recurse directories");
        recursive.setRequired(false);
        options.addOption(recursive);

        Option delete = new Option("d", "delete", false, "delete the duplicate files that are found, " +
                "without this, nothing will be deleted. be careful when using this with the -r switch!");
        delete.setRequired(false);
        options.addOption(delete);

        Option quiet = new Option("q", "quiet", false, "quiet mode, don't print anything");
        quiet.setRequired(false);
        options.addOption(quiet);

        Option verbose = new Option("v", "verbose", false, "verbose mode, print everything");
        verbose.setRequired(false);
        options.addOption(verbose);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(name, options);

            System.exit(1);
            return;
        }

        String folderName = cmd.getOptionValue("f");
        File folder = new File(folderName);

        if(!folder.isDirectory()) {
            System.out.println("Not a folder: " + folderName);
            System.exit(1);
        }

        int logLevel = Deduplicator.NORMAL;
        if(cmd.hasOption("q")) {
            logLevel = Deduplicator.QUIET;
        } else if(cmd.hasOption("v")) {
            logLevel = Deduplicator.VERBOSE;
        }

        // run the actual deduplication
        deduplicate(folder, cmd.hasOption("r"), cmd.hasOption("d"), logLevel);

    }

    private static void deduplicate(File folder, boolean recursive, boolean delete, int logLevel) {
        Deduplicator dedup = new Deduplicator(folder);
        dedup.setRecursive(recursive);
        dedup.setDelete(delete);
        dedup.setLogLevel(logLevel);
        dedup.deduplicate();
    }
}
