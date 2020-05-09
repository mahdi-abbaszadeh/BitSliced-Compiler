import com.jakewharton.fliptables.FlipTable;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mahdi2016 on 12/7/2018.
 */
public class PTG {
    private Path path;
    private List<String> lines;
    public ArrayList<GToken>[] RHST;
    public ArrayList<GToken> gtokens = new ArrayList<GToken>();
    public ArrayList<GToken>[] predict;
    public ArrayList<String> variables = new ArrayList<String>();
    public ArrayList<String> terminals = new ArrayList<String>();
    public ArrayList<String>[] grammars;
    private int numOfGrammar;
    public ArrayList<Integer>[][] ParseTable;
    public boolean isLL1 = true;

    public void get_grammar(String address)throws IOException {//read grammars and put them in RHST
        path = Paths.get(address);
        lines = Files.readAllLines(path);
        numOfGrammar = lines.size();

        //create RHST and grammars
        RHST = new ArrayList[numOfGrammar];
        grammars = new ArrayList[numOfGrammar];
        predict = new ArrayList[numOfGrammar];
        for (int i = 0; i < numOfGrammar; i++) {
            RHST[i] = new ArrayList<GToken>();
            grammars[i] = new ArrayList<String>();
            predict[i] = new ArrayList<GToken>();
        }

        for (int i = 0; i < lines.size(); i++) {//khat be khate grammar
            boolean isNull = false;
            String[] s = lines.get(i).split(" ");//joda kardane har terminal va variable

            for (int j = 0; j < s.length; j++) {
                if (j != 1 && s[j].charAt(0) != '@' && s[j].charAt(0) != '#')
                    grammars[i].add(s[j]);
            }

            for (int j = s.length - 1; j >= 0 ; j--) {//rikhtan besurat barax tu array

                if(j == 1) //baraye dar nazar nagreftan ->
                    continue;


                GToken gToken = new GToken();
                gToken.setValue(s[j]);

                if (s[j].charAt(0) != '@' && s[j].charAt(0) != '#') {//ignore semantic rule and switch parser

                    if (s[j].charAt(0) <= 90 && s[j].charAt(0) >= 65) {//variable
                        if (j == 0 && isNull) {
                            if (variables.contains(s[j])) {
                                get_GToken_from_String(s[j]).setNullable(true);
                            } else
                                gToken.setNullable(true);

                        }
                        gToken.setGtokenType(GToken.GTokenType.variable);
                        if (!variables.contains(s[j])) {
                            variables.add(s[j]);
                            gtokens.add(gToken);
                        }
                    } else { // terminal
                        gToken.setGtokenType(GToken.GTokenType.terminal);
                        if (s[j].equals("lambda"))
                            isNull = true;
                        if (!terminals.contains(s[j]) && !(s[j].equals("lambda"))) {
                            terminals.add(s[j]);
                        }
                        if (get_GToken_from_String(s[j]) == null)
                            gtokens.add(gToken);
                    }
                }

                if(j >= 2)
                    RHST[i].add(gToken);
            }
        }
        complete_nullable();
    }

