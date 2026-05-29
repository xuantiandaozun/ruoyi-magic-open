package com.ruoyi.project.miniapp.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 账单图片预处理：视觉模型按像素计费/计时，18KB 小文件也可能是高分辨率图。
 */
public final class BillImageOptimizer {

    /** 视觉识别最长边上限，兼顾小票可读性与 token 成本 */
    private static final int MAX_EDGE = 1280;
    /** 已足够小且为 JPEG 时跳过重编码 */
    private static final int SKIP_REENCODE_BYTES = 200 * 1024;
    private static final float JPEG_QUALITY = 0.82f;

    private BillImageOptimizer() {
    }

    @Getter
    public static class OptimizedImage {
        private final byte[] bytes;
        private final String mimeType;
        private final int width;
        private final int height;
        private final boolean resized;

        public OptimizedImage(byte[] bytes, String mimeType, int width, int height, boolean resized) {
            this.bytes = bytes;
            this.mimeType = mimeType;
            this.width = width;
            this.height = height;
            this.resized = resized;
        }
    }

    public static OptimizedImage optimize(byte[] input, String mimeType) throws IOException {
        if (input == null || input.length == 0) {
            throw new IOException("图片内容为空");
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
        if (image == null) {
            return new OptimizedImage(input, mimeType, 0, 0, false);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage working = image;
        boolean resized = false;

        int longestEdge = Math.max(width, height);
        if (longestEdge > MAX_EDGE) {
            double scale = (double) MAX_EDGE / longestEdge;
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));
            working = scaleImage(image, targetWidth, targetHeight);
            width = targetWidth;
            height = targetHeight;
            resized = true;
        }

        if (!resized && isJpeg(mimeType) && input.length <= SKIP_REENCODE_BYTES) {
            return new OptimizedImage(input, mimeType, width, height, false);
        }

        byte[] jpegBytes = encodeJpeg(working);
        return new OptimizedImage(jpegBytes, "image/jpeg", width, height, resized || !isJpeg(mimeType));
    }

    private static boolean isJpeg(String mimeType) {
        return StrUtil.equalsIgnoreCase(mimeType, "image/jpeg")
                || StrUtil.equalsIgnoreCase(mimeType, "image/jpg");
    }

    private static BufferedImage scaleImage(BufferedImage source, int targetWidth, int targetHeight) {
        Image scaled = source.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return output;
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        BufferedImage rgbImage = image;
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgbImage.createGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("未找到 JPEG 编码器");
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new IIOImage(rgbImage, null, null), param);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }
}
