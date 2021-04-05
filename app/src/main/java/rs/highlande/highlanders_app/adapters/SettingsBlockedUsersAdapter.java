/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
 * @author mbaldrighi on 3/26/2018.
 */
public class SettingsBlockedUsersAdapter extends RecyclerView.Adapter<SettingsBlockedUsersAdapter.ContactVH> {

	private List<HLUserGeneric> items;

	private BasicAdapterInteractionsListener mListener;

	public SettingsBlockedUsersAdapter(List<HLUserGeneric> items, BasicAdapterInteractionsListener listener) {
		this.items = items;
		this.mListener = listener;
	}

	@NonNull
	@Override
	public ContactVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ContactVH(
				LayoutInflater
						.from(parent.getContext())
						.inflate(R.layout.item_settings_blocked, parent, false),
				mListener
		);
	}

	@Override
	public void onBindViewHolder(@NonNull ContactVH holder, int position) {
		HLUserGeneric c = items.get(position);
		holder.setContact(c);
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		return items.get(position).hashCode();
	}


	/**
	 * The {@link RecyclerView.ViewHolder} responsible to retain the
	 * {@link View} objects of a {@link HLUserGeneric}.
	 */
	class ContactVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePic;
		private final TextView contactName;
		private final Button unblockBtn;

		private final BasicAdapterInteractionsListener mListener;

		private HLUserGeneric currentContact;

		ContactVH(View itemView, BasicAdapterInteractionsListener listener) {
			super(itemView);

			profilePic = itemView.findViewById(R.id.profile_picture);
			contactName = itemView.findViewById(R.id.contact_name);
			unblockBtn = itemView.findViewById(R.id.unblock_btn);

			mListener = listener;

			unblockBtn.setOnClickListener(this);
		}

		void setContact(HLUserGeneric contact) {
			if (contact == null)
				return;

			currentContact = contact;

			if (Utils.isStringValid(contact.getAvatarURL())) {
				MediaHelper.loadProfilePictureWithPlaceholder(profilePic.getContext(),
						contact.getAvatarURL(), profilePic);
			}
			else profilePic.setImageResource(R.drawable.ic_profile_placeholder);

			contactName.setText(contact.getName());
		}

		@Override
		public void onClick(View view) {
			int id = view.getId();
			switch (id) {
				case R.id.unblock_btn:
					mListener.onItemClick(currentContact);
					break;
			}
		}

	}

}
