package ch.obermuhlner.kimage;

import ch.obermuhlner.kimage.image.Channel;
import ch.obermuhlner.kimage.image.MatrixImage;
import ch.obermuhlner.kimage.matrix.Matrix;

public class TestJavaMain {

  public static void main(String[] args) {
    MatrixImage image = new MatrixImage(5, 10);

    double redPixel = image.get(0, 0, Channel.Red);
    image.setPixel(0, 0, Channel.Red, 0.5);

    Matrix matrix = image.getMatrix(Channel.Red);
    System.out.println(matrix);
  }
}
