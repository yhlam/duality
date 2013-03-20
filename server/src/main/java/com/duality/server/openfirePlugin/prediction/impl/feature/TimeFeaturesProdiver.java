package com.duality.server.openfirePlugin.prediction.impl.feature;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfUtils;
import com.duality.server.openfirePlugin.prediction.impl.feature.AtomicFeature.FeatureType;

public class TimeFeaturesProdiver implements AtomicFeaturesProvider {
	
	@Override
	public void constructFeatures(HistoryEntry history, List<AtomicFeature<?>> features) {
		final Date time = history.getTime();
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);
		
		final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		TfIdfUtils.addAtomicFeature(features, FeatureType.DAY_OF_WEEK, dayOfWeek);
		
		final int month = calendar.get(Calendar.MONTH);
		TfIdfUtils.addAtomicFeature(features, FeatureType.MONTH, month);
		
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		final TimeSection timeSection = TimeSection.getTimeSectionFromHour(hour);
		TfIdfUtils.addAtomicFeature(features, FeatureType.TIME, timeSection);
		
		// TODO: Add a feature of holiday
	}
	
	public static enum TimeSection {
		MID_NIGHT(2),
		MORNING(6),
		AFTERNOON(12),
		NIGHT(18);
		
		private final int startHour;
		
		private TimeSection(final int startHour) {
			this.startHour = startHour;
		}
		
		public static TimeSection getTimeSectionFromHour(int hour) {
			final TimeSection[] sections = TimeSection.values();
			
			final int lastIndex = sections.length - 1;
			for (int i=0; i<lastIndex; i++) {
				final TimeSection thisSection = sections[i];
				final TimeSection nextSection = sections[i+1];
				final int start = thisSection.startHour;
				final int end = nextSection.startHour;
				if(hour >= start && hour < end) {
					return thisSection;
				}
			}
			
			return sections[lastIndex];
		}
	}
}
