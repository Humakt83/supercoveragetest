package fi.ukkosnetti.coverage;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.fail;

public class Coverager {

	/**
	 * Call this method from your junit test to increase its code coverage
	 * @throws IOException
	 */
	public static void testAll() throws IOException {
		testAll(false);
	}

	public static void testAllPrintFiles() throws IOException {
		testAll(true);
	}

	private static void testAll(final boolean printFiles) throws IOException {
		final String prefix = "src\\main\\java\\", unixPrefix = "src/main/java/", postfix = ".java";
		try (Stream<Path> stream = Files.find(Paths.get(""), 20, (path, attr) ->
		(String.valueOf(path).startsWith(prefix) || String.valueOf(path).startsWith(unixPrefix)) &&String.valueOf(path).endsWith(postfix))) {
			stream.sorted()
			.map(String::valueOf)
			.peek(pathName -> {
				if (printFiles) {
					System.out.println(pathName);
				}
			})
			.map(pathName -> pathName.replace(prefix, ""))
			.map(pathName -> pathName.replace(unixPrefix, ""))
			.map(pathName -> pathName.replace(postfix, ""))
			.map(pathName -> pathName.replace("\\", "."))
			.map(pathName -> pathName.replace("/", "."))
			.map(Coverager::loadClass)
			.forEach(Coverager::testClass);
		}
	}

	private static Class<?> loadClass(String className) {
		try {
			return ClassLoader.getSystemClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			fail("Could not load class");
			throw new RuntimeException();
		}
	}

	private static void testClass(Class<?> cls) {
		try {
			List<Method> methods = Arrays.asList(cls.getDeclaredMethods());
			Arrays.asList(cls.getConstructors()).stream().forEach(constructor -> {
				testMethods(methods, constructor);
			});
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static void testMethods(List<Method> methods, Constructor<?> constructor) {
		try {
			Object obj;
			if (constructor.getParameterCount() > 0) {
				obj = constructor.newInstance(constructor.getParameters());				
			} else {
				obj = constructor.newInstance();
			}
			System.out.println(obj.getClass().getSimpleName());
			for(Method method : methods) {
				System.out.println(method.getName());
				method.setAccessible(true);
				if (method.getParameterCount() > 0) {
					method.invoke(obj, parameterTypesToObjects(method.getParameterTypes()));
				} else {
					method.invoke(obj);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Object[] parameterTypesToObjects(Class<?>[] classes) throws Exception {
		return Arrays.asList(classes).stream()
				.map((ThrowingCreatingFunction<Class<?>, Constructor<?>>)Class::getConstructor)
				.map((ThrowingCreatingFunction<Constructor<?>, Object>)Constructor::newInstance)
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
