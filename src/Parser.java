import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by Mahdi2016 on 12/14/2018.
 */
public class Parser {

    private PTG ptg;
    private SLR_PTG SLR_ptg;
    private Scanner scanner;
    private Token token;
    private Stack<GToken> ParseStack;
    private Stack<Integer> SLRStack;
    private ArrayList<Descriptor> ST;
    private int globalAddress;
    private int tempAddress = 4000;
    private Stack SemanticStack;
    private ArrayList<Instruction> code;
    private int pc = 0;
    private ArrayList<Descriptor> temps;
    private String SLR_id,SLR_num;

    Parser(String GrammarFile, String SourceCode, String SLR_Table)throws IOException{
        ptg = new PTG();
        ptg.get_grammar(GrammarFile);
        scanner = new Scanner(SourceCode);
        ParseStack = new Stack<GToken>();

        System.out.println("Firsts:");
        System.out.println();
        ptg.Firsts();
        System.out.println();
        System.out.println();

        System.out.println("Follows:");
        System.out.println();
        ptg.Follows();
        System.out.println();
        System.out.println();

        System.out.println("Predicts:");
        System.out.println();
        ptg.predicts();
        System.out.println();
        System.out.println();

        System.out.println("Parse Table:");
        System.out.println();
        ptg.GeneratePT();
        System.out.println();
        System.out.println();

        ParseStack.push(ptg.get_GToken_from_String("$"));
        ParseStack.push(ptg.get_GToken_from_String(ptg.grammars[0].get(0)));


        SLR_ptg = new SLR_PTG(SLR_Table);
        SLR_ptg.Generate_PT();
        SLR_ptg.Generate_LR();
        SLRStack = new Stack<Integer>();


        SLRStack.push(0);

        ST = new ArrayList<Descriptor>();
        temps = new ArrayList<Descriptor>();

        SemanticStack = new Stack();

        code = new ArrayList<Instruction>();
    }

    public ArrayList<Instruction> getCode() {return code;}

    public void Parse() throws IOException{
        token = scanner.nextToken();
        while (true){

            GToken TOP_ps = ParseStack.peek();
            int prod;

            if (token.getValue().equals("$") && !TOP_ps.getValue().equals("$")){
                System.err.println(scanner.line+":\tYou missed to finish a Statement truly");
                break;
            }

            //if we need to switch to SLR Parser
            if(TOP_ps.getValue().equals("#switch")){
                ParseStack.pop();
                SLR();
            }

            //if top parse stack is a semantic rule
            else if(TOP_ps.getValue().charAt(0) == '@'){
                generateCode(TOP_ps.getValue());
                ParseStack.pop();
            }


            //if top of parse stack is a variable
            else if (TOP_ps.getGtokenType() == GToken.GTokenType.variable){

                if (TOP_ps.getValue().equals("BE")){
                    prod = ptg.RHST.length - 1; //last line of grammar
                }
                else {
                    if (token.getTokenType() == Token.TokenType.num ||
                            token.getTokenType() == Token.TokenType.id ||
                            token.getTokenType() == Token.TokenType.character ||
                            token.getTokenType() == Token.TokenType.string)
                        prod = ptg.PT(TOP_ps.getValue(), token.getTokenType().name());
                    else
                        prod = ptg.PT(TOP_ps.getValue(), token.getValue());
                }

                //pop last variable anf push its RHST values
                ParseStack.pop();
                for (int i = 0; i < ptg.RHST[prod].size(); i++) {
                    //if the value of RHST is lambda we should not add it ti the stack
                    if (!ptg.RHST[prod].get(i).getValue().equals("lambda"))
                        ParseStack.push(ptg.RHST[prod].get(i));
                }
            }

            //if top is a terminal
            else if(TOP_ps.getGtokenType() == GToken.GTokenType.terminal){

                //if top is $ so we have to finish
                if (TOP_ps.getValue().equals("$")){
                    if (token.getValue().equals("$")) {
                        System.out.println("Accepted!");
                        break;
                    }
                    else{
                        System.err.println(scanner.line+":\tError! End of code is not the same as End of Stack");
                        break;
                    }
                }

                //if top is not $ so we have to update the token
                else{
                    if (TOP_ps.getValue().equals(token.getValue())
                            || TOP_ps.getValue().equals(token.getTokenType().name())){
                        ParseStack.pop();
                        token = scanner.nextToken();
                    }
                    else {
                        System.err.println(scanner.line+":\tError! top of parse stack is not the same as token");
                        break;
                    }
                }
            }
        }

        System.out.println("Instruction Sets:");
    }

