import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarInjector {


    public static void injectAll(Path jar, List<Path> sources, List<String> targetPaths) throws IOException {
        if (sources.size() != targetPaths.size()) {
            throw new IllegalArgumentException("Sources and targetPaths must have the same size");
        }

        Path temp = Files.createTempFile("inject-all-", ".jar");

        Manifest manifest;
        try (JarInputStream jis = new JarInputStream(Files.newInputStream(jar))) {
            manifest = jis.getManifest();
        }

        try (
                JarInputStream jis = new JarInputStream(Files.newInputStream(jar));
                JarOutputStream jos = new JarOutputStream(Files.newOutputStream(temp), manifest)
        ) {
            JarEntry e;
            while ((e = jis.getNextJarEntry()) != null) {
                if (targetPaths.contains(e.getName())) continue;
                jos.putNextEntry(new JarEntry(e.getName()));
                jis.transferTo(jos);
                jos.closeEntry();
            }

            for (int i = 0; i < sources.size(); i++) {
                JarEntry injected = new JarEntry(targetPaths.get(i));
                jos.putNextEntry(injected);
                Files.copy(sources.get(i), jos);
                jos.closeEntry();
            }
        }

        Files.move(temp, jar, StandardCopyOption.REPLACE_EXISTING);
    }

}
