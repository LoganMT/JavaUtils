package rabbitmqdemo.demo.pdf2img;

import com.google.gson.Gson;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ReadExcel2Text {

    public static void main(String[] args) throws IOException {
        File file = new File("D:\\testRescource\\test.xlsx");
        FileInputStream fileInputStream = new FileInputStream(file);
        MockMultipartFile mockMultipartFile = new MockMultipartFile("test",
                "test.xlsx", "testContent",fileInputStream);

        List<String[]> strings = readExcel(mockMultipartFile);
        String s = new Gson().toJson(strings);
        System.out.println(s);
    }

    static Logger logger = LoggerFactory.getLogger(ReadExcel2Text.class);
    private final static String xls = "xls";
    private final static String xlsx = "xlsx";

    /**
     * 读入excel文件，解析后返回
     * @param file
     * @throws IOException
     */
    public static List<String[]> readExcel(MultipartFile file) throws IOException{
        // 检查文件
        checkFile(file);
        // 获得Workbook工作薄对象
        Workbook workbook = getWorkBook(file);
        // 创建返回对象，把每行中的值作为一个数组，所有行作为一个集合返回
        List<String[]> list = new ArrayList<String[]>();
        if(workbook != null){
            for(int sheetNum = 0;sheetNum < workbook.getNumberOfSheets();sheetNum++){
                // 获得当前sheet工作表
                Sheet sheet = workbook.getSheetAt(sheetNum);
                if(sheet == null){
                    continue;
                }
                // 获得当前sheet的开始行
                int firstRowNum  = sheet.getFirstRowNum();
                // 获得当前sheet的结束行
                int lastRowNum = sheet.getLastRowNum();
                // 循环除了第一行的所有行
                for(int rowNum = firstRowNum; rowNum <= lastRowNum; rowNum++){
                    // 获得当前行
                    Row row = sheet.getRow(rowNum);
                    if(row == null){
                        continue;
                    }
                    // 获得当前行的列数
                    int lastCellNum = row.getLastCellNum();
                    String[] cells = new String[row.getLastCellNum()];
                    // 循环当前行
                    for(int cellNum = 0; cellNum < lastCellNum; cellNum++){
                        Cell cell = row.getCell(cellNum);
                        cells[cellNum] = getCellValue(cell);
                    }
                    list.add(cells);
                }
            }
            workbook.close();
        }
        return list;
    }
    public static void checkFile(MultipartFile file) throws IOException{
        //判断文件是否存在
        if(null == file){
            logger.error("文件不存在！");
            throw new FileNotFoundException("文件不存在！");
        }
        //获得文件名
        String fileName = file.getOriginalFilename();
        //判断文件是否是excel文件
        if(!fileName.endsWith(xls) && !fileName.endsWith(xlsx)){
            logger.error(fileName + "不是excel文件");
            throw new IOException(fileName + "不是excel文件");
        }
    }
    public static Workbook getWorkBook(MultipartFile file) {
        //获得文件名
        String fileName = file.getOriginalFilename();
        //创建Workbook工作薄对象，表示整个excel
        Workbook workbook = null;
        try {
            //获取excel文件的io流
            InputStream is = file.getInputStream();
            //根据文件后缀名不同(xls和xlsx)获得不同的Workbook实现类对象
            if(fileName.endsWith(xls)){
                //2003
                workbook = new HSSFWorkbook(is);
            }else if(fileName.endsWith(xlsx)){
                //2007
                workbook = new XSSFWorkbook(is);
            }
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        return workbook;
    }
    public static String getCellValue(Cell cell){
        String cellValue;
        DataFormatter dateFormatter = new DataFormatter(Locale.US);
        String originalString = dateFormatter.formatCellValue(cell);

        if(originalString.contains("%")){
            cellValue = originalString;
        }else {
            if (cell == null || "".equals(cell.toString())) {
                cellValue = "";
            } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
                    Date d = cell.getDateCellValue();
                    cellValue = sdf.format(d);
                    String yyyy = cellValue.substring(0, 4);
                    String mm = cellValue.substring(5, 7);
                    String dd = cellValue.substring(8, 10);
                    String hh = cellValue.substring(11, 13);
                    String min = cellValue.substring(14, 16);
                    String ss = cellValue.substring(17, 19);
                    if (yyyy.equals("1899") && mm.equals("12") && dd.equals("31")) {
                        SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm:ss");
                        cellValue = sdf1.format(d);
                    }
                    if (hh.equals("00") && min.equals("00") && ss.equals("00")) {
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd");
                        cellValue = sdf2.format(d);

                    }
                } else {
                    DecimalFormat df = new DecimalFormat("#.###########");
                    cellValue = df.format(cell.getNumericCellValue());
                }
            } else {
                cellValue = cell.toString().trim();
            }
        }

        return cellValue;
    }

}


