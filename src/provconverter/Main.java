
package provconverter;

import eug.parser.EUGFileIO;
import eug.shared.GenericObject;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 *
 * @author Michael
 */
public class Main {
    
    private static String inputFilename;
    private static String outputFilename;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        GenericObject config = EUGFileIO.load("mapping.txt");
        if (config == null) {
            JOptionPane.showMessageDialog(null, "Failed to load mappings from mapping.txt.", "Fatal error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        handleArgs(args);
        
        if (inputFilename == null || !(new File(inputFilename).exists())) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select source file (province.csv or provinces.txt)");
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                inputFilename = f.getAbsolutePath();
            } else {
                return;
            }
        }
        
        if (outputFilename == null || !(new File(outputFilename).exists())) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select output file (provinces.txt or province.csv)");
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                outputFilename = f.getAbsolutePath();
            } else {
                return;
            }
        }

        System.out.println("Loading provinces...");
        ProvinceData data = new ProvinceData(inputFilename, config);
        
        System.out.println("Saving converted provinces...");
        if (inputFilename.endsWith(".csv"))
            data.saveTxt(outputFilename);
        else
            data.saveCsv(outputFilename);
        System.out.println("Done.");
    }

    private static void handleArgs(final String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (equals(arg, "-h", "--help")) {
                printHelp();
            } else if (arg.equals("-input")) {
                inputFilename = stripQuotes(args[++i]);
            } else if (arg.equals("-output")) {
                outputFilename = stripQuotes(args[++i]);
            } else {
                System.err.println("Not a valid option: " + arg);
                printHelp();
            }
        }
    }
    
    private static boolean equals(String arg, String shortArg, String longArg) {
        return (arg.equals(shortArg) || arg.equalsIgnoreCase(longArg));
    }
    
    private static String stripQuotes(String str) {
        if (str.startsWith("\"")) {
            str = str.substring(1);
        }
        if (str.endsWith("\"")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }
    
    private static void printHelp() {
        System.out.println("Usage: java -jar FTG_Province_Converter.jar [-args...]");
        System.out.println();
        System.out.println("where args include:");
        System.out.println("    -input <filename>");
        System.out.println("        The name of the input file to be converted.");
        System.out.println("    -output <filename>");
        System.out.println("        The name of the file where the converted output should be saved.");
        System.out.println("    -h | --help");
        System.out.println("        Print this help.");
        System.out.println();
        System.out.println("Note that if either -input or -output is not present, a file chooser will be shown.");
        System.out.println();
        System.out.println("If the input file ends with .csv, the output file will be assumed to be .txt, and vice versa.");
        System.out.println();
        System.out.println("Example:");
        System.out.println("java -jar FTG_Province_Converter.jar -input \"D:\\games\\mynewmapmod\\db\\map\\provinces.txt\" -output \"D:\\games\\mynewmapmod\\db\\map\\province.csv\"");
    }
    
    private Main() { }
}
