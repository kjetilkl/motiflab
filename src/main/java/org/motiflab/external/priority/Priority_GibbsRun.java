package org.motiflab.external.priority;

import java.util.ArrayList;
import java.util.Random;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.data.BackgroundModel;
import org.motiflab.engine.data.DNASequenceData;
import org.motiflab.engine.data.DNASequenceDataset;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.RegionDataset;
import org.motiflab.engine.data.RegionSequenceData;
import org.motiflab.engine.data.Sequence;
import org.motiflab.engine.data.SequenceCollection;
import org.motiflab.engine.data.NumericDataset;
import org.motiflab.external.MotifDiscovery;


/**
 * Priority_GibbsRun - this class implements the gibbs sampler
 * @author raluca
 */
public class Priority_GibbsRun extends MotifDiscovery
{       private static final String SINGLE="Single";
        private static final String DOUBLE="Double";
    
        private ArrayList<String> sequenceNumberToName; // 
        private int numberofmotifs=0;
        private DNASequenceData[] dnaSequences;
        
	/* Path variables for output files: */
//	String fname_output; /* the output directory for a TF (different dir for each TF) */
//	String pans;         /* the complete path for the main output file (different file for each TF) */
//	String pbest;        /* the best results from pans */
//	String plogl;      /* the complete path for the file containing the logls (diff file for each TF) */
	
	/* Running parameters */
	double phi_prior[]; /* dirichlet prior counts for the model parameters (phi) */
	double back[];      /* the background model */
	String tf_names[];  /* TF names */
	
	int wsize;                  /* the window size */	
	int nc;                     /* the number of classes (prior_dirs.length + 1 for the "other" class) */	
	double comboprior[][][];    /* the priors for each class/prior, each sequence, each position (different for each TF) */
	String comboseq[];          /* the sequences for the current TF (as strings of {0,1,2,3} <-> {A,C,G,T} */
	String comboseq_names[];    /* the names of the genes corresponding to these sequences */
	
	/* Other variables */
	double cprior[]; /* dirichlet prior counts for the classes */
	double denom[][];    /* the normalization constant (for each class and each sequence) */
	double phi[][];      /* the PSSM */
	double phi_temp[][]; /* temporary phi used for printing */
	
	/* The variables we sample */
	int Z[], bestZ[], overallbestZ[] = null;
	int C[], bestC[], overallbestC[] = null;
	
	/* Variable used instead of 0 for the priors */
	double verySmall = Math.exp(-30);
	
	/** Constructor */
	public Priority_GibbsRun() {
            this.name="PRIORITY";
            this.programclass="MotifDiscovery";
            this.serviceType="bundled";
            
            addSourceParameter("Sequence", DNASequenceDataset.class, null, null, "input sequences");

            addParameter("Number of motifs", Integer.class, 1, new Integer[]{1,10},"Specifies the number of motifs to search for. Note that returned motifs are not guaranteed to be unique",true,false);
            addParameter("Motif length", Integer.class, 8, new Integer[]{3,20},"The length of the motifs to search for",true,false);
            addParameter("Priors", NumericDataset.class, null, new Class[]{NumericDataset.class},"Positional priors used to guide motif discovery",true,false); 
            addParameter("Background", BackgroundModel.class, null, new Class[]{BackgroundModel.class},"A Markov model describing the background probabilities",true,false); 
            addParameter("Strand", String.class, DOUBLE, new String[]{SINGLE,DOUBLE},"Search one strand or both strands (default=Double)",false,false);
            addParameter("Allow 0 occurrences", Boolean.class, Boolean.TRUE, new Boolean[]{Boolean.TRUE,Boolean.FALSE},"Allow for the possibility that a sequence does not contains an instance of a motif (default=yes)",false,false);

            addParameter("Trials", Integer.class, 10, new Integer[]{1,100},"The number of times the Gibbs sampling procedure should be restarted",false,false,true); // advanced
            addParameter("Iterations", Integer.class, 1000, new Integer[]{1,1000000},"The number of iterations in each trial of the Gibbs sampling procedure",false,false,true); // advanced
            addParameter("Scaling factor", Double.class, 0.0, new Double[]{0.0,0.5},"Called 'd' in the paper",false,false,true); // advanced
        
            addResultParameter("Result", RegionDataset.class, null, null, "output track");
            addResultParameter("Motifs", MotifCollection.class, null, null, "Motif collection");
                     
	}

