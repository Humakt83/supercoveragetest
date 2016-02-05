package fi.ukkosnetti.coverage;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mockito.Mockito;

import fi.ukkosnetti.coverage.strategy.CoverageStrategy;
import fi.ukkosnetti.coverage.strategy.DefaultCoverageStrategy;
import javaslang.control.Match;

public class Coverager {
	
	private CoverageStrategy strategy = new DefaultCoverageStrategy();

	public synchronized void coverage() throws IOException {
		testAll();
	}
	
	public void setCoverageStrategy(CoverageStrategy strategy) {
		this.strategy = strategy;
	}

	private void testAll() throws IOException {
		final String prefix = "src\\main\\java\\", unixPrefix = "src/main/java/", postfix = ".java";
		try (Stream<Path> stream = Files.find(Paths.get(""), strategy.getDepthToScanFilesFromFolders(), (path, attr) ->
		(String.valueOf(path).startsWith(prefix) || String.valueOf(path).startsWith(unixPrefix)) &&String.valueOf(path).endsWith(postfix))) {
			stream.sorted()
			.map(String::valueOf)
			.peek(strategy::printOut)
			.map(pathName -> pathName.replace(prefix, ""))
			.map(pathName -> pathName.replace(unixPrefix, ""))
			.map(pathName -> pathName.replace(postfix, ""))
			.map(pathName -> pathName.replace("\\", "."))
			.map(pathName -> pathName.replace("/", "."))
			.map(this::loadClass)
			.forEach(this::testClass);
		}
	}

	private Class<?> loadClass(String className) {
		try {
			return ClassLoader.getSystemClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			fail("Could not load class");
			throw new RuntimeException();
		}
	}

	private void testClass(Class<?> cls) {
		try {
			List<Method> methods = Arrays.asList(cls.getDeclaredMethods());
			Arrays.asList(cls.getConstructors()).stream().forEach(constructor -> {
				testMethods(methods, constructor);
			});
		} catch (Exception e) {
			strategy.printOut(e);
		}
	}

	private void testMethods(List<Method> methods, Constructor<?> constructor) {
		try {
			Object obj = constructObject(constructor);
			strategy.printOut(obj.getClass().getName());
			for(Method method : methods) {
				try {
					testMethod(obj, method);
				} catch (Exception e) {
					strategy.printOut(e);
				}
			}
		} catch (Exception e) {
			strategy.printOut(e);
		}
	}

	private void testMethod(final Object obj, final Method method)
			throws IllegalAccessException, InvocationTargetException, Exception {
		if (method.isSynthetic()) {
			return;
		}
		strategy.printOut(method.getName());
		method.setAccessible(true);
		Future<?> future = Executors.newSingleThreadExecutor().submit(invokeMethod(obj, method));
		try {
			future.get(strategy.getTimeoutForMethodExecution(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			strategy.printOut("Method timedout");
			future.cancel(true);
		}
	}

	private Callable<?> invokeMethod(final Object obj, final Method method) {
		return new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				try {
					if (method.getParameterCount() > 0) {
						method.invoke(obj, parameterTypesToObjects(method.getParameterTypes()));
					} else {
						method.invoke(obj);
					}
				} catch (Exception e) {
					strategy.printOut(e);
				}
				return null;
			}
			
		};
	}
	
	private Object constructObject(final Constructor<?> constructor) throws Exception {
		strategy.printOut(constructor.getDeclaringClass().getName());
		if (Modifier.isInterface(constructor.getDeclaringClass().getModifiers())) {
			return Mockito.mock(constructor.getDeclaringClass());
		}
		final int parameterCount = constructor.getParameterCount();
		final ThrowingCreatingFunction<Constructor<?>, Object> objectSupply = (con) -> {
			return parameterCount > 0 ? createWithParameters(con, parameterTypesToObjects(con.getParameterTypes()))
					: constructor.newInstance();
		};
		Optional<Object> optional = parameterCount != 1 ? Optional.empty() 
				: Optional.of(getParameterTypeAsBasicType(constructor.getParameterTypes()[0])).map((ThrowingCreatingFunction<Object, Object>)constructor::newInstance);
		return optional.orElse(objectSupply.apply(constructor));
	}
	
	private Object createWithParameters(Constructor<?> constructor, Object ... parameters) throws Exception {
		final StringBuilder sb = new StringBuilder("Parameters used to construct: ");
		for (Object param : parameters) {
			sb.append(param.getClass().getSimpleName());
			sb.append(", ");
		}
		strategy.printOut(sb.toString());
		return constructor.newInstance(parameters);
	}
	
	private Object getParameterTypeAsBasicType(Class<?> paramType) {
		if (paramType.isPrimitive()) {
			return returnPrimitiveType(paramType);
		}
		return Match.of(paramType)
			.whenType(String.class).then(t -> (Object)new String(""))
			.whenType(Double.class).then(t -> (Object)new Double(0.0))
			.whenType(Float.class).then(t -> (Object)new Float(0f))
			.whenType(Boolean.class).then(t -> (Object)new Boolean(false))
			.whenType(Character.class).then(t -> (Object)new Character('c'))
			.whenType(Long.class).then(t -> (Object)new Long(0))
			.whenType(Integer.class).then(t -> (Object)new Integer(0))
			.whenType(Byte.class).then(t -> (Object)new Byte(""))
			.otherwise(() -> Mockito.mock(paramType)).get();
	}

	private Object returnPrimitiveType(Class<?> paramType) {
		switch (paramType.getName()) {
		case "boolean": return false;
		case "double": return 0.0;
		case "long": return 0l;
		case "float": return 0f;
		case "int":
		case "byte": return 0;
		case "char": return 'a';
		default:
			return null;
		}
	}
	
	private Object[] parameterTypesToObjects(Class<?>[] classes) throws Exception {
		return Arrays.asList(classes).stream()
				.map(this::getParameterTypeAsBasicType)
				.collect(Collectors.toList()).toArray();
	}

	@FunctionalInterface
	private interface ThrowingCreatingFunction<F, T> extends Function<F, T> {

		@Override
		default T apply(final F elem) {
			try {
				return applyThrows(elem);
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}

		T applyThrows(F elem) throws Exception;

	}
	
}