    public void SLR()throws IOException{
        //String BE;
        String action;
        int num = 0;

        while (true) {
            if (token.getTokenType() == Token.TokenType.num ||
                    token.getTokenType() == Token.TokenType.id ||
                    token.getTokenType() == Token.TokenType.character ||
                    token.getTokenType() == Token.TokenType.string)
                action = SLR_ptg.SLR_PT(SLRStack.peek(), token.getTokenType().name());
            else
                action = SLR_ptg.SLR_PT(SLRStack.peek(), token.getValue());

            if (action.equals("")) {//error or the end of BE
                System.err.println(scanner.line+":\terror!\tnot a valid code");
                break;
            }


            else if (action.charAt(0) == 'S') {//Shift

                if (action.length() == 4)//addade 2 raghami
                    num = (action.charAt(3) - 48) + (action.charAt(2) - 48) * 10;
                else if (action.length() == 3)//adade 1 raghami
                    num = action.charAt(2) - 48;
                SLRStack.push(num);

                if (num == 8) {// push id
                   SLR_id = token.getValue();
                }

                if(num == 40 || num == 10){ // push num
                    SLR_num = token.getValue();
                }




                token = scanner.nextToken();
            }



            else if (action.charAt(0) == 'R') {//Reduce

                int gt = 0;

                if (action.length() == 3)//addade 2 raghami
                    num = (action.charAt(2) - 48) + (action.charAt(1) - 48) * 10;
                else if (action.length() == 2)//adade 1 raghami
                    num = action.charAt(1) - 48;

                num--; // reduce number is different from array ( 0 based and 1 based)

                String LHS = SLR_ptg.LR[num][0];//left side of grammar which is reduced
                int RHSL = Integer.parseInt(SLR_ptg.LR[num][1]);//number of pop

                //pop
                for (int i = 0; i < RHSL; i++) {
                    SLRStack.pop();
                }

                action = SLR_ptg.SLR_PT(SLRStack.peek(), LHS);//action is GOTO
                if (action.length() == 3)//addade 2 raghami
                    gt = (action.charAt(2) - 48) + (action.charAt(1) - 48) * 10;
                else if (action.length() == 2)//adade 1 raghami
                    gt = action.charAt(1) - 48;
                SLRStack.push(gt);

                //generate code
                if (!SLR_ptg.LR[num][2].equals(""))
                    generateCode(SLR_ptg.LR[num][2]);
            }


            else if (action.charAt(0) == 'A') {//Accepted!
                SLRStack.pop();
                token = scanner.nextToken(); // if we want to use $ at the end of BE
                return;
            }
        }
    }

    static String type = "";
    public void generateCode(String semantic_rule){
        Descriptor d1,d2,d3;
        //Descriptor te = new Descriptor();
        String id;
        String number;
        int pre_pc;
        switch (semantic_rule){

            case "@push_pc":
                SemanticStack.push(pc);
                break;

            case "@push_type":
                /*SemanticStack.push(token.getValue());*/
                type = token.getValue();
                break;

            case "@push_id":
                d1 = findSymbol(token.getValue());
                SemanticStack.push(d1);
                break;

            case "@push_dec_id":
                SemanticStack.push(token.getValue());
                break;

            case "@push_num":
                d1 = findSymbol(token.getValue());
                if (d1 == null){
                    Descriptor temp = new Descriptor();
                    temp.setName(token.getValue());
                    temp.setDescType("number");
                    temp.setVarType("int");
                    ST.add(temp);
                    SemanticStack.push(temp);
                }else {
                    SemanticStack.push(d1);
                }
                break;

            case "@push_neg_num":
                d1 = findSymbol("-"+token.getValue());
                if (d1 == null){
                    Descriptor temp = new Descriptor();
                    temp.setName("-"+token.getValue());
                    temp.setDescType("number");
                    temp.setVarType("int");
                    ST.add(temp);
                    SemanticStack.push(temp);
                }else {
                    SemanticStack.push(d1);
                }
                break;

            case "@push_inc_dec":
                SemanticStack.push(token.getValue());
                break;

            case "@create_var_desc":
                id = (String) SemanticStack.pop();
                //type = (String) SemanticStack.pop();
                d1 = findSymbol(id);
                if(d1 != null){
                    System.err.println(scanner.line+":\tvariable"+d1.getName()+"is already declared!");
                } else{
                    Descriptor temp = new Descriptor();
                    temp.setName(id);
                    temp.setDescType("variable");
                    temp.setVarType(type);
                    //TODO
                    // get size of variable for initilizing strings
                    for (int i = 0; i < 16; i++) {
                        temp.Registers.add(id+"_"+i);
                        System.out.println(temp.getVarType()+ " "+ temp.Registers.get(i)+ ";");
                    }
                    /*temp.setAddress(globalAddress);
                    globalAddress += getTypeSize(type);*/
                    ST.add(temp);
                }
                break;

            case "@create_arr_desc":
                d1 = (Descriptor) SemanticStack.pop();
                id = (String) SemanticStack.pop();
                d2 = findSymbol(id);
                //type = (String) SemanticStack.pop();
                if(d2 != null){
                    System.err.println(scanner.line+":\tarray name: "+d2.getName()+" is already declared!");
                } else{
                    Descriptor temp = new Descriptor();
                    temp.setName(id);
                    temp.setDescType("array");
                    temp.setVarType(type);
                    temp.setAddress(globalAddress);
                    globalAddress += getTypeSize(type)*Integer.parseInt(d1.getName());
                    ST.add(temp);
                }
                break;

            case "@create_pointer_desc":
                id = (String) SemanticStack.pop();
                //type = (String) SemanticStack.pop();
                d1 = findSymbol(id);
                if(d1 != null){
                    System.err.println(scanner.line+":\tvariable pointer"+d1.getName()+"is already declared!");
                } else{
                    Descriptor temp = new Descriptor();
                    temp.setName(id);
                    temp.setDescType("pointer");
                    temp.setVarType(type);
                    temp.setAddress(globalAddress);
                    globalAddress += getTypeSize(type);
                    ST.add(temp);
                }
                break;

            case "@pre_inc_dec":
                d1 = findSymbol(token.getValue());
                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    String s = (String) SemanticStack.pop();
                    Instruction temp = new Instruction();
                    temp.setOperand1(Integer.toString(d1.getAddress()));
                    temp.setOperand2("#1");
                    temp.setResult(Integer.toString(d1.getAddress()));

                    if (s.equals("++"))
                        temp.setOperator("ADD");
                    else if(s.equals("--"))
                        temp.setOperator("SUB");

                    code.add(pc++,temp);
                }
                break;


            case "@OR":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Descriptor te = new Descriptor();
                    te.setName("or_temp");
                    te.setDescType("variable");

                    for (int i = 0; i < d1.Registers.size(); i++) {
                        te.Registers.add(i,d1.Registers.get(i) + " | " + d2.Registers.get(i));
                    }

                    SemanticStack.push(te);

                }
                break;

