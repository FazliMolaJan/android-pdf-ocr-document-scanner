package com.pixelnetica.cropdemo.util;

import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.io.UncheckedIOException;

public class ParcelableHolder implements Parcelable {
	final Object mContent;
	final ReferenceBinder mBinder = new ReferenceBinder();

	public ParcelableHolder(@NonNull Object content) {
		this.mContent = content;
	}

	@SuppressWarnings("unchecked")
	public <T> T get() {
		return (T) mContent;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStrongBinder(mBinder);
	}

	public static final Parcelable.Creator<ParcelableHolder> CREATOR = new Parcelable.Creator<ParcelableHolder>() {
		@Override
		public ParcelableHolder createFromParcel(Parcel source) {
			try {
				return ((ReferenceBinder) source.readStrongBinder()).get();
			} catch (ClassCastException e) {
				return null;
			}
		}

		@Override
		public ParcelableHolder[] newArray(int size) {
			return new ParcelableHolder[size];
		}
	};

	private class ReferenceBinder extends Binder {
		ParcelableHolder get() {
			return ParcelableHolder.this;
		}
	}
}
