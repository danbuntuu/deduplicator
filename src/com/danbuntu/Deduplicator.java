package com.danbuntu;

import java.io.File;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Dan Griffin on 9/6/2016.
 * Have a great day!
 */
class Deduplicator {

    final static int ERROR = -1;
    final static int QUIET = 0;
    final static int NORMAL = 1;
    final static int VERBOSE = 2;
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

    Deduplicator(File folder) {
        mFolder = folder.getAbsoluteFile();
        mRecursive = false;
        mSizeDuplicates = new HashMap<>();
        mDuplicates = new ArrayList<>();
        duplicatedBytes = 0;
    }

    void setLogLevel(int logLevel) {
        mLogLevel = logLevel;
    }

    void setDelete(boolean set) {
        mDelete = set;
    }

    void deduplicate() {

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

    void setRecursive(boolean set) {
        mRecursive = set;
    }
}
