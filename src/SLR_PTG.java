import com.jakewharton.fliptables.FlipTable;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

/**
 * Created by Mahdi2016 on 1/19/2019.
 */
public class SLR_PTG {
    private String[][] PT;
    public String[][] LR;
    private ArrayList<String> tokens;
    private Workbook wb;
    private Sheet sheet;
    private Row row;
    private Cell cell;
    private int rows;
    private int cols;
    private int NumOfGram = 23; //number of grammars

    SLR_PTG(String SLR_table){
        try {
            wb = WorkbookFactory.create(new File(SLR_table));
            sheet = wb.getSheetAt(0);

            rows = sheet.getPhysicalNumberOfRows();
            cols = sheet.getRow(0).getPhysicalNumberOfCells(); // Num of columns
        } catch(Exception ioe) {
            ioe.printStackTrace();
        }

        PT = new String[rows - 1][cols - 1];
        tokens = new ArrayList<String>();
        LR = new String[NumOfGram][3]; // 3 is fixed!
    }

    public void Generate_PT(){

        //get tokens of first row to search in PT
        for(int c = 1; c < cols; c++) {
            row = sheet.getRow(0);
            tokens.add(row.getCell((short)c).getStringCellValue());
        }


        for(int r = 1; r < rows; r++) {
            row = sheet.getRow(r);
            if(row != null) {
                for(int c = 1; c < cols; c++) {
                    cell = row.getCell((short)c);
                    PT[r - 1][c - 1] = cell.getStringCellValue();
                }
            }
        }


        //print table
        String[] temp = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            temp[i] = tokens.get(i);
        }
        System.out.println("SLR Parse Table:");
        System.out.println(FlipTable.of(temp,PT));

    }

    public void Generate_LR(){

        //semantic rules must be completed
        LR[0][0] = "BE";
        LR[0][1] = "3";
        LR[0][2] = "@SLR_or";

        LR[1][0] = "BE";
        LR[1][1] = "1";
        LR[1][2] = "@";

        LR[2][0] = "BT";
        LR[2][1] = "3";
        LR[2][2] = "@SLR_and";

        LR[3][0] = "BT";
        LR[3][1] = "1";
        LR[3][2] = "@";

        LR[4][0] = "BF";
        LR[4][1] = "1";
        LR[4][2] = "@";

        LR[5][0] = "BF";
        LR[5][1] = "2";
        LR[5][2] = "@SLR_not";

        LR[6][0] = "BF";
        LR[6][1] = "3";
        LR[6][2] = "@";

        LR[7][0] = "BF";
        LR[7][1] = "3";
        LR[7][2] = "@SLR_l";

        LR[8][0] = "BF";
        LR[8][1] = "3";
        LR[8][2] = "@SLR_g";

        LR[9][0] = "BF";
        LR[9][1] = "3";
        LR[9][2] = "@SLR_e";

        LR[10][0] = "BF";
        LR[10][1] = "3";
        LR[10][2] = "@SLR_ne";

        LR[11][0] = "BF";
        LR[11][1] = "3";
        LR[11][2] = "@SLR_le";

        LR[12][0] = "BF";
        LR[12][1] = "3";
        LR[12][2] = "@SLR_ge";

        LR[13][0] = "E";
        LR[13][1] = "3";
        LR[13][2] = "@SLR_add";

        LR[14][0] = "E";
        LR[14][1] = "3";
        LR[14][2] = "@SLR_minus";

        LR[15][0] = "E";
        LR[15][1] = "1";
        LR[15][2] = "@";

        LR[16][0] = "T";
        LR[16][1] = "3";
        LR[16][2] = "@SLR_mult";

        LR[17][0] = "T";
        LR[17][1] = "3";
        LR[17][2] = "@SLR_divide";

        LR[18][0] = "T";
        LR[18][1] = "1";
        LR[18][2] = "@";

        LR[19][0] = "F";
        LR[19][1] = "1";
        LR[19][2] = "@SLR_push_id";

        LR[20][0] = "F";
        LR[20][1] = "3";
        LR[20][2] = "@";

        LR[21][0] = "F";
        LR[21][1] = "1";
        LR[21][2] = "@SLR_push_num";

        LR[22][0] = "F";
        LR[22][1] = "4";
        LR[22][2] = "@SLR_push_neg_num";
    }

    public String SLR_PT(int state, String token){
        int col = tokens.indexOf(token);
        return PT[state][col];
    }





}
