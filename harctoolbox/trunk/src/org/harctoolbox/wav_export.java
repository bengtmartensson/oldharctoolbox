/*
Copyright (C) 2009-2011 Bengt Martensson.

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

package org.harctoolbox;

import IrpMaster.IrSignal;
import IrpMaster.IrpMasterException;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.antlr.runtime.RecognitionException;

/**
 *
 */
public class wav_export {
    public static final int samplefreq = 44100;

    public static boolean export(byte[] buf, String filename) {
        ByteArrayInputStream bs = new ByteArrayInputStream(buf);
        //for (int i = 0; i < buf.length; i++)
        //    System.out.println(bs.read());
     
        bs.reset();

        AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplefreq, (int) 8, (int) 1, (int) 1, samplefreq, false);
        AudioInputStream ais = new AudioInputStream(bs, af, (long) buf.length);
        try {
            int result = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean export(int freq, int[] seq, String filename) {
        double c = ((double)samplefreq)/((double)freq);

        int length = 0;
        for (int i = 0; i < seq.length; i++)
            length += (int)(c*seq[i]+0.5);

        byte[] buf = new byte[length];
        int index = 0;
        for (int i = 0; i < seq.length-1; i += 2) {
            //System.out.println("$$$$$$$$" + i + "  " + index + " " + seq[i] + " " + seq[i+1]);
            for (int j = 0; j < (int)(c*seq[i]+0.5); j++) {
                double t = ((double)j)/(2*c);
                buf[index++] = (byte) (Byte.MAX_VALUE*Math.sin(2*Math.PI*(t - (int)t)));
            }
            
            for (int j = 0; j < (int)(c*seq[i+1]+0.5); j++)
                buf[index++] = 0;
        }
        //for (int i = 0; i < length; i++)
        //    System.out.println(i + " " + buf[i]);

        export(buf, filename);
        return true;
    }

    public static boolean export(IrSignal code, boolean repeat, String filename) {
        int freq = (int) code.getFrequency();
        //int [] seq = repeat ? code.get_repeat_array() : code.get_intro_array();
        int [] seq = repeat ? code.getRepeatPulses() : code.getIntroPulses();
        return export(freq, seq, filename);
    }

    public static boolean export(String protocolname, short deviceno,
            short subdevice, short command_no, toggletype toggle, boolean repeat, String filename) throws IrpMasterException, RecognitionException {
        return export(protocol.encode(protocolname, deviceno, subdevice, command_no, toggle, null/*additinal_parameters*/, false), repeat, filename);
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("wav_export [-o filename][-r] <protocol> <deviceno> [<subdevice_no>] commandno");
        System.exit(harcutils.exit_usage_error);
    }

    public static void main(String[] args) {
        boolean repeat = false;
        String protocolname = null;
        short device_no = 0;
        short subdevice = -1;
        short command_no = 0;
        toggletype toggle = toggletype.toggle_0;
        String outputfile = null;

        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0)
                    && args[arg_i].charAt(0) == '-') {

                 if (args[arg_i].equals("-t")) {
                    arg_i++;
                    toggle = toggletype.valueOf(args[arg_i++]);
                 } else if (args[arg_i].equals("-r")) {
                    arg_i++;
                    repeat = true;
                } else
                    usage();
            }
            protocolname = args[arg_i++];
            device_no = Short.parseShort(args[arg_i++]);
            if (args.length > arg_i+1)
                subdevice = Short.parseShort(args[arg_i++]);
            command_no = Short.parseShort(args[arg_i++]);
        } catch (Exception e) {
            e.printStackTrace();
            usage();
        }

        if (outputfile == null)
            outputfile = protocolname + "_" + device_no
                    + (subdevice != -1 ? ("_" + subdevice) : "")
                    + "_" + command_no
                    + (repeat ? "_repeat.wav" : "_intro.wav");
        try {
            export(protocolname, device_no, subdevice, command_no, toggle, repeat, outputfile);
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
