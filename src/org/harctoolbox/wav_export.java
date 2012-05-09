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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.antlr.runtime.RecognitionException;
import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import org.harctoolbox.IrpMaster.IrpMasterException;

/**
 * This class generates a wave audio file that can be played
 * on standard audio equipment and fed to a double IR sending diode,
 * which can thus control IR equipment.
 *
 * @see <a href="http://www.compendiumarcana.com/iraudio/">www.compendiumarcana.com/iraudio/</a>
 */
public class wav_export {
    private static final int samplefreq = 44100;

    private wav_export() {
    }

    private static boolean export(byte[] buf, File file) {
        ByteArrayInputStream bs = new ByteArrayInputStream(buf);
     
        bs.reset();

        AudioFormat af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplefreq, 8, 1, 1, samplefreq, false);
        AudioInputStream ais = new AudioInputStream(bs, af, (long) buf.length);
        try {
            int result = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Generates a wave audio file from its arguments.
     *
     * @param freq Carrier frequency.
     * @param seq Integer array of pulse/gap times in micro seconds.
     * @param file File to be written
     * @return success.
     */
    public static boolean export(int freq, int[] seq, File file) {
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

        export(buf, file);
        return true;
    }

    /**
     * Generates a wave file from the IrSignal given as first argument.
     *
     * @param code IrSignal to be exported
     * @param intro boolean, signals if the intro sequence should be included: normally true.
     * @param noRepeats Number of repeats to be included in the wave signal.
     * @param file File to be written.
     * @return success.
     */
    public static boolean export(IrSignal code, boolean intro, int noRepeats, File file) {
        int freq = (int) code.getFrequency();
        int length = (intro ? code.getIntroLength() : 0)
                     + noRepeats*code.getRepeatLength() + code.getEndingLength();
        int [] seq = new int[length];
        int index = 0;
        if (intro)
            for (int j = 0; j < code.getIntroLength(); j++)
                seq[index++] = code.getIntroPulses()[j];

        for (int i = 0; i < noRepeats; i++)
            for (int j = 0; j < code.getRepeatLength(); j++)
                seq[index++] = code.getRepeatPulses()[j];

        for (int j = 0; j < code.getEndingLength(); j++)
                seq[index++] = code.getEndingPulses()[j];

        assert(index == length-1);
        return export(freq, seq, file);
    }

    /**
     * Generates a wave file from the IrSignal given as first argument.
     *
     * @param protocolname Name of IR protocol
     * @param deviceno Protocol parameter
     * @param subdevice Protocol parameter
     * @param command_no Protocol parameter
     * @param toggle Protocol parameter
     * @param intro boolean, signals if the intro sequence should be included: normally true.
     * @param noRepeats Number of repeats to be included in the wave signal.
     * @param file File to be written.
     * @return success
     * @throws IrpMasterException
     * @throws RecognitionException
     */

    public static boolean export(String protocolname, short deviceno,
            short subdevice, short command_no, toggletype toggle,
            boolean intro, int noRepeats, File file)
            throws IrpMasterException, RecognitionException {
        return export(protocol.encode(protocolname, deviceno, subdevice, command_no, toggle, null/*additinal_parameters*/, false),
                intro, noRepeats, file);
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("wav_export [-o filename][-r <no_repeats>] <protocol> <deviceno> [<subdevice_no>] commandno");
        System.exit(harcutils.exit_usage_error);
    }

    /**
     * Provides a command line interface to the export functions.
     * 
     * Usage:
     * wav_export [-o filename][-r &lt;no_repeats&gt;] &lt;protocol&gt; &lt;deviceno&gt; [&lt;subdevice_no&gt;] commandno
     * @param args
     */
    public static void main(String[] args) {
        int noRepeats = 0;
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
                    noRepeats = Integer.parseInt(args[arg_i++]);
                 } else if (args[arg_i].equals("-o")) {
                     arg_i++;
                     outputfile = args[arg_i++];
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
                    + "_" + command_no + ".wav";
        try {
            protocol.initialize("/usr/local/irmaster/IrpProtocols.ini");
            File file = new File(outputfile);
            export(protocolname, device_no, subdevice, command_no, toggle, true, noRepeats, file);
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (IncompatibleArgumentException ex) {
            System.err.println(ex.getMessage());
        } catch (IrpMasterException ex) {
            System.err.println(ex.getMessage());
        } catch (RecognitionException ex) {
            System.err.println(ex.getMessage());
        }
    }
}
