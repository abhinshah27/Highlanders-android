/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.activities_and_fragments.activities_home.settings.SettingsCirclesFoldersFragment;
import rs.highlande.highlanders_app.utility.Constants;
import rs.highlande.highlanders_app.utility.Utils;

/**
 * @author mbaldrighi on 3/19/2018.
 */
public class SettingsCirclesFoldersAdapter extends ArrayAdapter<CharSequence> {

	@LayoutRes
	private int resourceId;

	private SettingsCirclesFoldersFragment.ViewType mViewType;

	private BasicAdapterInteractionsListener mListener;

	public SettingsCirclesFoldersAdapter(@NonNull Context context, int resource, @NonNull CharSequence[] objects,
	                                     SettingsCirclesFoldersFragment.ViewType viewType,
	                                     BasicAdapterInteractionsListener listener) {
		super(context, resource, objects);

		this.resourceId = resource;
		this.mViewType = viewType;
		this.mListener = listener;
	}

	public SettingsCirclesFoldersAdapter(@NonNull Context context, int resource, @NonNull List<CharSequence> objects,
	                                     SettingsCirclesFoldersFragment.ViewType viewType,
	                                     BasicAdapterInteractionsListener listener) {
		super(context, resource, objects);

		this.resourceId = resource;
		this.mViewType = viewType;
		this.mListener = listener;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		final CharSequence entry = getItem(position);

		SettingCircleVH viewHolder;
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);

			viewHolder = new SettingCircleVH();
			viewHolder.circleName = convertView.findViewById(R.id.circle_name);
			viewHolder.iconEdit = convertView.findViewById(R.id.btn_edit);
			viewHolder.iconRemove = convertView.findViewById(R.id.btn_remove);

			viewHolder.iconEdit.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mListener.onItemClick(entry, v);
				}
			});

			viewHolder.iconRemove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mListener.onItemClick(entry, v);
				}
			});

			convertView.setTag(viewHolder);
		}
		else {
			viewHolder = (SettingCircleVH) convertView.getTag();
		}

		if (entry != null && Utils.isStringValid(entry.toString())) {
			viewHolder.circleName.setText(entry);

			if ((entry.equals(Constants.INNER_CIRCLE_NAME) || entry.equals(Constants.CIRCLE_FAMILY_NAME)) &&
					mViewType == SettingsCirclesFoldersFragment.ViewType.CIRCLES) {
				viewHolder.iconEdit.setVisibility(View.GONE);
				viewHolder.iconRemove.setVisibility(View.GONE);
			}
		}


		return convertView;
	}


	static class SettingCircleVH {
		TextView circleName;
		View iconEdit, iconRemove;
	}
}
