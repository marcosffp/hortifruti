package com.hortifruti.sl.hortifruti.service.purchase;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

/**
 * Junta as fotos de comprovante das compras de um agrupamento em um único PDF (uma foto por página,
 * escalada para caber na página em A4 mantendo a proporção original) — mesma biblioteca (PDFBox)
 * usada pelos demais geradores de PDF do sistema, ver {@code AbstractPdfPageWriter}.
 */
@Component
public class PurchasePhotosPdfGenerator {

  private static final float MARGIN = 20f;

  // Fotos de câmera de celular chegam em resoluções bem acima do que a página A4 consegue exibir
  // (ex: 3024x4032) e isso inflava o PDF pra vários MB por página. 1600px no lado maior ainda
  // imprime nítido em A4 e a qualidade 0.6 do JPEG segura o tamanho sem ficar visivelmente pior.
  private static final int MAX_DIMENSION = 1600;
  private static final float JPEG_QUALITY = 0.6f;

  public byte[] generate(List<byte[]> images) throws IOException {
    try (PDDocument document = new PDDocument();
        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream()) {
      for (byte[] imageBytes : images) {
        addImagePage(document, optimize(imageBytes));
      }
      document.save(pdfOut);
      return pdfOut.toByteArray();
    }
  }

  /**
   * Corrige a orientação (fotos de câmera guardam a rotação real num campo EXIF em vez de já vir
   * com os pixels rotacionados — o PDFBox desenha os pixels crus como estão armazenados) e
   * redimensiona/recomprime a foto para reduzir o tamanho final do PDF. Cobre só as rotações
   * puras (3/6/8, de longe as mais comuns em fotos de câmera); os casos de espelhamento (2/4/5/7)
   * são deixados como estão. Se a imagem não puder ser decodificada, devolve os bytes originais.
   */
  private byte[] optimize(byte[] imageBytes) {
    int orientation = readExifOrientation(imageBytes);
    try {
      BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
      if (original == null) {
        return imageBytes;
      }

      BufferedImage oriented =
          (orientation == 3 || orientation == 6 || orientation == 8)
              ? rotate(original, orientation)
              : original;
      BufferedImage resized = resizeIfNeeded(oriented, MAX_DIMENSION);
      return encodeJpeg(resized, JPEG_QUALITY);
    } catch (IOException e) {
      return imageBytes;
    }
  }

  private BufferedImage resizeIfNeeded(BufferedImage image, int maxDimension) {
    int width = image.getWidth();
    int height = image.getHeight();
    if (width <= maxDimension && height <= maxDimension) {
      return image;
    }

    double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
    int newWidth = (int) Math.round(width * scale);
    int newHeight = (int) Math.round(height * scale);

    BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = resized.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.drawImage(image, 0, 0, newWidth, newHeight, null);
    g.dispose();
    return resized;
  }

  private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
    ImageWriter writer = writers.next();
    try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
      writer.setOutput(ios);
      ImageWriteParam param = writer.getDefaultWriteParam();
      param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      param.setCompressionQuality(quality);
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
    return out.toByteArray();
  }

  private int readExifOrientation(byte[] imageBytes) {
    try {
      Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
      ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
        return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
      }
    } catch (Exception e) {
      // Sem EXIF (ex: PNG) ou arquivo corrompido — assume orientação normal (1).
    }
    return 1;
  }

  private BufferedImage rotate(BufferedImage image, int orientation) {
    int width = image.getWidth();
    int height = image.getHeight();
    boolean swapDimensions = orientation == 6 || orientation == 8;
    int newWidth = swapDimensions ? height : width;
    int newHeight = swapDimensions ? width : height;

    AffineTransform transform = new AffineTransform();
    switch (orientation) {
      case 3 -> {
        transform.translate(width, height);
        transform.rotate(Math.PI);
      }
      case 6 -> {
        transform.translate(height, 0);
        transform.rotate(Math.PI / 2);
      }
      case 8 -> {
        transform.translate(0, width);
        transform.rotate(3 * Math.PI / 2);
      }
      default -> {}
    }

    BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = rotated.createGraphics();
    g.drawImage(image, transform, null);
    g.dispose();
    return rotated;
  }

  private void addImagePage(PDDocument document, byte[] imageBytes) throws IOException {
    PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "foto");

    PDPage page = new PDPage(PDRectangle.A4);
    document.addPage(page);

    float availableWidth = page.getMediaBox().getWidth() - 2 * MARGIN;
    float availableHeight = page.getMediaBox().getHeight() - 2 * MARGIN;
    float scale = Math.min(availableWidth / image.getWidth(), availableHeight / image.getHeight());

    float drawWidth = image.getWidth() * scale;
    float drawHeight = image.getHeight() * scale;
    float x = (page.getMediaBox().getWidth() - drawWidth) / 2;
    float y = (page.getMediaBox().getHeight() - drawHeight) / 2;

    try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
      cs.drawImage(image, x, y, drawWidth, drawHeight);
    }
  }
}
