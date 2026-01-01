package net.dialingspoon.standalonevc;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main {

    private static final String EMBEDDED_DIR = "META-INF/jars"; //TODO double click terminal //icon
    private static final String VOICECHAT_DIR = ".";

    public static void main(String[] args) {
        if (true) { //TODO if not in debug mode
            try {
                Path selfPath = getSelfJarPath();
                URLClassLoader loader = loadJars(selfPath);
                Thread.currentThread().setContextClassLoader(loader);
                Class<?> mainClass = loader.loadClass("net.dialingspoon.standalonevc.StandaloneVoiceChat");
                mainClass.getMethod("init", Path.class).invoke(null, selfPath.getParent());
            } catch (Throwable t) {
                System.err.println("Error in StandaloneVoiceChat: " + t.getLocalizedMessage());
                t.printStackTrace();
                System.exit(1);
            }
        } else {
            Path path = Path.of(".").toAbsolutePath();
            StandaloneVoiceChat.init(path);
        }
    }

    public static URLClassLoader loadJars(Path selfPath) throws IOException {
        List<URL> urls = new ArrayList<>();

        urls.add(copySelfJarToTemp(selfPath));
        urls.addAll(extractEmbeddedJars(selfPath));

        urls.addAll(scanExternalJars());

        return new ChildFirstClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    private static Path getSelfJarPath() throws IOException {
        try {
            URL selfUrl = Main.class.getProtectionDomain().getCodeSource().getLocation();
            Path selfPath = Path.of(selfUrl.toURI());
            if (!Files.isRegularFile(selfPath)) {
                throw new IllegalStateException("Not running from a JAR file");
            }
            return selfPath;
        } catch (Exception e) {
            throw new IOException("Failed to locate running JAR", e);
        }
    }

    public static URL copySelfJarToTemp(Path selfPath) throws IOException {
        Path tempJar = Files.createTempFile("self-copy-", ".jar");
        Files.copy(selfPath, tempJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        tempJar.toFile().deleteOnExit();
        return tempJar.toUri().toURL();
    }

    private static Collection<URL> extractEmbeddedJars(Path selfPath) throws IOException {
        List<URL> urls = new ArrayList<>();
        try (JarFile jf = new JarFile(selfPath.toFile())) {
            jf.stream()
                    .filter(entry -> entry.getName().startsWith(EMBEDDED_DIR) && entry.getName().endsWith(".jar"))
                    .forEach(entry -> {
                        try {
                            Path tmpEmbedded = Files.createTempFile("embedded-", ".jar");
                            tmpEmbedded.toFile().deleteOnExit();
                            try (InputStream is = jf.getInputStream(entry);
                                 OutputStream os = Files.newOutputStream(tmpEmbedded)) {
                                is.transferTo(os);
                            }
                            urls.add(tmpEmbedded.toUri().toURL());
                            System.out.println("Loaded embedded JAR: " + entry.getName());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
        return urls;
    }


    private static Collection<URL> scanExternalJars() {
        File vcDir = new File(VOICECHAT_DIR);
        File[] jars = vcDir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) return Collections.emptyList();

        List<URL> urls = new ArrayList<>(jars.length);
        for (File jar : jars) {
            try (JarFile jf = new JarFile(jar)) {
                JarEntry entry = jf.getJarEntry("META-INF/mods.toml");
                if (entry != null) {
                    try (InputStream is = jf.getInputStream(entry)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.startsWith("modId") && line.contains("\"voicechat\"")) {
                                urls.add(jar.toURI().toURL());
                                System.out.println("Loaded external JAR: " + jar.getName());
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Unable to scan jar file " + jar.getName());
            }
        }
        return urls;
    }

    /** Child-first URLClassLoader to prioritize embedded JARs */
    public static class ChildFirstClassLoader extends URLClassLoader {
        public ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("java.")) return super.loadClass(name, resolve);

            try {
                Class<?> c = findClass(name);
                if (resolve) resolveClass(c);
                return c;
            } catch (ClassNotFoundException ignored) {}

            return super.loadClass(name, resolve);
        }
    }
}
