/*
 * ProvinceData.java
 *
 * Created on Jan 12, 2008, 12:19:12 PM
 */

package provconverter;

import eug.parser.EUGFileIO;
import eug.shared.GenericList;
import eug.shared.GenericObject;
import eug.shared.Style;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author Michael Myers
 */
public class ProvinceData {
    
    private final Map<Integer, Province> allProvs = new HashMap<>();
    
    private static final Pattern SEMICOLON = Pattern.compile(";");
    
    private String headerString
            = "Id;Name;PIW name;Religion;Culture;SizeModifier;Pashas;Climate;Ice?;Storm?;Galleys;Manpower;Income;Terrain;ToT-SPA;ToT-POR;HRE;Mine(1) ?;MineValue;Goods;Upgradable;CoT Historical Modifier;Difficulty for Colonization;Native Combat Strength;Ferocity;Efficiency of Natives in combat;Negotiation Value for Trading Posts;Natives Tolerance value ;City XPos;City YPos;Army XPos;ArmyYPos;PortXPos;Port YPos;Manufactory XPos; Manufactory YPos;Port/sea Adjacency;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Area;Region;Continent;;Extra River Desc Link 1;Extra River Desc Link 2;Extra River Desc Link 3;;Fill coord X;Fill coord Y;;;;;;;;;;;;;;;;";
    
    private final GenericObject mappings;
    private final GenericObject defaultMappings;
    private final GenericObject conversions;
    
    public ProvinceData(String filename, GenericObject config) {
        this.mappings = config.getChild("mappings");
        this.defaultMappings = config.getChild("defaults");
        this.conversions = config.getChild("convert");
        
        if (filename.endsWith(".csv"))
            loadCsv(filename);
        else
            loadTxt(filename);
    }

