/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.utility.swipe_remove;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import rs.highlande.highlanders_app.R;

/**
 * @author mbaldrighi on 3/14/2018.
 */
@SuppressLint("ClickableViewAccessibility")
public class SwipeRemoveCallback extends ItemTouchHelper.Callback {

	enum ButtonsState {
		GONE,
		LEFT_VISIBLE,
		RIGHT_VISIBLE
	}
	private ButtonsState buttonsState = ButtonsState.GONE;
	private static final float BUTTON_WIDTH = 100;

	private Drawable deleteIcon;
	private int intrinsicWidth;
	private int intrinsicHeight;
	private ColorDrawable background = new ColorDrawable();
	private int backgroundColor;

	private boolean swipeBack = false;

	private RectF buttonInstance;

//	private OnItemSwipeListener mAdapter;

	private SwipeRemoveActions mSwipeActions;

	private RecyclerView.ViewHolder currentItemViewHolder;


	public SwipeRemoveCallback(Context context, SwipeRemoveActions swipeActions) {

		this.deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_trash_outline_white_30);

		if (deleteIcon != null) {
			intrinsicWidth = deleteIcon.getIntrinsicWidth();
			intrinsicHeight = deleteIcon.getIntrinsicHeight();
		}

		backgroundColor = ContextCompat.getColor(context, R.color.hl_red_dark);

		this.mSwipeActions = swipeActions;
	}

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//		int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		int swipeFlags = ItemTouchHelper.START;
		return makeMovementFlags(0, swipeFlags);
	}

//	@Override
//	public boolean isLongPressDragEnabled() {
//		return false;
//	}
//
//	@Override
//	public boolean isItemViewSwipeEnabled() {
//		return true;
//	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
		return false;
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}


	@Override
	public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

		if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
			if (buttonsState != ButtonsState.GONE) {
				if (buttonsState == ButtonsState.RIGHT_VISIBLE) dX = Math.min(dX, -BUTTON_WIDTH);
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			} else {
				setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}
		}

			if (buttonsState == ButtonsState.GONE) {
				super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			}
			currentItemViewHolder = viewHolder;

//			View itemView = viewHolder.itemView;
//			int itemHeight = itemView.getBottom() - itemView.getTop();
//
//			// Draw the red delete background
//			background.setColor(backgroundColor);
//			background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
//			background.draw(c);
//
//			// Calculate position of delete icon
//			int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
//			int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
//			int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
//			int deleteIconRight = itemView.getRight() - deleteIconMargin;
//			int deleteIconBottom = deleteIconTop + intrinsicHeight;
//
//			// Draw the delete icon
//			deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
//			deleteIcon.draw(c);

//			drawButtons(c, viewHolder);
//
//		super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
	}

	@Override
	public int convertToAbsoluteDirection(int flags, int layoutDirection) {
		if (swipeBack) {
			swipeBack = buttonsState != ButtonsState.GONE;
			return 0;
		}

		return super.convertToAbsoluteDirection(flags, layoutDirection);
	}


	private void setTouchListener(final Canvas c,
	                              final RecyclerView recyclerView,
	                              final RecyclerView.ViewHolder viewHolder,
	                              final float dX, final float dY,
	                              final int actionState, final boolean isCurrentlyActive) {

		recyclerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;

				if (swipeBack) {
					if (dX < -BUTTON_WIDTH) buttonsState = ButtonsState.RIGHT_VISIBLE;

					if (buttonsState != ButtonsState.GONE) {
						setTouchDownListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
						setItemsClickable(recyclerView, false);
					}
				}

				return false;
			}
		});
	}

	// SwipeController.java
	private void setTouchDownListener(final Canvas c,
	                                  final RecyclerView recyclerView,
	                                  final RecyclerView.ViewHolder viewHolder,
	                                  final float dX, final float dY,
	                                  final int actionState, final boolean isCurrentlyActive) {
		recyclerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					setTouchUpListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
				}
				return false;
			}
		});
	}

	private void setTouchUpListener(final Canvas c,
	                                final RecyclerView recyclerView,
	                                final RecyclerView.ViewHolder viewHolder,
	                                final float dX, final float dY,
	                                final int actionState, final boolean isCurrentlyActive) {
		recyclerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					SwipeRemoveCallback.super.onChildDraw(c, recyclerView, viewHolder, 0F, dY, actionState, isCurrentlyActive);
					recyclerView.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							return false;
						}
					});
					setItemsClickable(recyclerView, true);
					swipeBack = false;

					if (mSwipeActions != null && buttonInstance != null && buttonInstance.contains(event.getX(), event.getY())) {
						if (buttonsState == ButtonsState.RIGHT_VISIBLE) {
							mSwipeActions.onRightClicked(viewHolder);
						}
					}
					buttonsState = ButtonsState.GONE;
					currentItemViewHolder = null;
				}
				return false;
			}
		});
	}

	private void setItemsClickable(RecyclerView recyclerView,
	                               boolean isClickable) {
		for (int i = 0; i < recyclerView.getChildCount(); ++i) {
			recyclerView.getChildAt(i).setClickable(isClickable);
		}
	}


	private void drawButtons(Canvas c, RecyclerView.ViewHolder viewHolder) {
		float buttonWidthWithoutPadding = BUTTON_WIDTH - 20;
		float corners = 0;

		View itemView = viewHolder.itemView;
		Paint p = new Paint();

		RectF rightButton = new RectF(itemView.getRight() - buttonWidthWithoutPadding, itemView.getTop(), itemView.getRight(), itemView.getBottom());
		p.setColor(Color.RED);
		c.drawRoundRect(rightButton, corners, corners, p);
		drawText("DELETE", c, rightButton, p);

		buttonInstance = null;
		if (buttonsState == ButtonsState.RIGHT_VISIBLE) {
			buttonInstance = rightButton;
		}
	}

	private void drawText(String text, Canvas c, RectF button, Paint p) {
		float textSize = 60;
		p.setColor(Color.WHITE);
		p.setAntiAlias(true);
		p.setTextSize(textSize);

		float textWidth = p.measureText(text);
		c.drawText(text, button.centerX()-(textWidth/2), button.centerY()+(textSize/2), p);
	}


	public void onDraw(Canvas c) {
		if (currentItemViewHolder != null) {
			drawButtons(c, currentItemViewHolder);
		}
	}
}
