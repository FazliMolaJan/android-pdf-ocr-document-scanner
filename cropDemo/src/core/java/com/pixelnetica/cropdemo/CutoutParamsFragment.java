package com.pixelnetica.cropdemo;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

/**
 * Created by Denis on 13.03.2017.
 */

public class CutoutParamsFragment extends Fragment implements ISettingsFragment {
	// Stub

	@Override
	public boolean save(@NonNull SharedPreferences.Editor editor) {
		return true;
	}
}
