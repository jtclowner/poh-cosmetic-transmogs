package com.pohcosmetictransmogs;

import java.lang.reflect.Proxy;
import java.util.function.BiFunction;

/** Test-only interface doubles; no client internals or private members are accessed. */
final class ApiDouble
{
	static final Object DEFAULT = new Object();

	static <T> T of(Class<T> type, BiFunction<String, Object[], Object> calls)
	{
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "equals": return proxy == args[0];
					case "hashCode": return System.identityHashCode(proxy);
					case "toString": return type.getSimpleName();
					default: break;
				}
				Object result = calls.apply(method.getName(), args == null ? new Object[0] : args);
				if (result != DEFAULT)
				{
					return result;
				}
				Class<?> returns = method.getReturnType();
				if (returns == boolean.class) { return false; }
				if (returns == int.class) { return 0; }
				if (returns == long.class) { return 0L; }
				if (returns == float.class) { return 0f; }
				if (returns == double.class) { return 0d; }
				return returns.isInstance(proxy) ? proxy : null;
			}));
	}
}
