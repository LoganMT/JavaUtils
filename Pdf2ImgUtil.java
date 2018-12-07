package rabbitmqdemo.demo.pdf2img;

import com.itextpdf.text.pdf.PdfReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Pdf2ImgUtil {

        public static void main(String[] args) {
            pdf2Image("D:\\testRescource\\物理化学简明教程.pdf", "D:\\testRescource\\result", 150);
        }

        /***
         * PDF文件转PNG图片或者jpg图片或者tif图片或者bmp图片（更改相应后缀名即可！）
         * 可以全部页数转换
         * 缺点：耗时严重，pdf文件越大，所需转换时间越多
         *
         * @param PdfFilePath pdf完整路径
         * @param dstImgFolder 图片存放的文件夹
         * @param dpi dpi越大转换后越清晰，相对转换速度越慢
         * @return
         */
        public static void pdf2Image(String PdfFilePath, String dstImgFolder, int dpi) {
            File file = new File(PdfFilePath);
            PDDocument pdDocument;
            try {
                String imgPDFPath = file.getParent();
                int dot = file.getName().lastIndexOf('.');
                String imagePDFName = file.getName().substring(0, dot); // 获取图片文件名
                String imgFolderPath = null;
                if (dstImgFolder.equals("")) {
                    imgFolderPath = imgPDFPath + File.separator + imagePDFName;// 获取图片存放的文件夹路径
                } else {
                    imgFolderPath = dstImgFolder + File.separator + imagePDFName;
                }

                if (createDirectory(imgFolderPath)) {
                    long start = System.currentTimeMillis();
                    pdDocument = PDDocument.load(file);
                    PDFRenderer renderer = new PDFRenderer(pdDocument);
                    /* dpi越大转换后越清晰，相对转换速度越慢 */
                    PdfReader reader = new PdfReader(PdfFilePath);
                    int pages = reader.getNumberOfPages();
                    StringBuffer imgFilePath = null;
                    for (int i = 0; i < pages; i++) {
                        String imgFilePathPrefix = imgFolderPath + File.separator + imagePDFName;
                        imgFilePath = new StringBuffer();
                        imgFilePath.append(imgFilePathPrefix);
                        imgFilePath.append("_");
                        imgFilePath.append(String.valueOf(i + 1));
                        imgFilePath.append(".png");
                        File dstFile = new File(imgFilePath.toString());
                        BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                        ImageIO.write(image, "png", dstFile);
                    }
                    long end = System.currentTimeMillis();
                    System.out.println("PDF文档转PNG图片成功！"+"总耗时(秒)："+ (start - end)/1000);

                } else {
                    System.out.println("PDF文档转PNG图片失败：" + "创建" + imgFolderPath + "失败");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static boolean createDirectory(String folder) {
            File dir = new File(folder);
            if (dir.exists()) {
                return true;
            } else {
                return dir.mkdirs();
            }
        }

}
