package pairHMM.newGPU;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DatasetMod extends Dataset {

    public static void main(String[] args) {
        String datasetName = "test_data\\10s.in";
        DatasetMod datasetMod = new DatasetMod();
        datasetMod.readDataset(datasetName);
        datasetMod.printSubDataset(Type.Reads);

        PreprocessingMod prep = new PreprocessingMod(datasetMod);

        prep.printLinearObject(prep.getReads(), "Reads", prep.getPaddedReadLength(), 2);
        prep.printLinearObject(prep.getAlleles(), "Alleles", prep.getPaddedAlleleLength(), 2);

    }

    public DatasetMod() {
        super();
    }

    @Override
    public void readDataset(String datasetName) {
        this.name = datasetName;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(name));
            String line = reader.readLine();
            int s = 0;

            while (line != null) {
                this.utils.add(line);
                dataset.add(line.toCharArray());
                List<Integer> values = Arrays.stream(line.split(" ")).map(Integer::parseInt).collect(Collectors.toList());
                int rr = values.get(0);
                int aa = values.get(1);

                ArrayList<char[]> r = new ArrayList<>();
                ArrayList<char[]> q = new ArrayList<>();
                ArrayList<char[]> i = new ArrayList<>();
                ArrayList<char[]> d = new ArrayList<>();
                ArrayList<char[]> g = new ArrayList<>();
                ArrayList<char[]> a = new ArrayList<>();

                for (int j = 0; j < rr; j++) {
                    line = reader.readLine();
                    dataset.add(line.toCharArray());
                    String[] strings = line.split(" ");

                    r.add(strings[0].toCharArray());
                    q.add(strings[1].toCharArray());
                    i.add(strings[2].toCharArray());
                    d.add(strings[3].toCharArray());
                    g.add(strings[4].toCharArray());

                }

                for (int j = 0; j < aa; j++) {
                    line = reader.readLine();
                    dataset.add(line.toCharArray());
                    String[] strings = line.split(" ");

                    for (int k = 0; k < rr; k++) {
                        a.add(strings[0].toCharArray());
                        s++;
                    }

                    this.reads.add(r);
                    this.quals.add(q);
                    this.ins.add(i);
                    this.dels.add(d);
                    this.gcps.add(g);

                }

                this.alleles.add(a);

                line = reader.readLine();
            }
            samples = s;
        } catch (Exception e) {
            System.out.println("ERROR WHILE READING FILE \n");
        }
    }

}
