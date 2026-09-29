package com.nfsmw.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int REQUEST_GAME_FOLDER = 41;
    private static final String PREFS = "game_selection";
    private static final String TREE_URI = "tree_uri";

    static {
        System.loadLibrary("nfsmw_android");
    }

    private static native String nativeInitialize(String filesDir);
    private static native String nativeSurfaceReady(Surface surface);

    private TextView status;
    private TextView importStatus;
    private Button selectFolder;
    private final ExecutorService importer = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout content = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        content.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(28, 24, 28, 24);
        panel.setBackgroundColor(0xCC101820);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(18);
        status.setPadding(32, 32, 32, 32);
        status.setText(nativeInitialize(getFilesDir().getAbsolutePath()));
        importStatus = new TextView(this);
        importStatus.setGravity(Gravity.CENTER);
        importStatus.setTextSize(15);
        selectFolder = new Button(this);
        selectFolder.setText("Seleccionar carpeta extraída");
        selectFolder.setOnClickListener(view -> selectGameFolder());
        panel.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        panel.addView(importStatus, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        panel.addView(selectFolder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(panel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(content);
        showPrivateGameFolder();
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        status.setText(nativeSurfaceReady(holder.getSurface()));
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}

    private void selectGameFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        String saved = getPreferences(MODE_PRIVATE).getString(TREE_URI, null);
        if (saved != null && android.os.Build.VERSION.SDK_INT >= 26) {
            intent.putExtra("android.provider.extra.INITIAL_URI", Uri.parse(saved));
        }
        startActivityForResult(intent, REQUEST_GAME_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_GAME_FOLDER || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri tree = data.getData();
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(tree, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException error) {
            setImportStatus("No se pudo conservar el permiso de la carpeta: " + error.getMessage());
            return;
        }
        getPreferences(MODE_PRIVATE).edit().putString(TREE_URI, tree.toString()).apply();
        importGameFolder(tree);
    }

    private void importGameFolder(Uri tree) {
        selectFolder.setEnabled(false);
        setImportStatus("Copiando el juego al almacenamiento interno…");
        importer.execute(() -> {
            File appRoot = new File(getFilesDir(), "nfsmw");
            File staging = new File(appRoot, "game_root.importing");
            File gameRoot = new File(appRoot, "game_root");
            File previous = new File(appRoot, "game_root.previous");
            try {
                if (!gameRoot.exists() && previous.exists()) {
                    Files.move(previous.toPath(), gameRoot.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                deleteRecursively(staging);
                deleteRecursively(previous);
                DocumentFile source = DocumentFile.fromTreeUri(this, tree);
                if (source == null || !source.isDirectory()) throw new IOException("El proveedor no abrió la carpeta elegida.");
                copyDirectory(source, staging, staging.getCanonicalFile(), new long[]{0});
                if (!new File(staging, "default.xex").isFile() ||
                        !new File(staging, "NFS").isDirectory() || !new File(staging, "Movies").isDirectory()) {
                    throw new IOException("No encontré default.xex, NFS y Movies en la carpeta seleccionada.");
                }
                boolean movedPrevious = false;
                if (gameRoot.exists()) {
                    Files.move(gameRoot.toPath(), previous.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    movedPrevious = true;
                }
                try {
                    Files.move(staging.toPath(), gameRoot.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException error) {
                    if (movedPrevious) Files.move(previous.toPath(), gameRoot.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    throw error;
                }
                deleteRecursively(previous);
                mainHandler.post(() -> setImportStatus("Copia lista en almacenamiento interno:\n" + gameRoot.getAbsolutePath()));
            } catch (Exception error) {
                try { deleteRecursively(staging); } catch (IOException ignored) {}
                mainHandler.post(() -> setImportStatus("Importación fallida: " + error.getMessage()));
            } finally {
                mainHandler.post(() -> selectFolder.setEnabled(true));
            }
        });
    }

    private void copyDirectory(DocumentFile source, File destination, File safeRoot, long[] copiedBytes) throws IOException {
        if (!destination.getCanonicalFile().toPath().startsWith(safeRoot.toPath())) {
            throw new IOException("La carpeta contiene un nombre de archivo no válido.");
        }
        if (!destination.mkdirs() && !destination.isDirectory()) throw new IOException("No se pudo crear " + destination);
        for (DocumentFile child : source.listFiles()) {
            String name = child.getName();
            if (name == null || name.isEmpty() || name.equals(".") || name.equals("..") ||
                    name.contains("/") || name.contains("\\")) {
                throw new IOException("Nombre de archivo no válido en la carpeta del juego.");
            }
            File target = new File(destination, name);
            if (child.isDirectory()) {
                copyDirectory(child, target, safeRoot, copiedBytes);
            } else if (child.isFile()) {
                File parent = target.getParentFile();
                if (parent == null || !parent.getCanonicalFile().toPath().startsWith(safeRoot.toPath())) {
                    throw new IOException("Ruta de archivo fuera de la carpeta de importación.");
                }
                if (!parent.mkdirs() && !parent.isDirectory()) throw new IOException("No se pudo crear una carpeta del juego.");
                try (InputStream input = getContentResolver().openInputStream(child.getUri());
                     FileOutputStream output = new FileOutputStream(target)) {
                    if (input == null) throw new IOException("No se pudo leer " + name);
                    byte[] buffer = new byte[1024 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        copiedBytes[0] += count;
                        if ((copiedBytes[0] & ((32L * 1024 * 1024) - 1)) < count) {
                            long total = copiedBytes[0];
                            mainHandler.post(() -> setImportStatus("Copiados " + (total / (1024 * 1024)) + " MiB…"));
                        }
                    }
                    output.getFD().sync();
                }
            }
        }
    }

    private void showPrivateGameFolder() {
        File gameRoot = new File(new File(getFilesDir(), "nfsmw"), "game_root");
        if (new File(gameRoot, "default.xex").isFile()) {
            setImportStatus("Juego preparado en almacenamiento interno:\n" + gameRoot.getAbsolutePath());
        } else {
            setImportStatus("Se copiará a almacenamiento privado:\n" + gameRoot.getAbsolutePath());
        }
    }

    private void setImportStatus(String message) {
        if (importStatus != null) importStatus.setText(message);
    }

    private static void deleteRecursively(File file) throws IOException {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteRecursively(child);
        if (!file.delete()) throw new IOException("No se pudo limpiar " + file.getName());
    }

    @Override
    protected void onDestroy() {
        importer.shutdown();
        super.onDestroy();
    }
}
