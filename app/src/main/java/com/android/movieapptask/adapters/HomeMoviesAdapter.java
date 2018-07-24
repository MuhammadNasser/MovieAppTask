package com.android.movieapptask.adapters;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.movieapptask.MainActivity;
import com.android.movieapptask.R;
import com.android.movieapptask.Utility;
import com.android.movieapptask.data.MovieContract;
import com.android.movieapptask.models.Movie;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class HomeMoviesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private LayoutInflater inflater;
    private RecyclerView recyclerView;
    private MainActivity activity;
    private ArrayList<Movie> movies;

    public HomeMoviesAdapter(ArrayList<Movie> movies, RecyclerView recyclerView, MainActivity activity) {
        inflater = activity.getLayoutInflater();
        this.recyclerView = recyclerView;
        this.activity = activity;
        this.movies = movies;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        RecyclerView.ViewHolder holder;

        //inflate your layout and pass it to view holder
        view = inflater.inflate(R.layout.item_movie, viewGroup, false);
        holder = new ItemHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {


        StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
        layoutParams.setFullSpan(false);

        ItemHolder itemHolder = (ItemHolder) viewHolder;
        itemHolder.setDetails(movies.get(position));
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public int getSize() {
        return movies.size();
    }

    private class ItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView imageViewPoster;
        ImageView imageViewShare;
        ImageView imageViewFavorite;

        TextView textViewName;
        TextView textViewRelease;
        TextView textViewRating;
        Movie movie;

        private ItemHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewRelease = itemView.findViewById(R.id.textViewRelease);
            textViewRating = itemView.findViewById(R.id.textViewRating);

            imageViewPoster = itemView.findViewById(R.id.imageViewPoster);
            imageViewShare = itemView.findViewById(R.id.imageViewShare);
            imageViewFavorite = itemView.findViewById(R.id.imageViewFavorite);

            imageViewShare.setOnClickListener(this);
            imageViewFavorite.setOnClickListener(this);

        }

        @SuppressLint("DefaultLocale")
        private void setDetails(Movie movie) {
            this.movie = movie;
            textViewName.setText(movie.getTitle());
            textViewRelease.setText(movie.getReleaseDate());
            textViewRating.setText(String.format(" %d/10", movie.getRating()));

            Picasso.with(activity).load(movie.getImage()).
                    placeholder(R.drawable.placeholder).
                    error(R.drawable.warning).
                    into(imageViewPoster);

            int isFavorite = Utility.isFavorite(activity, movie.getId());
            imageViewFavorite.setImageResource(isFavorite == 1 ?
                    R.drawable.favourite_star :
                    R.drawable.favourite);
        }

        @Override
        @SuppressLint("StaticFieldLeak")
        public void onClick(View v) {
            if (v == imageViewShare) {

                createShareMovieIntent();

            } else if (v == imageViewFavorite) {

                new AsyncTask<Void, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(Void... params) {
                        return Utility.isFavorite(activity, movie.getId());
                    }

                    @Override
                    protected void onPostExecute(Integer isFavorite) {
                        // if it is in favorites
                        if (isFavorite == 1) {
                            // delete from favorites
                            new AsyncTask<Void, Void, Integer>() {
                                @Override
                                protected Integer doInBackground(Void... params) {
                                    return activity.getContentResolver().delete(
                                            MovieContract.MovieEntry.CONTENT_URI,
                                            MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                                            new String[]{movie.getId()}
                                    );
                                }

                                @Override
                                protected void onPostExecute(Integer rowsDeleted) {
                                    imageViewFavorite.setImageResource(R.drawable.favourite);
                                    Toast.makeText(activity, activity.getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show();
                                    movies.remove(getLayoutPosition());
                                    recyclerView.removeViewAt(getLayoutPosition());
                                    recyclerView.getAdapter().notifyItemRemoved(getLayoutPosition());
                                    recyclerView.getAdapter().notifyItemRangeChanged(getLayoutPosition(), movies.size());
                                }
                            }.execute();
                        }
                        // if it is not in favorites
                        else {
                            // add to favorites
                            new AsyncTask<Void, Void, Uri>() {
                                @Override
                                protected Uri doInBackground(Void... params) {
                                    ContentValues values = new ContentValues();

                                    values.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movie.getId());
                                    values.put(MovieContract.MovieEntry.COLUMN_TITLE, movie.getTitle());
                                    values.put(MovieContract.MovieEntry.COLUMN_IMAGE, movie.getImage());
                                    values.put(MovieContract.MovieEntry.COLUMN_RATING, movie.getRating());
                                    values.put(MovieContract.MovieEntry.COLUMN_DATE, movie.getReleaseDate());
                                    values.put(MovieContract.MovieEntry.COLUMN_WEBSITE, movie.getWebsite());

                                    return activity.getContentResolver().insert(MovieContract.MovieEntry.CONTENT_URI,
                                            values);
                                }

                                @Override
                                protected void onPostExecute(Uri returnUri) {
                                    imageViewFavorite.setImageResource(R.drawable.favourite_star);
                                    Toast.makeText(activity, activity.getString(R.string.added_from_favorites), Toast.LENGTH_SHORT).show();
                                    recyclerView.getAdapter().notifyDataSetChanged();
                                }
                            }.execute();
                        }
                    }
                }.execute();

            }
        }

        private void createShareMovieIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            } else {
                //noinspection deprecation
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            }
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Shared by " + activity.getResources().getString(R.string.app_name) + ": " + movie.getTitle() + " " +
                    movie.getWebsite());

            activity.startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    }
}