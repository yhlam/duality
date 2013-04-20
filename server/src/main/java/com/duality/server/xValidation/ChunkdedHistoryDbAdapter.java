package com.duality.server.xValidation;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import com.duality.server.openfirePlugin.InstanceLoader;
import com.duality.server.openfirePlugin.dataTier.CachingHistoryDbAdapter;
import com.duality.server.openfirePlugin.dataTier.HistoryEntry;
import com.duality.server.openfirePlugin.dataTier.Location;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

public class ChunkdedHistoryDbAdapter extends CachingHistoryDbAdapter {
	private final CachingHistoryDbAdapter underlying;
	private final int segment;
	private int chunk;

	private ChunkdedHistoryDbAdapter() {
		final InstanceLoader instanceLoader = InstanceLoader.singleton();
		underlying = instanceLoader.createInstance(CachingHistoryDbAdapter.class);

		final List<HistoryEntry> allHistory = underlying.getAllHistory();
		final int count = allHistory.size();
		segment = count / CrossValidation.N_FOLD;
		chunk = 0;
		refresh();
	}

	@Override
	protected List<HistoryEntry> loadAllHistory(OrderAttribute attr, OrderDirection dir) {
		if(underlying == null) {
			return Collections.emptyList();
		}
		
		final List<HistoryEntry> underlyingLoadHistory = invokeUnderlying("loadAllHistory", new Class[]{OrderAttribute.class, OrderDirection.class}, attr, dir);
		
		final int lower = segment * chunk;
		final int upper = lower + segment;

		final List<HistoryEntry> filtered = Lists.newLinkedList();
		for (HistoryEntry historyEntry : underlyingLoadHistory) {
			final int id = historyEntry.getId();
			if(id <= lower || id > upper) {
				filtered.add(historyEntry);
			}
		}

		return filtered;
	}
	
	public CachingHistoryDbAdapter getUnderlying() {
		return underlying;
	}
	
	public SortedSet<Integer> getTestSetIds() {
		final int lower = segment * chunk;
		final int upper = lower + segment;
		final Range<Integer> range = Range.openClosed(lower, upper);
		final ContiguousSet<Integer> set = ContiguousSet.create(range, DiscreteDomain.integers());
		return set;
	}

	public void setChunk(int chunk) {
		this.chunk = chunk;
	}
	
	public void nextChunk() {
		chunk++;
	}

	private <T> T invokeUnderlying(String name, @SuppressWarnings("rawtypes") Class[] paraTypes, Object ... paras) {
		try {
			final Class<? extends CachingHistoryDbAdapter> clazz = underlying.getClass();
			final Method method = clazz.getDeclaredMethod(name, paraTypes);
			method.setAccessible(true);
			@SuppressWarnings("unchecked")
			final T retval = (T) method.invoke(underlying, paras);
			return retval;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected HistoryEntry insertIntoDb(String sender, String receiver, Date time, String message, Location senderLocation, Location receiverLocation) {
		return null;
	}
}
