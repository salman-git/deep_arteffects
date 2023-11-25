package com.meancoder.meanarteffect;
import android.content.Context;
import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LutProcessor {
    private int[][][] LUT;
    private Context context;

    public LutProcessor(Context context) {
        this.context = context;
    }

//    public Bitmap applyLUT(Bitmap image, int[][][] lut) {
//        int width = image.getWidth();
//        int height = image.getHeight();
//        Bitmap output = Bitmap.createBitmap(width, height, image.getConfig());
//
//        // Iterate through each pixel in the image
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                // Get the RGB color values of the pixel
//                int pixelColor = image.getPixel(x, y);
//                int r = (pixelColor >> 16) & 0xFF;
//                int g = (pixelColor >> 8) & 0xFF;
//                int b = pixelColor & 0xFF;
//
//                // Map the RGB color to the LUT
//                int[] mappedColor = lut[r][g][b];
//
//                // Create the new pixel color from the mapped RGB values
//                int mappedPixelColor = (0xFF << 24) | (mappedColor[0] << 16) | (mappedColor[1] << 8) | mappedColor[2];
//
//                // Update the pixel in the output image
//                output.setPixel(x, y, mappedPixelColor);
//            }
//        }
//        return output;
//    }

    public void setLUT(String lutRestId) {
        try {
            this.LUT = loadLUT(this.context, lutRestId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int[][][] getActiveLUT() {
        return this.LUT;
    }

    private int[][][] loadLUT(Context context, String resName) throws IOException {
        int[][][] lut = new int[256][256][256];
        InputStream inputStream = context.getAssets().open("LUTs/" + resName);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            boolean isLUTData = false;

            // Skip the header lines until LUT data starts
            while (!isLUTData && (line = reader.readLine()) != null) {
                if (line.contains("LUT data points")) {
                    isLUTData = true;
                    break;
                }
            }

            if (isLUTData) {
                int r = 0;
                int g = 0;
                int b = 0;

                // Read the LUT data points and populate the lut array
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        break;
                    }

                    String[] colors = line.split(" ");
                    int rMapped = (int) (Float.parseFloat(colors[0]) * 255);
                    int gMapped = (int) (Float.parseFloat(colors[1]) * 255);
                    int bMapped = (int) (Float.parseFloat(colors[2]) * 255);

                    lut[rMapped][gMapped][bMapped] = 1;

                    b++;
                    if (b == 256) {
                        b = 0;
                        g++;
                    }
                    if (g == 256) {
                        g = 0;
                        r++;
                    }
                }
            }
            reader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lut;
    }

}
