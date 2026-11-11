package com.cleanroom.remotedesktop.client.capture;

import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenMetadata;
import com.cleanroom.remotedesktop.protocol.model.ScreenModels.ScreenTile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;

public class RobotScreenCaptureService implements ScreenCaptureService {
    private static final Logger log = LoggerFactory.getLogger(RobotScreenCaptureService.class);
    public static final int TILE_SIZE = 128;

    private Robot robot;
    private GraphicsDevice[] screenDevices;
    private int selectedMonitor = 0;
    private Rectangle captureBounds;
    private float jpegQuality = 0.65f;

    private int gridCols;
    private int gridRows;
    private long[] previousTileHashes;
    private boolean forceFullRefresh = true;

    public RobotScreenCaptureService() {
        this(0);
    }

    public RobotScreenCaptureService(int monitorIndex) {
        initDevices(monitorIndex);
    }

    private void initDevices(int monitorIndex) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            screenDevices = ge.getScreenDevices();
            if (screenDevices.length == 0) {
                throw new IllegalStateException("No screen devices available");
            }
            setSelectedMonitor(monitorIndex);
        } catch (Exception e) {
            log.error("Failed to initialize RobotScreenCaptureService", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void setSelectedMonitor(int monitorIndex) {
        if (monitorIndex < 0 || monitorIndex >= screenDevices.length) {
            monitorIndex = 0;
        }
        this.selectedMonitor = monitorIndex;
        GraphicsDevice device = screenDevices[monitorIndex];
        try {
            this.robot = new Robot(device);
        } catch (AWTException e) {
            throw new RuntimeException("Could not create Robot for screen device " + monitorIndex, e);
        }

        DisplayMode dm = device.getDisplayMode();
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        this.captureBounds = new Rectangle(bounds.x, bounds.y, dm.getWidth(), dm.getHeight());

        this.gridCols = (int) Math.ceil((double) captureBounds.width / TILE_SIZE);
        this.gridRows = (int) Math.ceil((double) captureBounds.height / TILE_SIZE);
        this.previousTileHashes = new long[gridCols * gridRows];
        Arrays.fill(previousTileHashes, -1L);
        this.forceFullRefresh = true;

        log.info("Initialized capture on monitor {}: {}x{} (grid: {}x{})",
                monitorIndex, captureBounds.width, captureBounds.height, gridCols, gridRows);
    }

    @Override
    public int getMonitorCount() {
        return screenDevices != null ? screenDevices.length : 1;
    }

    @Override
    public ScreenMetadata getMetadata() {
        return new ScreenMetadata(
                captureBounds.width,
                captureBounds.height,
                selectedMonitor,
                getMonitorCount(),
                TILE_SIZE,
                TILE_SIZE
        );
    }

    @Override
    public void setQuality(float jpegQuality) {
        this.jpegQuality = Math.max(0.1f, Math.min(1.0f, jpegQuality));
    }

    @Override
    public synchronized List<ScreenTile> captureDirtyTiles() {
        List<ScreenTile> dirtyTiles = new ArrayList<>();
        if (robot == null || captureBounds == null) {
            return dirtyTiles;
        }

        BufferedImage fullScreen;
        try {
            fullScreen = robot.createScreenCapture(captureBounds);
        } catch (Exception e) {
            log.warn("Failed to create screen capture: {}", e.getMessage());
            return dirtyTiles;
        }

        CRC32 crc = new CRC32();
        int[] tilePixels = new int[TILE_SIZE * TILE_SIZE];

        for (int r = 0; r < gridRows; r++) {
            for (int c = 0; c < gridCols; c++) {
                int tileX = c * TILE_SIZE;
                int tileY = r * TILE_SIZE;
                int tileW = Math.min(TILE_SIZE, captureBounds.width - tileX);
                int tileH = Math.min(TILE_SIZE, captureBounds.height - tileY);

                fullScreen.getRGB(tileX, tileY, tileW, tileH, tilePixels, 0, tileW);

                crc.reset();
                for (int i = 0; i < tileW * tileH; i++) {
                    int p = tilePixels[i];
                    crc.update((p >> 24) & 0xFF);
                    crc.update((p >> 16) & 0xFF);
                    crc.update((p >> 8) & 0xFF);
                    crc.update(p & 0xFF);
                }
                long hash = crc.getValue();

                int tileIndex = r * gridCols + c;
                if (forceFullRefresh || previousTileHashes[tileIndex] != hash) {
                    previousTileHashes[tileIndex] = hash;

                    BufferedImage subImage = fullScreen.getSubimage(tileX, tileY, tileW, tileH);
                    byte[] compressed = compressJpeg(subImage, jpegQuality);

                    dirtyTiles.add(new ScreenTile(tileX, tileY, tileW, tileH, "JPEG", compressed));
                }
            }
        }

        forceFullRefresh = false;
        return dirtyTiles;
    }

    private byte[] compressJpeg(BufferedImage image, float quality) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Convert to RGB if image has alpha, because standard JPEG does not support alpha
            BufferedImage rgbImage = image;
            if (image.getType() != BufferedImage.TYPE_INT_RGB) {
                rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgbImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                throw new IllegalStateException("No JPEG ImageWriter found");
            }
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos)) {
                writer.setOutput(mcios);
                writer.write(null, new IIOImage(rgbImage, null, null), param);
            } finally {
                writer.dispose();
            }
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to compress tile to JPEG", e);
            return new byte[0];
        }
    }

    @Override
    public void close() {
        robot = null;
    }
}