        /** Resolves (if necessary) and converts task parameters and puts the
         * values into the Priority_Parameters 
         */
        private void resolveParameters(OperationTask task) throws ExecutionError {
              nc = 1; // the number of priors to use (here: only one - from a numeric value track)
              // Input parameters have already been parsed and resolved,
              // but we need to place them in Priority_Parameters
             NumericDataset priorDataset=(NumericDataset)task.getParameter("Priors");
             if (priorDataset==null) throw new ExecutionError("No priors dataset");
             Priority_Parameters.iter=(Integer)task.getParameter("Iterations");
             Priority_Parameters.trials=(Integer)task.getParameter("Trials");
             Priority_Parameters.multiple_priors=false;
             Priority_Parameters.otherclass=false; // only allow for a single Priors class specified by a Priors Track
             int motifLength=(Integer)task.getParameter("Motif length");
             Priority_Parameters.wsize=motifLength;
             Priority_Parameters.wsizeMax=motifLength;
             Priority_Parameters.wsizeMin=motifLength;
             Priority_Parameters.wsizeMaxFromPrior=motifLength;
             Priority_Parameters.wsizeMinFromPrior=motifLength;
             wsize=motifLength;
             
             String strand=(String)task.getParameter("Strand");
             if (strand.equals(DOUBLE)) Priority_Parameters.revflag=true; else Priority_Parameters.revflag=false;
             Priority_Parameters.noocflag=(Boolean)task.getParameter("Allow 0 occurrences");
             Priority_Parameters.d=(Double)task.getParameter("Scaling factor");
                          
             // Read and convert FASTA files
             // Since PRIORITY refers to sequences by numbers instead of names,
             // we store the order of sequences at the start of the run, 
             // in case the user changes this order in the GUI during the run
             SequenceCollection sequenceCollection=(SequenceCollection)task.getParameter(OperationTask.SEQUENCE_COLLECTION);
             if (sequenceCollection.size()<2) throw new ExecutionError("Priority requires at least 2 sequences in order to find common motifs");
             sequenceNumberToName=sequenceCollection.getAllSequenceNames();
             
             Data[] sources=(Data[])task.getParameter(SOURCES);
             if (sources==null) throw new ExecutionError("SYSTEM ERROR: Missing SOURCES for Motif Discovery with PRIORITY");
             DNASequenceDataset sourceData=(DNASequenceDataset)sources[0];
             comboseq_names=new String[sequenceCollection.size()];
             comboseq=new String[sequenceCollection.size()];
             dnaSequences=new DNASequenceData[sequenceCollection.size()];
             for (int i=0;i<sequenceCollection.size();i++) {
                 String sequenceName=sequenceCollection.getSequenceNameByIndex(i);
                 DNASequenceData dnasequence=(DNASequenceData)sourceData.getSequenceByName(sequenceName);
                 dnaSequences[i]=dnasequence;
                 StringBuffer buffer=new StringBuffer(dnasequence.getSequenceAsString());
                 for (int j=0;j<buffer.length();j++) {
                      switch (buffer.charAt(j)) {
                        case 'a': case 'A': buffer.setCharAt(j, '0'); break;
                        case 'c': case 'C': buffer.setCharAt(j, '1'); break;
                        case 'g': case 'G': buffer.setCharAt(j, '2'); break;
                        case 't': case 'T': buffer.setCharAt(j, '3'); break;
                        default: buffer.setCharAt(j, '*'); break;                       
                      }
                 }
                 comboseq[i]=new String(buffer);   
                 comboseq_names[i]=sequenceName;
             }
  
             // Convert the background model and set Priority_Parameters.back  
             BackgroundModel bgmodel=(BackgroundModel)task.getParameter("Background");
             int order=bgmodel.getOrder();
             double[] background=BackgroundModel.convertBackgroundToPlainFormat(bgmodel);
             Priority_Parameters.back=background;
             back=background;
             if (order>5) order=5; // PRIORITY can not use higher than 5th order models. However, I am not sure if this line will solve that problem
             Priority_Parameters.bkgrOrder=order;
             Priority_Parameters.bkgrOrderMin=0;
             Priority_Parameters.bkgrOrderMax=order;
             
             // set the TF names according to the number of motifs
             numberofmotifs=(Integer)task.getParameter("Number of motifs");
             Priority_Parameters.tf_names=new String[numberofmotifs];
             for (int i=0;i<numberofmotifs;i++) {
                 Priority_Parameters.tf_names[i]="PRIORITY"+i; // this is just a temporary name!
             }
             this.tf_names=Priority_Parameters.tf_names;
             
             // Convert the priors to a format usable by PRIORITY             
             set_priors(sequenceNumberToName,priorDataset);      
             
             phi_prior=Priority_Parameters.phi_prior;
             
        }
    
