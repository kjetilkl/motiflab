package org.motiflab.engine.dataformat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

// NOTE. This class is a modification of a class obtained from: http://www.google.no/url?sa=t&rct=j&q=&esrc=s&source=web&cd=5&ved=0CFkQFjAE&url=http%3A%2F%2Fstorage.bioinf.fbb.msu.ru%2F~roman%2FTwoBitParser.java&ei=nenXUtPWKfHe7Aa89YHQAg&usg=AFQjCNGAazESk7qAIO5PxiRCkf694MlCPg&sig2=xDsLs9cY_5dp9ApapJOwVw&bvm=bv.59568121,d.ZGU&cad=rja

/**
 * Class is a parser of UCSC Genome Browser file format .2bit used to store 
 * nucleotide sequence information. This class extends InputStream and can
 * be used as it after choosing one of names of containing sequences. This
 * parser can be used to do some work like UCSC tool named twoBitToFa. For
 * it just run this class with input file path as single parameter and set
 * stdout stream into output file. If you have any problems or ideas don't 
 * hesitate to contact me through email: rsutormin[at]gmail.com.
 * @author Roman Sutormin
 * @author Kjetil Klepper (modifications)
 */
public class TwoBitParser {
	public static boolean DEBUG = false;
	public int DEFAULT_BUFFER_SIZE = 10000;
        private byte[] int_buf = new byte[4];    
        
	//
	private RandomAccessFile raf;
	private File f;
	private boolean reverse = false;
	private String[] seq_names;
	private HashMap<String,Long> seq2pos = new HashMap<String,Long>();
	private String cur_seq_name;
	private long[][] cur_nn_blocks;
	private long[][] cur_mask_blocks;
	private long cur_seq_pos;
	private long cur_dna_size;
	private int cur_nn_block_num;
	private int cur_mask_block_num;
	private int[] cur_bits;
	private byte[] buffer;
	private long buffer_size;
	private long buffer_pos;
	private long start_file_pos;
	private long file_pos;
	//
	private static final char[] bit_chars = {
		'T','C','A','G'
	};
	
	public TwoBitParser(File f) throws Exception {
            this.f = f;
            raf = new RandomAccessFile(f,"r");
            long sign = readFourBytes();
            if(sign==0x1A412743) {
                    if(DEBUG) System.err.println("2bit: Normal number architecture");
            }
            else if(sign==0x4327411A) {
                    reverse = true;
                    if(DEBUG) System.err.println("2bit: Reverse number architecture");
            }
            else throw new Exception("Wrong start signature in 2BIT format");
            readFourBytes();
            int seq_qnt = readFourBytes(); // the number of sequences in the file
            readFourBytes();
            seq_names = new String[seq_qnt];
            for(int i=0;i<seq_qnt;i++) {
                int name_len = raf.read();
                byte[] chars=new byte[name_len];
                raf.read(chars);
                seq_names[i] = new String(chars);
                long pos = readFourBytes();
                seq2pos.put(seq_names[i],pos);
                if(DEBUG) System.err.println("2bit: Sequence name=["+seq_names[i]+"], " +"pos="+pos);
            }
	}
          
        /** Reads the next four bytes (corresponding to a 32-bit Integer) and returns the value as an int */
        private int readFourBytes() throws IOException {
            if(!reverse) { // normal 
                return raf.readInt(); 
            } else {
                if (raf.read(int_buf) == 4) {
                  int result = (int_buf[0] & 0xff) + // masking required because java bytes are signed!
                    ((int_buf[1] & 0xff) << 8) +
                    ((int_buf[2] & 0xff) << 16) +
                    ((int_buf[3] & 0xff) << 24);
                  return result;             
                } else throw new IOException("insufficient bytes to read 32-bit int");                              
            }     
          }        
        
