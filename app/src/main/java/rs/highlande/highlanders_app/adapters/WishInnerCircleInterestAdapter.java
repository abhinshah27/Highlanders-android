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
import rs.highlande.highlanders_app.models.PostList;
import rs.highlande.highlanders_app.models.WishListElement;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.MediaHelper;

/**
 * @author mbaldrighi on 3/1/2018.
 */
public class WishInnerCircleInterestAdapter extends RecyclerView.Adapter<WishInnerCircleInterestAdapter.ElementVH> {

	public enum ItemType { TRIGGER, RECIPIENT }
	private ItemType type;

	private List<WishListElement> items;

	private OnElementClickedListener mListener;

	public WishInnerCircleInterestAdapter(List<WishListElement> items, OnElementClickedListener listener, ItemType type) {
		this.items = items;
		this.mListener = listener;
		this.type = type;
	}

	@NonNull
	@Override
	public ElementVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ElementVH(
				LayoutInflater
						.from(parent.getContext())
						.inflate(
								type == ItemType.RECIPIENT ?
										R.layout.item_wish_recipient : R.layout.item_wish_search,
								parent,
								false
						)
		);
	}

	@Override
	public void onBindViewHolder(@NonNull ElementVH holder, int position) {
		WishListElement post = items.get(position);
		holder.setElement(post);
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
	 * {@link View} objects of a {@link PostList}.
	 */
	class ElementVH extends RecyclerView.ViewHolder implements View.OnClickListener {

		private final View itemView;

		private final TextView name;
		private final ImageView picture;

		private WishListElement listElement;


		ElementVH(View itemView) {
			super(itemView);

			this.itemView = itemView;
			this.itemView.setOnClickListener(this);

			name = itemView.findViewById(R.id.name);
			picture = itemView.findViewById(R.id.profile_picture);
		}

		void setElement(final WishListElement wle) {
			if (wle == null)
				return;

			listElement = wle;

			itemView.setActivated(wle.isSelected());

			name.setText(wle.getName());

			if (Utils.isStringValid(wle.getAvatarURL()))
				MediaHelper.loadProfilePictureWithPlaceholder(picture.getContext(), wle.getAvatarURL(), picture);
			else
				picture.setImageResource(R.drawable.ic_profile_placeholder);
		}

		@Override
		public void onClick(View v) {
			mListener.onElementClick(listElement);
		}
	}


	public interface OnElementClickedListener {
		void onElementClick(@NonNull WishListElement listElement);
	}

}
