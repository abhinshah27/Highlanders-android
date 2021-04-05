/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.models;

import android.content.Context;

import rs.highlande.highlanders_app.models.enums.FeedFilterTypeEnum;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.Utils;

/**
 * @author mbaldrighi on 6/25/2018.
 */
public class FeedFilter {

	private final FeedFilterTypeEnum type;
	private final String name;
	private final String nameForServer;
	private boolean selected;


	public FeedFilter(String name) {
		this.type = FeedFilterTypeEnum.CIRCLE;
		this.name = name;
		this.nameForServer = FeedFilterTypeEnum.getCallValue(name);
	}

	public FeedFilter(Context context, FeedFilterTypeEnum type) {
		this.type = type;
		this.name = FeedFilterTypeEnum.getValue(context, type);
		this.nameForServer = FeedFilterTypeEnum.getCallValue(type);
	}

	public boolean isFamilyCircle() {
		return Utils.isStringValid(name) && name.equals(Constants.CIRCLE_FAMILY_NAME);
	}

	public boolean isInnerCircle() {
		return Utils.isStringValid(name) && name.equals(Constants.INNER_CIRCLE_NAME);
	}

	public boolean isAll() {
		return type == FeedFilterTypeEnum.ALL || type == FeedFilterTypeEnum.ALL_INT;
	}

	public boolean isCircle() {
		return type == FeedFilterTypeEnum.CIRCLE;
	}


	//region == Getters and setters ==

	public FeedFilterTypeEnum getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getNameForServer() {
		return nameForServer;
	}

	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	//endregion

}
