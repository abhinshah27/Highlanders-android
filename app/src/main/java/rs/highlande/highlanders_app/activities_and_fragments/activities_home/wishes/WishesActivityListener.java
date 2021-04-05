/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.wishes;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import rs.highlande.highlanders_app.models.HLWish;
import rs.highlande.highlanders_app.models.WishListElement;

/**
 * @author mbaldrighi on 2/14/2018.
 */
public interface WishesActivityListener {

	/* FRAGMENTS SECTION */
	void showWishNameFragment();


	/* VIEWS SECTION */
	TextView getStepTitle();
	TextView getStepSubTitle();
	void setStepTitle(String title);
	void setStepSubTitle(String subTitle);
	void hideTitles();
	void showTitles();
	void hideStepsBar();
	void showStepsBar();

	void setToolbarTitle(@StringRes int titleResId);

	void handleSteps();
	void handleSteps(boolean fromNameFragment);

	void enableDisableNextButton(boolean enable);
	void setNextAlwaysOn();

	void callServer(WishesActivity.CallType type, boolean root);

	HLWish getWishToEdit();
	boolean isEditMode();

	WishListElement getSelectedWishListElement();
	void setSelectedWishListElement(WishListElement element);
	void setTriggerFriendId(String friendId);
	void setDataBundle(@NonNull Bundle dataBundle);
	void setWishName(@NonNull String wishName);
	String getWishName();
	String getSpecificDateString();

	WishesActivity.OnNextClickListener getOnNextClickListener();
	void setOnNextClickListener(WishesActivity.OnNextClickListener listener);
	void resumeNextNavigation();

	void restoreReceiver();
	void disableReceiver();

	/**
	 * Serves ONLY for CREATE POST FRAGMENT because it's the only screen that doesn't want title and subtitle.
	 * @param ignore
	 */
	void setIgnoreTitlesInResponse(boolean ignore);


	void setProgressMessageForFinalOps(@StringRes int stringResId);

}
