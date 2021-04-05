/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.utility;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

/**
 * @author maxbaldrighi on 3/20/2018.
 */
class CustomTypefaceSpan extends TypefaceSpan {

	private Typeface newType;

	//region == Class constructors ==

	CustomTypefaceSpan(String family, Typeface type) {
		super(family);
		newType = type;
	}

	// TODO: 3/20/2018   if ever used, needs to be written correctly
	public CustomTypefaceSpan(Parcel in) {
		super(in);

		// typeface needs to be read from parcel
	}

	//endregion


	@Override
	public void updateDrawState(TextPaint ds) {
		applyCustomTypeface(ds, newType);
	}

	@Override
	public void updateMeasureState(TextPaint paint) {
		applyCustomTypeface(paint, newType);
	}

	private static void applyCustomTypeface(Paint paint, Typeface tf) {
		int oldStyle;
		Typeface old = paint.getTypeface();
		if (old == null)
			oldStyle = 0;
		else
			oldStyle = old.getStyle();

		int fake = oldStyle & ~tf.getStyle();
		if ((fake & Typeface.BOLD) != 0) {
			paint.setFakeBoldText(true);
		}

		if ((fake & Typeface.ITALIC) != 0) {
			paint.setTextSkewX(-0.25f);
		}

		paint.setTypeface(tf);
	}


	// region == Parcelable CREATOR ==

	public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
		public CustomTypefaceSpan createFromParcel(Parcel in) {
			return new CustomTypefaceSpan(in);
		}

		public CustomTypefaceSpan[] newArray(int size) {
			return new CustomTypefaceSpan[size];
		}
	};

	//endregion
}