package com.android.movieapptask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.movieapptask.adapters.HomeMoviesAdapter;
import com.android.movieapptask.data.MovieContract;
import com.android.movieapptask.models.Movie;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_MOVIE_ID,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_IMAGE,
            MovieContract.MovieEntry.COLUMN_RATING,
            MovieContract.MovieEntry.COLUMN_DATE,
            MovieContract.MovieEntry.COLUMN_WEBSITE
    };


    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.linearLayout)
    LinearLayout linearLayout;
    @BindView(R.id.editTextSearch)
    EditText editTextSearch;
    @BindView(R.id.imageViewSearch)
    ImageView imageViewSearch;

    private boolean isFavorites = false;

    public static final int COL_MOVIE_ID = 1;
    public static final int COL_TITLE = 2;
    public static final int COL_IMAGE = 3;
    public static final int COL_RATING = 4;
    public static final int COL_DATE = 5;
    public static final int COL_WEBSITE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set status bar Transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            int color;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                color = getResources().getColor(R.color.redTransparent, null);
            } else {
                // noinspection deprecation
                color = getResources().getColor(R.color.redTransparent);
            }
            getWindow().setStatusBarColor(color);
        }
        setContentView(R.layout.activity_main);
        setToolBar();
        ButterKnife.bind(this);

        imageViewSearch.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.favorite:
                new FavoritesMoviesTask(this).execute();
                linearLayout.setVisibility(View.GONE);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, R.id.toolbar);
                mRecyclerView.setLayoutParams(params);
                isFavorites = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (isFavorites) {
            linearLayout.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.BELOW, R.id.linearLayout);
            mRecyclerView.setLayoutParams(params);
            isFavorites = false;
        } else {
            super.onBackPressed();
        }
    }

    private void setToolBar() {
        Toolbar toolBar = findViewById(R.id.toolbar);
        assert toolBar != null;
        toolBar.setBackgroundResource(R.color.colorPrimary);
        View actionBarView = getLayoutInflater().inflate(R.layout.toolbar_customview, toolBar, false);
        actionBarView.setBackgroundResource(R.color.colorPrimary);

        setSupportActionBar(toolBar);

        if (Build.VERSION.SDK_INT >= 21) {
            // Set the status bar to dark-semi-transparentish
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            // Set paddingTop of toolbar to height of status bar.
            // Fixes statusbar covers toolbar issue
            int statusBarHeight = getStatusBarHeight();
            toolBar.setPadding(0, statusBarHeight, 0, 0);
        }
        TextView textViewTitle = actionBarView.findViewById(R.id.textViewTitle);

        // Set up the drawer.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(actionBarView);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        textViewTitle.setText(getString(R.string.app_name));
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = (int) getResources().getDimension(resourceId);
        }
        return result;
    }

    /**
     * If device has Internet the magic happens when app launches. The app will start the process
     * of collecting data from the API and present it to the user.
     * If the device has no connectivity it will display a Toast explaining that app needs
     * Internet to work properly.
     **/
    private void getMovies(String movieName) {
        if (isNetworkAvailable()) {
            // Execute task
            MovieAsyncTask movieAsyncTask = new MovieAsyncTask(getString(R.string.api_key), movieName);
            movieAsyncTask.execute();
        } else {
            Toast.makeText(this, getString(R.string.internet_connection), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Checks if there is Internet accessible.
     *
     * @return True if there is Internet. False if not.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onClick(View view) {
        String movieName = editTextSearch.getText().toString();
        if (!movieName.isEmpty()) {
            getMovies(movieName);
        } else {
            Toast.makeText(this, R.string.please_enter_movie, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class MovieAsyncTask extends AsyncTask<String, Void, ArrayList<Movie>> {

        private final String LOG_TAG = MovieAsyncTask.class.getCanonicalName();
        private final String mApiKey;
        private String movieName;

        /**
         * {@link java.lang.reflect.Constructor}
         *
         * @param movieName sorting Movies.
         * @param mApiKey   TMDb API key.
         */
        private MovieAsyncTask(String mApiKey, String movieName) {
            this.mApiKey = mApiKey;
            this.movieName = movieName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(VISIBLE);
        }

        @Override
        protected ArrayList<Movie> doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String moviesJsonStr;
            try {
                URL url = getApiUrl();

                // Start connecting to get JSON
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                StringBuilder builder = new StringBuilder();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Adds '\n' at last line if not already there.
                    // This supposedly makes it easier to debug.
                    builder.append(line).append("");
                }
                if (builder.length() == 0) {
                    return null;
                }
                moviesJsonStr = builder.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "error", e);
                return null;
            } finally {
                // Tidy up: release url connection and buffered reader
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "close", e);
                    }
                }
            }
            try {
                return getMoviesDataFromJson(moviesJsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        /**
         * Extracts data from the JSON object and returns an Array of movie objects.
         *
         * @param moviesJsonStr JSON string to be traversed
         * @return ArrayList of Movie objects
         * @throws JSONException throws JSONException
         */
        private ArrayList<Movie> getMoviesDataFromJson(String moviesJsonStr) throws JSONException {

            JSONObject moviesJson = new JSONObject(moviesJsonStr);
            Movie movie = new Movie(moviesJson);

            ArrayList<Movie> movies = new ArrayList<>();
            for (int i = 0; i < 1; i++) {
                movies.add(movie);
            }
            return movies;
        }

        /**
         * Creates and returns an URL.
         *
         * @return URL formatted with parameters for the API
         * @throws MalformedURLException throws MalformedURLException
         */
        private URL getApiUrl() throws MalformedURLException {
            final String BASE_URL = "http://www.omdbapi.com/";
            final String API_KEY_PARAM = "apikey";
            final String TITLE_PARAM = "t";
            Uri builtUri;
            builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(API_KEY_PARAM, mApiKey)
                    .appendQueryParameter(TITLE_PARAM, movieName)
                    .build();
            return new URL(builtUri.toString());
        }

        @Override
        protected void onPostExecute(ArrayList<Movie> movies) {
            progressBar.setVisibility(View.GONE);
            if (movies != null) {
                //setting recyclerView layout and adapter.

                StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
                layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
                mRecyclerView.setLayoutManager(layoutManager);
                mRecyclerView.setHasFixedSize(false);
                mRecyclerView.setItemAnimator(new DefaultItemAnimator());

                mRecyclerView.setAdapter(new HomeMoviesAdapter(movies, mRecyclerView, MainActivity.this));
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FavoritesMoviesTask extends AsyncTask<Void, Void, ArrayList<Movie>> {

        private Context mContext;

        /**
         * {@link java.lang.reflect.Constructor}
         *
         * @param context Activity Context.
         */
        private FavoritesMoviesTask(Context context) {
            mContext = context;
        }

        /**
         * Extracts data from the Cursor database and returns an Array of movie objects.
         *
         * @param cursor cursor of database
         * @return ArrayList of Movie objects
         */
        private ArrayList<Movie> getFavoriteMoviesDataFromCursor(Cursor cursor) {
            ArrayList<Movie> movies = new ArrayList<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Movie movie = new Movie(cursor);
                    movies.add(movie);
                } while (cursor.moveToNext());
                cursor.close();
            }
            return movies;
        }

        @Override
        protected ArrayList<Movie> doInBackground(Void... params) {
            Cursor cursor = mContext.getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    null
            );
            return getFavoriteMoviesDataFromCursor(cursor);
        }

        @Override
        protected void onPostExecute(ArrayList<Movie> movies) {
            if (movies != null) {
                //setting recyclerView layout and adapter.
                StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
                layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
                mRecyclerView.setLayoutManager(layoutManager);
                mRecyclerView.setHasFixedSize(false);
                mRecyclerView.setItemAnimator(new DefaultItemAnimator());

                mRecyclerView.setAdapter(new HomeMoviesAdapter(movies, mRecyclerView, MainActivity.this));
            }
        }
    }
}
