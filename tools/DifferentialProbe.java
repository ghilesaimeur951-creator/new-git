package com.ghiles.quizubuntu;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class DifferentialProbe {
    static String decode(String s) { return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8); }
    static String encode(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    public static void main(String[] args) throws Exception {
        BufferedReader reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
        String line;
        while((line=reader.readLine())!=null) {
            String[] parts=line.split("\t",-1);
            VirtualMachine vm=CatalogAuditProbe.fixture();
            vm.execute(decode(parts[0]));
            VirtualMachine.Result r=vm.execute(decode(parts[1]));
            StringBuilder out=new StringBuilder(r.stdout);
            for(VirtualMachine.FsEntry e:r.entries) out.append(e.name).append('\n');
            System.out.println(r.exitCode+"\t"+r.kind+"\t"+encode(out.toString()));
        }
    }
}