            case "@add":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"+");
                    Descriptor te = new Descriptor();
                    te.setName("add_temp");
                    te.setDescType("variable");
                    te.setVarType(type);
                    te.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    if (d1.getName().equals("arr_temp"))
                        temp.setOperand1("@"+addr1);
                    else
                        temp.setOperand1(addr1);
                    if (d2.getName().equals("arr_temp"))
                        temp.setOperand2("@"+addr2);
                    else
                        temp.setOperand2(addr2);
                    temp.setOperator("ADD");
                    temp.setResult(Integer.toString(te.getAddress()));

                    SemanticStack.push(te);
                    code.add(pc++,temp);
                }
                break;

            case "@minus":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"-");
                    Descriptor te = new Descriptor();
                    te.setName("minus_temp");
                    te.setDescType("variable");
                    te.setVarType(type);
                    te.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te);


                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    if (d1.getName().equals("arr_temp"))
                        temp.setOperand1("@"+addr1);
                    else
                        temp.setOperand1(addr1);
                    if (d2.getName().equals("arr_temp"))
                        temp.setOperand2("@"+addr2);
                    else
                        temp.setOperand2(addr2);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te.getAddress()));

                    SemanticStack.push(te);
                    code.add(pc++,temp);
                }
                break;

            case "@mult":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"*");
                    Descriptor te = new Descriptor();
                    te.setName("mult_temp");
                    te.setDescType("variable");
                    te.setVarType(type);
                    te.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te);


                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    if (d1.getName().equals("arr_temp"))
                        temp.setOperand1("@"+addr1);
                    else
                        temp.setOperand1(addr1);
                    if (d2.getName().equals("arr_temp"))
                        temp.setOperand2("@"+addr2);
                    else
                        temp.setOperand2(addr2);
                    temp.setOperator("MUL");
                    temp.setResult(Integer.toString(te.getAddress()));

                    SemanticStack.push(te);
                    code.add(pc++,temp);
                }
                break;

            case "@divide":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"/");
                    Descriptor te = new Descriptor();
                    te.setName("divide_temp");
                    te.setDescType("variable");
                    te.setVarType(type);
                    te.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te);


                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    if (d1.getName().equals("arr_temp"))
                        temp.setOperand1("@"+addr1);
                    else
                        temp.setOperand1(addr1);
                    if (d2.getName().equals("arr_temp"))
                        temp.setOperand2("@"+addr2);
                    else
                        temp.setOperand2(addr2);
                    temp.setOperator("DIV");
                    temp.setResult(Integer.toString(te.getAddress()));

                    SemanticStack.push(te);
                    code.add(pc++,temp);
                }
                break;

            case "@mode":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"%");
                    Descriptor te = new Descriptor();
                    te.setName("mode_temp");
                    te.setDescType("variable");
                    te.setVarType(type);
                    te.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te);


                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    if (d1.getName().equals("arr_temp"))
                        temp.setOperand1("@"+addr1);
                    else
                        temp.setOperand1(addr1);
                    if (d2.getName().equals("arr_temp"))
                        temp.setOperand2("@"+addr2);
                    else
                        temp.setOperand2(addr2);
                    temp.setOperator("MOD");
                    temp.setResult(Integer.toString(te.getAddress()));
                    code.add(pc++,temp);

                    SemanticStack.push(te);
                }
                break;

            case "@arr":
                Descriptor te;
                te = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();

                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{

                    Descriptor te2 = new Descriptor();
                    te2.setName("arr_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);


                    Instruction temp = new Instruction();
                    temp.setOperand1("#"+getTypeSize(d1.getVarType()));
                    if (te.getDescType().equals("number"))
                        temp.setOperand2("#"+te.getName());
                    else
                        temp.setOperand2(Integer.toString(te.getAddress()));
                    temp.setOperator("MUL");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#"+Integer.toString(d1.getAddress()));
                    temp2.setOperand2(Integer.toString(te2.getAddress()));
                    temp2.setOperator("ADD");
                    temp2.setResult(Integer.toString(te2.getAddress()));

                    tempAddress += 4;

                    code.add(pc++,temp2);

                    SemanticStack.push(te2);
                }
                break;

            case "@inc_dec_assign":

                String s = (String) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();

                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();
                    temp.setOperand1(Integer.toString(d1.getAddress()));
                    temp.setOperand2("#1");
                    temp.setResult(Integer.toString(d1.getAddress()));

                    if (s.equals("++"))
                        temp.setOperator("ADD");
                    else if(s.equals("--"))
                        temp.setOperator("SUB");

                    code.add(pc++,temp);
                }
                break;

            case "@assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{

                    //TODO
                    for (int i = 0; i < 16; i++) {
                        System.out.println(d2.Registers.get(i) + " = "+ d1.Registers.get(i));
                    }
                    /*Instruction temp = new Instruction();
                    if (d1.getDescType().equals("number"))
                        temp.setOperand1("#"+d1.getName());
                    else
                        temp.setOperand1(Integer.toString(d1.getAddress()));
                    temp.setOperand2("");
                    temp.setOperator("MOV");
                    if (d2.getName().equals("arr_temp"))
                        temp.setResult("@"+Integer.toString(d2.getAddress()));
                    else
                        temp.setResult(Integer.toString(d2.getAddress()));
                    code.add(pc++,temp);*/
                }
                break;

            case "@addr_assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();
                    temp.setOperand1("#"+Integer.toString(d1.getAddress()));
                    temp.setOperand2("");
                    temp.setOperator("MOV");
                    temp.setResult(Integer.toString(d2.getAddress()));
                    code.add(pc++,temp);
                }
                break;

            case "@le_assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                d3 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null || d3 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();


                    Descriptor te2 = new Descriptor();
                    te2.setName("ass_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    //d2 = d2 - d1
                    temp.setOperand1(Integer.toString(d2.getAddress()));
                    temp.setOperand2(Integer.toString(d1.getAddress()));
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JG");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp4);
                }
                break;

            case "@l_assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                d3 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null || d3 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();

                    Descriptor te2 = new Descriptor();
                    te2.setName("ass_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    //d2 = d2 - d1
                    temp.setOperand1(Integer.toString(d2.getAddress()));
                    temp.setOperand2(Integer.toString(d1.getAddress()));
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JGE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp4);
                }
                break;

            case "@ge_assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                d3 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null || d3 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();


                    Descriptor te2 = new Descriptor();
                    te2.setName("ass_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);


                    //d2 = d2 - d1
                    temp.setOperand1(Integer.toString(d2.getAddress()));
                    temp.setOperand2(Integer.toString(d1.getAddress()));
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JL");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp4);
                }
                break;

            case "@g_assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                d3 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null || d3 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();


                    Descriptor te2 = new Descriptor();
                    te2.setName("ass_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);


                    //d2 = d2 - d1
                    temp.setOperand1(Integer.toString(d2.getAddress()));
                    temp.setOperand2(Integer.toString(d1.getAddress()));
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JLE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp4);
                }
                break;

            case "@e_assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                d3 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null || d3 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();


                    Descriptor te2 = new Descriptor();
                    te2.setName("ass_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);


                    //d2 = d2 - d1
                    temp.setOperand1(Integer.toString(d2.getAddress()));
                    temp.setOperand2(Integer.toString(d1.getAddress()));
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JNE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp4);
                }
                break;

            case "@ne_assign":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                d3 = (Descriptor) SemanticStack.pop();

                if (d1 == null || d2 == null || d3 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();


                    Descriptor te2 = new Descriptor();
                    te2.setName("ass_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);


                    //d2 = d2 - d1
                    temp.setOperand1(Integer.toString(d2.getAddress()));
                    temp.setOperand2(Integer.toString(d1.getAddress()));
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(d3.getAddress()));
                    code.add(pc++,temp4);
                }
                break;

            case "@if_jz":
                d1 = (Descriptor) SemanticStack.pop();//Tbe
            {
                // Tbe == 0
                Instruction temp = new Instruction();
                temp.setOperand1(Integer.toString(d1.getAddress()));
                temp.setOperand2("#0");
                temp.setOperator("JZ");

                //push pc to set the result of this line later
                SemanticStack.push(pc);
                code.add(pc++, temp);
            }
                break;

            case "@if_comp_jz":
                pre_pc = (Integer) SemanticStack.pop();
                code.get(pre_pc).setResult(Integer.toString(pc));
                break;


            case "@if_jmp":
                pre_pc = (Integer) SemanticStack.pop();

            {
                Instruction temp = new Instruction();
                temp.setOperator("JMP");

                //push pc to set the result of this line later
                SemanticStack.push(pc);
                code.add(pc++, temp);

                //pc already incremented so we dont need to ++ the pc
                code.get(pre_pc).setResult(Integer.toString(pc));
            }
                break;

            case "@if_comp_jmp":
                pre_pc = (Integer) SemanticStack.pop();
                code.get(pre_pc).setResult(Integer.toString(pc));
                break;

            case "@while_jz":
                d1 = (Descriptor) SemanticStack.pop();//Tbe
            {
                // Tbe == 0
                Instruction temp = new Instruction();
                temp.setOperand1(Integer.toString(d1.getAddress()));
                temp.setOperand2("#0");
                temp.setOperator("JZ");

                //push pc to set the result of this line later
                SemanticStack.push(pc);
                code.add(pc++, temp);
            }
                break;

            case "@while_comp_jz":
            {
                pre_pc = (Integer) SemanticStack.pop();
                int pre_pc2 = (Integer) SemanticStack.pop();
                Instruction temp = new Instruction();
                temp.setOperator("JMP");
                temp.setResult(Integer.toString(pre_pc2));
                code.add(pc++, temp);

                //pc already incremented so we dont need to ++ the pc
                code.get(pre_pc).setResult(Integer.toString(pc));
            }
            break;

            case "@do_while":
                d1 = (Descriptor) SemanticStack.pop();//Tbe
                pre_pc = (Integer) SemanticStack.pop();
            {
                // Tbe == 0
                Instruction temp = new Instruction();
                temp.setOperand1(Integer.toString(d1.getAddress()));
                temp.setOperand2("#0");
                temp.setOperator("JZ");
                temp.setResult(Integer.toString(pre_pc));

                //push pc to set the result of this line later
                code.add(pc++, temp);
            }
            break;

            case "@move_num":
                d1 = (Descriptor) SemanticStack.pop();

                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();
                    temp.setOperand1("#"+token.getValue());
                    temp.setOperand2("");
                    temp.setOperator("MOV");
                    temp.setResult(Integer.toString(d1.getAddress()));
                    code.add(pc++,temp);
                }
                break;

            case "@for_jz":
                d1 = (Descriptor) SemanticStack.pop();//Tbe
            {
                // Tbe == 0
                Instruction temp = new Instruction();
                temp.setOperand1(Integer.toString(d1.getAddress()));
                temp.setOperand2("#0");
                temp.setOperator("JZ");
                code.add(pc++, temp);

                // Tbe == 0
                Instruction temp2 = new Instruction();
                temp2.setOperator("JMP");

                //push pc to set the result of this line later
                SemanticStack.push(pc);
                code.add(pc++, temp2);
            }
            break;


            case "@for_inc_dec":
            {
                pre_pc = (Integer) SemanticStack.pop();
                int pre_pc2 = (Integer) SemanticStack.pop();

                //push for last JMP
                SemanticStack.push(pre_pc);

                Instruction temp = new Instruction();
                temp.setOperator("JMP");
                temp.setResult(Integer.toString(pre_pc2));
                code.add(pc++, temp);

                //pc already incremented so we don't need to ++ the pc
                code.get(pre_pc).setResult(Integer.toString(pc));
            }
            break;


            case "@for_comp":
                pre_pc = (Integer) SemanticStack.pop();

                Instruction for_temp = new Instruction();
                for_temp.setOperator("JMP");
                for_temp.setResult(Integer.toString(pre_pc+1));
                code.add(pc++, for_temp);

                code.get(pre_pc - 1).setResult(Integer.toString(pc));

                break;


            case "@post_inc":
                d1 = (Descriptor) SemanticStack.pop();

                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();
                    temp.setOperand1(Integer.toString(d1.getAddress()));
                    temp.setOperand2("#1");
                    temp.setOperator("ADD");
                    temp.setResult(Integer.toString(d1.getAddress()));
                    code.add(pc++,temp);
                }
                break;

            case "@post_dec":
                d1 = (Descriptor) SemanticStack.pop();

                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    Instruction temp = new Instruction();
                    temp.setOperand1(Integer.toString(d1.getAddress()));
                    temp.setOperand2("#1");
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(d1.getAddress()));
                    code.add(pc++,temp);
                }
                break;

            case "@push_#":
                SemanticStack.push("#");
                break;

            case "@switch_jmp":
            {
                Instruction temp = new Instruction();
                temp.setOperator("JMP");
                code.add(pc++, temp);
            }
                break;

            case "@push_cv_char":
                d1 = findSymbol(Integer.toString((int)token.getValue().charAt(0)));
                if (d1 == null){
                    Descriptor temp = new Descriptor();
                    temp.setName(Integer.toString((int)token.getValue().charAt(0)));
                    temp.setDescType("number");
                    ST.add(temp);
                    SemanticStack.push(temp);
                }else {
                    SemanticStack.push(d1);
                }
                break;

            case "@push_cv_num":
                d1 = findSymbol(token.getValue());
                if (d1 == null){
                    Descriptor temp = new Descriptor();
                    temp.setName(token.getValue());
                    temp.setDescType("number");
                    temp.setVarType("int");
                    ST.add(temp);
                    SemanticStack.push(temp);
                }else {
                    SemanticStack.push(d1);
                }
                break;

            case "@push_default":
            {
                Descriptor temp = new Descriptor();
                temp.setName("-2424");
                temp.setDescType("number");
                SemanticStack.push(temp);
            }
                break;


            case "@switch_jmp_out":
            {
                Instruction temp = new Instruction();
                temp.setOperator("JMP");
                code.add(pc++, temp);
            }
                break;

            case "@switch":
            {
                ArrayList<CaseNode> list = new ArrayList<CaseNode>();
                int counter = 0;
                while (!SemanticStack.peek().equals("#")){
                    CaseNode c = new CaseNode();
                    c.setLabel((Integer) SemanticStack.pop());
                    c.setValue((Descriptor) SemanticStack.pop());
                    list.add(c);
                    counter++;
                }
                SemanticStack.pop();

                //id
                d1 = (Descriptor) SemanticStack.pop();

                //complete first jump of switch
                code.get(list.get(list.size() - 1).getLabel() - 1).setResult(Integer.toString(pc));

                //jump out of default
                code.get(pc-1).setResult(Integer.toString(pc+counter));

                //complete jump equal
                for (int i = 0; i < list.size() - 1; i++) {
                    Instruction temp = new Instruction();
                    temp.setOperand1(Integer.toString(d1.getAddress()));
                    temp.setOperand2("#"+list.get(list.size() - i - 1).getValue().getName());
                    temp.setOperator("JE");
                    temp.setResult(Integer.toString(list.get(list.size() - i - 1).getLabel()));
                    code.add(pc++,temp);
                }

                //last jump to default
                if (list.get(0).getValue().getName().equals("-2424")) {
                    Instruction temp = new Instruction();
                    temp.setOperator("JMP");
                    temp.setResult(Integer.toString(list.get(0).getLabel()));
                    code.add(pc++, temp);
                }

                //complete jump out
                for (int i = 0; i < list.size() - 1; i++) {
                    code.get(list.get(i).getLabel() - 1).setResult(Integer.toString(pc));
                }
            }
            break;

            /*****************************SLR**********************************/

            case "@SLR_or":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("or_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    temp.setOperand1(addr1);
                    temp.setOperand2(addr2);
                    temp.setOperator("ORR");
                    temp.setResult(Integer.toString(te2.getAddress()));

                    SemanticStack.push(te2);
                    code.add(pc++, temp);
                }
                break;

            case "@SLR_and":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("and_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    temp.setOperand1(addr1);
                    temp.setOperand2(addr2);
                    temp.setOperator("AND");
                    temp.setResult(Integer.toString(te2.getAddress()));

                    SemanticStack.push(te2);
                    code.add(pc++, temp);
                }
                break;

            case "@SLR_not":
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("not_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    String addr1;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    Instruction temp = new Instruction();
                    temp.setOperand1(addr1);
                    temp.setOperator("NOT");
                    temp.setResult(Integer.toString(te2.getAddress()));

                    SemanticStack.push(te2);
                    code.add(pc++, temp);
                }
                break;

            case "@SLR_l":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("l_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    Descriptor te3 = new Descriptor();
                    te3.setName("l_temp");
                    te3.setDescType("variable");
                    te3.setVarType("int");
                    te3.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te3);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    //te = d2 - d1
                    Instruction temp = new Instruction();
                    temp.setOperand1(addr2);
                    temp.setOperand2(addr1);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JGE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp4);

                    SemanticStack.push(te3);
                }
                break;

            case "@SLR_g":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("g_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    Descriptor te3 = new Descriptor();
                    te3.setName("g_temp");
                    te3.setDescType("variable");
                    te3.setVarType("int");
                    te3.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te3);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    //te = d2 - d1
                    Instruction temp = new Instruction();
                    temp.setOperand1(addr2);
                    temp.setOperand2(addr1);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JLE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp4);

                    SemanticStack.push(te3);
                }
                break;

            case "@SLR_e":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("e_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    Descriptor te3 = new Descriptor();
                    te3.setName("e_temp");
                    te3.setDescType("variable");
                    te3.setVarType("int");
                    te3.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te3);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    //te = d2 - d1
                    Instruction temp = new Instruction();
                    temp.setOperand1(addr2);
                    temp.setOperand2(addr1);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JNE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp4);

                    SemanticStack.push(te3);
                }
                break;

            case "@SLR_ne":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("ne_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    Descriptor te3 = new Descriptor();
                    te3.setName("ne_temp");
                    te3.setDescType("variable");
                    te3.setVarType("int");
                    te3.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te3);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    //te = d2 - d1
                    Instruction temp = new Instruction();
                    temp.setOperand1(addr2);
                    temp.setOperand2(addr1);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JE");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp4);

                    SemanticStack.push(te3);
                }
                break;

            case "@SLR_le":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("le_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    Descriptor te3 = new Descriptor();
                    te3.setName("le_temp");
                    te3.setDescType("variable");
                    te3.setVarType("int");
                    te3.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te3);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    //te = d2 - d1
                    Instruction temp = new Instruction();
                    temp.setOperand1(addr2);
                    temp.setOperand2(addr1);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JG");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp4);

                    SemanticStack.push(te3);
                }
                break;

            case "@SLR_ge":
                d1 = (Descriptor) SemanticStack.pop();
                d2 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else {
                    Descriptor te2 = new Descriptor();
                    te2.setName("ge_temp");
                    te2.setDescType("variable");
                    te2.setVarType("int");
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te2);

                    Descriptor te3 = new Descriptor();
                    te3.setName("ge_temp");
                    te3.setDescType("variable");
                    te3.setVarType("int");
                    te3.setAddress(tempAddress);
                    tempAddress += getTypeSize("int");
                    temps.add(te3);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#" + d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#" + d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    //te = d2 - d1
                    Instruction temp = new Instruction();
                    temp.setOperand1(addr2);
                    temp.setOperand2(addr1);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));
                    code.add(pc++,temp);

                    //d3 = 0
                    Instruction temp2 = new Instruction();
                    temp2.setOperand1("#0");
                    temp2.setOperand2("");
                    temp2.setOperator("MOV");
                    temp2.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp2);

                    //jump grater
                    Instruction temp3 = new Instruction();
                    temp3.setOperand1(Integer.toString(te2.getAddress()));
                    temp3.setOperand2("#0");
                    temp3.setOperator("JL");
                    temp3.setResult(Integer.toString(pc+2));
                    code.add(pc++,temp3);

                    //d3 = 1
                    Instruction temp4 = new Instruction();
                    temp4.setOperand1("#1");
                    temp4.setOperand2("");
                    temp4.setOperator("MOV");
                    temp4.setResult(Integer.toString(te3.getAddress()));
                    code.add(pc++,temp4);

                    SemanticStack.push(te3);
                }
                break;


            case "@SLR_add":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"+");
                    Descriptor te2 = new Descriptor();
                    te2.setName("add_temp");
                    te2.setDescType("variable");
                    te2.setVarType(type);
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te2);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    temp.setOperand1(addr1);
                    temp.setOperand2(addr2);
                    temp.setOperator("ADD");
                    temp.setResult(Integer.toString(te2.getAddress()));

                    SemanticStack.push(te2);
                    code.add(pc++,temp);
                }
                break;

            case "@SLR_minus":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"-");
                    Descriptor te2 = new Descriptor();
                    te2.setName("minus_temp");
                    te2.setDescType("variable");
                    te2.setVarType(type);
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te2);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    temp.setOperand1(addr1);
                    temp.setOperand2(addr2);
                    temp.setOperator("SUB");
                    temp.setResult(Integer.toString(te2.getAddress()));

                    SemanticStack.push(te2);
                    code.add(pc++,temp);
                }
                break;

            case "@SLR_mult":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"*");
                    Descriptor te2 = new Descriptor();
                    te2.setName("mult_temp");
                    te2.setDescType("variable");
                    te2.setVarType(type);
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te2);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    temp.setOperand1(addr1);
                    temp.setOperand2(addr2);
                    temp.setOperator("MUL");
                    temp.setResult(Integer.toString(te2.getAddress()));

                    SemanticStack.push(te2);
                    code.add(pc++,temp);
                }
                break;

            case "@SLR_divide":
                d2 = (Descriptor) SemanticStack.pop();
                d1 = (Descriptor) SemanticStack.pop();
                if (d1 == null || d2 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else{
                    type = checkType(d1,d2,"/");
                    Descriptor te2 = new Descriptor();
                    te2.setName("divide_temp");
                    te2.setDescType("variable");
                    te2.setVarType(type);
                    te2.setAddress(tempAddress);
                    tempAddress += getTypeSize(type);
                    temps.add(te2);

                    String addr1, addr2;
                    if (d1.getDescType().equals("number"))
                        addr1 = "#"+d1.getName();
                    else
                        addr1 = Integer.toString(d1.getAddress());

                    if (d2.getDescType().equals("number"))
                        addr2 = "#"+d2.getName();
                    else
                        addr2 = Integer.toString(d2.getAddress());

                    Instruction temp = new Instruction();
                    temp.setOperand1(addr1);
                    temp.setOperand2(addr2);
                    temp.setOperator("DIV");
                    temp.setResult(Integer.toString(te2.getAddress()));

                    SemanticStack.push(te2);
                    code.add(pc++,temp);
                }
                break;

            case "@SLR_push_id":
                d1 = findSymbol(SLR_id);
                if (d1 == null)
                    System.err.println(scanner.line+":\tvariable is not defined!");
                else
                    SemanticStack.push(d1);
                break;


            case "@SLR_push_num":
                d1 = findSymbol(SLR_num);
                if (d1 == null){
                    Descriptor temp = new Descriptor();
                    temp.setName(SLR_num);
                    temp.setDescType("number");
                    temp.setVarType("int");
                    ST.add(temp);
                    SemanticStack.push(temp);
                }else {
                    SemanticStack.push(d1);
                }
                break;

            case "@SLR_push_neg_num":
                d1 = findSymbol("-"+SLR_num);
                if (d1 == null){
                    Descriptor temp = new Descriptor();
                    temp.setName("-"+SLR_num);
                    temp.setDescType("number");
                    temp.setVarType("int");
                    ST.add(temp);
                    SemanticStack.push(temp);
                }else {
                    SemanticStack.push(d1);
                }
                break;

            default:
                break;

        }
    }

    public Descriptor findSymbol(String symbol){
        for (int i = 0; i < ST.size(); i++) {
            if (ST.get(i).getName().equals(symbol))
                return ST.get(i);
        }
        return null;
    }

    public int getTypeSize(String type){
        switch (type){
            case "int":
            case "float":
                return 4;
            case "char":
            case "bool":
                return 1;
            default:
                return 0;
        }
    }

    public String checkType(Descriptor d1, Descriptor d2, String operator){
        if (operator.equals("+") || operator.equals("-") || operator.equals("*")
                || operator.equals("/") || operator.equals("%")){

            if (d1.getVarType().equals("float") || d2.getVarType().equals("float")){
                if (!d1.getVarType().equals("bool") && !d2.getVarType().equals("bool"))
                    return "float";
            }

            else if (d1.getVarType().equals("int") ||d2.getVarType().equals("int")){
                if (!d1.getVarType().equals("bool") && !d2.getVarType().equals("bool"))
                    return "int";
            }

            else if(d1.getVarType().equals("char") && d2.getVarType().equals("char"))
                return "char";

            /*//for & and |
            else if(d1.getVarType().equals("bool") && d2.getVarType().equals("bool"))
                return "bool";*/
        }
        return null;
    }

    public void generateIS(){
        for (int i = 0; i < code.size(); i++) {
            System.out.print(i+"\t\t"+code.get(i).getOperator()+"\t");
            if (!code.get(i).getOperand1 ().equals(""))
                System.out.print("\t"+code.get(i).getOperand1()+"\t");
            if (!code.get(i).getOperand2().equals(""))
                System.out.print("\t"+code.get(i).getOperand2()+"\t");
            System.out.print("\t"+code.get(i).getResult());
            System.out.println();
        }
    }

}
