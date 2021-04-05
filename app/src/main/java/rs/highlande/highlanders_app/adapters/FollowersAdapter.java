/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import rs.highlande.highlanders_app.R;
import rs.highlande.highlanders_app.models.HLUserGeneric;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;

/**
 * @author mbaldrighi on 1/22/2018.
 */
public class FollowersAdapter extends RecyclerView.Adapter<FollowersAdapter.FollowerVH> {

	private List<HLUserGeneric> items;
	private BasicAdapterInteractionsListenerWithPosition listener;

	public FollowersAdapter(List<HLUserGeneric> items, BasicAdapterInteractionsListenerWithPosition listener) {
		this.items = items;
		this.listener = listener;
	}

	@NonNull
	@Override
	public FollowerVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new FollowerVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_interest_follower, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull FollowerVH holder, int position) {
		HLUserGeneric obj = items.get(position);

		if (obj != null) {
			holder.setFollower(obj);
			listener.setLastAdapterPosition(position);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
	}


	class FollowerVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePicture;
		private final TextView name;
		private final TextView shared;

		private HLUserGeneric currentObject;

		FollowerVH(View itemView) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);
			shared = itemView.findViewById(R.id.shared_people);
			itemView.setOnClickListener(this);
		}

		void setFollower(HLUserGeneric obj) {
			currentObject = obj;
			if (Utils.isStringValid(obj.getAvatarURL())) {
				MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
						obj.getAvatarURL(), profilePicture);
			}
			else
				profilePicture.setImageResource(R.drawable.ic_profile_placeholder);

			name.setText(obj.getName());

			if (obj.getId().equals(listener.getUser().getId())) {
				shared.setVisibility(View.VISIBLE);
				shared.setText(R.string.interest_follower_name_you);
			}
			else {
				shared.setVisibility(Utils.isStringValid(obj.getSharedPeople()) ? View.VISIBLE : View.GONE);
				shared.setText(obj.getSharedPeople());
			}
		}


		@Override
		public void onClick(View view) {
			listener.onItemClick(currentObject);
		}
	}

}
