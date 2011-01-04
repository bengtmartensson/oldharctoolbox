/*
Copyright (C) 2009 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package harc;

import java.io.*;

/**
 * This class parses files created by the program <a href="http://www.piclist.com/images/boards/irwidget/index.htm">IRScope</a> by Kevin Timmerman.
 */
public class ict_parse extends raw_ir {

    public ict_parse(BufferedReader reader) throws IOException {
        super();
        int[] data = null;
        String line = "";
        int index = 0;
        while ((line = reader.readLine()) != null) {
            String[] args = line.split("[ ,]");
            if (args[0].equals("carrier_frequency"))
                set_frequency(Integer.parseInt(args[1]));
            else if (args[0].equals("sample_count"))
                data = new int[Integer.parseInt(args[1])];
            else if (args[0].startsWith("+"))
                data[index++] = Integer.parseInt(args[1]);
            else if (args[0].startsWith("-"))
                data[index++] = (int) (((double) Integer.parseInt(args[0].substring(1)) * this.get_frequency()) / 1000000.0 + 0.5);
        }
        this.set_intro_sequence(data);
    }

    public ict_parse(String filename) throws FileNotFoundException, IOException {
        this(new BufferedReader(new InputStreamReader(new FileInputStream(filename))));
    }
    
    public ict_parse(File file) throws FileNotFoundException, IOException {
        this(new BufferedReader(new InputStreamReader(new FileInputStream(file))));
    }

    public static void main(String[] args) {
        try {
            ict_parse ip = new ict_parse(args[0]);
            System.out.println(ip.ccf_string());
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
