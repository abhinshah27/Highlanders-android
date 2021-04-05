/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
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
import rs.highlande.highlanders_app.models.Interest;
import rs.highlande.highlanders_app.models.InterestCategory;
import rs.highlande.highlanders_app.models.Tag;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;

/**
 * @author mbaldrighi on 12/26/2017.
 */
public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final int TYPE_PEOPLE = 0;
	private final int TYPE_INTEREST = 1;
	private final int TYPE_INTEREST_CATEGORY = 2;
	private final int TYPE_TAG = 3;
	private final int TYPE_INTEREST_W_CATEGORIES = 4;

	private List<Object> items;
	private OnItemClickListener listener;
	private boolean interestWithCategories = false;

	public SearchAdapter(List<Object> items, OnItemClickListener listener) {
		this.items = items;
		this.listener = listener;
	}

	public SearchAdapter(List<Object> items, OnItemClickListener listener, boolean interestWithCategories) {
		this.items = items;
		this.listener = listener;
		this.interestWithCategories = interestWithCategories;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		switch (viewType) {
			case TYPE_PEOPLE:
			case TYPE_INTEREST:
				return new SimpleItemVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_profile_contact, parent, false));
			case TYPE_INTEREST_CATEGORY:
				return new InterestSectionTitleVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_list_section_title, parent, false));
			case TYPE_INTEREST_W_CATEGORIES:
				return new SimpleInterestWithCategoriesVH(LayoutInflater.from(parent.getContext())
						.inflate(R.layout.item_search_list_interest, parent, false));
		}

		return new SimpleItemVH(LayoutInflater.from(parent.getContext())
				.inflate(R.layout.item_profile_contact, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object obj = items.get(position);

		switch (getItemViewType(position)) {
			case TYPE_PEOPLE:
			case TYPE_INTEREST:
			case TYPE_TAG:
				if (holder instanceof SimpleItemVH) {
					((SimpleItemVH) holder).setSimpleItem(obj);
				}
				break;
			case TYPE_INTEREST_W_CATEGORIES:
				if (holder instanceof SimpleInterestWithCategoriesVH) {
					((SimpleInterestWithCategoriesVH) holder).setSimpleItem(obj);
				}
				break;
			case TYPE_INTEREST_CATEGORY:
				if (holder instanceof InterestSectionTitleVH)
					((InterestSectionTitleVH) holder).setCategory(obj, position);
				break;
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public long getItemId(int position) {
		if (items != null && !items.isEmpty() && items.get(position) != null)
			return items.get(position).hashCode();
		else
			return super.getItemId(position);
	}

	@Override
	public int getItemViewType(int position) {
		Object obj = items.get(position);
		if (obj instanceof HLUserGeneric)
			return TYPE_PEOPLE;
		else if (obj instanceof Interest)
			return interestWithCategories ? TYPE_INTEREST_W_CATEGORIES : TYPE_INTEREST;
		else if (obj instanceof String)
			return TYPE_INTEREST_CATEGORY;
		else if (obj instanceof Tag)
			return TYPE_TAG;

		return super.getItemViewType(position);
	}


	class SimpleItemVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final ImageView profilePicture;
		private final TextView name;

		private Object currentObject;

		SimpleItemVH(View itemView) {
			super(itemView);

			profilePicture = itemView.findViewById(R.id.profile_picture);
			name = itemView.findViewById(R.id.name);

			View invite = itemView.findViewById(R.id.invite_btn);
			if (invite != null)
				invite.setVisibility(View.GONE);
			itemView.setOnClickListener(this);
		}

		void setSimpleItem(Object obj) {
			currentObject = obj;

			if (obj instanceof HLUserGeneric) {
				if (Utils.isStringValid(((HLUserGeneric) obj).getAvatarURL())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
							((HLUserGeneric) obj).getAvatarURL(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_placeholder_profile);

				name.setText(((HLUserGeneric) obj).getCompleteName());
			}
			else if (obj instanceof Interest) {
				if (Utils.isStringValid(((Interest) obj).getAvatarURL())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
						((Interest) obj).getAvatarURL(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_placeholder_profile);

				name.setText(((Interest) obj).getName());
			}
			else if (obj instanceof Tag) {
				if (Utils.isStringValid(((Tag) obj).getUserUrl())) {
					MediaHelper.loadProfilePictureWithPlaceholder(profilePicture.getContext(),
						((Tag) obj).getUserUrl(), profilePicture);
				}
				else profilePicture.setImageResource(R.drawable.ic_placeholder_profile);

				name.setText(((Tag) obj).getUserName());
			}
		}


		@Override
		public void onClick(View view) {
			listener.onItemClick(currentObject);
		}
	}

	public interface OnItemClickListener {
		void onItemClick(Object object);
	}

	public interface OnItemClickListenerForGlobalSearch extends OnItemClickListener {
		void onItemClick(Object object, boolean isInterest);
	}

	class InterestSectionTitleVH extends RecyclerView.ViewHolder {

		private final View divider;
		private final TextView title;

		public InterestSectionTitleVH(View itemView) {
			super(itemView);

			divider = itemView.findViewById(R.id.divider);
			title = itemView.findViewById(R.id.title);
		}

		void setCategory(Object obj, int position) {
			if (obj instanceof String) {
				title.setText((CharSequence) obj);

				divider.setVisibility(position != 0 ? View.VISIBLE : View.GONE);
			}
		}
	}

	class SimpleInterestWithCategoriesVH extends SimpleItemVH implements View.OnClickListener {

		private final TextView category;

		SimpleInterestWithCategoriesVH(View itemView) {
			super(itemView);

			category = itemView.findViewById(R.id.type);

			itemView.setOnClickListener(this);
		}

		void setSimpleItem(Object obj) {
			super.setSimpleItem(obj);

			if (obj instanceof Interest) {

				StringBuilder stringBuilder = new StringBuilder();
				if (((Interest) obj).getCategories() != null && !((Interest) obj).getCategories().isEmpty()) {
					for (InterestCategory cat :
							((Interest) obj).getCategories()) {
						stringBuilder.append(cat.getName()).append(", ");
					}
				}

				String s;
				if (Utils.isStringValid(s = stringBuilder.toString().trim())) {
					category.setText(s.substring(0, s.length() - 1));
					category.setVisibility(View.VISIBLE);
				}
				else
					category.setVisibility(View.GONE);
			}
		}


		@Override
		public void onClick(View view) {
			if (listener instanceof OnItemClickListenerForGlobalSearch) {
				((OnItemClickListenerForGlobalSearch) listener)
						.onItemClick(super.currentObject, super.currentObject instanceof Interest);
			}
		}
	}

}