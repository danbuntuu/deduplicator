package com.danbuntu;

import org.apache.commons.cli.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Dan Griffin on 9/6/2016.
 * Have a great day!
 */
public class Deduplicator {

    private final static String usage = "deduplicator -rd[qv]f /path/to/folder";

    public final static int QUIET = 0;
    public final static int NORMAL = 1;
    public final static int VERBOSE = 2;
    private final static int ERROR = -1;
    private int mLogLevel = NORMAL;

    private long duplicatedBytes;
    private File mFolder;
    private boolean mRecursive;
    private boolean mDelete = false;
    private HashMap<Long, ArrayList<File>> mSizeDuplicates;
    private ArrayList<File> mDuplicates;

    public Deduplicator(String path) {
        this(new File(path));
    }

    public Deduplicator(File folder) {
        mFolder = folder.getAbsoluteFile();
        mRecursive = false;
        mSizeDuplicates = new HashMap<>();
        mDuplicates = new ArrayList<>();
        duplicatedBytes = 0;
    }

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
            formatter.printHelp(usage, options);

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
        Deduplicator dedup = new Deduplicator(folder);
        dedup.setRecursive(cmd.hasOption("r"));
        dedup.setDelete(cmd.hasOption("d"));
        dedup.setLogLevel(logLevel);
        dedup.deduplicate();

    }

    public void setLogLevel(int logLevel) {
        mLogLevel = logLevel;
    }

    public void setDelete(boolean set) {
        mDelete = set;
    }

    public void setRecursive(boolean set) {
        mRecursive = set;
    }

    public void deduplicate() {

        long startTime = System.currentTimeMillis();

        // first check to see which files have matching sizes
        // much quicker than finding a checksum for each file
        scanSizeMatches(mFolder);

        for(long size : mSizeDuplicates.keySet()) {

            // this represents a list of files with the same size
            ArrayList<File> sizeDuplicates = mSizeDuplicates.get(size);

            if(sizeDuplicates != null && sizeDuplicates.size() > 1) {
                log("checking: " + sizeDuplicates.size() + " files with matching sizes", VERBOSE);

                // run a checksum on each of the files with matching sizes
                scanFileChecksums(sizeDuplicates, size);
            }
        }

        if(!mDelete) {
            for (File f : mDuplicates) {
                log("duplicate file: " + f.getAbsolutePath(), NORMAL);
            }
        } else {
            deleteFiles(mDuplicates);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        log("Found: " + mDuplicates.size() + " duplicate files taking up " + Utils.readableStorageSpace(duplicatedBytes), NORMAL);
        log("Took: " + Utils.readableDuration(duration), NORMAL);
    }

    private void deleteFiles(ArrayList<File> deadFilesWalking) {
        for(File f : deadFilesWalking) {
            try {
                if(f.delete()) {
                    log("Deleted file: " + f.getAbsolutePath(), NORMAL);
                } else {
                    log("Failed to delete file: " + f.getAbsolutePath(), ERROR);
                }
            } catch (SecurityException e) {
                log("Failed to delete file: " + f.getAbsolutePath(), ERROR);
            }
        }
    }

    private void scanFileChecksums(ArrayList<File> files, long size) {
        if(files == null) return;
        ArrayList<String> hashes = new ArrayList<>();

        for(File file : files) {
            String checksum = Utils.sha256file(file.getAbsolutePath());
            if(checksum == null) continue;

            // if the file checksum is in the hashes array list, treat it as a duplicate
            // this will leave only 1 file per unique checksum
            if(hashes.contains(checksum)) {
                mDuplicates.add(file);
                duplicatedBytes += size;
            } else {
                hashes.add(checksum);
            }
        }
    }

    private void scanSizeMatches(File folder) {
        File[] list = folder.listFiles();

        if(list == null) return;

        log("scanning: " + folder.getAbsolutePath(), VERBOSE);

        ArrayList<File> matches;
        for(File f : list) {
            if(f.isFile()) {

                long fileSize = Utils.getFileSize(f);

                if(mSizeDuplicates.containsKey(fileSize)) {
                    mSizeDuplicates.get(fileSize).add(f);

                } else {
                    matches = new ArrayList<>();
                    matches.add(f);
                    mSizeDuplicates.put(fileSize, matches);
                }

            } else if(mRecursive) {
                scanSizeMatches(f);
            }
        }
    }

    private void log(String message, int level) {
        if(mLogLevel == QUIET) return;

        // log everything that is under or equal to the set log level
        if(level == ERROR) {
            System.err.print(message);
        } else if(level <= mLogLevel) {
            System.out.println(message);
        }
    }
}