    public void Firsts(){
        boolean continues = true;

        /******** initializing with first_right_token ********/
        for (int i = 0; i < grammars.length - 1; i++) {// -1 is because of last line of grammar (BE)
            GToken left_var = new GToken();
            GToken first_right_token = new GToken();

            //find the left variable and first right token for each lines of grammar
            left_var = get_GToken_from_String(grammars[i].get(0));
                first_right_token = get_GToken_from_String(grammars[i].get(1));

            if (!first_right_token.getValue().equals("lambda")) {//if is not lambda

                for (int j = 1; j < grammars[i].size(); j++) {
                    first_right_token = get_GToken_from_String(grammars[i].get(j));
                    //if it is a terminal so done!
                    if (first_right_token.getGtokenType() == GToken.GTokenType.terminal
                            && !left_var.getFirst().contains(first_right_token)) {
                        left_var.addFirst(first_right_token);
                        break;
                    }
                    //otherwise, first add the variable and if it is nullable so iterate the loop again
                    if (!left_var.getFirst().contains(first_right_token)) {
                        left_var.addFirst(first_right_token);
                        if (!first_right_token.isNullable()) {
                            break;
                        }
                    }
                }
            }
        }
        /******** End of initializing with first_right_token ********/

        /******** replacing variables in the first list with their first ********/
        while(continues){
            continues = false;

            for (int i = 0; i < gtokens.size(); i++) {//iterate all gtokens
                for (int j = 0; j < gtokens.get(i).getFirst().size(); j++) {//iterate all firsts of every gtokens
                    //if it is a variable so raplace it with its firsts
                    if (gtokens.get(i).getFirst().get(j).getGtokenType() == GToken.GTokenType.variable){

                        //should iterate again
                        continues = true;

                        //remove the gtoken from list and add all its first to the list
                        GToken temp = gtokens.get(i).getFirst().remove(j);
                        for (int k = 0; k < temp.getFirst().size(); k++) {
                            //if it is not already added to the list
                            if (!gtokens.get(i).getFirst().contains(temp.getFirst().get(k)))
                                gtokens.get(i).addFirst(temp.getFirst().get(k));
                        }
                    }
                }
            }
        }
        /******** End of replacing variables in the first list with their first ********/


        /*********** print *********/
        for (int i = 0; i < variables.size(); i++) {
            GToken token = get_GToken_from_String(variables.get(i));
            System.out.print("the First of variable:\t"+token.getValue()+"\tis:\t");
            for (int j = 0; j < token.getFirst().size(); j++) {
                System.out.print(token.getFirst().get(j).getValue()+"\t");
            }
            System.out.println();
        }
    }

    public void Follows(){
        boolean continues = true;

        /******** Initializing ********/
        GToken dollar = new GToken();
        dollar.setGtokenType(GToken.GTokenType.terminal);
        dollar.setValue("$");
        terminals.add("$");
        gtokens.add(dollar);

        //add dollar to first start variable
        get_GToken_from_String(grammars[0].get(0)).addFollow(dollar);

        for (int i = 0; i < grammars.length; i++) {

            GToken token = new GToken();
            GToken follow_token = new GToken();
            GToken left_var = new GToken();

            //check every variable except the last one
            for (int j = 1; j < grammars[i].size() - 1; j++) {
                token = get_GToken_from_String(grammars[i].get(j));
                follow_token = get_GToken_from_String(grammars[i].get(j+1));
                left_var = get_GToken_from_String(grammars[i].get(0));

                //if it is a variable so we have to add follow
                if (token.getGtokenType() == GToken.GTokenType.variable){
                    //if the follow gtoken is a terminal , just add it
                    if (follow_token.getGtokenType() == GToken.GTokenType.terminal){
                        //if it has not added before
                        if (!token.getFollow().contains(follow_token))
                            token.addFollow(follow_token);
                    }
                    // follow gtoken is a variable
                    else{
                        //first of all we add all firsts of follow token
                        for (int k = 0; k < follow_token.getFirst().size(); k++) {
                            //if it has not added before
                            if (!token.getFollow().contains(follow_token.getFirst().get(k)))
                                token.addFollow(follow_token.getFirst().get(k));
                        }
                        //if follow is nullable so we have to add the follows of left var
                        if (follow_token.isNullable()){
                            //if variable and left variable are not the same
                            if (!left_var.getValue().equals(token.getValue()))
                                token.addFollow(left_var);
                        }
                    }
                }
            }

            //check the last one -> lambda must ignore
            token = get_GToken_from_String(grammars[i].get(grammars[i].size() - 1));
            left_var = get_GToken_from_String(grammars[i].get(0));
            follow_token = null;

            if (!token.getValue().equals("lambda")
                    && token.getGtokenType() == GToken.GTokenType.variable
                    && !left_var.getValue().equals(token.getValue())){
                token.addFollow(left_var);
            }
        }
        /******** End of Initializing ********/

        /******** replacing variables in the follows list with their follow terminals ********/
        while(continues){
            continues = false;
            for (int i = 0; i < gtokens.size(); i++) {//iterate all gtokens
                for (int j = 0; j < gtokens.get(i).getFollow().size(); j++) {//iterate all follows of every gtokens

                    //if it is a variable so raplace it with its follows
                    if (gtokens.get(i).getFollow().get(j).getGtokenType() == GToken.GTokenType.variable){

                        //should iterate again
                        continues = true;

                        //remove the gtoken from list and add all its follows to the list
                        GToken temp = gtokens.get(i).getFollow().remove(j);
                        for (int k = 0; k < temp.getFollow().size(); k++) {
                            //if it is not already added to the list
                            if (!gtokens.get(i).getFollow().contains(temp.getFollow().get(k)))
                                gtokens.get(i).addFollow(temp.getFollow().get(k));
                        }
                    }
                }
            }
        }
        /******** End of replacing variables in the follows list with their follow terminals ********/

        /*********** print *********/
        for (int i = 0; i < variables.size(); i++) {
            GToken token = get_GToken_from_String(variables.get(i));
            System.out.print("the Follow of variable:\t"+token.getValue()+"\tis:\t");
            for (int j = 0; j < token.getFollow().size(); j++) {
                System.out.print(token.getFollow().get(j).getValue()+"\t");
            }
            System.out.println();
        }
    }

