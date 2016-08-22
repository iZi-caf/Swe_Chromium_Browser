/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package org.chromium.chrome.browser.bookmarks;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.content.Context;

import org.chromium.base.ThreadUtils;
import org.chromium.base.test.util.CommandLineFlags;
import org.chromium.base.test.util.Feature;
import org.chromium.chrome.browser.ChromeSwitches;
import org.chromium.chrome.browser.bookmarks.BookmarkBridge;
import org.chromium.chrome.browser.bookmarks.BookmarkBridge.BookmarkItem;
import org.chromium.chrome.browser.bookmarks.BookmarkBridge.BookmarksCallback;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.components.bookmarks.BookmarkId;
import org.chromium.content.browser.test.NativeLibraryTestBase;
import org.chromium.content.browser.test.util.Criteria;
import org.chromium.content.browser.test.util.CriteriaHelper;
import org.chromium.base.PathUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.Map;

class BookmarkData {
    public static class Bookmark {
        Integer id;
        String title;
        String url;
        boolean is_folder;
        Integer parent_id;
        Integer created_date;
        Integer modified_date;

        private Bookmark(Integer id, String title, String url,
            boolean is_folder, Integer parent_id, Integer created_date,
            Integer modified_date) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.is_folder = is_folder;
            this.parent_id = parent_id;
            this.created_date = created_date;
            this.modified_date = modified_date;
        }
    }

    /*
    -- Bookmarks
       |-- F1
       |   |-- F1 B1 (http://www.f1b1.com)
       |   |-- F1 B2 (http://www.f1b2.com)
       |   `-- F1 F2
       |       |-- F1 F2 B1 (http://www.f1f2b1.com)
       |       `-- F1 F2 B2 (http://www.f1f2b2.com)
       |-- F2
       |-- F3
       |   `-- F3 B1 (f3b1)
       |-- B1 (http://www.b1.com)
    */
    // NOTE: Title has to be unique
    // NOTE: Change CHROME_BOOKMARKS_FILE_DATA if CONTENT is changed
    public static final Bookmark[] CONTENT = {
        new Bookmark(1, "Bookmarks", null, true, null, null, null),
        new Bookmark(2, "F1", null, true, 1, null, null),
        new Bookmark(3, "F1 B1", "http://www.f1b1.com", false, 2, null, null),
        new Bookmark(4, "F1 B2", "http://www.f1b2.com", false, 2, null, null),
        new Bookmark(5, "F1 F2", null, true, 2, null, null),
        new Bookmark(6, "F1 F2 B1", "http://www.f1f2b1.com", false, 5, null, null),
        new Bookmark(7, "F1 F2 B2", "http://www.f1f2b2.com", false, 5, null, null),
        new Bookmark(8, "F2", null, true, 1, null, null),
        new Bookmark(9, "F3", null, true, 1, null, null),
        new Bookmark(10, "F3 B1", "f3b1", false, 9, null, null),  // invalid url
        new Bookmark(11, "B1", "http://www.b1.com", false, 1, null, null)
    };

    public static final int[] CONTENT_ORDER = {0, 2, 6, 8, 7, 9, 3, 4, 1, 5, 10};

    // NOTE: Should be JSON equivalent of CONTENT
    public static final String CHROME_BOOKMARKS_FILE_DATA =
        "{\n" +
        "   \"checksum\": \"2e4219a52d2553b31d3e95e133d0fa1d\",\n" +
        "   \"roots\": {\n" +
        "      \"bookmark_bar\": {\n" +
        "         \"children\": [  ],\n" +
        "         \"date_added\": \"13106718585529803\",\n" +
        "         \"date_modified\": \"0\",\n" +
        "         \"id\": \"1\",\n" +
        "         \"name\": \"Desktop bookmarks\",\n" +
        "         \"type\": \"folder\"\n" +
        "      },\n" +
        "      \"other\": {\n" +
        "         \"children\": [  ],\n" +
        "         \"date_added\": \"13106718585530109\",\n" +
        "         \"date_modified\": \"0\",\n" +
        "         \"id\": \"2\",\n" +
        "         \"name\": \"Other bookmarks\",\n" +
        "         \"type\": \"folder\"\n" +
        "      },\n" +
        "      \"synced\": {\n" +
        "         \"children\": [ {\n" +
        "            \"children\": [ {\n" +
        "               \"date_added\": \"13106718585576681\",\n" +
        "               \"id\": \"7\",\n" +
        "               \"name\": \"F1 B1\",\n" +
        "               \"type\": \"url\",\n" +
        "               \"url\": \"http://www.f1b1.com\"\n" +
        "            }, {\n" +
        "               \"date_added\": \"13106718585576715\",\n" +
        "               \"id\": \"8\",\n" +
        "               \"name\": \"F1 B2\",\n" +
        "               \"type\": \"url\",\n" +
        "               \"url\": \"http://www.f1b2.com\"\n" +
        "            }, {\n" +
        "               \"children\": [ {\n" +
        "                  \"date_added\": \"13106718585577569\",\n" +
        "                  \"id\": \"10\",\n" +
        "                  \"name\": \"F1 F2 B1\",\n" +
        "                  \"type\": \"url\",\n" +
        "                  \"url\": \"http://www.f1f2b1.com\"\n" +
        "               }, {\n" +
        "                  \"date_added\": \"13106718585577604\",\n" +
        "                  \"id\": \"11\",\n" +
        "                  \"name\": \"F1 F2 B2\",\n" +
        "                  \"type\": \"url\",\n" +
        "                  \"url\": \"http://www.f1f2b2.com\"\n" +
        "               } ],\n" +
        "               \"date_added\": \"13106718585576748\",\n" +
        "               \"date_modified\": \"0\",\n" +
        "               \"id\": \"9\",\n" +
        "               \"name\": \"F1 F2\",\n" +
        "               \"type\": \"folder\"\n" +
        "            } ],\n" +
        "            \"date_added\": \"13106718585576389\",\n" +
        "            \"date_modified\": \"0\",\n" +
        "            \"id\": \"6\",\n" +
        "            \"name\": \"F1\",\n" +
        "            \"type\": \"folder\"\n" +
        "         }, {\n" +
        "            \"children\": [  ],\n" +
        "            \"date_added\": \"13106718585576433\",\n" +
        "            \"date_modified\": \"0\",\n" +
        "            \"id\": \"12\",\n" +
        "            \"name\": \"F2\",\n" +
        "            \"type\": \"folder\"\n" +
        "         }, {\n" +
        "            \"children\": [ {\n" +
        "               \"date_added\": \"13106718585577041\",\n" +
        "               \"id\": \"14\",\n" +
        "               \"name\": \"F3 B1\",\n" +
        "               \"type\": \"url\",\n" +
        "               \"url\": \"http://www.f3b1.com\"\n" +
        "            } ],\n" +
        "            \"date_added\": \"13106718585576469\",\n" +
        "            \"date_modified\": \"0\",\n" +
        "            \"id\": \"13\",\n" +
        "            \"name\": \"F3\",\n" +
        "            \"type\": \"folder\"\n" +
        "         }, {\n" +
        "            \"date_added\": \"13106718585576503\",\n" +
        "            \"id\": \"15\",\n" +
        "            \"name\": \"B1\",\n" +
        "            \"type\": \"url\",\n" +
        "            \"url\": \"http://www.b1.com\"\n" +
        "         } ],\n" +
        "         \"date_added\": \"13106718585530119\",\n" +
        "         \"date_modified\": \"0\",\n" +
        "         \"id\": \"3\",\n" +
        "         \"name\": \"Mobile bookmarks\",\n" +
        "         \"type\": \"folder\"\n" +
        "      }\n" +
        "   },\n" +
        "   \"version\": 1\n" +
        "}\n";
}