	public String[] getSequenceNames() {
            String[] ret = new String[seq_names.length];
            System.arraycopy(seq_names,0,ret,0,seq_names.length);
            return ret;
	}
	/**
	 * Method open nucleotide stream for sequence with given name. 
	 * @param seq_name name of sequence (one of returned by getSequenceNames()).
	 * @throws Exception
	 */
	public void setCurrentSequence(String seq_name, boolean keepMasks) throws Exception {
            if(cur_seq_name!=null) {
                    throw new Exception("Sequence ["+cur_seq_name+"] was not closed");
            }
            if(seq2pos.get(seq_name)==null) {
                    throw new Exception("Sequence ["+seq_name+"] was not found in 2bit file");
            }
            cur_seq_name = seq_name;
            long pos = seq2pos.get(seq_name);
            raf.seek(pos);
            long dna_size = readFourBytes();
            if(DEBUG) System.err.println("2bit: Sequence name=["+cur_seq_name+"], dna_size="+dna_size);
            cur_dna_size = dna_size;
            int nn_block_qnt = (int)readFourBytes();
            cur_nn_blocks = new long[nn_block_qnt][2];
            for(int i=0;i<nn_block_qnt;i++) {
                 cur_nn_blocks[i][0] = readFourBytes(); // N-block starts
            }
            for(int i=0;i<nn_block_qnt;i++) {
                 cur_nn_blocks[i][1] = readFourBytes(); // N-block lengths
            }
            if(DEBUG) {
                    System.err.print("NN-blocks["+nn_block_qnt+"]: ");
                    for(int i=0;i<nn_block_qnt;i++) {
                            System.err.print("["+cur_nn_blocks[i][0]+","+cur_nn_blocks[i][1]+"] ");
                    }
                    System.err.println();
            }
            int mask_block_qnt = (int)readFourBytes();           
            if (keepMasks) {
                cur_mask_blocks = new long[mask_block_qnt][2];
                for(int i=0;i<mask_block_qnt;i++) {
                    cur_mask_blocks[i][0] = readFourBytes(); // mask-block starts
                }
                for(int i=0;i<mask_block_qnt;i++) {
                    cur_mask_blocks[i][1] = readFourBytes(); // mask-block lengths
                }
            } else raf.skipBytes(mask_block_qnt*8); // 8 bytes per mask
            if(DEBUG && cur_mask_blocks!=null) {
                    System.err.print("Mask-blocks["+mask_block_qnt+"]: ");
                    for(int i=0;i<mask_block_qnt;i++) {
                            System.err.print("["+cur_mask_blocks[i][0]+","+cur_mask_blocks[i][1]+"] ");
                    }
                    System.err.println();
            }
            readFourBytes();
            start_file_pos = raf.getFilePointer();
            reset();
	}
        
	/**
	 * Method resets current position to the beginning of sequence stream. 
	 */
	public synchronized void reset() throws IOException {
            cur_seq_pos = 0;
            cur_nn_block_num = (cur_nn_blocks.length>0)?0:-1;
            cur_mask_block_num = (cur_mask_blocks!=null && cur_mask_blocks.length>0)?0:-1;
            cur_bits = new int[4];
            file_pos = start_file_pos;
            buffer_size = 0;
            buffer_pos = -1;
	}
        

	public void setCurrentSequencePosition(long pos) throws IOException {
            if(cur_seq_name==null) throw new RuntimeException("Sequence is not set");
            if(pos>cur_dna_size) throw new RuntimeException(
                            "Requested DNA position '"+(pos+1)+"' is outside the bounds of the chromosome/scaffold ("+cur_dna_size+" bp)");
            if(cur_seq_pos>pos) {
                    reset();
            }
            skip(pos-cur_seq_pos);
	}
        
