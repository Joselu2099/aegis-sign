import com.aegis.sign.domain.service.BiometricValidationService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BenchmarkTester {
    public static void main(String[] args) throws Exception {
        BiometricValidationService service = new BiometricValidationService();
        byte[] imageBytes = createMockImage(4000, 4000, Color.GRAY);

        // Warmup
        for (int i = 0; i < 50; i++) {
            service.validate(imageBytes);
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 200; i++) {
            service.validate(imageBytes);
        }
        long end = System.currentTimeMillis();

        System.out.println("Time taken for 200 validations of 4000x4000 image: " + (end - start) + "ms");
    }

    private static byte[] createMockImage(int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(10, 10, 100, 100);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(200, 200, 100, 100);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        if (bytes.length < 5120) {
            byte[] padded = new byte[6000];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            return padded;
        }
        return bytes;
    }
}