    public void predicts(){

        boolean null_all;
        for (int i = 0; i < grammars.length; i++) {

            GToken token = new GToken();
            GToken left_var = new GToken();
            left_var = get_GToken_from_String(grammars[i].get(0));

            null_all = true;
            for (int j = 1; j < grammars[i].size(); j++) {

                //get the token and check if it is a terminal or variable
                token = get_GToken_from_String(grammars[i].get(j));

                // if it is a terminal just add it to predict(i) and go to next grammar line
                if (token.getGtokenType() == GToken.GTokenType.terminal){
                    //if it is not lambda
                    if (!token.getValue().equals("lambda")) {
                        predict[i].add(token);
                        null_all = false;
                        break;
                    }
                }

                //it is a variable, so we have to add its firsts and check if it is nullable add next var
                else{
                    //add firsts of token to predict[i]
                    for (int k = 0; k < token.getFirst().size(); k++) {
                        predict[i].add(token.getFirst().get(k));
                    }
                    //if it is not nullable so done!
                    if (!token.isNullable()){
                        null_all = false;
                        break;
                    }
                }

            }
            if (null_all){
                for (int j = 0; j < left_var.getFollow().size(); j++) {
                    predict[i].add(left_var.getFollow().get(j));
                }
            }
        }

        /*********** print *********/
        for (int i = 0; i < predict.length; i++) {
            System.out.print("predict("+(i+1)+") is:\t");
            for (int j = 0; j < predict[i].size(); j++) {
                System.out.print(predict[i].get(j).getValue()+"\t");
            }
            System.out.println();
        }
    }

    void complete_nullable(){
        boolean continues = true;
        boolean null_all = true;

        while (continues){
            continues = false;

            for (int i = 0; i < grammars.length; i++) {
                null_all = true;
                for (int j = 1; j < grammars[i].size(); j++) {
                    //if (get_GToken_from_String(grammars[i].get(j)) != null){//if token != lambda
                        if (!get_GToken_from_String(grammars[i].get(j)).isNullable()) {
                            null_all = false;
                            break;
                        }
                    //}
                }
                if (null_all && !get_GToken_from_String(grammars[i].get(0)).isNullable()){
                    get_GToken_from_String(grammars[i].get(0)).setNullable(true);
                    continues = true;
                }
            }
        }
    }