        /**
         * This methods puts the four DNA bases that are 2-bit encoded within 
         * the byte currently pointed to by the global "long file_pos" into the global "int[4] cur_bits".
         * The bases are encoded thus: A=2, C=1, G=3 and T=0.
         * In order to be more efficient, the method uses a buffering strategy whereby
         * a larger portion of the RAF file is read at a time rather than just one byte at a time.
         * The portion that is read is kept in the global "byte[] buffer" and this buffer is updated
         * as necessary the if the current "file_pos" has been updated so that it falls outside the current buffer.
         * (The start of the current buffer is stored in the global "long buffer_pos".)
         * @throws IOException 
         */
	private void loadBits() throws IOException {
            if((buffer==null)||(buffer_pos<0)||(file_pos<buffer_pos)|| (file_pos>=buffer_pos+buffer_size)) { // file_pos is outside of current buffer, hence we must update the buffer
                if((buffer==null)||(buffer.length!=DEFAULT_BUFFER_SIZE)) {
                    buffer = new byte[DEFAULT_BUFFER_SIZE];
                }
                buffer_pos = file_pos;
                buffer_size = raf.read(buffer);
            }
            int cur_byte = buffer[(int)(file_pos-buffer_pos)]& 0xff; // the &0xff will convert the "signed byte" (range -128 to 127) to an "unsigned byte" in the range 0 to 255.
            for(int i=0;i<4;i++) {
                cur_bits[3-i] = cur_byte%4;
                cur_byte /= 4;
            }
	}
        
	/**
	 * Method reads 1 nucleotide from sequence stream. 
         * You should set current sequence before using it.  
	 */
	public int read() throws IOException {
            if(cur_seq_name==null) throw new IOException("Sequence is not set");
            if(cur_seq_pos==cur_dna_size) {
                    if(DEBUG) System.err.println("End of sequence (file position:"+raf.getFilePointer()+" )");
                    return -1;
            }
            int bit_num = (int)cur_seq_pos%4;
            if(bit_num==0) { // the file position has been updated. Load next 4 bases 
                loadBits();
            }
            else if(bit_num==3) {
                file_pos++;
            }
            char ret = 'N';
            // check if the current sequence position is within an N-block. If so, return 'N' else return the character read from file
            if((cur_nn_block_num>=0)&&
                            (cur_nn_blocks[cur_nn_block_num][0]<=cur_seq_pos)) {
                    if(cur_bits[bit_num]!=0) {
                            throw new IOException("Wrong data in NN-block ("+cur_bits[bit_num]+") "+
                                            "at position "+cur_seq_pos);
                    }
                    if(cur_nn_blocks[cur_nn_block_num][0]+cur_nn_blocks[cur_nn_block_num][1]==cur_seq_pos+1) {
                            cur_nn_block_num++;
                            if(cur_nn_block_num>=cur_nn_blocks.length) {
                                    cur_nn_block_num = -1;
                            }
                    }
                    ret = 'N';
            }
            else {
                 ret = bit_chars[cur_bits[bit_num]];
            }
            // check if the base is within a mask-block
            if(cur_mask_blocks!=null && (cur_mask_block_num>=0)&& (cur_mask_blocks[cur_mask_block_num][0]<=cur_seq_pos)) {
                    ret = Character.toLowerCase(ret);
                    if(cur_mask_blocks[cur_mask_block_num][0]+cur_mask_blocks[cur_mask_block_num][1]==cur_seq_pos+1) {
                            cur_mask_block_num++;
                            if(cur_mask_block_num>=cur_mask_blocks.length) {
                                    cur_mask_block_num = -1;
                            }
                    }
            }
            cur_seq_pos++;
            return (int)ret;
	}
        
