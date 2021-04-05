/*
 * Copyright (c) 2018. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.widgets;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
import rs.highlande.highlanders_app.utility.LogUtils;

/**
 * Class copied from {@link FragmentStatePagerAdapter} and modified to handle
 * tag objects to solve fragment null reference crash for Create Post screen.
 *
 * @author mbaldrighi on 7/11/2018.
 */
public abstract class FragmentStatePagerAdapterWithTags extends PagerAdapter {

	private static final String TAG = "FragmentStatePagerAdapt";
	private static final boolean DEBUG = false;

	private final FragmentManager mFragmentManager;
	private FragmentTransaction mCurTransaction = null;

	private ArrayList<Fragment.SavedState> mSavedState = new ArrayList<Fragment.SavedState>();
	private ArrayList<String> mSavedFragmentTags = new ArrayList<String>();
	private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
	private Fragment mCurrentPrimaryItem = null;

	public FragmentStatePagerAdapterWithTags(FragmentManager fm) {
		mFragmentManager = fm;
	}

	/**
	 * Return the Fragment associated with a specified position.
	 */
	public abstract Fragment getItem(int position);

	@Override
	public void startUpdate(@NonNull ViewGroup container) {
		if (container.getId() == View.NO_ID) {
			throw new IllegalStateException("ViewPager with adapter " + this
					+ " requires a view id");
		}
	}

	@NonNull
	@Override
	public Object instantiateItem(@NonNull ViewGroup container, int position) {
		// If we already have this item instantiated, there is nothing
		// to do.  This can happen when we are restoring the entire pager
		// from its saved state, where the fragment manager has already
		// taken care of restoring the fragments we previously had instantiated.
		if (mFragments.size() > position) {
			Fragment f = mFragments.get(position);
			if (f != null) {
				return f;
			}
		}

		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}

		Fragment fragment = getItem(position);
		String fragmentTag = getTag(position);
		if (DEBUG) LogUtils.d(TAG, "Adding item #" + position + ": f=" + fragment + " t=" + fragmentTag);
		if (mSavedState.size() > position) {
			String savedTag = mSavedFragmentTags.get(position);
			if (TextUtils.equals(fragmentTag, savedTag)) {
				Fragment.SavedState fss = mSavedState.get(position);
				if (fss != null) {
					fragment.setInitialSavedState(fss);
				}
			}
		}
		while (mFragments.size() <= position) {
			mFragments.add(null);
		}
		fragment.setMenuVisibility(false);
		fragment.setUserVisibleHint(false);
		mFragments.set(position, fragment);
		mCurTransaction.add(container.getId(), fragment, fragmentTag);

		return fragment;
	}

	@Override
	public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
		Fragment fragment = (Fragment) object;

		if (mCurTransaction == null) {
			mCurTransaction = mFragmentManager.beginTransaction();
		}
		if (DEBUG) LogUtils.d(TAG, "Removing item #" + position + ": f=" + object
				+ " v=" + ((Fragment)object).getView());
		while (mSavedState.size() <= position) {
			mSavedState.add(null);
			mSavedFragmentTags.add(null);
		}
		mSavedState.set(position, fragment.isAdded()
				? mFragmentManager.saveFragmentInstanceState(fragment) : null);
		mSavedFragmentTags.set(position, fragment.getTag());
		mFragments.set(position, null);

		mCurTransaction.remove(fragment);
	}

	@Override
	@SuppressWarnings("ReferenceEquality")
	public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
		Fragment fragment = (Fragment)object;
		if (fragment != mCurrentPrimaryItem) {
			if (mCurrentPrimaryItem != null) {
				mCurrentPrimaryItem.setMenuVisibility(false);
				mCurrentPrimaryItem.setUserVisibleHint(false);
			}
			if (fragment != null) {
				fragment.setMenuVisibility(true);
				fragment.setUserVisibleHint(true);
			}
			mCurrentPrimaryItem = fragment;
		}
	}

	@Override
	public void finishUpdate(@NonNull ViewGroup container) {
		if (mCurTransaction != null) {
			mCurTransaction.commitNowAllowingStateLoss();
			mCurTransaction = null;
		}
	}

	@Override
	public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
		return ((Fragment)object).getView() == view;
	}

	@Override
	public Parcelable saveState() {
		Bundle state = null;
		if (mSavedState.size() > 0) {
			state = new Bundle();
			Fragment.SavedState[] fss = new Fragment.SavedState[mSavedState.size()];
			mSavedState.toArray(fss);
			state.putParcelableArray("states", fss);
			state.putStringArrayList("tags", mSavedFragmentTags);
		}
		for (int i=0; i<mFragments.size(); i++) {
			Fragment f = mFragments.get(i);
			if (f != null && f.isAdded()) {
				if (state == null) {
					state = new Bundle();
				}
				String key = "f" + i;
				mFragmentManager.putFragment(state, key, f);
			}
		}
		return state;
	}

	@Override
	public void restoreState(Parcelable state, ClassLoader loader) {
		if (state != null) {
			Bundle bundle = (Bundle)state;
			bundle.setClassLoader(loader);
			Parcelable[] fss = bundle.getParcelableArray("states");
			mSavedState.clear();
			mFragments.clear();
			if (fss != null) {
				for (int i=0; i<fss.length; i++) {
					mSavedState.add((Fragment.SavedState)fss[i]);
				}
			}
			mSavedFragmentTags = bundle.getStringArrayList("tags");

			Iterable<String> keys = bundle.keySet();
			for (String key: keys) {
				if (key.startsWith("f")) {
					int index = Integer.parseInt(key.substring(1));
					Fragment f = mFragmentManager.getFragment(bundle, key);
					if (f != null) {
						while (mFragments.size() <= index) {
							mFragments.add(null);
						}
						f.setMenuVisibility(false);
						mFragments.set(index, f);
					} else {
						LogUtils.e(TAG, "Bad fragment at key " + key);
					}
				}
			}
		}
	}


	//region == Missing code ==

	public String getTag(int position) {
		return null;
	}

	//endregion

}
