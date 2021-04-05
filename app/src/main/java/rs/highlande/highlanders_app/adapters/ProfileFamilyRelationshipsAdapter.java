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
import rs.highlande.highlanders_app.models.GenericUserFamilyRels;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;

/**
 * @author mbaldrighi on 4/30/2018.
 */
public class ProfileFamilyRelationshipsAdapter extends RecyclerView.Adapter<ProfileFamilyRelationshipsAdapter.MemberCircleVH> {

	private List<GenericUserFamilyRels> items;
	private BasicAdapterInteractionsListener mListener;


	public ProfileFamilyRelationshipsAdapter(List<GenericUserFamilyRels> items, BasicAdapterInteractionsListener mListener) {
		this.items = items;
		this.mListener = mListener;
	}

	@NonNull
	@Override
	public MemberCircleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new MemberCircleVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_settings_circle_member, parent, false), mListener);
	}

	@Override
	public void onBindViewHolder(@NonNull MemberCircleVH holder, int position) {
		GenericUserFamilyRels member = items.get(position);
		holder.setMember(member);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}


	@Override
	public long getItemId(int position) {
		GenericUserFamilyRels user = items.get(position);
		if (user != null)
			return user.hashCode();
		return super.getItemId(position);
	}



	static class MemberCircleVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePicture;
		private final TextView name;

		private final BasicAdapterInteractionsListener mListener;

		private GenericUserFamilyRels currentMember;


		MemberCircleVH(View itemView, BasicAdapterInteractionsListener mListener) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);
			itemView.findViewById(R.id.check).setVisibility(View.GONE);

			this.mListener = mListener;

			itemView.setOnClickListener(this);
		}


		private void setMember(GenericUserFamilyRels member) {
			if (member != null) {

				currentMember = member;

				if (Utils.isStringValid(member.getAvatarURL())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
							member.getAvatarURL(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_profile_placeholder);

				name.setText(member.getCompleteName());
			}
		}

		@Override
		public void onClick(View v) {
			mListener.onItemClick(currentMember);
		}
	}

}