    void GeneratePT(){

        //the index of table is based on variables and terminals index
        //initialize with value of -1
        //our grammars start with line 0
        ParseTable = new ArrayList[variables.size()][terminals.size()];
        for (int i = 0; i < variables.size(); i++) {
            for (int j = 0; j < terminals.size(); j++) {
                ParseTable[i][j] = new ArrayList<Integer>();
            }
        }

        for (int i = 0; i < predict.length; i++) {

            GToken variable = new GToken();
            GToken terminal = new GToken();
            int variable_index, terminal_index;

            //get the left variable of every line of grammar and its index in table
            variable = get_GToken_from_String(grammars[i].get(0));
            variable_index = variables.indexOf(grammars[i].get(0));

            //check all terminals of predict(i)
            for (int j = 0; j < predict[i].size(); j++) {
                terminal = predict[i].get(j);
                terminal_index = terminals.indexOf(predict[i].get(j).getValue());
                if (variable.getValue().equals("IFST''") && terminal.getValue().equals("else")) {
                        if (ParseTable[variable_index][terminal_index].isEmpty()
                                && predict[i].size() > 1)
                            continue;
                }
                ParseTable[variable_index][terminal_index].add(i);
            }
        }

        /*********** check if the ParseTable is LL1 *********/
        for (int i = 0; i < variables.size(); i++) {
            for (int j = 0; j < terminals.size(); j++) {
                if (ParseTable[i][j].size() > 1){
                    isLL1 = false;
                    break;
                }
                if (!isLL1)
                    break;
            }
        }
        if (isLL1)
            System.out.println("The grammar is LL1 and the table is shown below:");
        else
            System.out.println("The grammar is not LL1 and the table is shown below:");
        System.out.println();

        /*********** print *********/
        String[][] table_for_print = new String[variables.size()][terminals.size() + 1];

        for (int i = 0; i < variables.size(); i++) {
            table_for_print[i][0] = variables.get(i);
        }

        for (int i = 0; i < variables.size(); i++) {
            for (int j = 1; j < terminals.size() + 1; j++) {
                if (ParseTable[i][j-1].isEmpty())
                    table_for_print[i][j] = "0";
                else{
                    for (int k = 0; k < ParseTable[i][j - 1].size(); k++) {
                        if (k == 0)
                            table_for_print[i][j] = Integer.toString((ParseTable[i][j-1].get(k)+1));
                        else{
                            table_for_print[i][j] += " "+Integer.toString((ParseTable[i][j-1].get(k)+1));
                        }
                    }
                }
            }
        }
        /*System.out.print("\t");
        for (int i = 0; i < terminals.size(); i++) {
            System.out.print(terminals.get(i)+"\t");
        }
        System.out.println();
        System.out.println();

        for (int i = 0; i < variables.size(); i++) {
            System.out.print(variables.get(i)*//*+ "\t"*//*);
            System.out.println();
            System.out.print("\t");
            for (int j = 0; j < terminals.size(); j++) {
                if (ParseTable[i][j].isEmpty())
                    System.out.print("0\t");
                else {
                    for (int k = 0; k < ParseTable[i][j].size(); k++) {
                        System.out.print((ParseTable[i][j].get(k)+1)+" ");
                    }
                    System.out.print("\t");
                }
            }
            System.out.println();
        }*/
        String[] terminal_as_String = new String[terminals.size() + 1];
        terminal_as_String[0] = "\t";
        for (int i = 0; i < terminals.size(); i++) {
            terminal_as_String[i+1] = terminals.get(i);
        }
        System.out.println(FlipTable.of(terminal_as_String,table_for_print));
    }

    int PT(String variable, String terminal){
        int variable_index = variables.indexOf(variable);
        int terminal_index = terminals.indexOf(terminal);
        return ParseTable[variable_index][terminal_index].get(0);
    }

    GToken get_GToken_from_String(String s){
        for (int i = 0; i < gtokens.size(); i++) {
            if (gtokens.get(i).getValue().equals(s)) {
                return gtokens.get(i);
            }
        }
        return null;
    }
}

