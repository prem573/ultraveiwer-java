package com.cleanroom.remotedesktop.client.file;

import com.cleanroom.remotedesktop.protocol.model.FeatureModels.FileChunk;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.FileItem;
import com.cleanroom.remotedesktop.protocol.model.FeatureModels.FileListResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class FileTransferService {
    private static final Logger log = LoggerFactory.getLogger(FileTransferService.class);
    public static final int CHUNK_SIZE = 64 * 1024; // 64 KB

    // transferId -> active file output stream
    private final Map<String, FileOutputStream> incomingTransfers = new ConcurrentHashMap<>();
    private final Map<String, Path> incomingPartPaths = new ConcurrentHashMap<>();
    private final Map<String, Path> incomingFinalPaths = new ConcurrentHashMap<>();

    public FileListResp listFiles(String requestedPath) {
        Path path;
        if (requestedPath == null || requestedPath.trim().isEmpty()) {
            path = Paths.get(System.getProperty("user.home"));
        } else {
            path = Paths.get(requestedPath);
        }

        List<FileItem> items = new ArrayList<>();
        File file = path.toFile();
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    items.add(new FileItem(
                            f.getName(),
                            f.isDirectory(),
                            f.isDirectory() ? 0 : f.length(),
                            f.lastModified()
                    ));
                }
            }
        }
        return new FileListResp(path.toAbsolutePath().toString(), items);
    }

    public void sendFile(File localFile, String remoteTargetPath, Consumer<FileChunk> chunkSender, Consumer<Double> progressCallback) {
        if (!localFile.exists() || !localFile.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a file: " + localFile);
        }

        String transferId = UUID.randomUUID().toString();
        long totalBytes = localFile.length();
        int totalChunks = (int) Math.ceil((double) totalBytes / CHUNK_SIZE);
        if (totalChunks == 0) totalChunks = 1;

        byte[] buffer = new byte[CHUNK_SIZE];
        try (FileInputStream fis = new FileInputStream(localFile)) {
            long bytesSent = 0;
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                int read = fis.read(buffer);
                byte[] chunkData;
                if (read > 0) {
                    chunkData = Arrays.copyOf(buffer, read);
                } else {
                    chunkData = new byte[0];
                }

                String sha256 = calculateSha256(chunkData);
                FileChunk chunk = new FileChunk(
                        transferId,
                        remoteTargetPath,
                        chunkIndex,
                        totalChunks,
                        chunkData,
                        sha256
                );
                chunkSender.accept(chunk);

                bytesSent += chunkData.length;
                if (progressCallback != null && totalBytes > 0) {
                    progressCallback.accept((double) bytesSent / totalBytes);
                }
            }
            log.info("Finished sending file: {} ({} bytes, {} chunks)", localFile.getName(), totalBytes, totalChunks);
        } catch (Exception e) {
            log.error("Error sending file", e);
            throw new RuntimeException("File transfer failed: " + e.getMessage(), e);
        }
    }

    public synchronized boolean receiveChunk(FileChunk chunk) {
        String transferId = chunk.getTransferId();
        try {
            FileOutputStream fos = incomingTransfers.get(transferId);
            if (fos == null) {
                Path targetDir = Paths.get(chunk.getTargetPath());
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                Path partPath = targetDir.resolve(UUID.randomUUID() + ".part");
                fos = new FileOutputStream(partPath.toFile());
                incomingTransfers.put(transferId, fos);
                incomingPartPaths.put(transferId, partPath);
            }

            if (chunk.getData() != null && chunk.getData().length > 0) {
                fos.write(chunk.getData());
            }

            // If final chunk
            if (chunk.getChunkIndex() == chunk.getTotalChunks() - 1) {
                fos.flush();
                fos.close();
                incomingTransfers.remove(transferId);
                Path partPath = incomingPartPaths.remove(transferId);

                log.info("File transfer complete for ID: {}. File saved at: {}", transferId, partPath);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Error receiving file chunk", e);
            incomingTransfers.remove(transferId);
            return false;
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}

