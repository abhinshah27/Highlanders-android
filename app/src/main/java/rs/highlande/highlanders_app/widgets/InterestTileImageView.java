/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.widgets;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

/**
 * Subclass of {@link androidx.appcompat.widget.AppCompatImageView} needed to show a square image in
 * {@link rs.highlande.highlanders_app.activities_and_fragments.activities_home.profile.FollowInterestFragment}.
 *
 * @author mbaldrighi on 1/12/2018.
 */
public class InterestTileImageView extends androidx.appcompat.widget.AppCompatImageView {
	public InterestTileImageView(Context context) {
		super(context);
	}

	public InterestTileImageView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public InterestTileImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int width = getMeasuredWidth();
		setMeasuredDimension(width, width);
	}
}
