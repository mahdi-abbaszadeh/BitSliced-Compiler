import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;


public class Scanner {
    //File sourcefile;
    private ArrayList<String> keywords = new ArrayList<String>();
    //private String sourcefile;
    private InputStream input;
    private int input_size;
    private char ch;
    private int index = 0;
    public int line = 1;

    Scanner(/*File file*/ String sourcefile) throws IOException{
        initial_keyword();
        //this.sourcefile = sourcefile;
        this.input = new FileInputStream(sourcefile);
        this.input_size = this.input.available();
        ch = (char) input.read();
    }

    public Token nextToken()throws IOException{
        Token token = new Token();

        /*******    id  &   keywords   **********/
        if ( (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ){
            String str = "";
            do {
                str += ch;
                ch = (char) input.read();
            }while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || (ch == '_') );
            if(keywords.contains(str)){
                token.setTokenType(Token.TokenType.keyword);
            } else{
                token.setTokenType(Token.TokenType.id);
            }
            token.setValue(str);
            return token;
        }

        /******    numbers   *********/
        else if((ch >= '0' && ch <= '9')){
            int left = ch - '0';
            ch = (char) input.read();
            while ((ch >= '0' && ch <= '9')){
                left = 10 * left + (ch - '0');
                ch = (char) input.read();
            }
            if (ch == '.'){
                ch = (char) input.read();
                int right = ch - '0';
                while ((ch >= '0' && ch <= '9')){
                    right = 10 * right + (ch - '0');
                    ch = (char) input.read();
                }
                token.setTokenType(Token.TokenType.num);
                token.setValue(Integer.toString(left) +"."+ Integer.toString(right));
                return token;
            }else{
                token.setTokenType(Token.TokenType.num);
                token.setValue(Integer.toString(left));
                return token;
            }

        }

        /******    Special Token(only 1 character)   *********/
        else if(ch == '(' || ch == ')' ||
                ch == '[' || ch == ']' ||
                ch == '{' || ch == '}' ||
                ch == '.' || ch == ',' ||
                ch == '~' || ch == ';' ||
                ch == '?' || ch == ':'
                ){
            token.setTokenType(Token.TokenType.st);
            token.setValue(String.valueOf(ch));
            ch = (char) input.read();
            return token;
        }

        else if(ch == (char)-1){
            token.setTokenType(Token.TokenType.st);
            token.setValue(String.valueOf('$'));
            return token;
        }

        else if (ch == '$'){
            token.setTokenType(Token.TokenType.st);
            token.setValue("$");
            ch = (char) input.read();
            return token;

        }


        switch (ch){
            /******    White Space   *********/
            case ' ' :
            case '\t':
            case '\r':
            case '\n':
                do {
                    if (ch == '\n')
                        line++;
                    ch = (char) input.read();
                }while ((ch == ' ') || (ch == '\t') || (ch == '\n'));
                return nextToken();

            /******    Special Token(more than 1 character)   *********/

            /**    + ++ +=   **/
            case '+' :
                ch = (char) input.read();
                if (ch == '+'){
                    token.setValue("++");
                    ch = (char) input.read();
                } else if(ch == '='){
                    token.setValue("+=");
                    ch = (char) input.read();
                } else{
                    token.setValue("+");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**    - -- -=   **/
            case '-' :
                ch = (char) input.read();
                if (ch == '-'){
                    token.setValue("--");
                    ch = (char) input.read();
                } else if(ch == '='){
                    token.setValue("-=");
                    ch = (char) input.read();
                } else{
                    token.setValue("-");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**    < << <=   **/
            case '<' :
                ch = (char) input.read();
                if (ch == '<'){
                    ch = (char) input.read();
                    if(ch == '='){
                        token.setValue("<<=");
                        ch = (char) input.read();
                    } else {
                        token.setValue("<<");
                    }
                } else if(ch == '='){
                    token.setValue("<=");
                    ch = (char) input.read();
                } else{
                    token.setValue("<");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**    > >> >=   **/
            case '>' :
                ch = (char) input.read();
                if (ch == '>'){
                    ch = (char) input.read();
                    if(ch == '='){
                        token.setValue(">>=");
                        ch = (char) input.read();
                    } else {
                        token.setValue(">>");
                    }
                } else if(ch == '='){
                    token.setValue(">=");
                    ch = (char) input.read();
                } else{
                    token.setValue(">");
                }
                token.setTokenType(Token.TokenType.st);
                return token;


            /**    = ==   **/
            case '=' :
                ch = (char) input.read();
                if (ch == '='){
                    token.setValue("==");
                    ch = (char) input.read();
                } else{
                    token.setValue("=");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**    ! !=   **/
            case '!' :
                ch = (char) input.read();
                if (ch == '='){
                    token.setValue("!=");
                    ch = (char) input.read();
                } else{
                    token.setValue("!");
                }
                token.setTokenType(Token.TokenType.st);
                return token;


            /**    & && &=   **/
            case '&' :
                ch = (char) input.read();
                if (ch == '&'){
                    token.setValue("&&");
                    ch = (char) input.read();
                } else if(ch == '='){
                    token.setValue("&=");
                    ch = (char) input.read();
                } else{
                    token.setValue("&");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**  | || |=   **/
            case '|' :
                ch = (char) input.read();
                if (ch == '|'){
                    token.setValue("||");
                    ch = (char) input.read();
                } else if(ch == '='){
                    token.setValue("|=");
                    ch = (char) input.read();
                } else{
                    token.setValue("|");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**    * *=   **/
            case '*' :
                ch = (char) input.read();
                if (ch == '='){
                    token.setValue("*=");
                    ch = (char) input.read();
                } else{
                    token.setValue("*");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**    / /=  and Comment   **/
            case '/' :
                ch = (char) input.read();
                if (ch == '='){
                    token.setValue("/=");
                    token.setTokenType(Token.TokenType.st);
                    ch = (char) input.read();
                } else if(ch == '/'){
                    String str = "";
                    do {
                        ch = (char) input.read();
                        //don't add \n to the end of comment value
                        if (ch != '\r' && ch != '\n') {str += ch;}
                    }while (ch != '\r' && ch != '\n');

                    token.setValue(str);
                    token.setTokenType(Token.TokenType.comment);

                } else if(ch == '*'){
                    String str = "";
                    ch = (char) input.read();
                    do {
                        if (ch == '\n')
                            line++;
                        while (ch != '*') {
                            str += ch;
                            ch = (char) input.read();
                            if (ch == '\n')
                                line++;
                        }
                        ch = (char) input.read();
                        if (ch == '\n')
                            line++;

                        //add '*' as a comment
                        if (ch != '/')
                            str += '*';

                    }while (ch != '/');
                    token.setValue(str);
                    token.setTokenType(Token.TokenType.comment);
                    ch = (char) input.read();
                }
                else{
                    token.setValue("/");
                    token.setTokenType(Token.TokenType.st);
                }

                if (token.getTokenType() != Token.TokenType.comment)
                    return token;
                else
                    return this.nextToken();

            /**    % %=   **/
            case '%' :
                ch = (char) input.read();
                if (ch == '='){
                    token.setValue("%=");
                    ch = (char) input.read();
                } else{
                    token.setValue("%");
                }
                token.setTokenType(Token.TokenType.st);
                return token;

            /**    ^ ^=   **/
            case '^' :
                ch = (char) input.read();
                if (ch == '='){
                    token.setValue("^=");
                    ch = (char) input.read();
                } else{
                    token.setValue("^");
                }
                token.setTokenType(Token.TokenType.st);
                return token;
            /******    character   *********/
            case '\'':
                ch = (char) input.read();
                char val = ch;
                ch = (char) input.read();
                if (ch == '\''){
                    token.setValue(String.valueOf(val));
                    token.setTokenType(Token.TokenType.character);
                }
                ch = (char) input.read();
                return token;

            /******    String   *********/
            case '\"':
                String str = "";
                do {
                    ch = (char) input.read();
                    if (ch != '\"')
                        str += ch;
                }while (ch != '\"');

                token.setValue(str);
                token.setTokenType(Token.TokenType.string);
                ch = (char) input.read();
                return token;

            default:
                break;
        }
        return token;
    }

    private void initial_keyword(){
        keywords.add("if");
        keywords.add("while");
        keywords.add("do");
        keywords.add("for");
        keywords.add("main");
        keywords.add("case");
        keywords.add("switch");
        keywords.add("default");
        keywords.add("break");
        keywords.add("continue");
        keywords.add("return");
        keywords.add("int");
        keywords.add("float");
        keywords.add("double");
        keywords.add("char");
        keywords.add("String");
        keywords.add("else");
        keywords.add("void");
        keywords.add("bool");
    }
}