	/**
	 * Method skips n nucleotides in sequence stream. You should set current sequence before use it. 
         * 
	 */
	public synchronized long skip(long n) throws IOException {
            if(cur_seq_name==null) throw new IOException("Sequence is not set");
            if(n<4) {
                    int ret = 0;
                    while((ret<n)&&(read()>=0)) ret++;
                    return ret;
            }
            if(n>cur_dna_size-cur_seq_pos) {
                    n = cur_dna_size-cur_seq_pos;
            }
            cur_seq_pos += n;
            file_pos = start_file_pos+(cur_seq_pos/4); // with 2 bits per base each byte (position in file) contains 4 bases. 
            raf.seek(file_pos); // Set position in file to the byte containing the requested nucleotide
            if((cur_seq_pos%4)!=0) { // the current sequence position is not at the start of a byte in the file
                loadBits();
            }
            // Update the current N-block and mask-block pointers so that they correspond with the current DNA sequence position
            while((cur_nn_block_num>=0)&& (cur_nn_blocks[cur_nn_block_num][0]+cur_nn_blocks[cur_nn_block_num][1]<=cur_seq_pos)) {
                    cur_nn_block_num++;
                    if(cur_nn_block_num>=cur_nn_blocks.length) cur_nn_block_num = -1;
            }
            while(cur_mask_blocks!=null && (cur_mask_block_num>=0)&& (cur_mask_blocks[cur_mask_block_num][0]+cur_mask_blocks[cur_mask_block_num][1]<=cur_seq_pos)) {
                    cur_mask_block_num++;
                    if(cur_mask_block_num>=cur_mask_blocks.length) cur_mask_block_num = -1;
            }
            return n;
	}
        
	/**
	 * Method closes current sequence and it's necessary to invoke it before setting
	 * new current sequence.
	 */
	public void close() throws IOException {
            cur_seq_name = null;
            cur_nn_blocks = null;
            cur_mask_blocks = null;
            cur_seq_pos = -1;
            cur_dna_size = -1;
            cur_nn_block_num = -1;
            cur_mask_block_num = -1;
            cur_bits = null;
            buffer_size = 0;
            buffer_pos = -1;
            file_pos = -1;
            start_file_pos = -1;
	}
        
	/**
	 * Method closes random access file descriptor. You can't use any reading methods
	 * after it.
	 * @throws Exception
	 */
	public void closeParser() throws Exception {
            raf.close();
	}
        
	public File getFile() {
            return f;
	}
        
        /**
         * 
         * @param sequence The name of the sequence (usually chromosome name)
         * @param start The genomic start coordinate (1-indexed)
         * @param end The genomic end coordinate 
         * @param keepMask if TRUE, masked character in the DNA sequence will be in lowercase. If FALSE, all characters will be uppercase
         * @return
         * @throws IOException 
         */
	public char[] loadFragmentAsBuffer(String sequence, long start, long end, boolean keepMask) throws Exception {
            try {
                setCurrentSequence(sequence, keepMask);
                setCurrentSequencePosition(start-1); // subtract 1 since the sequence is 0-indexed in file
                int length=(int)(end-start+1);
                char[] ret = new char[length];
                int i = 0;
                for(;i<length;i++) {
                    int ch = read();                   
                    if(ch<0) break;
                    ret[i] = (char)ch;
                }
                return ret;
            } catch (Exception e) {
                throw e;
            } finally {
                close(); // this is necessary to close the sequence
            }           
	}
      
	public static void main(String[] args) throws Exception {
            args=new String[]{"X:/MotifLab_Datatracks/rawData/human/hg19.2bit","chr1","3000000","3000100"}; // just for testing
            // args=new String[]{"X:/MotifLab_Datatracks/rawData/human/hg19.2bit","chr1"};
            if(args.length==0) {
                    System.out.println("Usage: <program> <input.2bit> [<seq_name> [<start> [<length>]]]");
                    System.out.println("Resulting fasta data will be written in stdout.");
                    return;
            }
            TwoBitParser parser = new TwoBitParser(new File(args[0]));
            if(args.length==1) {
                    String[] names = parser.getSequenceNames();
                    for(int i=0;i<names.length;i++) {
                        System.err.println(names[i]); // print names
                    }
            }
            else if(args.length==2) {
                    String name = args[1];
                    parser.setCurrentSequence(name,false);
                    parser.close();
            } 
            else if(args.length==4) {
                    String name = args[1];
                    long start = Long.parseLong(args[2]);
                    long end = Long.parseLong(args[3]);
                    char[] buffer=parser.loadFragmentAsBuffer(name, start, end, false);
                    System.err.println(">"+name);
                    System.err.println(buffer);
            } else System.err.println("Wrong number of arguments");
            parser.closeParser();
	}
}
