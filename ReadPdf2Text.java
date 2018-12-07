package rabbitmqdemo.demo.pdf2img;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;

public class ReadPdf2Text {
        public static void readPDF(String pdfSourcePath, String targetTextPath) {
            PDDocument helloDocument = null;
            try {
                helloDocument = PDDocument.load(new File(pdfSourcePath));
                PDFTextStripper textStripper = new PDFTextStripper();
                String text = textStripper.getText(helloDocument);

                File file = new File(targetTextPath);
                FileOutputStream fileOutputStream = new FileOutputStream(file);

                byte[] bytes;
                bytes = text.getBytes();
                int length = bytes.length;
                fileOutputStream.write(bytes, 0 , length);
                fileOutputStream.flush();
                fileOutputStream.close();

                helloDocument.close();
            } catch (IOException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
            }
        }
    public static void readTxtFile(String filePath){
        try {
            String encoding="utf-8";
            File file=new File(filePath);
            if(file.isFile() && file.exists()){ //判断文件是否存
                InputStreamReader read = new InputStreamReader(new FileInputStream(file),encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                while((lineTxt = bufferedReader.readLine()) != null){
                        System.out.println(lineTxt);
                }
                read.close();
        }else{
            System.out.println("找不到指定的文件");
        }
        } catch (Exception e) {
            System.out.println("读取文件内容出错");
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {

        String pdfSourcePath = "D:\\testRescource\\LicensingEndUserGuide.pdf";
        String targetTextPath ="D:\\testRescource\\2018002.txt";
        readPDF(pdfSourcePath,targetTextPath );

        readTxtFile("D:\\testRescource\\2018002.txt");
    }
}
