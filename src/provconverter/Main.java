
package provconverter;

import eug.parser.EUGFileIO;
import eug.shared.GenericObject;
import java.io.File;
import javax.swing.JFileChooser;

/**
 *
 * @author Michael
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        GenericObject config = EUGFileIO.load("mapping.txt");
        GenericObject mappings = config.getChild("mappings");
        
//        JFileChooser chooser = new JFileChooser();
//        int result = chooser.showDialog(null, null);
//        if (result == JFileChooser.APPROVE_OPTION) {
//            File f = chooser.getSelectedFile();
//        }

        ProvinceData data = new ProvinceData("eu2_province.csv", mappings);
        data.saveTxt("eu2_provinces.txt");

    }
    
}
