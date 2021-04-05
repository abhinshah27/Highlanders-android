/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.activities_and_fragments.activities_home.global_search;

import rs.highlande.highlanders_app.models.enums.GlobalSearchTypeEnum;

/**
 * @author mbaldrighi on 4/10/2018.
 */
public interface GlobalSearchActivityListener {

//	String getGeneralQuery();
//	EditText getSearchBox();
//	boolean hasValidQuery();


	void showInterestsUsersListFragment(String query, GlobalSearchTypeEnum returnType, String title);
	void showGlobalTimelineFragment(String listName, String postId, String userId, String name,
	                                String avatarUrl, String query);
}
