package com.ghiles.quizubuntu;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Durable session metadata; working files never belong to the UI lifecycle. */
final class TerminalSessions {
    private final File files, directory;
    TerminalSessions(File files) throws IOException {
        this.files = files;
        directory = new File(files, "terminal-sessions");
        Files.createDirectories(directory.toPath());
        if (!file("1").exists()) { Properties p = new Properties(); p.setProperty("name", "Session 1"); save("1", p); }
    }
    private File file(String id) {
        if (!id.matches("[1-9][0-9]*")) throw new IllegalArgumentException("Identifiant de session invalide");
        return new File(directory, id + ".properties");
    }
    synchronized List<String> list() {
        List<String> ids = new ArrayList<>();
        String[] names = directory.list();
        if (names != null) for (String name : names) if (name.matches("[1-9][0-9]*\\.properties")) ids.add(name.replace(".properties", ""));
        ids.sort(Comparator.comparingLong(Long::parseLong)); return ids;
    }
    synchronized Properties load(String id) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file(id))) { p.load(in); }
        return p;
    }
    synchronized void save(String id, Properties p) throws IOException {
        File target = file(id), temp = new File(directory, id + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) { p.store(out, "Terminal session"); out.getFD().sync(); }
        // Same-filesystem replacement preserves the last complete snapshot on interruption.
        try { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING); }
    }
    synchronized String create(String name) throws IOException {
        List<String> ids = list();
        String id = Long.toString(Long.parseLong(ids.get(ids.size()-1)) + 1);
        Properties p = new Properties(); p.setProperty("name", name.isEmpty() ? "Session " + id : name);
        save(id, p); return id;
    }
    File workspace(String id) { file(id); return id.equals("1") ? new File(files, "github-real") : new File(directory, "workspace-" + id); }
}
