package main.app;

import main.pojo.Sentence;
import main.proxy.ISentence;

public class StressTest {

    private static final String JON = "IRC";
    private static final int NUMBER_OF_OPERATIONS = 10000;

    public static void main(String[] args) throws InterruptedException {
        final ISentence sentence = Sentence.newSharedInstance(JON);
        final String initVal = sentence.read();
        if (initVal.isEmpty()) sentence.write("0");

        for (int j = 0; j < NUMBER_OF_OPERATIONS; j++) {
            String val = sentence.read();
            System.out.println("Value at " + j + " : " + val);
            sentence.write(""+(Integer.valueOf(val) + 1));
            Thread.sleep(50);
        }
    }
}
