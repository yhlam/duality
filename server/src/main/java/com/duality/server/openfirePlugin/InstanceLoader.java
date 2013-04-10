package com.duality.server.openfirePlugin;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.duality.server.openfirePlugin.dataTier.HistoryDatabaseAdapter;
import com.duality.server.openfirePlugin.dataTier.OpenfireDbAdapter;
import com.duality.server.openfirePlugin.prediction.PredictionEngine;
import com.duality.server.openfirePlugin.prediction.impl.TfIdfNgramPredictionEngine;
import com.google.common.collect.Maps;

public final class InstanceLoader {
	private static final InstanceLoader INSTANCE = new InstanceLoader();

	public static InstanceLoader singleton() {
		return INSTANCE;
	}

	private final Map<Class<?>, Class<?>> bindings;

	private InstanceLoader() {
		bindings = Maps.newConcurrentMap();
		bindings.put(HistoryDatabaseAdapter.class, OpenfireDbAdapter.class);
		bindings.put(PredictionEngine.class, TfIdfNgramPredictionEngine.class);
	}

	public <T> void setBinding(final Class<T> iface, final Class<? extends T> implementation) {
		bindings.put(iface, implementation);
	}

	public <T> T createInstance(final Class<T> iface) {
		final Class<?> implementation = bindings.get(iface);
		if (implementation == null) {
			throw new RuntimeException("No binding of " + iface + " is found.");
		} else {
			try {
				final Constructor<?> constuctor = implementation.getDeclaredConstructor();
				constuctor.setAccessible(true);

				final Object instance = constuctor.newInstance();
				final T casted = iface.cast(instance);
				return casted;
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