class SWEBrowserDb {
    public void open(File dbFile) {
        mDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
    }

    public void close() {
        mDb.close();
        mDb = null;
    }

    private SQLiteDatabase mDb;
    private SWEBookmarksTable mBookmarksTable;

    class SWEBookmarksTable {
        public void create() {
            SWEBrowserDb.this.mDb.execSQL(CREATE_SQL);
        }

        public boolean exists() {
            boolean exists = false;
            Cursor c = SWEBrowserDb.this.mDb.rawQuery(EXISTS_SQL, null);
            if (null != c) {
                exists = (c.getCount() > 0);
                c.close();
            }

            return exists;
        }

        public boolean insertContent(final BookmarkData.Bookmark[] content,
            final int[] contentOrder) {
            boolean r = true;
            for (int o = 0; o < contentOrder.length; o++) {
                int i = contentOrder[o];

                r = insertBookmark(content[i]);
                if (!r)
                    break;
            }

            return r;
        }

        private boolean insertBookmark(BookmarkData.Bookmark b) {
            boolean r = false;

            ContentValues vals = new ContentValues();
            vals.put("_id", b.id);
            if (null != b.title)
                vals.put("title", b.title);
            else
                vals.putNull("title");
            if (null != b.url)
                vals.put("url", b.url);
            else
                vals.putNull("url");
            vals.put("folder", b.is_folder ? 1 : 0);
            vals.put("parent", b.parent_id);
            vals.put("position", 0);
            if (null != b.created_date)
                vals.put("created", b.created_date);
            else
                vals.putNull("created");
            if (null != b.modified_date)
                vals.put("modified", b.modified_date);
            else
                vals.putNull("modified");
            try {
                SWEBrowserDb.this.mDb.insertOrThrow(TABLE_NAME, null, vals);
                r = true;
            }
            catch (SQLException errorStr) {
                r = false;
            }

            return r;
        }

