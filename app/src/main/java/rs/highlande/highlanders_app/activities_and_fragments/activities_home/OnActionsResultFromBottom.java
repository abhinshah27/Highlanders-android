/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home;

import androidx.annotation.NonNull;

/**
 * @author mbaldrighi on 12/29/2017.
 */
public interface OnActionsResultFromBottom {
	void onPostDeleted(@NonNull String postId);
	void onInterestUnFollowed(@NonNull String postId, boolean followed);
	void onUserBlocked(@NonNull String postId);
}