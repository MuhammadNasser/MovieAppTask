package com.android.movieapptask.models;

import android.database.Cursor;


import com.android.movieapptask.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Movie implements Serializable {

    private String id;
    private String title; // original_title
    private String image; // poster_path
    private int rating; // vote_average
    private String releaseDate; // release_date
    private String Website; // release_date

    /**
     * Constructor for a movie object it's fill data in Strings from JSONObject.
     */
    public Movie(JSONObject movie) throws JSONException {
        this.id = movie.optString("imdbID");
        this.title = movie.optString("Title");
        this.image = movie.optString("Poster");
        this.rating = movie.optInt("imdbRating");
        this.releaseDate = movie.optString("Released");
        this.Website = movie.optString("Website");
    }

    /**
     * Constructor for a movie object it's fill data in Strings from Cursor.
     */
    public Movie(Cursor cursor) {
        this.id = cursor.getString(MainActivity.COL_MOVIE_ID);
        this.title = cursor.getString(MainActivity.COL_TITLE);
        this.image = cursor.getString(MainActivity.COL_IMAGE);
        this.rating = cursor.getInt(MainActivity.COL_RATING);
        this.releaseDate = cursor.getString(MainActivity.COL_DATE);
        this.Website = cursor.getString(MainActivity.COL_WEBSITE);
    }

    /**
     * Gets the data that's added in Strings.
     *
     * @return Strings data
     */
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public int getRating() {
        return rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getWebsite() {
        return Website;
    }
}
