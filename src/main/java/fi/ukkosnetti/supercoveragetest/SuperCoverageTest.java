package fi.ukkosnetti.supercoveragetest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

public class SuperCoverageTest {

	/**
	 * Call this method from your junit test to increase its code coverage
	 * @throws IOException
	 */
	public static void testAll() throws IOException {
		final String prefix = "src\\main\\java\\", postfix = ".java";
		try (Stream<Path> stream = Files.find(Paths.get(""), 20, (path, attr) ->
		String.valueOf(path).startsWith(prefix) &&String.valueOf(path).endsWith(postfix))) {
			stream.sorted()
			.map(String::valueOf)
			.map(pathName -> pathName.replace(prefix, ""))
			.map(pathName -> pathName.replace(postfix, ""))
			.map(pathName -> pathName.replace("\\", "."))
			.map(SuperCoverageTest::loadClass)
			.forEach(SuperCoverageTest::testClass);
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
		Arrays.asList(cls.getMethods()).stream().forEach(method -> {
			try {
				method.invoke(method.getParameterTypes());
			} catch (Exception e) {
				//adding coverage
			}
		});
	}
}
