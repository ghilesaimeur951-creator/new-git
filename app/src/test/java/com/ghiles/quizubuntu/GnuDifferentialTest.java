package com.ghiles.quizubuntu;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;
import static org.junit.Assert.*;
/** Golden expectations generated independently by GNU utilities, not by the simulator. */
public class GnuDifferentialTest {
    private String decode(String encoded) { return new String(Base64.getDecoder().decode(encoded),StandardCharsets.UTF_8); }
    @Test public void compareAllTextVariantsAcrossSixIndependentFixtures() throws Exception {
        InputStream resource=getClass().getResourceAsStream("/gnu-text-corpus.tsv");
        assertNotNull("GNU oracle fixtures missing",resource);
        int count=0;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(resource,StandardCharsets.UTF_8))) {
            String line;
            while((line=reader.readLine())!=null) {
                String[] values=line.split("\t",-1);
                VirtualMachine vm=new VirtualMachine();vm.saveEditedFile("/home/ubuntu/README.md",decode(values[1]));
                String command=decode(values[0]);VirtualMachine.Result result=vm.execute(command);
                assertEquals("exit code: "+command+" fixture "+count,Integer.parseInt(values[2]),result.exitCode);
                assertEquals("stdout: "+command+" fixture "+count,decode(values[3]),result.stdout);
                count++;
            }
        }
        assertEquals(5904,count);
        System.out.println("GNU differential comparisons passed: "+count);
    }
}
