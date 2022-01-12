package custom;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Dataset {

    private String name;

    private ArrayList<String> utils = new ArrayList<>();
    private ArrayList<char[]> dataset = new ArrayList<>();

    private ArrayList<ArrayList<char[]>> reads = new ArrayList<>();
    private ArrayList<ArrayList<char[]>> quals = new ArrayList<>();
    private ArrayList<ArrayList<char[]>> ins = new ArrayList<>();
    private ArrayList<ArrayList<char[]>> dels = new ArrayList<>();
    private ArrayList<ArrayList<char[]>> gcps = new ArrayList<>();
    private ArrayList<ArrayList<char[]>> alleles = new ArrayList<>();

    private enum Type {
        Reads,
        Quals,
        Ins,
        Dels,
        Gcps,
        Alleles
    }

    public Dataset() {
    }

    public static void main(String[] args) {
        String datasetName = "test_data\\10s.in";
        Dataset dataset = new Dataset();
        dataset.readDataset(datasetName);
        dataset.printDataset(dataset.dataset);
        dataset.printSubDataset(Type.Reads);
        dataset.printUtilsDetails();

    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getUtils() {
        return utils;
    }

    public ArrayList<char[]> getDataset() {
        return dataset;
    }

    public ArrayList<ArrayList<char[]>> getReads() {
        return reads;
    }

    public ArrayList<ArrayList<char[]>> getQuals() {
        return quals;
    }

    public ArrayList<ArrayList<char[]>> getIns() {
        return ins;
    }

    public ArrayList<ArrayList<char[]>> getDels() {
        return dels;
    }

    public ArrayList<ArrayList<char[]>> getGcps() {
        return gcps;
    }

    public ArrayList<ArrayList<char[]>> getAlleles() {
        return alleles;
    }

    public void readDataset(String datasetName) {
        this.name = datasetName;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(name));
            String line = reader.readLine();

            while (line != null) {
                this.utils.add(line);
                dataset.add(line.toCharArray());
                List<Integer> values = Arrays.stream(line.split(" ")).map(Integer::parseInt).collect(Collectors.toList());

                ArrayList<char[]> r = new ArrayList<>();
                ArrayList<char[]> q = new ArrayList<>();
                ArrayList<char[]> i = new ArrayList<>();
                ArrayList<char[]> d = new ArrayList<>();
                ArrayList<char[]> g = new ArrayList<>();
                ArrayList<char[]> a = new ArrayList<>();

                for (Integer value : values) {
                    for (int j = 0; j < value; j++) {
                        line = reader.readLine();
                        dataset.add(line.toCharArray());
                        String[] strings = line.split(" ");
                        if (strings.length == 1)
                            a.add(strings[0].toCharArray());
                        else {
                            r.add(strings[0].toCharArray());
                            q.add(strings[1].toCharArray());
                            i.add(strings[2].toCharArray());
                            d.add(strings[3].toCharArray());
                            g.add(strings[4].toCharArray());
                        }
                    }
                }

                this.reads.add(r);
                this.quals.add(q);
                this.ins.add(i);
                this.dels.add(d);
                this.gcps.add(g);
                this.alleles.add(a);

                line = reader.readLine();
            }
        } catch (Exception e) {
            System.out.println("ERROR WHILE READING FILE \n");
        }
    }

    public void printDataset(ArrayList<char[]> dataset) {
        for (char[] line : dataset) {
            String output = "";
            for (char ch : line) {
                output = output + ch;
            }
            System.out.println(output + "\n");
        }
    }

    public void printUtilsDetails() {
        for (String string : this.utils) {
            System.out.println(string + "\n");
        }
    }

    public void printSubDataset(Type type) {
        switch (type) {
            case Reads -> subPrintSubDataset(this.reads);
            case Quals -> subPrintSubDataset(this.quals);
            case Ins -> subPrintSubDataset(this.ins);
            case Dels -> subPrintSubDataset(this.dels);
            case Gcps -> subPrintSubDataset(this.gcps);
            case Alleles -> subPrintSubDataset(this.alleles);
        }
    }

    private void subPrintSubDataset(ArrayList<ArrayList<char[]>> subDataset) {
        for (ArrayList<char[]> arrayList : subDataset) {
            for (char[] charArray : arrayList) {
                String output = "";
                for (char ch : charArray) {
                    output = output + ch;
                }
                System.out.println(output + "\n");
            }
        }
    }

}