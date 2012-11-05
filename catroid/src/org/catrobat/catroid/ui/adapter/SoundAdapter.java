/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2012 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui.adapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.utils.UtilFile;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;

public class SoundAdapter extends ArrayAdapter<SoundInfo> {

	protected ArrayList<SoundInfo> soundInfoItems;
	protected Context context;

	private OnSoundEditListener onSoundEditListener;

	private int selectMode;
	private static int elapsedSeconds;
	private static long elapsedMilliSeconds;
	private static long currentPlayingBase;
	private boolean showDetails;
	private boolean initializedChronometer = false;
	private Set<Integer> checkedSounds = new HashSet<Integer>();

	private int currentPlayingPosition = -1;

	public SoundAdapter(final Context context, int textViewResourceId, ArrayList<SoundInfo> items, boolean showDetails) {
		super(context, textViewResourceId, items);
		this.context = context;
		this.showDetails = showDetails;
		soundInfoItems = items;
		selectMode = Constants.SELECT_NONE;
	}

	public void setOnSoundEditListener(OnSoundEditListener listener) {
		onSoundEditListener = listener;
	}

	private static class ViewHolder {
		private ImageButton playButton;
		private ImageButton pauseButton;
		private CheckBox checkbox;
		private TextView titleTextView;
		private TextView timeSeperatorTextView;
		private TextView timeDurationTextView;
		private TextView soundFileSizeTextView;
		private Chronometer timePlayedChronometer;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			Log.d("CATROID", "Initialize HOLDER!!!");

			convertView = View.inflate(context, R.layout.fragment_sound_soundlist_item, null);

			holder = new ViewHolder();

			holder.playButton = (ImageButton) convertView.findViewById(R.id.btn_sound_play);
			holder.pauseButton = (ImageButton) convertView.findViewById(R.id.btn_sound_pause);

			holder.playButton.setVisibility(Button.VISIBLE);
			holder.pauseButton.setVisibility(Button.GONE);

			holder.checkbox = (CheckBox) convertView.findViewById(R.id.checkbox);

			holder.titleTextView = (TextView) convertView.findViewById(R.id.sound_title);
			holder.timeSeperatorTextView = (TextView) convertView.findViewById(R.id.sound_time_seperator);
			holder.timeDurationTextView = (TextView) convertView.findViewById(R.id.sound_duration);
			holder.soundFileSizeTextView = (TextView) convertView.findViewById(R.id.sound_size);

			holder.timePlayedChronometer = (Chronometer) convertView.findViewById(R.id.sound_chronometer_time_played);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final SoundInfo soundInfo = soundInfoItems.get(position);

