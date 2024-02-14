import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.fabricmc.tinyremapper.BulkTest;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.IntegrationTest1;
import net.fabricmc.tinyremapper.IntegrationTest3;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TestUtil;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

public class Dpyq {
	private static final String MAPPING_PATH = "/mite/mapping.tiny";
	private static final String INPUT_PATH = "/mite/dpyq.jar";

	@Test
	public void fabricApi() throws IOException {
		TinyRemapper remapper;

		try (BufferedReader reader = getMappingReader()) {
			remapper = TinyRemapper.newRemapper()
					.withMappings(TinyUtils.createTinyMappingProvider(reader, "intermediary", "named"))
					.build();

			Map<Path, InputTag> files = new HashMap<>();

			try (ZipInputStream zis = new ZipInputStream(getInputStream(INPUT_PATH))) {
				ZipEntry entry;

				while ((entry = zis.getNextEntry()) != null) {
					if (!entry.isDirectory() && entry.getName().endsWith(".jar")) {
						String name = entry.getName();
						name = name.substring(name.lastIndexOf('/') + 1);

						Path file = tmpDir.resolve(name);
						Files.copy(zis, file);
						files.put(file, remapper.createInputTag());
					}
				}
			}

			for (Map.Entry<Path, InputTag> entry : files.entrySet()) {
				remapper.readInputsAsync(entry.getValue(), entry.getKey());
			}


			for (Map.Entry<Path, InputTag> entry : files.entrySet()) {
				Path file = entry.getKey();

				try (OutputConsumerPath consumer = new OutputConsumerPath.Builder(tmpDir.resolve(file.getFileName().toString().replace(".jar", "-out.jar"))).build()) {
					remapper.apply(consumer, entry.getValue());
				}
			}
		}
	}

	private static BufferedReader getMappingReader() throws IOException {
		InputStream is = getInputStream(MAPPING_PATH);

		return new BufferedReader(new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8));
	}

	private static InputStream getInputStream(String file) {
		return Dpyq.class.getClassLoader().getResourceAsStream(file);
	}

	@TempDir
	static Path tmpDir;
}
