package rabbitmqdemo.demo.pdf2img;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import javax.imageio.ImageIO;


public class ImgAddWatermark {

        /**
         * 至少可为png、jpg、bmp格式的图片添加水印
         * 至少可设置生成png、bmp、jpg格式的带有水印的目标图片
         * @param srcImgPath 源图片路径
         * @param tarImgPath 保存的图片路径
         * @param waterMarkContent 水印内容
         * @param markContentColor 水印颜色
         * @param font 水印字体
         */
        public void addWaterMark(String srcImgPath, String tarImgPath, String waterMarkContent,Color markContentColor,Font font) {

            try {
                // 读取原图片信息
                File srcImgFile = new File(srcImgPath);// 得到文件
                Image srcImg = ImageIO.read(srcImgFile);// 文件转化为图片
                int srcImgWidth = srcImg.getWidth(null);// 获取图片的宽
                int srcImgHeight = srcImg.getHeight(null);// 获取图片的高
                // 加水印
                BufferedImage bufImg = new BufferedImage(srcImgWidth, srcImgHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bufImg.createGraphics();
                g.drawImage(srcImg, 0, 0, srcImgWidth, srcImgHeight, null);
                g.setColor(markContentColor); // 背景设置水印颜色
                g.setFont(font);
                // 设置旋转度，此处为-45度
                g.rotate(Math.toRadians(-45),(double)bufImg.getWidth()/2,(double)bufImg.getHeight()/2);

                // 设置水印的坐标
                int x = 0;
                int y = srcImgHeight/2;
                g.drawString(waterMarkContent, x, y);  //画出水印
                g.dispose();

                // 输出图片
                FileOutputStream outImgStream = new FileOutputStream(tarImgPath);
                ImageIO.write(bufImg, "jpg", outImgStream);
                System.out.println("添加水印完成");
                outImgStream.flush();
                outImgStream.close();

            } catch (Exception e) {
                // TODO
            }
        }
        public static void main(String[] args) {
            Font font = new Font("微软雅黑", Font.ITALIC, 40);   // 水印字体
            String srcImgPath="D:\\testRescource\\实践_2.jpg"; // 源图片地址
            String tarImgPath="D:\\testRescource\\tt6u.jpg"; // 待存储的地址
            String waterMarkContent="内部资料 禁止外传 内部资料 禁止外传 内部资料 禁止外传  内部资料 禁止外传 内部资料 禁止外传";  // 水印内容
            Color color=new Color(255,255,25,128);        // 印图片色彩以及透明度
            new ImgAddWatermark().addWaterMark(srcImgPath, tarImgPath, waterMarkContent, color, font);

        }


}
