package pairHMM;

import static pairHMM.MainLoadDatasetAndCompareCustom.accuracy_level;

public class Utils {
    private static String format = "#.";

    public static void setAccuracyFormat() {
        for (int i = 0; i < accuracy_level; i++) {
            format += "#";
        }
        printAccuracyFormat();
    }

    public static String getAccuracyFormat() {
        return format;
    }

    private static void printAccuracyFormat() {
        System.out.println("Checking result with accuracy: 10^-" + accuracy_level + ", Accuracy format: " + format);
    }
}
