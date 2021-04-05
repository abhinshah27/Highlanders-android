/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.adapters;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.timeline.OnTimelineFragmentInteractionListener;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.timeline.TimelineFragment;
import rs.highlande.highlanders_app.models.Post;

/**
 * @author mbaldrighi on 9/28/2017.
 */
public class FeedMemoryTextVH extends FeedMemoryViewHolder {

	private TextView mMainView;


	public FeedMemoryTextVH(View view, TimelineFragment fragment, OnTimelineFragmentInteractionListener mListener) {
		super(view, fragment, mListener);

//		mMainView = (TextView) super.mMainView;
	}

	@Override
	public void onBindViewHolder(@NonNull Post object) {
		super.onBindViewHolder(object);

		// from now on text is handled from lower post mask.

//		if (Utils.isStringValid(mItem.getCaption())) {
//			mMainView.setText(mItem.getCaption());
//			ResizingTextWatcher.resizeTextInView(mMainView);
//		}

	}


}
