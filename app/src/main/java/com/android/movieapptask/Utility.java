package com.android.movieapptask;

import android.content.Context;
import android.database.Cursor;

import com.android.movieapptask.data.MovieContract;

public class Utility {

    public static int isFavorite(Context context, String id) {
        Cursor cursor = context.getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,   // projection
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?", // selection
                new String[]{id},   // selectionArgs
                null    // sort order
        );
        int numRows = 0;
        if (cursor != null) {
            numRows = cursor.getCount();
            cursor.close();
        }
        return numRows;
    }
}