        private static final String CREATE_SQL = "CREATE TABLE bookmarks(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "title TEXT," +
            "url TEXT," +
            "folder INTEGER NOT NULL DEFAULT 0," +
            "parent INTEGER," +
            "position INTEGER NOT NULL," +
            "insert_after INTEGER," +
            "deleted INTEGER NOT NULL DEFAULT 0," +
            "account_name TEXT," +
            "account_type TEXT," +
            "sourceid TEXT," +
            "version INTEGER NOT NULL DEFAULT 1," +
            "created INTEGER," +
            "modified INTEGER," +
            "dirty INTEGER NOT NULL DEFAULT 0," +
            "sync1 TEXT," +
            "sync2 TEXT," +
            "sync3 TEXT," +
            "sync4 TEXT," +
            "sync5 TEXT)";
        private static final String EXISTS_SQL = "SELECT name " + "FROM sqlite_master " +
            "WHERE type=\"table\" AND name=\"bookmarks\" " +
            "COLLATE NOCASE";
        private static final String TABLE_NAME = "bookmarks";
    }
}

public class MigrateSWEBookmarksTest extends NativeLibraryTestBase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Context targetCtx = getInstrumentation().getTargetContext();
        mChromeBookmarksFile = new File(
            targetCtx.getDir(PRIVATE_DATA_SUFFIX, targetCtx.MODE_PRIVATE),
            PRIVATE_DATA_PROFILE_DIR + File.separatorChar + CHROME_BOOKMARKS_FILE);
        mSweBrowserDbDir = new File(targetCtx.getDatabasePath("foo").getParent());
        mSweBrowserDbFile = new File(mSweBrowserDbDir.getPath(),
            SWE_BROWSER_DB_FILE);

        RenameFile(mChromeBookmarksFile);
        RenameFile(mSweBrowserDbDir);

        File chromeBookmarksFileDir = new File(mChromeBookmarksFile.getParent());
        chromeBookmarksFileDir.mkdir();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        RenameFileBack(mChromeBookmarksFile);
        RenameFileBack(mSweBrowserDbDir);

        mSweBrowserDbFile = null;
        mSweBrowserDbDir = null;
        mChromeBookmarksFile = null;
    }

    @SmallTest
    @Feature({"Bookmark"})
    public void testDatabasesDirAbsent() throws Exception {
        preTestRun();
        generateChromeBookmarksFile();
        loadNativeLibraryAndInitBrowserProcess();
        loadBookmarksModel();
        verifyChromeBookmarks(false);
        unloadBookmarksModel();
        postTestRun();
    }

    @SmallTest
    @Feature({"Bookmark"})
    public void testSWEBookmarksDbFileAbsent() throws Exception {
        preTestRun();
        mSweBrowserDbDir.mkdir();
        generateChromeBookmarksFile();
        loadNativeLibraryAndInitBrowserProcess();
        loadBookmarksModel();
        verifyChromeBookmarks(false);
        unloadBookmarksModel();
        postTestRun();
    }

    @SmallTest
    @Feature({"Bookmark"})
    public void testEmptyBookmarksTable() throws Exception {
        preTestRun();
        mSweBrowserDbDir.mkdir();
        generateSWEBrowserDbFile();
        generateChromeBookmarksFile();
        loadNativeLibraryAndInitBrowserProcess();
        loadBookmarksModel();
        verifyChromeBookmarks(false);
        postTestRun();
        unloadBookmarksModel();
    }

    @SmallTest
    @Feature({"Bookmark"})
    public void testNormalMigration() throws Exception {
        preTestRun();
        mSweBrowserDbDir.mkdir();
        generateSWEBrowserDbFile();
        generateSWEBookmarksTable();
        loadNativeLibraryAndInitBrowserProcess();
        loadBookmarksModel();
        verifyChromeBookmarks(false);
        verifySWEBookmarksTable();
        unloadBookmarksModel();
        postTestRun();
    }

    @SmallTest
    @Feature({"Bookmark"})
    public void testNormalMigrationMerge() throws Exception {
        preTestRun();
        mSweBrowserDbDir.mkdir();
        generateSWEBrowserDbFile();
        generateSWEBookmarksTable();
        generateChromeBookmarksFile();
        loadNativeLibraryAndInitBrowserProcess();
        loadBookmarksModel();
        verifyChromeBookmarks(true);
        verifySWEBookmarksTable();
        unloadBookmarksModel();
        postTestRun();
    }

    private void preTestRun() {
        assertFalse("Folder shouldn't exist: " + mSweBrowserDbDir.getPath(),
            mSweBrowserDbDir.exists());
        assertFalse("File shouldn't exist: " + mChromeBookmarksFile.getPath(),
            mChromeBookmarksFile.exists());
    }

    private void postTestRun() {
        mChromeBookmarksFile.delete();
        mSweBrowserDbFile.delete();
        File sweBrowserDbFileJournal = new File(mSweBrowserDbDir.getPath(),
            SWE_BROWSER_DB_FILE_JOURNAL);
        sweBrowserDbFileJournal.delete();
        mSweBrowserDbDir.delete();
    }

    private void loadBookmarksModel() throws Exception {
        ThreadUtils.runOnUiThreadBlocking(new Runnable() {
            @Override
            public void run() {
                Profile profile = Profile.getLastUsedProfile();
                sBookmarkModel = new BookmarkModel(profile);
                sBookmarkModel.loadEmptyPartnerBookmarkShimForTesting();
            }
        });

        CriteriaHelper.pollUiThread(new Criteria() {
            @Override
            public boolean isSatisfied() {
                return sBookmarkModel.isBookmarkModelLoaded() &&
                    mChromeBookmarksFile.exists();
            }
        });
    }

    private void unloadBookmarksModel() {
        sBookmarkModel.destroy();
        sBookmarkModel = null;
    }

    private void generateChromeBookmarksFile() {
        try {
            FileWriter fw = new FileWriter(mChromeBookmarksFile);
            fw.write(BookmarkData.CHROME_BOOKMARKS_FILE_DATA, 0,
                BookmarkData.CHROME_BOOKMARKS_FILE_DATA.length());
            fw.close();
        }
        catch (Exception e) {
            fail("Chrome Bookmark file generation failed");
        }
    }

    private void generateSWEBrowserDbFile() {
        SWEBrowserDb sweDb = new SWEBrowserDb();
        sweDb.open(mSweBrowserDbFile);
        sweDb.close();

        assertTrue("File should exist: " + mSweBrowserDbFile.getPath(),
            mSweBrowserDbFile.exists());
    }

    private void generateSWEBookmarksTable() {
        SWEBrowserDb sweDb = new SWEBrowserDb();
        SWEBrowserDb.SWEBookmarksTable bookmarksTable =
            sweDb.new SWEBookmarksTable();

        sweDb.open(mSweBrowserDbFile);

        assertFalse("Table shouldn't exist", bookmarksTable.exists());
        bookmarksTable.create();
        assertTrue(bookmarksTable.insertContent(BookmarkData.CONTENT,
            BookmarkData.CONTENT_ORDER));
        assertTrue("Table should exist", bookmarksTable.exists());

        sweDb.close();
    }

    private void verifyChromeBookmarks(boolean merge) throws Exception {
        Map<String, Integer> visitMap = new HashMap<String, Integer>();
        visitMap.put(BookmarkData.CONTENT[0].title, 1);
        for (int i = 0; i < BookmarkData.CONTENT.length; i++) {
            visitMap.put(BookmarkData.CONTENT[i].title, 0);
        }

        BookmarkId rootId = sBookmarkModel.getMobileFolderId();
        makeVisitCount(rootId, visitMap);

        // Start from 1 since root node of SWE bookmarks, which is titled
        // "Bookmarks" is skipped by SWE Bookmark migration process.
        for (int i = 1; i < BookmarkData.CONTENT.length; i++) {
            assertEquals("Bookmark visited exactly once",
                merge ? 2 : 1,
                (int)visitMap.get(BookmarkData.CONTENT[i].title));
        }
    }

    private void verifySWEBookmarksTable() {
        SWEBrowserDb sweDb = new SWEBrowserDb();
        SWEBrowserDb.SWEBookmarksTable bookmarksTable =
            sweDb.new SWEBookmarksTable();

        sweDb.open(mSweBrowserDbFile);
        assertFalse("Table shouldn't exist", bookmarksTable.exists());
        sweDb.close();
    }

    private void makeVisitCount(final BookmarkId rootId, Map<String, Integer> visitMap) {
        List<BookmarkItem> folderItems = getBookmarksForFolder(rootId);
        ListIterator<BookmarkItem> it = folderItems.listIterator();
        while (it.hasNext()) {
            BookmarkItem item = it.next();
            if (!item.getTitle().isEmpty()) {
                Integer newCount = visitMap.get(item.getTitle()) + 1;
                visitMap.put(item.getTitle(), newCount);
            }
            if (item.isFolder())
                makeVisitCount(item.getId(), visitMap);
        }
    }

    private List<BookmarkItem> getBookmarksForFolder(BookmarkId folderId) {
        final List<BookmarkItem> items = new ArrayList<BookmarkItem>();

        BookmarksCallback callbackWrapper = new BookmarksCallback() {
            @Override
            public void onBookmarksFolderHierarchyAvailable(BookmarkId folderId,
                List<BookmarkItem> bookmarksList) {
            }

            @Override
            public void onBookmarksAvailable(BookmarkId folderId,
                List<BookmarkItem> bookmarksList) {
                items.addAll(bookmarksList);
            }
        };

        sBookmarkModel.getBookmarksForFolder(folderId, callbackWrapper);
        return items;
    }

    private static void RenameFile(File f) {
        File orgFile = new File(f.getPath());
        File backupFile = new File(orgFile.getParent(),
            orgFile.getName() + TEMP_SUFFIX);
        if (orgFile.exists())
            assertTrue("Rename should succeed", orgFile.renameTo(backupFile));
    }

    private static void RenameFileBack(File f) {
        File orgFile = new File(f.getPath());
        File backupFile = new File(orgFile.getParent(),
            orgFile.getName() + TEMP_SUFFIX);
        orgFile.delete();
        if (backupFile.exists())
            assertTrue("Rename should succeed", backupFile.renameTo(orgFile));
    }

    private static final String PRIVATE_DATA_SUFFIX = "chrome";
    private static final String PRIVATE_DATA_PROFILE_DIR = "Default";
    private static final String CHROME_BOOKMARKS_FILE = "Bookmarks";
    private static final String SWE_BROWSER_DB_FILE = "browser2.db";
    private static final String SWE_BROWSER_DB_FILE_JOURNAL = "browser2.db-journal";
    private static final String TEMP_SUFFIX = ".migrate_swe_bk";

    private File mChromeBookmarksFile;
    private File mSweBrowserDbDir;
    private File mSweBrowserDbFile;
    private BookmarkModel sBookmarkModel;
}
