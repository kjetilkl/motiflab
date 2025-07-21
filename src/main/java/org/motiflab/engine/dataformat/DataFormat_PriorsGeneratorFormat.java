/*
 
 
 */

package org.motiflab.engine.dataformat;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.data.DataSegment;
import org.motiflab.engine.data.PriorsGenerator;
import org.motiflab.engine.protocol.ParseError;


/**
 * A format to read serialized PriorsGenerators
 * @author kjetikl
 */
public class DataFormat_PriorsGeneratorFormat extends DataFormat {
    private String name="PriorsGeneratorFormat";
    private Class[] supportedTypes=new Class[]{PriorsGenerator.class};

    public DataFormat_PriorsGeneratorFormat() {}

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return false;
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return false;
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof PriorsGenerator);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(PriorsGenerator.class));

    }

    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "pge";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("Inappropriate use of format() method in PriorsGeneratorFormat");
    }

    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Inappropriate use of parseInput(ArrayList<String>...) method in PriorsGeneratorFormat");
    }

    @Override
    public Data parseInput(InputStream input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        ObjectInputStream stream=null;
        try {
             stream=new ObjectInputStream(input);
             Object value=stream.readObject();
             if (value==null) throw new ParseError("Unable to parse PriorsGenerator from source. Source empty!");
             if (value instanceof PriorsGenerator) return (PriorsGenerator)value;
             else throw new ParseError("Unable to parse PriorsGenerator from source. Returned data was not a PriorsGenerator but a '"+value.getClass().toString()+"'");
        } catch (Exception e) {
             throw new ParseError(e.getClass().getSimpleName()+":"+e.getMessage());
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}
    }

    @Override
    public DataSegment parseInput(ArrayList<String> input, DataSegment target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
         throw new ParseError("Unable to parse input to DataSegment in PriorsGeneratorFormat)");
    }

    /** Saves the given PriorsGenerator to file (in java serialized format) */
    public void savePriorsGeneratorToFile(PriorsGenerator priorsgenerator, String filename) throws Exception {
        ObjectOutputStream stream=null;
        try {
             Object target=engine.getDataSourceForString(filename);
             if (!(target instanceof File)) throw new ExecutionError("'"+filename+"' is not a recognized file");
             OutputStream outputStream=MotifLabEngine.getOutputStreamForFile((File)target);
             stream = new ObjectOutputStream(new BufferedOutputStream(outputStream));
             stream.writeObject(priorsgenerator);
             stream.close();
        } catch (Exception e) {
            throw e;
        } finally {try {if (stream!=null) stream.close();} catch (Exception x){}}
    }

}