	/** Makes local copies of some of the variables in Priority_Parameters. */
	private void set_local_params()
	{                       
		wsize = Priority_Parameters.wsize;
		// nc = Priority_Parameters.prior_dirs.length; // original line

		if (Priority_Parameters.otherclass) 
			nc = nc + 1;
		phi_prior = Priority_Parameters.phi_prior;
		
		back = Priority_Parameters.back;
		tf_names = Priority_Parameters.tf_names;	
	}
	
	/** Computes COMBOPRIOR. */
	private void set_priors(ArrayList<String> sequenceNames, NumericDataset priorTrack) throws ExecutionError
	{     
		/* comboprior[c][i][j] = the prior for class/priortype c, sequence i, position j */

               comboprior = new double[nc][comboseq.length][];

		/* set all the priors (except the uniform/other) */
		int c, i, j, length;
		for (c=0; c<nc; c++) { // read each prior for this TF from file
                                String filename="Priority priors";
				// file exists and is readable. Use method in Priority_GibbsStatic to read it
                                // first index is sequence number. Second index is position (I think it might be zero-indexed)
				double temp[][] = Priority_GibbsStatic.get_positional_prior(sequenceNames,priorTrack);
				
				for (i=0; i<comboseq.length; i++)
				{       int priorssize=temp[i].length;
                                        if (Priority_Parameters.revflag) priorssize=priorssize/2;
					if (priorssize != comboseq[i].length()) {
						String mess = "The size of a DNA sequence (" + comboseq[i].length() + 
						              ") does not match the size of the prior (" + priorssize + ").";
						throw new ExecutionError(mess);
					}
				}
				comboprior[c] = temp;
				
				/* if the wsizes (min and max) are specified in the prior file
				 * check that the current wsize is in that range */
				if ((Priority_Parameters.wsizeMinFromPrior != -1) &&	(Priority_Parameters.wsizeMinFromPrior != -1))
					if ((Priority_Parameters.wsize < Priority_Parameters.wsizeMinFromPrior) ||
						(Priority_Parameters.wsize > Priority_Parameters.wsizeMaxFromPrior)) {
						String mess = "Warning: the motif length (" + Priority_Parameters.wsize +
						              ") is not in the range specified in the prior file\n  " +
						              filename + " (" + Priority_Parameters.wsizeMinFromPrior + ".." +
						              Priority_Parameters.wsizeMaxFromPrior + ").";
						throw new ExecutionError(mess);					
					}
				
		}		
           
		if (Priority_Parameters.otherclass) {
			if (nc > 1) /*otherclass is NOT the only class*/
				comboprior[nc-1] = (double[][])(comboprior[0]).clone();
			for (i=0; i<comboseq.length; i++)
			{
				length = comboseq[i].length();
				comboprior[nc-1][i] = new double[length]; 
				for (j=0; j<length; j++)
					comboprior[nc-1][i][j] = Priority_Parameters.flat_prior_other_class;
			}
		}

		/* scale the prior to be between d and 1-d */
		for (c=0; c<nc; c++) {
			for (i=0; i<comboseq.length; i++){
				for (j=0; j<comboprior[c][i].length; j++) {
                                        //System.err.println("Sequence length["+i+"]="+comboseq[i].length());
					if (comboprior[c][i][j] == 0)
						comboprior[c][i][j] = this.verySmall; /* e^(-30) */
					if (comboprior[c][i][j] == 1) /* to avoid values of 1 */
						comboprior[c][i][j] = comboprior[c][i][j] - this.verySmall;
					comboprior[c][i][j] = comboprior[c][i][j]
						* (1-2*Priority_Parameters.d) + Priority_Parameters.d;
				}	
                        }
                }

                
		/* set the prior to 0 for all wmers that contain, besides 0,1,2,3 the 
		 * special character '*' (for masked sequences). Do not scale these. */
		boolean foundNotMasked;
		for (i=0; i<comboseq.length; i++) {
			foundNotMasked = false;
                        int seqlength=comboseq[i].length();
			for (j=0; j<seqlength-wsize+1; j++)
				if (comboseq[i].substring(j, j+wsize).contains("*")) {
					for (c=0; c<nc; c++) comboprior[c][i][j] = 0; /* set the priors to 0 */
                                        if (Priority_Parameters.revflag) {
                                            for (c=0; c<nc; c++) comboprior[c][i][j+seqlength] = 0; /* set the priors to 0 */
                                        }
				}
				else foundNotMasked = true;
			if (foundNotMasked == false) {
				String mess = "PRIORITY Error: the DNA sequence \"" + comboseq_names[i] 
				+ "\" contains only masked wmers.";
				throw new ExecutionError(mess);
			}
		}
		
		/* this is to ensure we do not sample from the middle part or the end part*/
		for (c=0; c<nc; c++) {
			for (i=0; i<comboseq.length; i++) {
				 if (Priority_Parameters.revflag) {
					for (j=comboprior[c][i].length/2-wsize+1; j<comboprior[c][i].length/2; j++)
						comboprior[c][i][j]=0;
                                 }
				 for (j=comboprior[c][i].length-wsize+1; j<comboprior[c][i].length; j++)
					comboprior[c][i][j]=0;
			}
                }

	} // end of set_priors
	
	
	/** The gibbs sampler */ 
        @Override
	public void execute(OperationTask task) throws Exception
	{
                resolveParameters(task); // this method was added by Kjetil Klepper and converts MotifLab parameters to PRIORITY parameters
                //set_local_params(); // this was in the original code, but is replaced by the line above
                MotifCollection motifCollection = new MotifCollection("PriorityMotifs"); // the name is just a temp-name anyway
                RegionDataset regionDataset=new RegionDataset("Priority");
                ArrayList<Data> sequences=engine.getAllDataItemsOfType(Sequence.class);
                for (Data sequence:sequences) {
                     RegionSequenceData regionsequence=new RegionSequenceData((Sequence)sequence);
                     regionDataset.addSequence(regionsequence);
                }
                
                /* append the reverse complement if necessary */
                if (Priority_Parameters.revflag) {
                    for (int i=0; i<comboseq.length; i++) {
                        comboseq[i] = comboseq[i] + Priority_GibbsStatic.get_reverse(comboseq[i]); // string concatenation
                        //System.err.println(comboseq[i]);
                    }  
                }
                
                // -- debugging --
//                for (int i=0;i<comboprior[0].length;i++) {
//                    System.err.print("[i="+i+"] ");
//                    for (int j=0;j<comboprior[0][i].length;j++) {
//                       System.err.print("("+j+")"+comboprior[0][i][j]+",");
//                   }
//                   System.err.println("\n");
//                }
//                System.err.println();
                
                /* for each TF (number of motifs to search for): */
		for (int tf=0; tf<tf_names.length; tf++)
		{
			 task.checkExecutionLock(); // checks to see if this task should suspend execution
                         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
			
			/* read the sequences... */
                        // temp contains all strings in the FASTA file. i.e. alternating between headers (>name) and sequences
                        // so the actual number of sequences is half the number of lines in temp
                         /*
                        String[] tempFasta = Priority_GibbsStatic.get_DNAsequences(tf_names[tf]);
                        comboseq = new String[tempFasta.length/2];
                        comboseq_names = new String[tempFasta.length/2];
                        for (int h=0; h<tempFasta.length; h+=2) {
                                comboseq_names[h/2] = tempFasta[h];
                                comboseq[h/2] = tempFasta[h+1];
                        }
                        */ 
			
			 task.checkExecutionLock(); // checks to see if this task should suspend execution
                         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

			
			/* set the priors for each position in each of the sequences */
                         
			// set_priors(tf); /* modifies comboprior */
                        
                        // KK: The above line has been taken out of the for-each-TF loop and executed only once in resolveParameters(task) 

			
			
			/* set the pseudocounts for gamma (the prior on the class) */
			cprior = new double[nc];
			for (int c=0; c<nc; c++) 
				cprior[c] = Priority_Parameters.pseudocounts_class;
			if (Priority_Parameters.putative_class >= 0) /* the slight advantage when the class is known */
				cprior[Priority_Parameters.putative_class] = Priority_Parameters.pseudocounts_putative_class; 
			
			 task.checkExecutionLock(); // checks to see if this task should suspend execution
                         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

			
			/* compute the normalization constant */
			denom = new double[nc][comboseq.length];
			int c,i,j;
			for (i=0; i<comboseq.length; i++)
			{
				for (c=0; c<nc; c++) {
					denom[c][i] = 1;
					if (comboprior[c][i].length != comboseq[i].length()) {
						String mess = "The size of a fasta sequence (" + 
						              comboseq[i].length() + 
						              ") does not match the size of the prior (" +
						              comboprior[c][i].length + ").";
						throw new ExecutionError(mess+"\n");
					}
				}
				
				for (j=0; j<comboseq[i].length(); j++)
					for (c=0; c<nc; c++) { 
						denom[c][i] = denom[c][i] + comboprior[c][i][j]/(1-comboprior[c][i][j]);
					}
			}
			
			 task.checkExecutionLock(); // checks to see if this task should suspend execution
                         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

			
			/* create the output files */
                        /*
			fname_output = Priority_Parameters.path_output + "/" + tf_names[tf];	
			pans = fname_output + ".trials.txt";
			pbest = fname_output + ".best.txt";
			plogl = fname_output + ".logl";
			try { initial_print(tf); }
			catch (ExecutionError err) {
				System.out.println(err.getMessage());
				continue;
			}
                        */
			
			
			/* initialize sampled variables */
			Z = new int[comboseq.length];
			bestZ = new int[comboseq.length];
			overallbestZ = new int[comboseq.length];
			C = new int[comboseq.length];
			bestC = new int[comboseq.length];
			overallbestC = new int[comboseq.length];
			
			phi = new double[4][wsize];
				
			for (int a=0; a<4; a++)
				for (int b=0; b<wsize; b++)
					phi[a][b] = 0;
			
			double overallbestlogl = -Double.MAX_VALUE;
			int overallbestlogl_trial = -1, overallbestlogl_iter = -1;
			
			int tr, cnt;
			task.setStatusMessage("Running TF " + tf_names[tf] + " ");
			for (tr=0; tr<Priority_Parameters.trials; tr++) //if (comboseq.length >= 10)
			{				
                                task.checkExecutionLock(); // checks to see if this task should suspend execution
                                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

				Random rand = new Random();
				/* initialize C and Z (uniform probability) */
				for (i=0; i<comboseq.length; i++)
				{
					C[i] = rand.nextInt(nc); /* random integers between 0 and nc-1 */
					
					/* generate a weight vector that has 0 wherever the prior for the
					 * class C[i] has 0, and 1 everywhere else */
					double temp[] = new double[comboseq[i].length()];
					double tempsum = 0;
					for (j=0; j<comboseq[i].length(); j++) {
						temp[j] = comboprior[ C[i] ][i][j];
						if (temp[j] > 0)
							temp[j] = 1;
						tempsum += temp[j];
					}
					Z[i] = Priority_GibbsStatic.rand_sample(temp, tempsum);
				}
					
				if (tr == Priority_Parameters.trials) continue;
				
                                task.setStatusMessage("Executing PRIORITY: motif="+(tf+1)+", trial="+(tr+1)+" of "+Priority_Parameters.trials);
                                task.setProgress((tf*Priority_Parameters.trials*Priority_Parameters.iter)+tr*Priority_Parameters.iter,numberofmotifs*Priority_Parameters.iter*Priority_Parameters.trials);				
        			task.checkExecutionLock(); // checks to see if this task should suspend execution
                                if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
				
				double bestlogl = -Double.MAX_VALUE;
				int bestlogl_iter = -1;				
				double[] logl = new double[Priority_Parameters.iter];
				int index = rand.nextInt(comboseq.length); /* choose random index */
				
				
				/* run with different values for power and powerpssm */
				int power;
				int powerpssm;
				boolean flag_for_oldsampling = false;
				if (tr < Priority_Parameters.trials/5) {
					powerpssm = 1; power = 1; // should be 1
					//Priority_Parameters.noocflag = false;
				} else if (tr < 2*Priority_Parameters.trials/5) {
					powerpssm = 1; power = 12;
					Priority_Parameters.noocflag = false;
				} if (tr < 3*Priority_Parameters.trials/5) {
					powerpssm = 6; power = 1; // should be 1
					Priority_Parameters.noocflag = false;
				} 
				if (tr < 4*Priority_Parameters.trials/5) {
					powerpssm = 6; power = 12; 
					Priority_Parameters.noocflag = false;
				} 
				else {
					powerpssm = 6; power = 12;
				    Priority_Parameters.noocflag = false;
				    flag_for_oldsampling = true;
				}
				
				
				for (cnt=0; cnt<Priority_Parameters.iter; cnt++)
				{       
                                        task.checkExecutionLock(); // checks to see if this task should suspend execution
                                        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

					if ((cnt+1) % Priority_Parameters.outputStep == 0) {
						// printToScreen(tf, tr, cnt, bestZ, bestC, bestlogl, -1, -1);
                                            task.setStatusMessage("Executing PRIORITY: motif="+(tf+1)+", trial="+(tr+1)+" of "+Priority_Parameters.trials+",   iteration="+(cnt+1));
                                            task.setProgress((tf*Priority_Parameters.trials*Priority_Parameters.iter)+tr*Priority_Parameters.iter+cnt,numberofmotifs*Priority_Parameters.iter*Priority_Parameters.trials);
                                        }
					if ((cnt+1) % 200 == 0) { // yield every 200th iteration
                                              Thread.yield();
                                        }
                                        if((cnt+1)%Priority_Parameters.outputStep == 0)
					{
						power=Math.max(1,power-3);
						powerpssm=Math.max(1,powerpssm-1);
						if(power > 1 || powerpssm >1)
							Priority_Parameters.noocflag=false;
						else
							Priority_Parameters.noocflag=true;
						
					}
					
					/* pick the next sequence (+1 or randomly: index = rand.nextInt(comboseq.length))*/
					index = (index+1) % (comboseq.length);
					
					/* calculate the current estimate of phi */
					
					phi = Priority_GibbsStatic.calPhi(Z, comboseq, index, wsize, phi_prior);
					
					/* next we have to sample Z[index], but before that we must compute the weights:*/
					int length = comboseq[index].length();
					double[] W = new double[length+1];
					double sumW = 0;
					
					/* we initialize the weight vector for the class also (will be used later) */
					double[] Wc = new double[nc];
					double sumWc = 0;
					
                			 task.checkExecutionLock(); // checks to see if this task should suspend execution
                                         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

					
					for (j=0; j<length; j++) 
					{	
						if (comboprior[C[index]][index][j] == 0) 
								W[j] = 0;
						else { 
							double quantity1 = Priority_GibbsStatic.calA(phi, wsize, 
									comboseq[index].substring(j,j+wsize), back, Priority_Parameters.bkgrOrder);
							if(flag_for_oldsampling) {
								quantity1=quantity1*Math.pow(Priority_GibbsStatic.calonlypssm(phi, wsize, 
									comboseq[index].substring(j,j+wsize), back, Priority_Parameters.bkgrOrder),powerpssm-1);
							}
							else {
								quantity1=Math.pow(quantity1,powerpssm);
							}
							
							double quantity2 = Math.pow(comboprior[C[index]][index][j],power) / (1-Math.pow(comboprior[C[index]][index][j],power));
							W[j] =  quantity1 * quantity2;							
							sumW += W[j];
//                                                        if (Double.isInfinite(quantity1)) {System.err.println("quantity1 infinite for "+j);}
//				                          if (Double.isInfinite(quantity2)) {System.err.println("quantity2 infinite for "+j+":   divisor="+Math.pow(comboprior[C[index]][index][j],power)+"  dividend="+(1-Math.pow(comboprior[C[index]][index][j],power))+"  comboprior="+comboprior[C[index]][index][j]+"   power="+power);}
//                                                        if (comboprior[C[index]][index][j]==1.0) System.err.println("comboprior=1.0  for j="+j);
                                                }
					}
										
                			 task.checkExecutionLock(); // checks to see if this task should suspend execution
                                         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

					
					if (Priority_GibbsStatic.max(W) <= 0) {
						Z[index] = -1; /* debugging case!!! "all weights 0") Should not happen!!! */
                                        } else {
						if (Priority_Parameters.noocflag)
							W[length] = 1; /* to sample the "no motif" case */
						else
							W[length] = 0;
						sumW += W[length];
						
						/* now we finally sample Z[index] */
                                                //System.err.println("sumW="+sumW);
                                                if (sumW==0) throw new ExecutionError("PRIORITY Error: All priors are zero");
						Z[index] = Priority_GibbsStatic.rand_sample(W, sumW);

						if(Z[index]==-1)
							throw new ExecutionError("PRIORITY Error: Invalid priors. Try normalizing to [0,1]");
						
						/* next we compute the weights for sampling C[index] */										
						if (Z[index] == length) /* the sampler picked the appended 1 ("no motif") */ 
						{  
							Z[index] = -1; // signals "no motifs found"
							for (c=0; c<nc; c++) 
								Wc[c] = (1/denom[c][index]) * Priority_GibbsStatic.prob_gen_prior(c,nc,cprior,C,index);
						} 
						else /* we set the weights to sample class where a motif exists */ 
						{    
							for (c=0; c<nc; c++)
							{
								if(Z[index]>length-wsize+1 || Z[index]<0)
								{
									throw new ExecutionError("PRIORITY: "+index+" "+Z[index]+" "+length);
								}
								Wc[c] = comboprior[c][index][Z[index]] * 
								        Priority_GibbsStatic.prob_gen_prior(c,nc,cprior,C,index);
								
							}
						}
						sumWc = 0;
						for (c=0; c<nc; c++)
							sumWc += Wc[c];

					} /* end if (Priority_GibbsStatic.max(W) == 0) - the debug case */
					
					
					/* now we sample C[index] */
					C[index] = Priority_GibbsStatic.rand_sample(Wc, sumWc);
                                        
					/* recompute phi */
					phi = Priority_GibbsStatic.calPhi(Z, comboseq, -1, wsize, phi_prior);
					
                                        task.checkExecutionLock(); // checks to see if this task should suspend execution
                                        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

					
					/* now the MAP calculation: */
					double gamma[] = new double[nc];
					double sum_gamma = 0;
					for (int cl=0; cl<nc; cl++) {
						gamma[cl] = Priority_GibbsStatic.prob_gen_prior(cl,nc,cprior,C,-1);
						sum_gamma += gamma[cl];
					}
					/* normalize it => a prob distribution */
					for (int cl=0; cl<nc; cl++)
						gamma[cl] = gamma[cl] / sum_gamma;
					

					
					/* sum log(P(gamma)) and log(P(phi)) */
					logl[cnt] = Priority_GibbsStatic.logl_phi_gamma(phi, phi_prior, gamma, cprior);
					
                			 task.checkExecutionLock(); // checks to see if this task should suspend execution
                                         if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

					
					for (i=0; i<Z.length; i++)
						if (Z[i] > -1) /* there is an occurrence of the motif */
							logl[cnt] = logl[cnt] +
							   Math.log(Priority_GibbsStatic.calA(phi, wsize, comboseq[i].substring(Z[i],
									   Z[i]+wsize), back, Priority_Parameters.bkgrOrder)) +
							   Math.log(comboprior[C[i]][i][Z[i]]) - 
							   Math.log(1-comboprior[C[i]][i][Z[i]]) - 
							   Math.log(denom[C[i]][i]) + 
							   Math.log(Priority_GibbsStatic.prob_gen_prior(C[i], nc, cprior, C, -1));
						else {/* no occurrence */               
							logl[cnt] = logl[cnt] -
							   Math.log(denom[C[i]][i]) +
							   Math.log(Priority_GibbsStatic.prob_gen_prior(C[i], nc, cprior, C, -1));
                                                }
                                        task.checkExecutionLock(); // checks to see if this task should suspend execution
                                        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

					
					if (logl[cnt] > bestlogl) 
					{
						bestlogl = logl[cnt];
						bestlogl_iter = cnt;
						for (i=0; i<Z.length; i++) bestZ[i] = Z[i];
						for (i=0; i<C.length; i++) bestC[i] = C[i];
					}
				} /* end for (int cnt=0; cnt<iter; cnt++) */
				
//				try {
//					printCurrentTrialInfo(tr, bestlogl, logl, bestZ, bestC, -1, -1);
//				} catch (Exception e) { e.printStackTrace(); }
				if (overallbestlogl < bestlogl) {
					overallbestlogl = bestlogl;
					overallbestlogl_trial = tr;
					overallbestlogl_iter = bestlogl_iter; 
					for (i=0; i<bestZ.length; i++) overallbestZ[i] = bestZ[i];
					for (i=0; i<bestC.length; i++) overallbestC[i] = bestC[i];
				}
			}/* end for (tr=0; tr<this.trials; tr++) */
			//System.out.print("\n");
			if (tr > 0) {
				
                                //printToScreen(tf, -1, -1, overallbestZ, overallbestC, overallbestlogl, overallbestlogl_trial, overallbestlogl_iter);
				//printCurrentTrialInfo(-1, overallbestlogl, null, overallbestZ, overallbestC,overallbestlogl_trial, overallbestlogl_iter);
				double[][] overallbest_phi = Priority_GibbsStatic.calPhi(overallbestZ, comboseq, -1, wsize, Priority_GibbsStatic.noprior);
				Priority_Parameters.setOutput(overallbestZ, overallbestC, overallbest_phi, comboseq, comboseq_names, tf_names[tf]);
                                // the previous line sets "output" for one TF only
                                String motifName=Priority_Parameters.tf_names[tf];
                                Motif motif=new Motif(motifName);
                                double[][] matrix = new double[wsize][4];
                                for (int row=0;row<wsize;row++) {
                                    for (int col=0;col<4;col++) {
                                        matrix[row][col]=(double)overallbest_phi[col][row]; // note that overallbest_phi is oriented the other way, so we swap columns and rows!
                                    }
                                }                                
                                motif.setMatrix(matrix);
                                motifCollection.addMotifToPayload(motif);
                                String consensusMotif=motif.getConsensusMotif();
                                // add regions to track
                                for (int s=0;s<overallbestZ.length;s++) {
                                    String sequenceName=sequenceNumberToName.get(s);
                                    RegionSequenceData parent=(RegionSequenceData)regionDataset.getSequenceByName(sequenceName);
                                    int start=overallbestZ[s];
                                    int orientation=Region.DIRECT;
                                    if (start>=parent.getSize()) {
                                        start=(parent.getSize()*2-wsize)-start; // convert from "reverse" position
                                        orientation=Region.REVERSE;
                                    }
                                    Region region=new Region(parent, start,start+wsize-1,motifName,1,orientation);
                                    int genomicStart=dnaSequences[s].getGenomicPositionFromRelative(start);
                                    int genomicEnd=genomicStart+wsize-1;
                                    region.setProperty("consensus motif", consensusMotif);
                                    // char[] tfbsSequence=(char[])dnaSequences[s].getValueInGenomicInterval(genomicStart, genomicEnd);
                                    // if (orientation==Region.DIRECT) region.setProperty("sequence", new String(tfbsSequence));
                                    // else region.setProperty("sequence", new String(reverseSeq(tfbsSequence)));
                                    parent.addRegion(region);
                                }
                                // System.err.println(motifname+": overallbest_phi="+overallbest_phi.length+"x"+overallbest_phi[0].length);
                        }
			
		}/* end for (int tf=0; tf<fname.length; tf++) */
         task.setProgress(100);
         // set result parameters in task (they are processed further in Operation_motifDiscovery

         task.setParameter("Result", regionDataset);
         task.setParameter("Motifs", motifCollection);
         //
         resetLocalFieldVariable();
}/* end execute() */

