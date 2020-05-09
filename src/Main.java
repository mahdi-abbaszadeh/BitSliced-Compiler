import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Created by Mahdi2016 on 10/18/2018.
 */
public class Main {
    static String Grammar = "", SourceCode = "", SLR_Table = "";

    public static void main(String[] args) {

        //Graphic
        File file1 = new File("BS_Grammar.txt");
        File file2 = new File("BS_sourceCode.txt");
        File file3 = new File("SLR_Table.xlsx");
        Grammar = file1.getName();
        SourceCode = file2.getName();
        SLR_Table = file3.getName();
        //openFiles();


        try {
            Parser parser = new Parser(Grammar,SourceCode,SLR_Table);
            parser.Parse();
            parser.generateIS();
            printAssembly(parser);


        }catch (Exception i){
            i.printStackTrace();
            System.out.println(i);
        }
    }

    public static void openFiles(){
        for (int i = 0; i < 3; i++) {
            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView()
                    .getParentDirectory(new File("E:\\Semester 7\\Compiler\\Project\\part3\\src")));

            if (i == 0)
                jfc.setDialogTitle("Open Grammar");
            if (i == 1)
                jfc.setDialogTitle("Open SourceCode");
            if (i == 2)
                jfc.setDialogTitle("Open SLR_Table");

            int returnValue = jfc.showOpenDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION){
                File file = jfc.getSelectedFile();
                if (i == 0)
                    Grammar = file.getName();
                if (i == 1)
                    SourceCode = file.getName();
                if (i == 2)
                    SLR_Table = file.getName();
            }
        }
    }

    public static void printAssembly(Parser parser){
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String text= "";
        for (int i = 0; i < parser.getCode().size(); i++) {
            text += i+"\t"+parser.getCode().get(i).getOperator()+"\t";
            if (!parser.getCode().get(i).getOperand1 ().equals(""))
                text += "\t"+parser.getCode().get(i).getOperand1()+"\t";
            if (!parser.getCode().get(i).getOperand2().equals(""))
                text += "\t"+parser.getCode().get(i).getOperand2()+"\t";
            text += "\t"+parser.getCode().get(i).getResult();
            text += "\n";
        }




        JTextArea jTextArea = new JTextArea(text,40,60);
        JScrollPane jScrollPane = new JScrollPane(jTextArea);
        jTextArea.setLineWrap(true);
        jTextArea.setEditable(false);
        frame.add(jScrollPane);
        frame.pack();
        frame.setTitle("Assembly Code");
        frame.setVisible(true);
    }
}
