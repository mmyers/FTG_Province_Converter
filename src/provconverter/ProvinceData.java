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
    
    public static final int NUM_PROVINCES = 2020;
    
    private final Map<Integer, Province> allProvs = new HashMap<>();
//    private final List<Province> extras = new ArrayList<Province>();
    
    private static final Pattern SEMICOLON = Pattern.compile(";");
    
    private String headerString;
    
    private GenericObject provincesObj; // if FTG-style provinces.txt rather than CSV
    
    private final GenericObject mappings;
    
    public ProvinceData(String filename, GenericObject mappings) {
        this.mappings = mappings;
        
        if (filename.endsWith(".csv"))
            loadCsv(filename);
        else
            loadTxt(filename);
    }

    private void loadCsv(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String currLine;
            // Id;Name;PIW name;Religion;Culture;SizeModifier;Pashas;Climate;Ice?;Storm?;Galleys;Manpower;Income;Terrain;Mine(1) ?;MineValue;Goods;Upgradable;CoT Historical Modifier;Difficulty for Colonization;Native Combat Strength;Ferocity;Efficiency of Natives in combat;Negotiation Value for Trading Posts;Natives Tolerance value ;City XPos;City YPos;Army XPos;ArmyYPos;PortXPos;Port YPos;Manufactory XPos; Manufactory YPos;Port/sea Adjacency;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Terrain x;Terrain Y;Terrain variant;Area;Region;Continent;;Extra River Desc Link 1;Extra River Desc Link 2;Extra River Desc Link 3;;Fill coord X;Fill coord Y;;;;;;;;;;;;;;;;
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
                        } /*else {
                            extras.add(new Province(args));
                        }*/
                    } /*else {
                        extras.add(new Province(args));
                    }*/
                } catch (RuntimeException e) {
                    System.err.print("Error with "+id+": ");
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadTxt(String filename) {
        provincesObj = EUGFileIO.load(filename);
        
        for (GenericObject prov : provincesObj.getChildren("province")) {
            String idStr = prov.getString("id");
            int id = Integer.parseInt(idStr);
            allProvs.put(id, new ProvinceTxt(prov));
        }
    }
    
    public void saveCsv(String filename) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            
            if (headerString != null) {
                writer.write(headerString);
                writer.newLine();
            }

            boolean writtenInvalid = false;
            int maxProv = allProvs.keySet().stream().max(Comparator.naturalOrder()).get();
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
                    p.writeOut(writer);
                }
                writer.newLine();
            }
        } catch (IOException ex) {
            Logger.getLogger(ProvinceData.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException ex) {
                Logger.getLogger(ProvinceData.class.getName()).log(Level.SEVERE, null, ex);
            }
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
        
        EUGFileIO.save(root, filename, EUGFileIO.NO_COMMENT, true, Style.AGCEEP);
    }
    
    private void convertCsvProvince(ProvinceCsv p, GenericObject provObj) {
        for (int i = 0; i < p.entry.length; i++) {
            if (p.entry[i].equals("#N/A") || p.entry[i].equals("-") || p.entry[i].equals("0") || p.entry[i].equals("-100"))
                continue;
            String mapping = mappings.getString(Integer.toString(i));
            if (!mapping.isEmpty() && !mapping.equals("unused")) {
                normalizeAndAdd(provObj, mapping, p.entry[i]);
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
    
    public Province getProvince(int id) {
        return allProvs.get(id);
    }
    
    public boolean isPTI(int id) {
        final Province p = getProvince(id);
        return p == null;
    }
    
    public String getName(int id) {
        final Province p = getProvince(id);
        if (p == null)
            return "Terra Incognita";
        
        return p.getName();
    }
    
    public interface Province {
        
        String getName();
        
        void writeOut(BufferedWriter out) throws IOException;
    }
    
    private static class ProvinceCsv implements Province {
        private final String[] entry;
        
        public static final int NAME_IDX = 1;
        public static final int TERRAIN_IDX = 13;
        // NOTE: Entries 14, 15, and 16 were added in 1.09
        public static final int CITY_IDX = 28;
        public static final int ARMY_IDX = 30;
        public static final int PORT_IDX = 32;
        public static final int MANU_IDX = 34;
        public static final int TERRAIN_1_IDX = 37;
        public static final int TERRAIN_1_TYPE_IDX = 39;
        public static final int TERRAIN_2_IDX = 40;
        public static final int TERRAIN_2_TYPE_IDX = 42;
        public static final int TERRAIN_3_IDX = 43;
        public static final int TERRAIN_3_TYPE_IDX = 45;
        public static final int TERRAIN_4_IDX = 46;
        public static final int TERRAIN_4_TYPE_IDX = 48;
        
        private ProvinceCsv(String[] entry) {
            this.entry = entry;
        }
        
        public String getString(int idx) {
            return entry[idx];
        }
        
        public int getInt(int idx) {
            return Integer.parseInt(entry[idx]);
        }
        
        @Override
        public String getName() {
            return entry[NAME_IDX];
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
        private final String name;
        
        ProvinceTxt(GenericObject obj) {
            this.provObj = obj;
            this.name = obj.getString("name");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void writeOut(BufferedWriter out) throws IOException {
            provObj.toFileString(out, Style.AGCEEP);
        }
        
    }
}