 private void resetLocalFieldVariable() {
        sequenceNumberToName=null; //
        dnaSequences=null;
	phi_prior=null;
	back=null;
	tf_names=null;
        comboprior=null;
        comboseq=null;
	comboseq_names=null;
	cprior=null;
	denom=null;
	phi=null;
	phi_temp=null;
	Z=null;bestZ=null;overallbestZ=null;
	C=null;bestC=null;overallbestC=null;
  }
	

        
        public char[] reverseSeq(char[] seq) {
            char[] buffer=new char[seq.length];
            for (int i=0;i<buffer.length;i++) {
                switch (buffer[i]) {
                    case 'A':buffer[buffer.length-(i+1)]='T';break;
                    case 'a':buffer[buffer.length-(i+1)]='t';break;
                    case 'C':buffer[buffer.length-(i+1)]='G';break;
                    case 'c':buffer[buffer.length-(i+1)]='g';break;
                    case 'G':buffer[buffer.length-(i+1)]='C';break;
                    case 'g':buffer[buffer.length-(i+1)]='c';break;
                    case 'T':buffer[buffer.length-(i+1)]='A';break;
                    case 't':buffer[buffer.length-(i+1)]='a';break;
                    default:buffer[buffer.length-(i+1)]=buffer[i];break;
                }
            }
            return buffer;
        }
        
} /* end Priority_GibbsRun */
