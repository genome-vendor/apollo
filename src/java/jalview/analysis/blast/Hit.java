package jalview.analysis.blast;

//
//  Hit.java
//
//
//  Created by Michele Clamp on Thu Jan 23 2003.
//  Almost a direct copy of Ian Korf's perl BPlite parser
//  combined with the alignment parsing of the ensembl pipeline.

import jalview.datamodel.*;
import com.stevesoft.pat.*;
import java.io.*;
import java.util.*;
import apollo.datamodel.SeqFeature;

public class Hit {

    static Regex reg_score       = new Regex("Score =\\s+(\\S+) bits \\((\\d+)");
    static Regex reg_identities  = new Regex("Identities = (\\d+)\\/(\\d+)");
    static Regex reg_positives   = new Regex("Positives = (\\d+)");
    static Regex reg_pvalue      = new Regex("Expect(\\(\\d+\\))? =\\s+(\\S+)");
    static Regex reg_warn        = new Regex("^WARNING: |^NOTE:");
    static Regex reg_strand_hsp  = new Regex("Strand HSP");
    static Regex reg_strand      = new Regex("^\\s*Strand");
    static Regex reg_score2      = new Regex("^\\s*Score");
    static Regex reg_end_data    = new Regex("^>|^Parameters|^\\s+Database:|^CPU\\stime");
    static Regex reg_null        = new Regex("\\S");
    static Regex reg_frame       = new Regex("^\\s*Frame\\s*=\\s*([\\-\\+])(\\d+)");
    static Regex reg_query       = new Regex("^Query:\\s+(\\d+)\\s*([\\D\\S]+)\\s+(\\d+)");
    static Regex reg_hit         = new Regex("^Sbjct:\\s+(\\d+)\\s*([\\D\\S]+)\\s+(\\d+)");

    String name;
    String hitName;

    int length;
    int qinc;
    int hinc;
    int qtype;
    int htype;

    BufferedReader reader;
    String last_line;
    BPLite parent;

    boolean all_parsed = false;

    public Hit(BufferedReader reader) {
        this.reader = reader;
    }

