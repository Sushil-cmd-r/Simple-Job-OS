package phase2;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

public class VM {
    char[][] M= new char[300][4];
    int IC, PTR, VA, RA, SI, TI, PI, TLC = 0, LLC = 0;
    int[] flag = new int[30];

    char[] IR = new char[4];
    char[] R = new char[4];
    boolean C;
    String EM;
    String[] errors = {
            "No Error",
            "Out Of Data",
            "Line Limit Exceeded",
            "Time Limit Exceeded",
            "Operation Code Error",
            "Operand Code Error",
            "Invalid Page Fault"
    };
    char[] buff = new char[40];
    File in = new File("C:\\Users\\kandh\\OneDrive\\Desktop\\OSLAB\\Lab\\Assign3\\Java\\src\\phase2\\input.txt");
    File out = new File("C:\\Users\\kandh\\OneDrive\\Desktop\\OSLAB\\Lab\\Assign3\\Java\\src\\phase2\\output.txt");
    Scanner sc;
    {
        try {
            sc = new Scanner(in);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    class PCB {
        char[] job = new char[4];
        char[] TTL = new char[4];
        char[] TLL = new char[4];
        @Override
        public String toString() {
            return "JobID: " + Arrays.toString(job) + "\nTTL: " + Arrays.toString(TTL) + "\nTLL: " + Arrays.toString(TLL);
        }

    }




    void run() throws Exception {
        FileWriter fw = new FileWriter(out, false);
        fw.write("");
        fw.close();

        while(sc.hasNextLine()) {
            String l = sc.nextLine();
            resetBuffer();

            if(l.startsWith("$AMJ")) {
                init(l);
            }else if(l.startsWith("$END")) {
                printMemory();
                break;
            }else if(l.startsWith("$DTA")) {
                startExecution();
            }
            else {
                addToBuffer(l);
                loadProgram();
            }
        }
    }

    private void startExecution() throws IOException{
        IC = 0;
        executeUserProgram();
    }
    private void executeUserProgram() throws IOException{
        while(true) {

            if(SI != 0) {
                MOS();
                resetBuffer();
                SI = 0;
            }
            RA = addressMap(IC);
            IC++;
            IR = M[RA];
            String opCode = IR[0] + "" + IR[1];
            if(opCode.startsWith("H")) {
                SI = 3;
                MOS();
                break;
            }
            int operand = Integer.parseInt(IR[2] + "" + IR[3]);

            switch (opCode) {
                case "GD":
                    SI = 1;
                    // check for the memory
                    RA = addressMap(operand);
                    TLC+=2;
                    break;
                case "PD":
                    SI = 2;
                    RA = addressMap(operand);
                    TLC+=1;
                    break;
                case "SR":
                    RA = addressMap(operand);
                    for(int i = 0; i < 4; i++) {
                        M[RA][i] = R[i];
                    }
                    TLC+=1;
                    break;
                case "LR":
                    RA = addressMap(operand);
                    for(int i = 0; i < 4; i++) {
                        R[i] = M[RA][i];
                    }
                    TLC+=2;
                    break;
            }
        }
    }
    private void MOS() throws IOException {
        switch (SI) {
            case 1 -> {
                String l = sc.nextLine();
                addToBuffer(l);
                writeToMemoryBlock(buff,RA);
                System.out.println(RA);
            }

            case 2 -> {
                StringBuilder s = new StringBuilder();
                for (int i = RA; i < RA + 10; i++) {
                    for (int j = 0; j < 4; j++) {
                        s.append(M[i][j]);
                    }
                }
                FileWriter fw = new FileWriter(out, true);
                fw.write(s.toString()+'\n');
                fw.close();
                SI = 0;
            }

            case 3 -> {
                FileWriter fw = new FileWriter(out, true);
                fw.write("\n\n");
                fw.close();
                SI = 0;
            }
        }
    }

    // Inits
    PCB pcb = new PCB();
    void init(String l) {
        SI = 0;
        TI = 0;
        TLC = 0;
        LLC = 0;
        initializePCB(l);
        // Create Random number between 0-29
        PTR = allocate();
        flag[PTR/10] = 1;
        resetMemory();
        createPageTable();
        resetBuffer();
    }
    private void createPageTable() {
        for(int i = PTR; i < PTR + 10; i++) {
            for(int j = 0;j < 4;j++) {
                M[i][j] = '_';
            }
        }
    }
    private void initializePCB(String l) {
        int i,j;
        for(i = 4, j = 0; i < 8; i++, j++) {
            pcb.job[j] = l.charAt(i);
        }
        for(i=8,j=0;i<12;i++,j++)
        {
            pcb.TTL[j]=l.charAt(i);
        }
        for(i=12,j=0;i<16;i++,j++)
        {
            pcb.TLL[j]=l.charAt(i);
        }
    }

    // Resets
    private void resetBuffer() {
        for(int i = 0; i < 40; i++) {
            buff[i] = ' ';
        }
    }
    private void resetMemory() {
        for(int i = 0; i < 300; i++){
            for(int j = 0; j < 4; j++){
                M[i][j]=' ';
            }
        }
    }

    // Misc
    private int allocate() {
        return (int)(Math.floor(Math.random() * 30)) * 10;
    }
    private int addressMap(int va) {
        int PTE = PTR + va/10;
        StringBuilder s = new StringBuilder();
        int i;
        for(i = 0; i < 4; i++) {
            if(M[PTE][i] != '_') s.append(M[PTE][i]);
        }
        // valid Page fault
        if(s.toString().equals("")) {
            int rand = allocate();
            while(flag[rand/10] != 0) rand = allocate();
            String strRand = String.valueOf(rand/10);
            if(strRand.length() == 2) {
                M[PTE][2] = strRand.charAt(0);
                M[PTE][3] = strRand.charAt(1);
            }else {
                M[PTE][3] = strRand.charAt(0);
            }
            return rand + va%10;

        }
        int ra = Integer.parseInt(s.toString()) * 10 + va%10;
        System.out.println(ra);
        return ra;
    }
    private void addToBuffer(String l) {
        for(int i = 0; i < l.length(); i++) {
            buff[i] = l.charAt(i);
        }
    }
    private void loadProgram() {
        int rand = allocate();
        while(flag[rand/10] != 0) rand = allocate();
        flag[rand/10] = 1;
        // add the address to page table
        int i = 0;
        while(i < 10) {
            if(M[PTR + i][2] != '_' && M[PTR + i][3] != '_' ) {
                i++;
            }else {
                String strRand = String.valueOf(rand/10);
                if(strRand.length() == 2) {
                    M[PTR + i][2] = strRand.charAt(0);
                    M[PTR + i][3] = strRand.charAt(1);
                }else {
                    M[PTR+i][3] = strRand.charAt(0);
                }
                break;
            }
        }

        int m = 0;
        for(int j = rand; j < rand + 10; j++) {
            for(int k = 0; k < 4; k++) {
                M[j][k] = buff[m++];
            }
        }
    }
    private void printMemory() {
        int i, j;
        for(i = 0; i < 300; i++){
            System.out.print(i+" : ");
            for(j = 0; j < 4; j++){
                System.out.print(M[i][j]);
            }
            System.out.println();
        }
    }
    void writeToMemoryBlock(char[] buff, int block) {
        int k = 0;
        for(int i = block;i < block+10; i++) {
            for (int j = 0; j < 4; j++) {
                M[i][j] = buff[k];
                k++;
            }
        }
    }
}