		if (soundInfo != null) {
			holder.titleTextView.setTag(position);
			holder.playButton.setTag(position);
			holder.pauseButton.setTag(position);

			holder.titleTextView.setText(soundInfo.getTitle());

			holder.checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (selectMode == Constants.MULTI_SELECT) {
						if (isChecked) {
							checkedSounds.add(position);
						} else {
							checkedSounds.remove(position);
						}
					} else if (selectMode == Constants.SINGLE_SELECT) {
						if (isChecked) {
							clearCheckedSounds();
							checkedSounds.add(position);
						} else {
							checkedSounds.remove(position);
						}
						notifyDataSetChanged();
					}
				}
			});

			if (selectMode != Constants.SELECT_NONE) {
				holder.checkbox.setVisibility(View.VISIBLE);
			} else {
				holder.checkbox.setVisibility(View.GONE);
				holder.checkbox.setChecked(false);
				clearCheckedSounds();
			}

			if (checkedSounds.contains(position)) {
				holder.checkbox.setChecked(true);
			} else {
				holder.checkbox.setChecked(false);
			}

			try {
				MediaPlayer tempPlayer = new MediaPlayer();
				tempPlayer.setDataSource(soundInfo.getAbsolutePath());
				tempPlayer.prepare();

				long milliseconds = tempPlayer.getDuration();
				int seconds = (int) ((milliseconds / 1000) % 60);
				int minutes = (int) ((milliseconds / 1000) / 60);
				int hours = (int) ((milliseconds / 1000) / 3600);

				String duration = "";

				if (hours == 0) {
					duration = String.format("%02d:%02d", minutes, seconds);
				} else {
					duration = String.format("%02d:%02d:%02d", hours, minutes, seconds);
				}
				holder.timeDurationTextView.setText(duration);

				if (initializedChronometer == false) {
					elapsedMilliSeconds = 0;
					initializedChronometer = true;
				} else if (currentPlayingPosition != -1) {
					elapsedMilliSeconds = SystemClock.elapsedRealtime() - currentPlayingBase;
				}

				if (soundInfo.isPlaying) {
					holder.playButton.setVisibility(Button.GONE);
					holder.pauseButton.setVisibility(Button.VISIBLE);

					holder.timeSeperatorTextView.setVisibility(TextView.VISIBLE);
					holder.timePlayedChronometer.setVisibility(Chronometer.VISIBLE);

					if ((currentPlayingPosition == -1) && (elapsedMilliSeconds == 0)) {
						Log.d("CATROID", "-----START-----");
						currentPlayingPosition = position;
						currentPlayingBase = SystemClock.elapsedRealtime();
						holder.timePlayedChronometer.setBase(currentPlayingBase);
						holder.timePlayedChronometer.start();
					} else if ((position == currentPlayingPosition) && (elapsedMilliSeconds > (milliseconds - 1000))) {
						Log.d("CATROID", "STOP BEFORE!");
						holder.timePlayedChronometer.stop();
						holder.timePlayedChronometer.setBase(SystemClock.elapsedRealtime());

						initializedChronometer = false;
						elapsedMilliSeconds = 0;
						currentPlayingPosition = -1;

						soundInfo.isPlaying = false;
					} else {
						holder.timePlayedChronometer.setBase(SystemClock.elapsedRealtime());
						holder.timePlayedChronometer.start();
					}
				} else {
					Log.d("CATROID", "NOT PLAYING -> elapsed:" + elapsedMilliSeconds);

					holder.playButton.setVisibility(Button.VISIBLE);
					holder.pauseButton.setVisibility(Button.GONE);

					holder.timeSeperatorTextView.setVisibility(TextView.GONE);
					holder.timePlayedChronometer.setVisibility(Chronometer.GONE);

					if ((position == currentPlayingPosition)
							&& ((elapsedSeconds > (seconds - 2)) || (elapsedMilliSeconds != 0))) {

						if ((elapsedSeconds > (seconds - 2)) && elapsedMilliSeconds == 0) {
							Log.d("CATROID", "should stop now!!! (no normal stop)");
						}
						Log.d("CATROID", "------STOP-----: ellapsed: " + elapsedSeconds + " Seconds");

						holder.timePlayedChronometer.stop();
						holder.timePlayedChronometer.setBase(SystemClock.elapsedRealtime());

						initializedChronometer = false;
						elapsedMilliSeconds = 0;
						currentPlayingPosition = -1;
					}
				}

				if (showDetails) {
					holder.soundFileSizeTextView.setText(getContext().getString(R.string.sound_size) + " "
							+ UtilFile.getSizeAsString(new File(soundInfo.getAbsolutePath())));
					holder.soundFileSizeTextView.setVisibility(TextView.VISIBLE);
				} else {
					holder.soundFileSizeTextView.setVisibility(TextView.GONE);
				}

				tempPlayer.reset();
				tempPlayer.release();
			} catch (IOException e) {
				Log.e("CATROID", "Cannot get view.", e);
			}

			holder.playButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (onSoundEditListener != null) {
						onSoundEditListener.onSoundPlay(v);
					}
				}
			});

			holder.pauseButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (onSoundEditListener != null) {
						onSoundEditListener.onSoundPause(v);
					}
				}
			});
		}
		return convertView;
	}

	public Set<Integer> getCheckedSounds() {
		return checkedSounds;
	}

	public void clearCheckedSounds() {
		checkedSounds.clear();
	}

	public void setSelectMode(int mode) {
		selectMode = mode;
	}

	public int getSelectMode() {
		return selectMode;
	}

	public void setShowDetails(boolean showDetails) {
		this.showDetails = showDetails;
	}

	public boolean getShowDetails() {
		return showDetails;
	}

	public interface OnSoundEditListener {

		public void onSoundPlay(View v);

		public void onSoundPause(View v);
	}
}
