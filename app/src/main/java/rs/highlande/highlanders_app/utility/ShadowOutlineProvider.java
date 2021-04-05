/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.utility;

import android.graphics.Outline;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.RequiresApi;

/**
 * @author Massimo on 4/24/2018.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ShadowOutlineProvider extends ViewOutlineProvider {

	private Rect rect = new Rect();
	private int shiftX, shiftY;

	@Override
	public void getOutline(View view, Outline outline) {
		if (view != null && outline != null) {
			view.getBackground().copyBounds(rect);
			rect.offset(1, 1);

			outline.setRoundRect(rect, 2f);
		}
	}
}
