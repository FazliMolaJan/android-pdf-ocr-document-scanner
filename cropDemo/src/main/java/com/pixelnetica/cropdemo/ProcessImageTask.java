package com.pixelnetica.cropdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pixelnetica.cropdemo.util.Utils;
import com.pixelnetica.imagesdk.Corners;
import com.pixelnetica.imagesdk.MetaImage;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Denis on 28.10.2016.
 */

class ProcessImageTask extends AsyncTask<MetaImage, Void, ProcessImageTask.ProcessImageResult> {

	class ProcessImageResult extends TaskResult {
		final MetaImage targetImage;

		ProcessImageResult(@NonNull MetaImage targetImage) {
			this.targetImage = targetImage;
		}

		ProcessImageResult(@TaskError int error) {
			super(error);
			targetImage = null;
		}

	}

	interface Listener {
		void onProcessImageComplete(@NonNull ProcessImageTask task, @NonNull ProcessImageResult result);
	}

	/**
	 * Processing profile
	 */
	@Retention(RetentionPolicy.SOURCE)
	@IntDef(flag = true, value = {NoBinarization, BWBinarization, GrayBinarization, ColorBinarization, ProcessingMask, StrongShadows})
	@interface ProcessingProfile {};
	static final int NoBinarization = 0;
	static final int BWBinarization = 1;
	static final int GrayBinarization = 2;
	static final int ColorBinarization = 3;

	static final int ProcessingMask = 0xFF;
	static final int StrongShadows = 1 << 30;

	@NonNull
	private final SdkFactory factory;

	@Nullable
	protected final Corners corners;

	@ProcessingProfile
	private final int profile;

	// Output
	@NonNull
	private final Listener listener;

	ProcessImageTask(@NonNull SdkFactory factory, @Nullable Corners corners, @ProcessingProfile int profile,
	                 @NonNull Listener listener) {
		this.factory = factory;
		this.corners = corners;
		this.profile = profile;
		this.listener = listener;
	}

	@Override
	protected ProcessImageResult doInBackground(MetaImage... params) {
		try (SdkFactory.Routine routine = factory.createRoutine()) {

			// DEBUG ONLY!!!
			if (routine.sdk == null) {
				// Simple rotate image
				Matrix matrix = new Matrix();
				switch (params[0].getExifOrientation()) {
					case MetaImage.ExifRotate90:
						matrix.postRotate(90);
						break;
					case MetaImage.ExifRotate180:
						matrix.postRotate(180);
						break;
					case MetaImage.ExifRotate270:
						matrix.postRotate(270);
						break;
					// etc... DEBUG only
				}
				Bitmap bitmap = params[0].getBitmap();
				Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				return new ProcessImageResult(new MetaImage(rotated));
			}

			// Usually image already cropped
			MetaImage croppedImage;
			if (corners != null) {
				croppedImage = routine.sdk.correctDocument(params[0], corners);
				if (croppedImage == null) {
					return new ProcessImageResult(TaskResult.INVALIDCORNERS);
				}
			} else {
				croppedImage = params[0];
				if (croppedImage == null) {
					return new ProcessImageResult(TaskResult.INVALIDFILE);
				}
			}

			croppedImage.setStrongShadows((profile & StrongShadows) != 0);
			MetaImage targetImage = null;
			switch (profile & ProcessingMask) {
				case NoBinarization:
					targetImage = routine.sdk.imageOriginal(croppedImage);
					break;

				case BWBinarization:
					targetImage = routine.sdk.imageBWBinarization(croppedImage);
					break;

				case GrayBinarization:
					targetImage = routine.sdk.imageGrayBinarization(croppedImage);
					break;

				case ColorBinarization:
					targetImage = routine.sdk.imageColorBinarization(croppedImage);
					break;

				default:
					throw new IllegalStateException("Unknown processing " + Integer.toString(profile & ProcessingMask));
			}

			return new ProcessImageResult(targetImage);
		} catch (OutOfMemoryError e) {
			return new ProcessImageResult(TaskResult.OUTOFMEMORY);
		} catch (Error | Exception e) {
			e.printStackTrace();
			return new ProcessImageResult(TaskResult.PROCESSING);
		}
	}

	@Override
	protected void onPostExecute(ProcessImageResult result) {
		listener.onProcessImageComplete(this, result);
	}
}
