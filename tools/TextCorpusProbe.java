package com.ghiles.quizubuntu;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class TextCorpusProbe {
    public static void main(String[] args) throws Exception {
        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));String line;
        while((line=reader.readLine())!=null) {
            String[] fields=line.split("\t",-1);
            String command=new String(Base64.getDecoder().decode(fields[0]),StandardCharsets.UTF_8);
            String data=new String(Base64.getDecoder().decode(fields[1]),StandardCharsets.UTF_8);
            VirtualMachine vm=new VirtualMachine();vm.saveEditedFile("/home/ubuntu/README.md",data);
            VirtualMachine.Result r=vm.execute(command);
            System.out.println(r.exitCode+"\t"+Base64.getEncoder().encodeToString(r.text.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