    public HSP nextHSP() throws IOException {
        if (all_parsed) {
            return null;
        }

        String score_line = last_line;
        String next_line  = reader.readLine();

//        System.out.println("Line is " + next_line + "\n");

        if (next_line == null) {
            return null;
        }

        score_line = score_line + next_line;

        double score     = -1.0;
        double bits      = -1.0;
        int    positives = -1;
        int    pid       = -1;
        int    match     = -1;
        int    length    = -1;
        int    frame     = -1;
        double pvalue    = -1.0;

        Vector hsplines = new Vector();

        if (reg_score.search(score_line)) {
            score = Double.parseDouble(reg_score.stringMatched(1));
            bits  = Double.parseDouble(reg_score.stringMatched(2));
        }

        if (reg_identities.search(score_line)) {
            match = Integer.parseInt(reg_identities.stringMatched(1));
            length = Integer.parseInt(reg_identities.stringMatched(2));
            pid = match*100/length;
        }

        if (reg_positives.search(score_line)) {
            positives = Integer.parseInt(reg_positives.stringMatched(1));
        }

        if (reg_pvalue.search(score_line)) {
            String tmp = reg_pvalue.stringMatched(2);

            if (tmp.indexOf("e") == 0)  {
                tmp = "1" + tmp;
            }
            pvalue = Double.parseDouble(tmp);
            //System.out.println("Pvalue " + pvalue);
        }

        //System.out.println("score/bits/match/length/positives/pvalue" + score + " | " + bits + " | " + match + " | " + length + " |  " + positives + " | " + pvalue);
outer:

        while ((next_line = reader.readLine()) != null) {
//          System.out.println("Next " + next_line);
            if (reg_warn.search(next_line)) {
                while ((next_line = reader.readLine()) != null) {
                    if (!reg_null.search(next_line)) {
                        break outer;
                    }
                }
            } else if (! reg_null.search(next_line)) {
                continue;
            } else if (reg_strand_hsp.search(next_line)) {
                continue;
            } else if (reg_strand.search(next_line)) {
                continue;
            } else if (reg_score2.search(next_line)) {
                last_line = next_line;
                parent.setLastLine(last_line);
                break outer;
            } else if (reg_end_data.search(next_line)) {
                last_line = next_line;
                parent.setLastLine(last_line);
                all_parsed = true;
                break outer;
            } else if (reg_frame.search(next_line)) {

                String sign = reg_frame.stringMatched(1);
                frame = Integer.parseInt(reg_frame.stringMatched(2));

                if (sign.equals("-")) {
                    frame = frame*-1;
                }

            } else {
                hsplines.addElement(next_line);
                hsplines.addElement(reader.readLine());
                hsplines.addElement(reader.readLine());

            }
        }

        String  query_line = "";
        String  hit_line   = "";
        String  align_line = "";

        int query_start = -1;
        int query_end   = -1;
        int hit_start   = -1;
        int hit_end     = -1;

        String query_seq = "";


        Vector query_lines = new Vector();
        Vector align_lines = new Vector();
        Vector hit_lines   = new Vector();

        for (int i = 0; i < hsplines.size(); i+=3) {
            String line1 = (String)hsplines.elementAt(i);
            String line2 = (String)hsplines.elementAt(i+1);
            String line3 = (String)hsplines.elementAt(i+2);

            if (reg_query.search(line1)) {
                query_line = query_line + reg_query.stringMatched(2);
                query_seq   = reg_query.stringMatched(2);
                if (query_start == -1) {
                    query_start = Integer.parseInt(reg_query.stringMatched(1));
                }

                query_end = Integer.parseInt(reg_query.stringMatched(3));
            }

            int offset = line1.indexOf(query_seq);
            //System.out.println("line1 " + line1);
            //System.out.println("Query " + query_seq);

            //System.out.println("Offset " + offset + " " + query_seq.length() + " " + query_seq);
            align_line = align_line + line2.substring(offset,offset + query_seq.length());

            if (reg_hit.search(line3)) {
                hit_line = hit_line + reg_hit.stringMatched(2);

                if (hit_start == -1) {
                    hit_start = Integer.parseInt(reg_hit.stringMatched(1));
                }
                hit_end = Integer.parseInt(reg_hit.stringMatched(3));
            }

            query_lines.addElement(query_line);
            align_lines.addElement(align_line);
            hit_lines.addElement(hit_line);
        }

        HSP hsp = new HSP(new SeqFeature(),new SeqFeature());

        int qstrand = 1;
        int hstrand = 1;

        if (query_end < query_start) {
          qstrand = -1;
          int tmp = query_start;
          query_start = query_end;
          query_end   = tmp;
        }

        if (hit_end < hit_start) {
          hstrand = -1;
          int tmp = hit_start;
          hit_start = hit_end;
          hit_end   = tmp;
        }

        System.out.println("strands " + qstrand + " " + hstrand);

        // Types - from the program

        if (parent.getProgram().equals("BLASTP") ||
            parent.getProgram().equals("WUBLASTP")) {

          qtype = HSP.PEP;
          htype = HSP.PEP;

          qinc = 1*qstrand;
          hinc = 1*hstrand;

        } else if (parent.getProgram().equals("BLASTN") ||
                   parent.getProgram().equals("WUBLASTN")) {
          qtype = HSP.DNA;
          htype = HSP.DNA;

          qinc = 1*qstrand;
          hinc = 1*hstrand;

        } else if (parent.getProgram().equals("BLASTX") ||
                   parent.getProgram().equals("WUBLASTX")) {
          qtype = HSP.DNA;
          htype = HSP.PEP;

          qinc = 3*qstrand;
          hinc = 1*hstrand;

        } else if (parent.getProgram().equals("TBLASTX") ||
                   parent.getProgram().equals("WUTBLASTX")) {
          qtype = HSP.DNA;
          htype = HSP.DNA;

          qinc = 3*qstrand;
          hinc = 3*hstrand;

        } else if (parent.getProgram().equals("TBLASTN") ||
                   parent.getProgram().equals("WUTBLASTN")) {
          qtype = HSP.PEP;
          htype = HSP.DNA;

          qinc = 1*qstrand;
          hinc = 3*hstrand;
        } else {
          System.out.println("Unrecognised program  " + parent.getProgram());
        }

        System.out.println("Inc/type " + qtype + " " + htype + " " + qinc + " " + hinc);
        hsp.setId(getName());
        hsp.setName(getHitName());
        hsp.setStart(query_start);
        hsp.setEnd  (query_end);
        hsp.setHstart(hit_start);
        hsp.setHend(hit_end);

        hsp.setScore(score);
        hsp.setPValue(pvalue);
        hsp.setPositives(positives);
        hsp.setPercentId(pid);
        hsp.setQueryString(query_line);
        hsp.setHitString(hit_line);
        hsp.setAlignString(align_line);
        hsp.setFrame(frame);
        hsp.setBitScore(bits);
        hsp.setQueryLength(length);

        hsp.setQueryIncrement(qinc);
        hsp.setHitIncrement(hinc);
        hsp.setQueryType(qtype);
        hsp.setHitType(htype);
        hsp.setStrand(qstrand);
        hsp.setHstrand(hstrand);

        return hsp;
    }


    public String getHitName() {
        return hitName;
    }
    public void setHitName(String name) {
        this.hitName  = name;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getLength() {
        return length;
    }
    public void setLength(int length) {
        this.length = length;
    }
    public BufferedReader getBufferedReader() {
        return reader;
    }
    public void setBufferedReader(BufferedReader reader) {
        this.reader = reader;
    }
    public String getLastLine() {
        return last_line;
    }
    public void setLastLine(String line) {
        this.last_line = line;
    }

    public BPLite getParent() {
        return parent;
    }
    public void setParent(BPLite bp) {
        this.parent = bp;
    }


}