    private void loadCsv(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String currLine;
            // Id;Name;PIW name;Religion;Culture;SizeModifier;Pashas;Climate;Ice?;Storm?;Galleys;Manpower;Income;Terrain;ToT-SPA;ToT-POR;HRE;Mine(1) ?;MineValue;Goods;Upgradable;CoT Historical Modifier;Difficulty for Colonization;Native Combat Strength;Ferocity;Efficiency of Natives in combat;Negotiation Value for Trading Posts;Natives Tolerance value ;City XPos;City YPos;Army XPos;ArmyYPos;PortXPos;Port YPos;Manufactory XPos; Manufactory YPos;Port/sea Adjacency;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Area;Region;Continent;;Extra River Desc Link 1;Extra River Desc Link 2;Extra River Desc Link 3;;Fill coord X;Fill coord Y;;;;;;;;;;;;;;;;
            // 380;Flandern;coastal;reformed;dutch;0;0;3;0;0;0;6;17;0;0;0;clo;1;4;0;0;0;0;0;0;9505;1636;9548;1671;9475;1615;9502;1677;938;-100;-100;0;-100;-100;0;-100;-100;0;-100;-100;0;Low Countries;Western Europe;Europe;Antwerpen;0;0;0;1;9522;1649;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1
            
            int id = -1;
            
            headerString = br.readLine(); // eat first line but save for future use
            
            while ((currLine = br.readLine()) != null) {
                if (currLine.charAt(0) == '#')
                    continue;
                
                String[] args = SEMICOLON.split(currLine, -1);
                
                try {
                    String sid = args[0];
                    
                    if (sid.length() != 0) {
                        id = Integer.parseInt(sid);
                        
                        if (id >= 0) {
                            allProvs.put(id, new ProvinceCsv(args));
                        }
                    }
                } catch (RuntimeException e) {
                    Logger.getLogger(ProvinceData.class.getName()).log(Level.SEVERE, "Error with " + id, e);
                }
            }
        } catch (IOException e) {
            Logger.getLogger(ProvinceData.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    private void loadTxt(String filename) {
        GenericObject provincesObj = EUGFileIO.load(filename);
        
        for (GenericObject prov : provincesObj.getChildren("province")) {
            String idStr = prov.getString("id");
            int id = Integer.parseInt(idStr);
            allProvs.put(id, new ProvinceTxt(prov));
        }
    }
    
    public void saveCsv(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            
            if (headerString != null) {
                writer.write(headerString);
                writer.newLine();
            }

            boolean writtenInvalid = false;
            int maxProv = allProvs.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
            int numMappings = mappings.values.stream().mapToInt(v -> Integer.parseInt(v.varname)).max().getAsInt();
            for (int i = 0; i <= maxProv; i++) {
                Province p = getProvince(i);
                if (p == null) {
                    if (!writtenInvalid) {
                        writer.write("-1;Filler;inland;exotic;none;-1;-1;-1;-1;-1;0;-1;-1;9;0;0;0;0;-1;nothing;0;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;0;0;0;0;0;0;0;0;0;0;0;0;0;;;;#N/A;0;0;0;0;0;0");
                        writtenInvalid = true;
                    } else {
                        writer.write(";;;;;;;;;;;;;;0;0;0;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;");
                    }
                } else {
                    String[] entry = new String[numMappings];
                    convertTxtProvince((ProvinceTxt)p, entry);
                    
                    for (int col = 0; col < entry.length-1; col++) {
                        writer.write(entry[col]);
                        writer.write(';');
                    }
                    writer.write(entry[entry.length-1]);
                }
                writer.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(ProvinceData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void saveTxt(String filename) {
        // save CSV entries as generic objects using the mappings
        GenericObject root = new GenericObject();
        
        int maxProv = allProvs.keySet().stream().max(Comparator.naturalOrder()).get();
        for (int i = 0; i <= maxProv; i++) {
            Province p = getProvince(i);
            if (p != null) {
                GenericObject provObj = root.createChild("province");
                convertCsvProvince((ProvinceCsv) p, provObj);
            }
        }
        
        EUGFileIO.save(root, filename, EUGFileIO.NO_COMMENT, true, FTG_PROVINCES_STYLE);
    }
    
    private void convertCsvProvince(ProvinceCsv p, GenericObject provObj) {
        for (int i = 0; i < p.entry.length; i++) {
            if (p.entry[i].equals("#N/A") || p.entry[i].equals("-") || p.entry[i].equals("-100"))
                continue;
            String colStr = Integer.toString(i);
            String mapping = mappings.getString(colStr);
            if (!mapping.isEmpty() && !mapping.equals("unused")) {
                String csvVal = p.entry[i];
                
                if (conversions.containsChild(colStr)) {
                    GenericObject convert = conversions.getChild(colStr);
                    if (convert.contains(csvVal))
                        csvVal = convert.getString(csvVal);
                }
                
                if (!csvVal.equals("0") && !csvVal.equals("no"))
                    normalizeAndAdd(provObj, mapping, csvVal);
            }
        }
        for (String unusedMapping : mappings.getStrings("-1")) {
            normalizeAndAdd(provObj, unusedMapping, null);
        }
    }
    
    private void normalizeAndAdd(GenericObject parent, String path, String value) {
        String[] s = path.split("\\.");
        GenericObject curr = parent;
        
        // iterate until we have only the last piece left
        for (int i = 0; i < s.length - 1; ++i) {
            if (s[i+1].equals("list")) {
                // if the next piece is "list", create a list instead of an object
                // then add the value to the list and we're done
                GenericList tmp = curr.getList(s[i]);
                if (tmp == null)
                    tmp = curr.createList(s[i]);
                if (value != null)
                    tmp.add(value, false);
                return;
            } else {
                GenericObject tmp = curr.getChild(s[i]);
                if (tmp == null) {
                    tmp = curr.createChild(s[i]);
                }
                curr = tmp;
            }
        }
        
        if (value != null)
            curr.setString(s[s.length-1], value);
    }
    
    private void convertTxtProvince(ProvinceTxt p, String[] entry) {
        for (int i = 0; i < entry.length; i++) {
            String colStr = Integer.toString(i);
            String mapping = mappings.getString(colStr);
            if (!mapping.isEmpty() && !mapping.equals("unused")) {
                entry[i] = normalizeAndGet(p.provObj, mapping);
            } 
            if (entry[i] == null || entry[i].isEmpty()) {
                entry[i] = defaultMappings.getString(colStr); // will be "" if no default exists
            } else if (conversions.containsChild(colStr)) {
                GenericObject convert = conversions.getChild(colStr);
                if (convert.contains(entry[i])) {
                    entry[i] = convert.getString(entry[i]);
                }
            }
        }
    }
    
    
    private String normalizeAndGet(GenericObject parent, String path) {
        String[] s = path.split("\\.");
        GenericObject curr = parent;
        
        // iterate until we have only the last piece left
        for (int i = 0; i < s.length - 1; ++i) {
            if (s[i+1].equals("list")) {
                // if the next piece is "list", create a list instead of an object
                // then add the value to the list and we're done
                GenericList tmp = curr.getList(s[i]);
                if (tmp == null)
                    return "";
                
                int index = Integer.parseInt(s[i+2]); // TODO: will crash if mapping doesn't have index
                return tmp.get(index);
            } else {
                GenericObject tmp = curr.getChild(s[i]);
                if (tmp == null) {
                    tmp = curr.createChild(s[i]);
                }
                curr = tmp;
            }
        }
        
        return curr.getString(s[s.length-1]);
    }
    
    public Province getProvince(int id) {
        return allProvs.get(id);
    }
    
    
    
    public interface Province {
        void writeOut(BufferedWriter out) throws IOException;
    }
    
    private static class ProvinceCsv implements Province {
        private final String[] entry;
        
        private ProvinceCsv(String[] entry) {
            this.entry = entry;
        }
        
        @Override
        public void writeOut(BufferedWriter out) throws IOException {
            for (int i = 0; i < entry.length-1; i++) {
                out.write(entry[i]);
                out.write(';');
            }
            out.write(entry[entry.length-1]);
        }
    }
    
    private static final class ProvinceTxt implements Province {
        private final GenericObject provObj;
        
        ProvinceTxt(GenericObject obj) {
            this.provObj = obj;
        }

        @Override
        public void writeOut(BufferedWriter out) throws IOException {
            provObj.toFileString(out, Style.AGCEEP);
        }
    }
    
    // same as Style.AGCEEP except objects are never inline if they have children
    private static final Style FTG_PROVINCES_STYLE = new Style() {
        @Override
        public String getTab(int depth) {
            return Style.AGCEEP.getTab(depth);
        }

        @Override
        public String getEqualsSign(int depth) {
            return Style.AGCEEP.getEqualsSign(depth);
        }

        @Override
        public String getCommentStart() {
            return Style.AGCEEP.getCommentStart();
        }

        @Override
        public void printTab(BufferedWriter bw, int depth) throws IOException {
            Style.AGCEEP.printTab(bw, depth);
        }

        @Override
        public void printEqualsSign(BufferedWriter bw, int depth) throws IOException {
            Style.AGCEEP.printEqualsSign(bw, depth);
        }

        @Override
        public void printOpeningBrace(BufferedWriter bw, int depth) throws IOException {
            Style.AGCEEP.printOpeningBrace(bw, depth);
        }

        @Override
        public void printCommentStart(BufferedWriter bw, int depth) throws IOException {
            Style.AGCEEP.printCommentStart(bw, depth);
        }

        @Override
        public void printHeaderCommentStart(BufferedWriter bw, int depth) throws IOException {
            Style.AGCEEP.printHeaderCommentStart(bw, depth);
        }

        @Override
        public void printHeaderCommentEnd(BufferedWriter bw, int depth) throws IOException {
            Style.AGCEEP.printHeaderCommentEnd(bw, depth);
        }

        @Override
        public boolean isInline(GenericObject obj) {
            return Style.AGCEEP.isInline(obj) && obj.children.isEmpty() && obj.lists.isEmpty();
        }

        @Override
        public boolean isInline(GenericList list) {
            return Style.AGCEEP.isInline(list);
        }

        @Override
        public boolean newLineAfterObject() {
            return Style.AGCEEP.newLineAfterObject();
        }
    };
}
