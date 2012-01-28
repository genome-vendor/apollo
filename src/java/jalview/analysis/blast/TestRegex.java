//
//  TestRegex.java
//  
//
//  Created by Michele Clamp on Thu Jan 23 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//
package jalview.analysis.blast;

import com.stevesoft.pat.*;
import java.io.*;


public class TestRegex {


    public static void main(String[] args) {
        try {
            File file = new File(args[0]);
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;

            Regex r = new Regex("Score =\\s+(\\S+) bits \\((\\d+)\\)");


            while ((line = reader.readLine()) != null) {
                System.out.println("Line is " + line);
                if (r.search(line)) {
                    System.out.println("Found match");

                    for (int i = 1; i <= r.numSubs(); i++) {
                        System.out.println("Sub " + i + " " + r.stringMatched(i));
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("IOException " + e);
        }
    }    
}
