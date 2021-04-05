/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.utility.LogUtils;
import rs.highlande.highlanders_app.utility.Utils;

/**
 * Adapter class handling the custom gallery list items.
 *
 * @author VISH on 12/6/2017.
 * @author mbaldrighi on 01/5/2017.
 * @
 */
public class CustomGalleryAdapter extends RecyclerView.Adapter {

    private static final String LOG_TAG = CustomGalleryAdapter.class.getCanonicalName();

    private Cursor myCursor;
    private final Activity myActivity;
    private OnMediaClickListener onMediaClickListener;
    private static final int MEDIA_TYPE_IMAGE = 0;
    private static final int MEDIA_TYPE_VIDEO = 1;
    public int n = 0;

    public interface OnMediaClickListener {
        void onClickImage(String imageUri);
        void onClickVideo(String videoUri);
    }

    public CustomGalleryAdapter(Activity activity){
        this.myActivity = activity;
        if (activity instanceof OnMediaClickListener)
            this.onMediaClickListener = (OnMediaClickListener) activity;
    }

    public CustomGalleryAdapter(Activity activity, OnMediaClickListener listener){
        this.myActivity = activity;
        this.onMediaClickListener = listener;
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        switch (viewType){
            case  MEDIA_TYPE_IMAGE:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_custom_gallery_picture,
                        parent, false);
                return new ImageViewHolder(view);

            case MEDIA_TYPE_VIDEO:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_custom_gallery_video,
                        parent, false);
                return new VideoViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        n = position+1;

        ImageView imgView = null;
        switch (holder.getItemViewType()){
            case MEDIA_TYPE_IMAGE:
                ImageViewHolder imageViewHolder = (ImageViewHolder)holder;
                imageViewHolder.MediaCounter.setText(String.valueOf(n));
                imgView = imageViewHolder.imageView;
                break;
            case MEDIA_TYPE_VIDEO:
                VideoViewHolder videoViewHolder = (VideoViewHolder)holder;
                videoViewHolder.mediaCounter.setText(String.valueOf(n));
                imgView = videoViewHolder.imageView;

                break;
        }

        if (imgView != null) {
            Glide.with(myActivity)
                    .load(getMediaUri(position))
                    .thumbnail(0.5f)
                    .into(imgView);
        }
    }

    @Override
    public int getItemCount() {
        return (myCursor == null || myCursor.isClosed()) ? 0 : myCursor.getCount();
    }

    private Cursor swapCursor(Cursor cursor){
        if (myCursor == cursor) {
            return null;
        }
        Cursor old_cursor = myCursor;
        this.myCursor = cursor;
        if (cursor != null){
            this.notifyDataSetChanged();
        }
        return old_cursor;
    }

    public void changeCursor(Cursor cursor) {
        Cursor old_cur = swapCursor(cursor);
        if (old_cur != null){
            old_cur.close();
        }
    }

    // FUNCTION TO GET URIS OF ALL MEDIA FILE TO PASS TO GLIDE.
    private Uri getMediaUri(int position) {

        try {
            int dataIndex = myCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);

            myCursor.moveToPosition(position);

            String dataString = myCursor.getString(dataIndex);
            return Uri.parse("file://" + dataString);
        }
        catch (NullPointerException e){
            LogUtils.e(LOG_TAG, "Null Pointer Exception at " + " " + position);
            return null;
        }

    }

    @Override
    public int getItemViewType(int position){
        int mediaTypeIndex = myCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE);
        myCursor.moveToPosition(position);
        int mediaType = myCursor.getInt(mediaTypeIndex);
        switch (mediaType) {
            case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
                return MEDIA_TYPE_IMAGE;
            case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
                return MEDIA_TYPE_VIDEO;
            default:
                return -1;

        }
    }


    // FUNCTION TO GET URI OF THE CLICKED IMAGE TO PASS IN ONCLICK FUNCTIONS TO DISPLAY MEDIA IN FULL-SCREEN.
    private void getOnClickUri(int position) {
        try {
            int mediaTypeIndex = myCursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int dataIndex = myCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            myCursor.moveToPosition(position);
            String dataString = myCursor.getString(dataIndex);
//            String authority = myActivity.getPackageName() + ".provider";
//            Uri mediaUri = FileProvider.getUriForFile(myActivity, authority, new File(dataString));

            if (Utils.isStringValid(dataString) && onMediaClickListener != null) {
                switch ((myCursor.getInt(mediaTypeIndex))) {

                    case MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE:
                        onMediaClickListener.onClickImage(dataString);
                        break;
                    case MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO:
                        onMediaClickListener.onClickVideo(dataString);
                        break;
                    default: // Do nothing!
                }
            }
        } catch (NullPointerException e){
            LogUtils.e(LOG_TAG, "Null Pointer Exception at" + position + " " +  myCursor.getString(myCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA) ));

        }
    }


    // IMAGE VIEW-HOLDER
    public class ImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView imageView;
        TextView MediaCounter;
        //CardView cardView;

        ImageViewHolder(View itemView){
            super(itemView);
            this.imageView = itemView.findViewById(R.id.image_view);
            this.MediaCounter = itemView.findViewById(R.id.counter);
            this.imageView.setOnClickListener(this);
          //  this.cardView = (CardView)itemView.findViewById(R.id.row_view);

        }

        @Override
        public void onClick(View view) {
            getOnClickUri(getAdapterPosition());
        }

    }

    // VIDEO VIEW-HOLDER
    private class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView imageView;
        ImageView videoIcon;
        TextView mediaCounter;

        VideoViewHolder(View itemView){
            super(itemView);
            this.imageView = itemView.findViewById(R.id.video_image_view);
            this.imageView.setOnClickListener(this);

            this.videoIcon = itemView.findViewById(R.id.video_icon);
            this.mediaCounter = itemView.findViewById(R.id.counter);
        }

        @Override
        public void onClick(View view) {
            getOnClickUri(getAdapterPosition());

        }
    }
}
